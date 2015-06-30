package org.irmacard.web.restapi.resources;

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
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.ProtocolStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class VerificationProtocolResource extends ProtocolBaseResource {	
	@Override
	public String handleProtocolStep(String id, int step, String value) throws InfoException {
		String verifier = (String) getRequestAttributes().get("verifier");
		String specName = (String) getRequestAttributes().get("specname");

		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolCommand.class,
					new ProtocolCommandSerializer()).create();

		VerifyCredentialInformation vci = null;
		try {
			vci = new VerifyCredentialInformation(verifier, specName);
		} catch (InfoException e1) {
			// TODO Auto-generated catch block
			// TODO Is this still used in practice?
			e1.printStackTrace();
			return "Error";
		}
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
		
		ProtocolStep ps = null;
		switch (step) {
		case 0:
			ps = createVerificationProtocolStep(id, vspec);
			ps.responseurl = makeResponseURL(id, step+1);
			ProtocolState.putStatus(id, "step1");
			break;
		case 1:
			ps = new ProtocolStep();
			ps.protocolDone = true;
			ps.status = "failure";
			try {
				Attributes attr = processVerificationResponse(id, vspec, value);
				if (attr != null) {
					ps.status = "success";
					if (verifier.equalsIgnoreCase("NYTimes")) {
						String age = new String(attr.get("over12"));
						if (age.equalsIgnoreCase("yes")) {
							ps.result = "http://www.nytimes.com";
							ProtocolState.putResult(id, ps.result);
						} else {
							ps.status = "failure";
						}
					} else {
						String age = new String(attr.get("over16"));
						if (age.equalsIgnoreCase("yes")) {
							ps.result = "http://spuitenenslikken.bnn.nl";
							ProtocolState.putResult(id, ps.result);
						} else {
							ps.status = "failure";
						}
					}
				}
			} catch (CredentialsException e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
		ProtocolState.putStatus(id, ps.status);
		return gson.toJson(ps);
	}

	public static ProtocolStep createVerificationProtocolStep(String id, IdemixVerifySpecification vspec) {
		ProtocolStep ps = new ProtocolStep();

		IdemixCredentials ic = new IdemixCredentials(null);

		try {
			Nonce nonce = ic.generateNonce(vspec);
			ps.commands = ic.requestProofCommands(vspec, nonce);
			ps.feedbackMessage = "Verifying";

			ProtocolState.putNonce(id, ((IdemixNonce) nonce).getNonce());			
		} catch (CredentialsException e) {
			e.printStackTrace();
		}
		return ps;
	}

	public static Attributes processVerificationResponse(String id, IdemixVerifySpecification vspec, String value) throws CredentialsException {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolResponse.class,
				new ProtocolResponseDeserializer()).create();

		IdemixNonce nonce = new IdemixNonce(ProtocolState.getNonce(id));

		ProtocolResponses responses = gson.fromJson(value, ProtocolResponses.class);
		IdemixCredentials ic = new IdemixCredentials(null);

		return ic.verifyProofResponses(vspec, nonce, responses);
	}
}
