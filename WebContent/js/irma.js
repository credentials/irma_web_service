var IRMA = {
	base_url: "/irma_web_service/protocols/verification/SpuitenEnSlikken",
	irma_html: "../../irma/",
	irma_aid: '49524D4163617264',
	
	irma_issue_state: 'idle',
	issue_url: '',
	responseurl: '',

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
	
	helper: function(url) {
		$.ajax({
			url: url,
			type: 'POST',
			async: false,
			success: function(res) {
				console.log(res);
			}
		});
	},

	start_verify: function() {
		IRMA.disableVerify(); // Reset state
		SmartCardHandler.init();
		console.log("Starting IRMA verification");
		
		IRMA.show_verify();
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
					if (data.status === "issueready") {
						$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ok_520px.png");
						$("#IRMA_status_text").html("Hit 'CONTINUE' to proceed to the issuing step");
						$("#IRMA_button_verify").html("CONTINUE");
						$("#IRMA_button_verify").addClass("enabled");
						$("#IRMA_button_verify").button().on("click", function(event) {
							startIssue(attributesurl);
						});
					}
					if (data.status === "issuing") {
						$("#IRMA_button_issue").html("ISSUING...");
					}
					if (data.status === "error") {
						window.clearInterval(checkInterval);
						IRMA.show_failure_credential_not_found();
					}
					if (data.status === "success") {
						window.clearInterval(checkInterval);
						IRMA.onVerifySuccess();
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
					IRMA.onVerifySuccess(data);
				} else if (data.status === 'issue') {
					IRMA.onVerifySuccessIssue(data);
				} else if (data.status === 'error') {
					IRMA.show_failure(data.feedbackMessage, "FAILED");
				} else {
					IRMA.show_failure_credential_not_found();
				}
			}
		});
	},

	start_batch_issue: function(selection, issue_url) {
		$("#IRMA_issue").fadeIn();
		IRMA.selection = selection;
		IRMA.issue_url = issue_url;

		console.log("Contacting: " + issue_url);
		$.ajax({
			url: issue_url,
			type: "POST",
			success: IRMA.display_issue_credentials,
		});

		// Handlers
		SmartCardHandler.bind("cardInserted", function() {
			SmartCardHandler.connectFirstCard();
			if (SmartCardHandler.selectApplet(IRMA.irma_aid)) {
				IRMA.enable_issue();
			} else {
				$("#IRMA_status_icon").prop("src", "../../img/irma_icon_warning_520px.png");
				$("#IRMA_status_text").html("Inserted card is not an IRMA card");
			}
		});
		SmartCardHandler.bind("cardRemoved", function() {
			IRMA.disable_issue();
		});
		if (SmartCardHandler.connectFirstCard() && SmartCardHandler.selectApplet(IRMA.irma_aid)) {
			IRMA.enable_issue();
		}
	},

	issue_button_clicked: function(event) {
		console.log("Issue button clicked");
		$("#IRMA_button_issue").off("click");
		$("#IRMA_button_issue").removeClass("enabled");
		$("#IRMA_button_issue").html("ISSUING...");

		IRMA.irma_issue_state = "issue";
		IRMA.current_credential_idx = 0;
		IRMA.current_credential = IRMA.selection[IRMA.current_credential_idx];

		console.log(SmartCardHandler.applet.verifyPin());
		IRMA.issue_step_one();
	},

	issue_step_one: function() {
		IRMA.issue_set_active(IRMA.current_credential);
		IRMA.issue_set_status(IRMA.current_credential, "Issuing..");
		$.ajax({
			url: IRMA.issue_url + '/' + IRMA.current_credential + '/1',
			contentType: 'application/json',
			type: 'POST',
			success: function(data) {
				console.log('Got first batch of data for issuing ' + IRMA.current_credential);
				console.log(data);
				IRMA.responseurl = data.responseurl;

				var response = SmartCardHandler.transmitCommandSet(data.commands);
				console.log(response);
				IRMA.issue_step_two(response);
			}
		});
	},

	issue_step_two: function(response) {
		IRMA.issue_set_status(IRMA.current_credential, "Issuing....");
		console.log("Querying response url: " + IRMA.responseurl);
		$.ajax({
			url: IRMA.responseurl,
			contentType: 'application/json',
			data: JSON.stringify(response),
			type: 'POST',
			success: function(data) {
				console.log('Got second batch of data for issuing ' + IRMA.current_credential);
				console.log(data);
				IRMA.responseurl = data.responseurl;

				var response = SmartCardHandler.transmitCommandSet(data.commands);
				console.log(response);
				IRMA.issue_step_three(response);
			}
		});
	},

	issue_step_three: function(response) {
		IRMA.issue_set_status(IRMA.current_credential, "Issuing.......");
		$.ajax({
			url: IRMA.responseurl,
			contentType: 'application/json',
			data: JSON.stringify(response),
			type: 'POST',
			success: function(data) {
				console.log('Completed issuance for ' + IRMA.current_credential);
				console.log(data);

				IRMA.issue_next_credential();
			}
		});
	},

	issue_next_credential: function() {
		IRMA.issue_set_done(IRMA.current_credential);
		IRMA.current_credential_idx++;
		if(IRMA.current_credential_idx < IRMA.selection.length) {
			IRMA.current_credential = IRMA.selection[IRMA.current_credential_idx];
			console.log("Now proceeding with credential " + IRMA.current_credential);
			IRMA.issue_step_one();
		} else {
			console.log("Done issuing");
			IRMA.finish_issuing();
		}
	},

	finish_issuing: function() {
		$("#IRMA_button_issue").html("DONE");
		$("#IRMA_button_issue").addClass("enabled");
		$("#IRMA_button_issue").button().on("click", function(event) {
			window.location = "http://www.ru.nl/cybersecurity";
		});
	},

	display_issue_credentials: function(data) {
		console.log(data);
		var credentials = data.info.issue_information;
		for(var i = 0; i < IRMA.selection.length; i++) {
			IRMA.display_issue_credential(credentials[IRMA.selection[i]], IRMA.selection[i]);
		}
	},

	display_issue_credential: function(cred, cred_key) {
		cred.attribute_array = IRMA.make_array_from_map(cred.attributes);
		cred.key = cred_key;
		console.log(cred);
		console.log(Mustache.to_html($("#credAccordionTpl").html(), cred));
		$("#IRMA_issue_credential_list_content").append(Mustache.to_html($("#credAccordionTpl").html(), cred));
	},

	//
	// UI code goes here
	//
	show_verify: function() {
		$("#IRMA_verify").fadeIn();
	},

	hide_verify: function() {
		$("#IRMA_verify").fadeOut();
	},

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

	enable_issue: function() {
		$("#IRMA_button_issue").addClass("enabled");
		$("#IRMA_button_issue").html("ISSUE");
		$("#IRMA_button_issue").on('click', IRMA.issue_button_clicked);
	},

	disable_issue: function() {
		$("#IRMA_button_issue").off("click");
		$("#IRMA_button_issue").removeClass("enabled");
		$("#IRMA_button_issue").html("WAITING FOR CARD...");
	},

	onVerifySuccess: function(data) {
		console.log("Internal on verify succes function called");
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ok_520px.png");
		$("#IRMA_status_text").html("Hit 'CONTINUE' to proceed to the website");
		$("#IRMA_button_verify").html("CONTINUE");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", function(event) {
			window.location = data.result;
		});
	},

	onVerifySuccessIssue: function(data) {
		$("#IRMA_status_icon").prop("src", "../../img/irma_icon_ok_520px.png");
		$("#IRMA_status_text").html("Hit 'CONTINUE' to proceed to the issuing step");
		$("#IRMA_button_verify").html("CONTINUE");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", function(event) {
			IRMA.start_issue();
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

	issue_set_status: function(credential, text) {
		$("#IRMA-issue-status-" + credential).html("(" + text + ")");
	},

	issue_set_error: function(credential, text) {
		IRMA.issue_set_status(credential, text);
		IRMA.issue_set_failed(credential);
	},

	issue_set_active: function(credential) {
		var heading = $("#IRMA-issue-heading-" + credential);
		heading.removeClass("btn-danger");
		heading.removeClass("btn-success");
		heading.addClass("btn-info");
	},

	issue_set_done: function(credential) {
		IRMA.issue_set_status(credential, "Done");
		var heading = $("#IRMA-issue-heading-" + credential);
		heading.removeClass("btn-info");
		heading.removeClass("btn-danger");
		heading.addClass("btn-success");
	},

	issue_set_failed: function(credential) {
		var heading = $("#IRMA-issue-heading-" + credential);
		heading.removeClass("btn-info");
		heading.removeClass("btn-success");
		heading.addClass("btn-danger");
	},

	// Helpers
	make_array_from_map: function(map) {
		var array = [];
		var i = 0;
		for(var key in map) {
			if(map.hasOwnProperty(key)) {
				array[i] = {name: key, value: map[key]};
				i += 1;
			}
		}
		return array;
	}
};

//$(function() {
//	IRMA.init();
//    IRMA.show();
//});