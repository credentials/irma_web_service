var IRMAURL = new Object();
IRMAURL.base = "/irma_web_service";
IRMAURL.action = IRMAURL.base + "/protocols/verification/SpuitenEnSlikken";
IRMAURL.html = IRMAURL.base + "/irma";
IRMAURL.img = IRMAURL.base + "/img";
IRMAURL.issuerLogo = IRMAURL.base + "/img/RU_logo_issuer.png";
IRMAURL.verifierLogo = IRMAURL.base + "/img/RU_logo_verifier.png";

var IRMA = {
	irma_aid: 'F849524D4163617264',
	irma_aid_0_7: '49524D4163617264',

	// Target to go to after issuing is done
	after_issue_target: "http://www.ru.nl/cybersecurity",

	irma_issue_state: 'idle',
	issue_url: '',
	issue_data: '',
	responseurl: '',

	card_connected: false,

	// Some state to keep track of what we are verifying now
	current_verification_idx: 0,
	verification_commands: {},
	verification_responses: {},

	verification_names: {},

	Handler: ProxyReader,

	// Can be overridden by implementing pages
	onBackButtonPressed: function() {},

	onIssuanceFinished: function(event) {
		window.location = IRMA.after_issue_target;
	},

	init: function() {
		IRMA.load_extra_html(IRMAURL.html + "/issue.html");
		IRMA.load_extra_html(IRMAURL.html + "/verify.html");
		IRMA.load_extra_html(IRMAURL.html + "/qr.html");

		// Initialize readers
		ProxyReader.init();
		SmartCardHandler.init();
	},

	bindCallback: function(event, fct) {
		ProxyReader.bind(event, function(data) {fct(data, ProxyReader);});
		SmartCardHandler.bind(event, function(data) {fct(data, SmartCardHandler);});
	},

	createCardInsertedCallback: function(callback) {
		return function(data, handler) {
			IRMA.Handler = handler;
			if(handler === SmartCardHandler) {
				// Card inserted into SmartCardHandler, connect to it
				// FIXME: this would be the place to final fix this
				console.log("Connecting to card for applet");
				SmartCardHandler.connectFirstCard();
			}
			IRMA.card_connected = true;
			callback(data);
		};
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

	// Return system to workable initial state
	// TODO: work on state engine to make this robust.
	verification_reset: function() {
		IRMA.retrieve_verifications();
	},

	start_verify: function() {
		IRMA.disableVerify(); // Reset state
		console.log("Starting IRMA verification");

		IRMA.setup_qr();
		$("#IRMA_verifier_logo_img").prop("src", IRMAURL.verifierLogo);
		IRMA.show_verify();
		IRMA.retrieve_verifications();

		console.log("Overriding readerfoundCallback");
		IRMA.bindCallback("cardReaderFound", function() {
			$("#qr_overlay").hide();
		});

		// Setup this.Handlers
		IRMA.bindCallback("cardInserted", IRMA.createCardInsertedCallback(function() {
			IRMA.Handler.selectApplet(IRMA.irma_aid, IRMA.enableVerify,
					function() { IRMA.Handler.selectApplet(IRMA.irma_aid_0_7, IRMA.enableVerify,
							function() { IRMA.show_warning("Inserted card is not an IRMA card"); }); });
			IRMA.bindCallback("cardRemoved", function() {
				IRMA.disableVerify();
			});
		}));

		IRMA.bindCallback("timeout", function() {
			// TODO not clear how to handle this in the UI
			console.log("Connection timed out");
		});

		SmartCardHandler.poll();
	},

	retrieve_verifications: function() {
		console.log("Retrieving verification information");
		$.ajax({
			url: IRMAURL.action,
			contentType: 'application/json',
			type: 'POST',
			success: function(data) {
				console.log("Got data for step 0");
				console.log(data);
				IRMA.responseurl = data.responseurl;
				IRMA.show_verifications(data);
			}
		});
	},

	show_verifications: function(data) {
		IRMA.verification_names = data.info.verification_names;
		$(".IRMA_content_verify_credentials").html("");
		for(var key in IRMA.verification_names) {
			if(IRMA.verification_names.hasOwnProperty(key)) {
				var verification = IRMA.verification_names[key];
				console.log("Hello here: " + verification);
				$(".IRMA_content_verify_credentials").append("<span class=\"IRMA_content_credential\">" + verification + "</span>");
			}
		}
	},

	setup_qr: function() {
		$("#IRMA_button_usephone").on("click",function(event) {
			$("#qr_overlay").show();
		});
	},

	verifyButtonClicked: function(event) {
		$("#IRMA_button_verify").off("click");
		$("#IRMA_button_verify").removeClass("enabled");
		$("#IRMA_button_verify").html("VERIFYING...");

		IRMA.Handler.bind("cardRemoved", function() {});

		$.ajax({
			url : IRMA.responseurl,
			contentType : 'application/json',
			type : 'POST',
			data: "{\"cardVersion\" : \"" + IRMA.Handler.cardVersion + "\"}",
			success: function(data) {
				console.log("Starting verification");
				console.log(data);
				IRMA.verification_responses = {};
				IRMA.verification_commands = IRMA.make_array_from_map(data.commandsSets);
				console.log("Verification commands set to: ", IRMA.verification_commands);
				IRMA.current_verification_idx = 0;
				IRMA.responseurl = data.responseurl;

		    IRMA.Handler.verifyPin(IRMA.verify_start);
			}
		});
	},

	verify_start: function(response) {
		console.log("Pin verified", response);
		if(response.arguments.result === "success") {
      IRMA.verifyStepOne();
		} else {
			// FIXME Do some error handling
			// TODO test blocking pin and feedback for that
		}
	},

	// Select next set of commands to send
	verifyStepOne: function() {
		if(IRMA.current_verification_idx >= IRMA.verification_commands.length) {
			// We are done
			console.log("We are done!");
			console.log(IRMA.verification_responses);
			IRMA.finishVerify(IRMA.verification_responses, IRMA.verification_data);
			return;
		}

		console.log(IRMA.verification_commands);
		var commands = IRMA.verification_commands[IRMA.current_verification_idx];
		IRMA.Handler.sendFeedback("Verifying "
				+ IRMA.verification_names[commands.name], "none");
		IRMA.Handler.transmitCommandSet(commands.value, IRMA.verifyStepTwo);
	},

	// Store the result
	verifyStepTwo: function(responses) {
		var commands = IRMA.verification_commands[IRMA.current_verification_idx];
		console.log("This is what we had to do", commands);
		console.log("This is what we got: ", responses);
		IRMA.verification_responses[commands.name] = responses.arguments.responses;

		// Goto next verification
		IRMA.current_verification_idx++;
		IRMA.verifyStepOne();
	},

	finishVerify: function(responses, data) {
		console.log("Finished IRMA verification");

		// Test whether all communication succeeded
		for(var key in responses) {
			if(responses.hasOwnProperty(key)) {
				var response = responses[key];
				if(response.smartcardstatus === "failed") {
					IRMA.handle_verification_failure(key, response);
					return;
				}
			}
		}

		console.log("Sending handler feedback success");
		IRMA.Handler.sendFeedback("Done", "success");

		// Send results to webserver
		$.ajax({
			url : this.responseurl,
			contentType : 'application/json',
			data : JSON.stringify(responses),
			type : 'POST',
			success : function(data) {
				console.log(data);
				if (data.status === 'success') {
					IRMA.onVerifySuccess(data);
				} else if (data.status === 'error') {
					IRMA.show_failure(data.feedbackMessage, "FAILED");
				} else {
					IRMA.show_failure_credential_not_found();
				}
			}
		});
	},

	handle_verification_failure: function(key, response) {
		console.log("Offending command: " + response['failed-key']);
		var cred_name = IRMA.verification_names[key];
		if(IRMA.is_communication_error(response)) {
			// Lost contact with the card
			IRMA.show_failure("Card Lost", "FAILED");
		} else if (response['failed-key'] === "startprove" && response.startprove.apdu === "6A88") {
			IRMA.show_failure("Credential for " + cred_name + " does not exists.", "FAILED");
		} else {
			IRMA.show_failure("Unknown error verifying " + cred_name, "FAILED");
		}
		IRMA.verification_reset();
	},

	is_communication_error: function(response) {
		var key = response['failed-key'];
		return response[key].apdu.indexOf("SCARD_E_NOT_TRANSACTED") != -1;
	},

	start_batch_issue: function(selection, issue_url) {
		$("#IRMA_issuer_logo_img").prop("src", IRMAURL.issuerLogo);
		$("#IRMA_issuer_arrow_img").prop("src", IRMAURL.img + "/irma_arrow_white_312px.png");
		$("#IRMA_issuer_irma_img").prop("src", IRMAURL.img + "/irma_logo_1024px.png");
		IRMA.show_issue();
		IRMA.selection = selection;
		IRMA.issue_url = issue_url;

		if(typeof issue_data === "undefined") {
			issue_data = new Object();
			issue_data.empty = true;
		}

		console.log("Contacting: " + issue_url);
		$.ajax({
			headers: {
				'Accept': 'application/json',
				'Content-Type': 'application/json'
			},
			url: issue_url,
			data: JSON.stringify(issue_data),
			type: "POST",
			success: IRMA.display_issue_credentials,
		});

		IRMA.bindCallback("cardInserted", IRMA.createCardInsertedCallback(function() {
			IRMA.Handler.selectApplet(IRMA.irma_aid, IRMA.enable_issue, function() {
				IRMA.Handler.selectApplet(IRMA.irma_aid_0_7, IRMA.enable_issue, function() {
					$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_warning_520px.png");
					$("#IRMA_status_text").html("Inserted card is not an IRMA card");
				});
			});
			IRMA.bindCallback("cardRemoved", function() {IRMA.disable_issue();});
		}));

		if(IRMA.card_connected) {
			IRMA.enable_issue();
			IRMA.bindCallback("cardRemoved", function() {IRMA.disable_issue();});
		}
	},

	issue_button_clicked: function(event) {
		console.log("Issue button clicked");
		$("#IRMA_button_issue").off("click");
		$("#IRMA_button_issue").removeClass("enabled");
		$("#IRMA_button_issue").html("ISSUING...");
		IRMA.Handler.bind("cardRemoved", function() {});

		IRMA.irma_issue_state = "issue";
		IRMA.current_credential_idx = 0;
		IRMA.current_credential = IRMA.selection[IRMA.current_credential_idx];

		IRMA.Handler.verifyPin(IRMA.issue_start);
	},

	issue_start: function(response) {
		console.log("Pin verified", response);
		if(response.arguments.result === "success") {
			IRMA.issue_step_one();
		} else {
			// FIXME Do some error handling
			// TODO test blocking pin and feedback for that
		}
	},

	issue_step_one: function() {
		IRMA.issue_set_active(IRMA.current_credential);
		IRMA.issue_set_status(IRMA.current_credential, "Issuing..");
		IRMA.Handler.sendFeedback("Issueing " + IRMA.current_credential, "none");

		$.ajax({
			url: IRMA.issue_url + '/' + IRMA.current_credential + '/1',
			contentType: 'application/json',
			type: 'POST',
			data: "{\"cardVersion\" : \"" + IRMA.Handler.cardVersion + "\"}",
			success: function(data) {
				console.log('Got first batch of data for issuing ' + IRMA.current_credential);
				console.log(data);
				IRMA.responseurl = data.responseurl;

				IRMA.Handler.transmitCommandSet(data.commands, IRMA.issue_step_one_process);
			}
		});
	},

	issue_step_one_process: function(data) {
		var response = data.arguments.responses;
		console.log(response);
		if(response.smartcardstatus === "failed") {
			IRMA.handle_issue_failure(response);
			IRMA.issue_next_credential();
		} else {
			IRMA.issue_step_two(response);
		}
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

				IRMA.Handler.transmitCommandSet(data.commands, IRMA.issue_step_two_process);
			}
		});
	},

	issue_step_two_process: function(data) {
		var response = data.arguments.responses;
		console.log(response);
		if(response.smartcardstatus === "failed") {
			IRMA.handle_issue_failure(response);
			IRMA.issue_next_credential();
		} else {
			IRMA.issue_step_three(response);
		}
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

				IRMA.issue_set_done(IRMA.current_credential);
				IRMA.issue_next_credential();
			}
		});
	},

	issue_next_credential: function() {
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
		IRMA.done();
		$("#IRMA_button_issue").on("click", function() {
			IRMA.onIssuanceFinished();
		});
	},

	done: function() {
		IRMA.Handler.close();
	},

	handle_issue_failure: function(response) {
		console.log("Offending command: " + response['failed-key']);

		if(IRMA.is_communication_error(response)) {
			// Lost contact with the card
			IRMA.issue_set_error(IRMA.current_credential, "Card Lost");

			// Break the chain, no more attempts
			current_credential_idx = IRMA.selection.length;
		} else if (response['failed-key'] === "start_issuance" && response['start_issuance'].apdu === "6986") {
			IRMA.issue_set_error(IRMA.current_credential, "Already issued");
		} else {
			IRMA.issue_set_error(IRMA.current_credential, "Unknown error");
		}
	},

	display_issue_credentials: function(data) {
		console.log(data);

		// First clear display
		$("#IRMA_issue_credential_list_content").empty();

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
		$("#IRMA_button_back_verify").on("click", function() {
			// You can handle some internal stuff here if necessary
			IRMA.onBackButtonPressed();
		});
	},

	hide_verify: function() {
		$("#IRMA_verify").fadeOut();
	},

	disableVerify: function () {
		$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_waiting_520px.png");
		$("#IRMA_verifier_arrow_img").prop("src", IRMAURL.img + "/irma_arrow_312px.png");
		$("#IRMA_verifier_irma_img").prop("src", IRMAURL.img + "/irma_logo_1024px.png");
		$("#IRMA_status_text").html("Insert your IRMA card or use your phone");
		$("#IRMA_button_verify").off("click");
		$("#IRMA_button_verify").removeClass("enabled");
		$("#IRMA_button_verify").html("WAITING FOR CARD...");
	},

	enableVerify: function() {
		$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_ready_520px.png");
		$("#IRMA_status_text").html("Hit 'VERIFY' to check your credential");
		$("#IRMA_button_verify").html("VERIFY");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", IRMA.verifyButtonClicked);
	},

	show_issue: function() {
		$("#IRMA_issue").fadeIn();
		$("#IRMA_button_back_issue").on("click", function() {
			// You can handle some internal stuff here if necessary
			IRMA.onBackButtonPressed();
		});
	},

	hide_issue: function() {
		$("#IRMA_issue").fadeOut();
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
		$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_ok_520px.png");
		$("#IRMA_status_text").html("Hit 'CONTINUE' to proceed to the website");
		$("#IRMA_button_verify").html("CONTINUE");
		$("#IRMA_button_verify").addClass("enabled");
		$("#IRMA_button_verify").on("click", function(event) {
			IRMA.Handler.close();
			window.location = data.result;
		});
	},

	show_warning: function(text) {
        $("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_warning_520px.png");
        $("#IRMA_status_text").html(text);
        IRMA.Handler.sendFeedback(text, "warning");
	},

	show_failure_credential_not_found: function() {
		IRMA.show_failure("Credential not found", "NOT FOUND");
	},

	show_error_connection_lost: function() {
		IRMA.show_error("Connection lost", "COMMUNICATION ERROR");
	},

	show_error: function(text, status) {
		$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_warning_520px.png");
		$("#IRMA_status_text").html(text);
		$("#IRMA_button_verify").html(status);
		IRMA.Handler.sendFeedback(text, "failure");
	},

	show_failure: function(text, status) {
		$("#IRMA_status_icon").prop("src", IRMAURL.img + "/irma_icon_missing_520px.png");
		$("#IRMA_status_text").html(text);
		IRMA.Handler.sendFeedback(text, "failure");
		$("#IRMA_button_verify").html(status);
	},

	issue_set_status: function(credential, text) {
		IRMA.Handler.sendFeedback(text, "none");
		$("#IRMA-issue-status-" + credential).html("(" + text + ")");
	},

	issue_set_error: function(credential, text) {
		IRMA.issue_set_status(credential, text);
		IRMA.issue_set_failed(credential);
		console.log(credential);
		IRMA.Handler.sendFeedback(text, "failure");
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
