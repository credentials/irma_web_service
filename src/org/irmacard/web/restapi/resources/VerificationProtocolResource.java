package org.irmacard.web.restapi.resources;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ProtocolResponses;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.Nonce;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.IdemixNonce;
import org.irmacard.credentials.idemix.spec.IdemixVerifySpecification;
import org.irmacard.credentials.idemix.util.VerifyCredentialInformation;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolStep;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Resource for the verification protocol.
 * @author Maarten Everts
 *
 */
public class VerificationProtocolResource extends ServerResource {
	private final String ISSUER = "MijnOverheid";
	private final String CRED_NAME = "ageLower";
	private final String VERIFIER = "UitzendingGemist";
	private final String SPEC_NAME = "ageLowerOver16";

	@Post("json")
	public String handlePost (String value) {
		Integer crednr = Integer.parseInt((String) getRequestAttributes().get("crednr"));
		String id = (String) getRequestAttributes().get("id");
		String round = (String) getRequestAttributes().get("round");
		if (id == null) {
			return step0(crednr,value);
		} else if (round != null && round.equals("1")) {
			return step1(crednr,value,id);
		}
		return null;
	}
	
	@Get
	public String handleGet() {
		return CRED_NAME;
		
	}
	/**
	 * Start new verification protocol
	 * @param crednr credential number
	 * @param value request body
	 * @return
	 */
	public String step0(int crednr, String value) {
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				registerTypeAdapter(ProtocolCommand.class, new ProtocolCommandSerializer()).
				create();
		
		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();

		try {
			ProtocolStep cs = new ProtocolStep();
			Nonce nonce = ic.generateNonce(vspec);
			cs.commands = ic.requestProofCommands(vspec, nonce);
			
			// Save the state, use random id as key
			UUID id = UUID.randomUUID();
			BigInteger intNonce = ((IdemixNonce)nonce).getNonce();
			
			ProtocolState.putNonce(id.toString(), intNonce);

			cs.responseurl = getReference().getPath() + "/" + id.toString() + "/1";
			return gson.toJson(cs);
		} catch (CredentialsException e) {
			e.printStackTrace();
		}

		return null; 		
	}
	
	/**
	 * Start new verification protocol
	 * @param crednr credential number
	 * @param value request body
	 * @return
	 */
	public String step0qr(int crednr, String value) {
		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
		try {
			Nonce nonce = ic.generateNonce(vspec);
			
			// Save the state, use random id as key
			UUID id = UUID.randomUUID();
			BigInteger intNonce = ((IdemixNonce)nonce).getNonce();
			
			ProtocolState.putNonce(id.toString(), intNonce);
		} catch (CredentialsException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Handle the next step of the verification protocol.
	 * @param crednr credential number
	 * @param value request body (with the card responses)
	 * @param verificationId 
	 * @return
	 */
	public String step1(int crednr, String value, String verificationId) {
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				registerTypeAdapter(ProtocolResponse.class, new ProtocolResponseDeserializer()).
				create();
		
		// Get the nonce based on the id
		BigInteger intNonce = ProtocolState.getNonce(verificationId);
		IdemixNonce nonce = new IdemixNonce(intNonce);
		
		ProtocolResponses responses = gson.fromJson(value, ProtocolResponses.class);		

		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();

		try {
			Attributes attr = ic.verifyProofResponses(vspec, nonce, responses);

			// TODO: do something with the results!
			if (attr == null) {
				return "{\"response\": \"invalid\"}";
			} else {
				attr.print();
				return "{\"response\": \"valid\", \"url\": \"http://spuitenenslikken.bnn.nl/\"}";
			}
		} catch (CredentialsException e) {
			e.printStackTrace();
			return "{\"response\": \"invalid\"}";
		}
	}

}
