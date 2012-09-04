  var SmartCardHandler = {
		  
		  init: function(applet) {
			  this.waitTime = 100;
			  this.applet = applet;
			  this.callbacks = {};
			  this.waitForApplet();	
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
		  transmit: function(command) {
			  console.log("Transmit: " + command);
			  return this.applet.transmitString(command);
		  },
		  transmitCommandSet: function(commands) {
			  var responses = {};
			  for(var i=0, len=commands.length; i < len; i++) {
				  response = this.transmit(commands[i].command);
				  responses[commands[i].key] = response;
				  if (response.slice(-4) !== "9000") {
					  // Don't bother continuing when the response is not ok
					  break;
				  }
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