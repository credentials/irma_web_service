var IRMA = {
	base_url: "/irma_web_service/protocols/verification/SpuitenEnSlikken",
	irma_html: "../../irma/",
	irma_aid: '49524D4163617264',
	
	init: function() {
		IRMA.load_extra_html(IRMA.irma_html + "issue.html");
		IRMA.load_extra_html(IRMA.irma_html + "verify.html");
		IRMA.load_extra_html(IRMA.irma_html + "qr.html");
	},

	load_extra_html: function(url) {
		$.ajax({
			url: url,
			type: 'GET',
			async: false,
			success: function(res) {
				$('body').prepend(res);
			}
		});
	},

	
	show: function() {
		IRMA.disableVerify(); // Reset state
		SmartCardHandler.init();
		console.log("Starting IRMA verification");
	    
		// May need to refactor this a bit further
		$("#IRMA_verify").fadeIn();
		
		IRMA.retrieve_verifications();
		
		// Setup handlers
		SmartCardHandler.bind("cardInserted", function() {
			SmartCardHandler.connectFirstCard();
			if (SmartCardHandler.selectApplet(IRMA.irma_aid)) {
				IRMA.enableVerify();
			} else {
				IRMA.show_warning("Inserted card is not an IRMA card");
			}
		});

		SmartCardHandler.bind("cardRemoved", function() {
			IRMA.disableVerify();
		});
		
		//IRMA.setup_qr_code();

		SmartCardHandler.connectFirstCard();
		if (SmartCardHandler.connectFirstCard() && SmartCardHandler.selectApplet(IRMA.irma_aid)) {
			IRMA.enableVerify();
		};
	},
	
	retrieve_verifications: function() {
		console.log("Retrieving verification information");
		$.ajax({
			url: IRMA.base_url,
			contentType: 'application/json',
			type: 'POST',
			success: function(data) {
				console.log("Got data for step 0");
				console.log(data);
				lastData = data;
				IRMA.show_verifications(data);
			}
		});
	},
	
	show_verifications: function(data) {
	    // TODO: do something with Protocol info, now in data.
	    // TODO 800 hardcoded
		for(var key in data.info.verification_names) {
			if(data.info.verification_names.hasOwnProperty(key)) {
				var verification = data.info.verification_names[key];
				console.log("Hello here: " + verification);
				$(".IRMA_content_verify").prepend("<span class=\"IRMA_content_credential\">" + verification + "</span>");
			}
		}
	    $(".IRMA_content_credential").html(data.info.verification_names['800']);
	},
	
	// This still needs some cleaning up!
	setup_qr_code: function() {
		$("#IRMA_button_usephone").on("click",function(event) {
			$("#qr_image").attr("src", data.info.qr_url);
			$("#qr_overlay").show();
			checkInterval = window.setInterval(function(){
				var url = data.info.qr_url.substring(0, data.info.qr_url.lastIndexOf("/")) + '/status';
				$.get(url,function(data) {
					if (data.status !== "start") {
						$("#qr_overlay").hide();
						$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ready_520px.png");
						$("#IRMA_status_text").html("Apply your IRMA card to your phone");
						$("#IRMA_button_verify").html("VERIFYING...");
					} 
					if (data.status === "success") {
						window.clearInterval(checkInterval);
						IRMA.onVerifySucces();
					}
					if (data.status === "failure") {
						window.clearInterval(checkInterval);
						IRMA.show_failure_credential_not_found();
					}
				}, "json");
			},500);
		});
	},

	
	verifyButtonClicked: function(event) {
		$("#IRMA_button_verify").off("click");
		$("#IRMA_button_verify").removeClass("enabled");
		$("#IRMA_button_verify").html("VERIFYING...");
		$.ajax({
			url : lastData.responseurl,
			contentType : 'application/json',
			type : 'POST',
			success: function(data) {
				console.log(data);
				nextAction = data;
				SmartCardHandler.connectFirstCard();
				var responses = {};
				for (var key in data.commandsSets) {
					if(data.commandsSets.hasOwnProperty(key)) {
						var commands = data.commandsSets[key];
						responses[key] = SmartCardHandler.transmitCommandSet(commands);
					}
				}
				console.log(responses);
				IRMA.finishVerify(responses, data);
			}
		});
	},
	
	finishVerify: function(responses, data) {
		console.log("Finished IRMA verification");

		// Test whether all communication succeeded
		for(var key in responses) {
			if(responses.hasOwnProperty(key)) {
				var response = responses[key];
				if(response.smartcardstatus === "failed") {
					//TODO: This is not all that can go wrong!!
					IRMA.show_error_connection_list();
					return;
				}
			}
		}
		
		// Send results to webserver
		$.ajax({
			url : data.responseurl,
			contentType : 'application/json',
			data : JSON.stringify(responses),
			type : 'POST',
			success : function(data) {
				console.log(data);
				if (data.status === 'success') {
					IRMA.onVerifySucces(data);
				} else {
					IRMA.show_failure_credential_not_found();
				}
			}
		});
	},

	//
	// UI code goes here
	//
	disableVerify: function () {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_waiting_520px.png");
		$("#IRMA_status_text").html("Insert your IRMA card or use your phone");
		$("#IRMA_button_verify").off("click");
		$("#IRMA_button_verify").removeClass("enabled");
		$("#IRMA_button_verify").html("WAITING FOR CARD...");
	},
	
	enableVerify: function() {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ready_520px.png");
		$("#IRMA_status_text").html("Hit 'VERIFY' to check your credential");
		$("#IRMA_button_verify").html("VERIFY");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", IRMA.verifyButtonClicked);
	},
	
	onVerifySucces: function(data) {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ok_520px.png");
		$("#IRMA_status_text").html("Hit 'CONTINUE' to proceed to the website");
		$("#IRMA_button_verify").html("CONTINUE");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", function(event) {
			window.location = data.result;
		});
	},

	show_warning: function(text) {
        $("#IRMA_status_icon").prop("src", "../../img/irma_icon_warning_520px.png");
        $("#IRMA_status_text").html(text);
	},
	
	show_failure_credential_not_found: function() {
		IRMA.show_failure("Credential not found", "NOT FOUND");
	},
	
	show_error_connection_lost: function() {
		IRMA.show_error("Connection lost", "COMMUNICATION ERROR");
	},
	
	show_error: function(text, status) {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_warning_520px.png");
		$("#IRMA_status_text").html(text);
		$("#IRMA_button_verify").html(status);
	},
	
	show_failure: function(text, status) {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_missing_520px.png");
		$("#IRMA_status_text").html(text);
		$("#IRMA_button_verify").html(status);
	},
};

//$(function() {
//	IRMA.init();
//    IRMA.show();
//});