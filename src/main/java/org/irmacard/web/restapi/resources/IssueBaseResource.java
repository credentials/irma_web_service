package org.irmacard.web.restapi.resources;

import java.math.BigInteger;
import java.util.Map;

import net.sf.scuba.smartcards.ProtocolCommand;
import net.sf.scuba.smartcards.ProtocolCommands;
import net.sf.scuba.smartcards.ProtocolResponse;
import net.sf.scuba.smartcards.ProtocolResponses;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.descriptions.IdemixCredentialDescription;
import org.irmacard.credentials.idemix.info.IdemixKeyStore;
import org.irmacard.credentials.idemix.irma.IRMAIdemixIssuer;
import org.irmacard.credentials.idemix.messages.IssueCommitmentMessage;
import org.irmacard.credentials.idemix.messages.IssueSignatureMessage;
import org.irmacard.credentials.info.CredentialDescription;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.idemix.IdemixSmartcard;
import org.irmacard.idemix.util.CardVersion;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.CardVersionDeserializer;
import org.irmacard.web.restapi.util.IssueCredentialInfo;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolInfo;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.ProtocolStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public abstract class IssueBaseResource  extends ProtocolBaseResource {
	@Override
    public String handleProtocolStep(String id, int step, String value)
            throws InfoException, CredentialsException {
		System.out.println("Handle post called!!!");
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolCommand.class,
					new ProtocolCommandSerializer()).create();

		ProtocolStep ps = null;
		String cred = (String) getRequestAttributes().get("cred");

		switch (step) {
		case 0:
			// Send overview of what can be issued
			ps = new ProtocolStep();
			ps.info = new ProtocolInfo();
			ps.info.issue_information = getIssueCredentialInfos(id, value);
			break;
		case 1:
			// Send first step of issuance commands, for specific credential
			ps = createIssuanceProtocolStep1(id, cred, value);
			ps.responseurl = makeIssueResponseURL(id, cred, step + 1);
			break;
		case 2:
			// Send second step of issuance commands, for specific credential
			ps = createIssuanceProtocolStep2(id, cred, value);
			ps.responseurl = makeIssueResponseURL(id, cred, step + 1);
			break;
		case 3:
			// If necessary final step
			ps = createIssuanceProtocolStepEnd(id, value);
			break;
		}

		return gson.toJson(ps);
	}

    private ProtocolStep createIssuanceProtocolStep1(String id, String cred, String value)
            throws InfoException, JsonSyntaxException, CredentialsException {
		Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(CardVersion.class,
				new CardVersionDeserializer()).create();

		// Check if eligible
		Map<String, IssueCredentialInfo> issuer_info = getIssueCredentialInfos(id, value);
		if(!issuer_info.containsKey(cred)) {
			return ProtocolStep.newError("You are not allowed to be issued " + cred);
		}

		CardVersion cv = gson.fromJson(value, CardVersion.class);
		ProtocolState.putCardVersion(id, cv);

		IdemixCredentialDescription icd = getIdemixCredentialDescription(cred);
		CredentialDescription cd = icd.getCredentialDescription();

		BigInteger nonce1 = icd.generateNonce();
		ProtocolState.putNonce(id, nonce1);

		Attributes attributes = makeAttributes(issuer_info.get(cred).attributes);
		attributes.setCredentialID(cd.getId());
		ProtocolState.putAttributes(id, attributes);

		ProtocolState.putStatus(id, "issueready");

		// Run part one of protocol
		ProtocolCommands commands = IdemixSmartcard.requestIssueCommitmentCommands(cv,
				icd, attributes, nonce1);

		ProtocolStep ps = new ProtocolStep();
		ps.commands = commands;
		ps.confirmationMessage = "Are you sure you want this credential to be issued to your IRMA card?";
		ps.askConfirmation = true;
		ps.usePIN = true;
		ps.protocolDone = false;
		ps.feedbackMessage = "Issuing credential (1)";

		return ps;
	}

	private ProtocolStep createIssuanceProtocolStep2(String id, String cred, String value) throws InfoException {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolCommand.class,
					new ProtocolCommandSerializer())
			.registerTypeAdapter(ProtocolResponse.class,
					new ProtocolResponseDeserializer()).create();

		IdemixCredentialDescription icd = getIdemixCredentialDescription(cred);
		CredentialDescription cd = icd.getCredentialDescription();

		CardVersion cv = ProtocolState.getCardVersion(id);
		BigInteger nonce1 = ProtocolState.getNonce(id);
		Attributes attributes = ProtocolState.getAttributes(id);

		// Initialize the issuer
		IRMAIdemixIssuer issuer = new IRMAIdemixIssuer(icd.getPublicKey(),
				IdemixKeyStore.getInstance().getSecretKey(cd), icd.getContext());

		ProtocolResponses responses = gson.fromJson(value,
				ProtocolResponses.class);
		IssueCommitmentMessage commit_msg = IdemixSmartcard
				.processIssueCommitmentCommands(cv, responses);

		// Create signature
		IssueSignatureMessage signature_msg;
		try {
			signature_msg = issuer.issueSignature(commit_msg, icd, attributes, nonce1);
		} catch (CredentialsException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Error while issuing.");
		}

		ProtocolCommands commands = IdemixSmartcard.requestIssueSignatureCommands(cv, icd,
				signature_msg);

		ProtocolState.putStatus(id, "issuing");

		ProtocolStep ps = new ProtocolStep();
		// FIXME
		ps.commands = commands;
		ps.protocolDone = false;
		ps.feedbackMessage = "Issuing credential (2)";

		return ps;
	}

	private ProtocolStep createIssuanceProtocolStepEnd(String id, String value) {
		// TODO: actually check what is sent
		ProtocolState.putStatus(id, "success");
		ProtocolStep ps = new ProtocolStep();
		ps.feedbackMessage = "Issuance successful";
		ps.protocolDone = true;
		ps.status = "success";
		return ps;
	}

	public abstract Map<String,IssueCredentialInfo> getIssueCredentialInfos(String id, String value);

	public abstract IdemixCredentialDescription getIdemixCredentialDescription(
			String cred) throws InfoException;

	protected String makeIssueResponseURL(String id, String cred, int step) {
		if (getRequestAttributes().get("id") == null) {
			return getBaseURL() + getBasePath() + '/' + id + '/' + cred + '/' + Integer.toString(step);
		}
		return getBaseURL() + getBasePath().substring(0, getBasePath().lastIndexOf('/')+1) + Integer.toString(step);
	}

	protected Attributes makeAttributes(Map<String,String> map) {
		Attributes attributes = new Attributes();
		for(String key : map.keySet()) {
			attributes.add(key, map.get(key).getBytes());
		}
		return attributes;
	}
}
