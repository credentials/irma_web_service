  var SmartCardHandler = {

			cardVersion: "",

		  init: function(applet) {
			  this.waitTime = 100;
			  this.applet = applet;
			  this.callbacks = {};
			  this.waitForApplet();
		  },

		  poll: function() {
			  console.log("Testing if card was already present");
			  try {
				  this.connectFirstCard();
				  test = this.transmit("802B0100");
				  if(!(typeof test === "undefined")) {
					  console.log("Simulating card inserted event later");
					  setTimeout(function () {SmartCardHandler.handleCallback("cardInserted");}, 500);
				  }
			  } catch (e) {};
		  },

		  waitForApplet: function() {
			  this.waitTime = Math.min(this.waitTime * 2,2000);
			  if (typeof document.embeds[0].run != 'undefined') {
				  console.log('It is loaded!');
				  this.applet = document.embeds[0];
				  this.setup();
				  this.showReaderList();
			  } else {
				  console.log('Waiting for',this.waitTime,'miliseconds');
				  var _this = this;
				  setTimeout(function() {_this.waitForApplet(); },this.waitTime);
			  }
		  },
		  setup: function() {
			  this.applet.run();
			  this.applet.enableSignals('SmartCardHandler');
			  this.handleCallback('appletReady');
		  },
		  dispatch: function(signal) {
			  console.log(signal);
			  this.handleCallback(signal.getEvent());
		  },
		  showReaderList: function() {
			  console.log(this.applet.getReaderList());
		  },
		  connectFirstCard: function() {
			  console.log("Connecting");
			  return this.applet.connectFirstCard();
		  },
		  selectApplet: function(aid, success, failure) {
			  var hexlength = (aid.length/2).toString(16),
			  	  selectAPDU = '00A40400' + (hexlength.length == 1 ? '0' : '') + hexlength + aid + '00';
			  rapdu = this.transmit(selectAPDU);
              console.log("response: " + rapdu);
			  if( rapdu.slice(-4) === '9000' ) {
				SmartCardHandler.cardVersion = rapdu.slice(0, rapdu.length - 4);
				success();
			  } else {
			  	failure();
			  }
		  },
		  transmit: function(command) {
			  console.log("Transmit: " + command);
			  response = this.applet.transmitString(command);
			  console.log("Response: " + response)
			  return response;
		  },
		  sendFeedback: function(message, state) {
			// Not possible on normal cardreaders
			  console.log("CardReader feedback: " + message);
		  },
		  close: function() {
			  // TODO: still need to implement this somehow
			  console.log("Closing connection not implemented for smartcard");
		  },
		  transmitCommandSet: function(commands, callback) {
			  var responses = {};
			  for(var i=0, len=commands.length; i < len; i++) {
				  response = this.transmit(commands[i].command);
				  responses[commands[i].key] = { key: commands[i].key, apdu: response };
				  if (!(response.slice(-4) === "9000" ||  
						  response.slice(-4) === "6985" /* FIXME: Workaround for broken signature verification on the card */)) {
					  // Don't bother continuing when the response is not ok
					  responses['smartcardstatus'] = 'failed';
					  responses['failed-key'] = commands[i].key;
					  break;
				  }
			  }

			  // Construct well-formed result
			  var result = {};
			  result.type = "response";
			  result.name = "transmitCommandSet";
			  result.arguments = {"responses": responses};
			  callback(result);
		  },
		  verifyPin: function(callback) {
			  tries = 3;
			  while(tries > 0 && tries < 10) {
				  // Repeat since pin incorrect
				  // FIXME: need better feedback for this but that needs to go into applet.
				  tries = this.applet.verifyPin();
				  console.log("Pin Incorrect, tries left: " + tries);
			  }

			  var response = {type: "response", name: "authorizeWithPin", arguments: {}};

			  if(tries == 0) {
				  response.arguments.result = "failure";
			  } else {
				  response.arguments.result = "success";
			  }
			  callback(response);
		  },
		  // Merge above two at one time
		  transmitCommandSetWithCB: function(commands, callback) {
			  var responses = {};
			  responses['result'] = 'succes';
			  for(var i=0, len=commands.length; i < len; i++) {
				  response = this.transmit(commands[i].command);
				  responses[commands[i].key] = { key: commands[i].key, apdu: response };
				  if (response.slice(-4) !== "9000") {
					  // Don't bother continuing when the response is not ok
					  responses['result'] = 'failed';
					  break;
				  }
				  callback((i+1)/commands.length);
			  }
			  return responses;
		  },
		  bind: function(eventName,callback) {
			  this.callbacks[eventName] = callback;
		  },
		  handleCallback: function(eventName) {
			  if (typeof this.callbacks[eventName] !== 'undefined') {
				  this.callbacks[eventName]();
			  }
		  }
  };
