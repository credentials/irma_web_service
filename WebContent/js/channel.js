var Channel = function() {
	var ChannelProto = function() {
		this.active = true;
		this.listen = function(dataReceivedCallback) {
			var thisReadURL = this.read_url;
			var that = this;
			that.waitForMessage = function() {};
			that.waitForMessage = function() {
				$.ajax({
					type : "GET",
					url : thisReadURL,
					dataType: "text",
					async : true, /* If set to non-async, browser shows page as "Loading.."*/
					cache : false,
					timeout : 50000, /* Timeout in ms */

					success : function(data) {
						console.log("Received data from", thisReadURL);
						console.log("data: ", data);
						if (data !== "") {
							dataReceivedCallback(data);
						}
						if(that.active) {
							setTimeout(that.waitForMessage, 200); // Wait for 200ms and request next message
						}
					},
					error : function(XMLHttpRequest, textStatus, errorThrown) {
						console.log("onReceive error: ",textStatus, errorThrown);
						setTimeout(that.waitForMessage, /* Try again after.. */
						15000); /* milliseconds (15seconds) */
					}
				});
			};
			that.waitForMessage();
		};
		this.send = function(sendData, async) {
			that = this;
			async = typeof sync !== "undefined" ? async : true;
			$.ajax({
				url : that.write_url,
				contentType : 'application/json',
				type : 'POST',
				async: async,
				data : JSON.stringify(sendData),
				success : function(data) {
					// nothing for now, maybe add callback in future?
					console.log("Sent something to ", that.write_url);
				},
			});
		};
		this.close = function() {
			that = this;
			that.active = false;
		};
	};
	return {
		setup: function(baseURL, onSucces, onError) {
			console.log("Setup called: " + baseURL);
			$.ajax({
				url : baseURL,
				contentType : 'application/json',
				type : 'POST',
				cache : false,
				success : function(data) {
					var toProxy = new ChannelProto();
					toProxy.read_url = data.side_b_read_url;
					toProxy.write_url = data.side_a_write_url;
					toProxy.qr_url = data.qr_url;

					var fromProxy = new ChannelProto();
					fromProxy.read_url = data.side_a_read_url;
					fromProxy.write_url = data.side_b_write_url;

					onSucces(toProxy, fromProxy);
				},
				error : function(data) {
					console.log("Error creating channel");
					onError();
				}
			});				
		},
		fromReadURL: function(read_url) {
			var c = new ChannelProto();
			c.read_url = read_url;
			return c;
		}, 
		fromWriteURL: function(write_url) {
			var c = new ChannelProto();
			c.write_url = write_url;
			return c;
		}
	}; 
}();
