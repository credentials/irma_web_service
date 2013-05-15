var ProxyReader = {
	// Some local variables
	channelBaseURL: "/cardproxy_web_relay/w",
	
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
		ProxyReader.setup_from_proxy();
	},
	
	setup_from_proxy: function() {
		// Dirty hack: add parameter to channelBaseURL so that this POST request and
		// the next for the to proxy channel will not be merged
		Channel.setup(ProxyReader.channelBaseURL + "?q", function(channel) {
			console.log("Created from proxy channel", channel);
			
			// Store channel
			ProxyReader.fromProxy = channel;
			
			// Bind listen to this
			ProxyReader.fromProxy.listen( function(msg) {
				ProxyReader.handle_message(msg);
			});
			
			ProxyReader.setup_to_proxy();
		});
	},
	
	setup_to_proxy: function() {
		// Run from within setup_from_proxy()
		Channel.setup(ProxyReader.channelBaseURL, function(channel) {
			console.log("Created to proxy channel ", channel);
			ProxyReader.toProxy = channel;
			
			// The Proxy app needs to listen to
			console.log("HELLO HELLO: ", ProxyReader.toProxy.qr_url);
			$("#qr_image").attr("src", ProxyReader.toProxy.qr_url);
			
			// Tell the proxy where to send its responses
			ProxyReader.toProxy.send({write_url: ProxyReader.fromProxy.write_url});
		});
	},
	
	handle_message: function(msg) {
		data = JSON.parse(msg);
		console.log("Got data", data);
		if(data.type === "event") {
			console.log("Processing event: ", data.name);
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
			if( response.arguments.responses["select_aid"].apdu.slice(-4) === '9000' ) {
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
		
		this.register_callback(cmd.id, callback);
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