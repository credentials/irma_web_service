var ProxyReader = {
	// Some local variables
	channelBaseURL: "/irma_web_relay/create",
	cardVersion: "",

	// Channels
	toProxy: null,
	fromProxy: null,

	// Callbacks
	cardFoundCallback: function() {},
	cardLostCallback: function() {},
	readerFoundCallback: function() {},

	response_callbacks: {},
	callbacks: {},

	init: function(url) {
		ProxyReader.setup_channels();
	},

	setup_channels: function() {
		Channel.setup(ProxyReader.channelBaseURL, function(toProxy, fromProxy) {
			console.log("Created to proxy channel", toProxy);
			console.log("Created from proxy channel", fromProxy);

			// Store channels
			ProxyReader.toProxy = toProxy;
			ProxyReader.fromProxy = fromProxy;

			// Bind listen to this
			ProxyReader.fromProxy.listen( function(msg) {
				ProxyReader.handle_message(msg);
			});

			// The Proxy app needs to listen to
			console.log("HELLO HELLO: ", ProxyReader.toProxy.qr_url);
			$("#qr_image").attr("src", ProxyReader.toProxy.qr_url);

			// Tell the proxy where to send its responses
			ProxyReader.toProxy.send({write_url: ProxyReader.fromProxy.write_url});
		}, function() {
			//FIXME: handle this in a proper way
			console.log("Failed to setup from proxy channel");
			ProxyReader.fromProxy = null;
		});
	},

	handle_message: function(msg) {
		data = JSON.parse(msg);
		console.log("Got data", data);
		if(data.type === "event") {
			console.log("Processing event: ", data.name);
			if(data.name === "timeout") {
				// Got a timeout, close the channels
				this.fromProxy.close();
			}

			if(this.callbacks[data.name] != undefined) {
				this.callbacks[data.name]();
			} else {
				console.log("ERROR: unknown event: " + data.name);
			}
		} else if(data.type === "response") {
			console.log("Processing response");
			this.response_callbacks[data.id](data);
			delete this.response_callbacks[data.id];
		}
	},

	selectApplet: function(aid, success, failure) {
		var hexlength = (aid.length/2).toString(16);
		var selectAPDU = '00A40400' + (hexlength.length == 1 ? '0' : '') + hexlength + aid + '00';
		var commands = [{key: "select_aid", command: selectAPDU}];

		ProxyReader.transmitCommandSet(commands, function(response) {
			console.log(response);
			rapdu = response.arguments.responses["select_aid"].apdu;
			if( rapdu.slice(-4) === '9000' ) {
				ProxyReader.cardVersion = rapdu.slice(0, rapdu.length - 4);
				success();
			} else {
				failure();
			}
		});
	},

	transmitCommandSet: function(commands, callback) {
		var cmd = {};
		cmd.type = "command";
		cmd.name = "transmitCommandSet";
		cmd.id = this.randomId();
		cmd.arguments = {};
		cmd.arguments.commands = commands;
		console.log(cmd);

		// Check whether one of the commands failed
		var wrapper_fct = function(data) {
			var responses = data.arguments.responses;
			for(var key in responses) {
				if(responses.hasOwnProperty(key)) {
					var response = responses[key];
					if(!(response.apdu.slice(-4) === "9000" ||
						response.apdu.slice(-4) === "6985" /* FIXME: Workaround for broken signature verification on the card */)) {
						responses["smartcardstatus"] = "failed";
						responses["failed-key"] = key;
					}
				}
			}
			data.arguments.responses = responses;
			callback(data);
		};

		this.register_callback(cmd.id, wrapper_fct);
		this.toProxy.send(cmd);
	},

	verifyPin: function(callback) {
		var cmd = {};
		cmd.type = "command";
		cmd.name = "authorizeWithPin";
		cmd.id = this.randomId();
		cmd.arguments = {};
		console.log(cmd);

		this.register_callback(cmd.id, callback);
		this.toProxy.send(cmd);
	},

	sendFeedback: function(feedback, state) {
		var cmd = {};
		cmd.type = "event";
		cmd.name = "statusUpdate";
		cmd.id = this.randomId();
		cmd.arguments = {
			data : {
				state : state,
				feedback : feedback,
			}
		};
		console.log(cmd);

		this.toProxy.send(cmd);
	},

	close: function() {
		console.log("Closing connection");
		var cmd = {};
		cmd.type = "event";
		cmd.name = "done";
		cmd.id = this.randomId();

		this.toProxy.send(cmd, false);
		this.fromProxy.close();
	},

	bind: function(eventName, callback) {
		this.callbacks[eventName] = callback;
		console.log("Bound ", eventName, this.callbacks);
	},

	randomId: function() {
		return Math.floor((1 + Math.random()) * 0x100000000)
			.toString(16).substring(1);
	},

	register_callback: function(id, callback) {
		this.response_callbacks[id] = callback;
	},
};
