package org.irmacard.web.restapi.resources;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Map;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolCommands;
import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ProtocolResponses;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.spec.IdemixIssueSpecification;
import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.credentials.info.InfoException;
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
import com.ibm.zurich.idmx.issuance.Issuer;

public abstract class IssueBaseResource  extends ProtocolBaseResource {	
	@Override
	public String handleProtocolStep(String id, int step, String value) throws InfoException {
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
	
	private ProtocolStep createIssuanceProtocolStep1(String id, String cred, String value) throws InfoException, JsonSyntaxException {
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

		Attributes attributes = makeAttributes(issuer_info.get(cred).attributes);

		// FIXME: should not really do this here.
		attributes.setExpireDate(null);
		
		ProtocolState.putStatus(id, "issueready");
		
		IdemixCredentials ic = new IdemixCredentials(null);
		IssueCredentialInformation ici;
		try {
			ici = getIssueCredentialInformation(cred);
		} catch (InfoException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Cannot read issuance information");
		}
		IdemixIssueSpecification spec = ici.getIdemixIssueSpecification();
		spec.setCardVersion(cv);

		// Initialize the issuer
		Issuer issuer = ici.getIssuer(attributes);

		// Run part one of protocol
		ProtocolCommands commands;
		try {
			commands = ic.requestIssueRound1Commands(spec, attributes, issuer);
		} catch (CredentialsException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Error while issuing.");
		}
		
		// Save state, this is the nasty part
		BigInteger nonce1 = null;
		try {
			Field nonce1Field = Issuer.class.getDeclaredField("nonce1");
			nonce1Field.setAccessible(true);
			nonce1 = (BigInteger) nonce1Field.get(issuer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ProtocolState.putIssuer(id, issuer);
		ProtocolState.putNonce(id, nonce1);
		ProtocolState.putAttributes(id, attributes);
		ProtocolState.putCardVersion(id, cv);
		
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

		// FIXME: setup the actual idemix issue specification
		System.out.println("==== Setting up credential infromation ===");
		IdemixCredentials ic = new IdemixCredentials(null);
		IssueCredentialInformation ici;
		try {
			ici = getIssueCredentialInformation(cred);
		} catch (InfoException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Cannot read issuance information");
		}

		System.out.println("=== Getting issuance information ===");
		IdemixIssueSpecification spec = ici.getIdemixIssueSpecification();
		spec.setCardVersion(ProtocolState.getCardVersion(id));

		BigInteger nonce1 = ProtocolState.getNonce(id);
		Attributes attributes = ProtocolState.getAttributes(id);

		// Initialize the issuer
		System.out.println("=== Getting issuer ===");
		Issuer issuer = ici.getIssuer(attributes);

		// Restore the state, this is the nasty part
		try {
			Field nonce1Field = Issuer.class.getDeclaredField("nonce1");
			nonce1Field.setAccessible(true);
			nonce1Field.set(issuer, nonce1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// FIXME: superfluous?
		issuer = ProtocolState.getIssuer(id);

		ProtocolResponses responses = gson.fromJson(value,
				ProtocolResponses.class);

		// Run part one of protocol
		ProtocolCommands commands;
		try {
			// Run next part of protocol
			commands = ic.requestIssueRound3Commands(spec, attributes, issuer, responses);
		} catch (CredentialsException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Error while issuing.");
		} catch (NullPointerException e) {
			// This appears to be thrown here if some verification fails
			e.printStackTrace();
			return ProtocolStep.newError("Error while issuing (null-pointer).");
		}

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
	public abstract IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException;
	
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
