package org.irmacard.web.restapi.resources;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.idemix.util.CardVersion;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.CardVersionDeserializer;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolInfo;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.ProtocolStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


public abstract class VerificationBaseResource extends ProtocolBaseResource {
	
	static final Type protocolResponsesType = new TypeToken<Map<String, ProtocolResponses>>() {}.getType();
	
	@Override
	public String handleProtocolStep(String id, int step, String value) {

		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolCommand.class,
					new ProtocolCommandSerializer()).create();

		ProtocolStep ps = null;
		switch (step) {
		case 0:
			ps = new ProtocolStep();
			ps.info = new ProtocolInfo();
			ps.info.qr_url = getBaseURL() + getBasePath() + "/" + id + "/qr";
			ps.info.status_url = getBaseURL() + getBasePath() + "/" + id + "/status";
			ps.info.verification_names = new HashMap<Short, String>();
			ps.responseurl = makeResponseURL(id, step+1);
			for(VerificationDescription vd : getVerifications(id)) {
				ps.info.verification_names.put(vd.getID(), vd.getName());
			}
			break;
		case 1:
			ps = createVerificationProtocolStep(id, getVerifications(id), value);
			ps.responseurl = makeResponseURL(id, step+1);
			ProtocolState.putStatus(id, "step1");
			break;
		case 2:
			ps = onSuccess(id, processVerificationResponse(id, getVerifications(id), value));
			ProtocolState.putResult(id, ps.result);
			break;
		default:
			break;
		}
		ProtocolState.putStatus(id, ps.status);
		System.out.println(gson.toJson(ps));
		return gson.toJson(ps);
	}
	
	public abstract List<VerificationDescription> getVerifications(String id);
	
	public abstract ProtocolStep onSuccess(String id, Map<String,Attributes> attrMap);
	
	public static ProtocolStep createVerificationProtocolStep(String id, List<VerificationDescription> specs, String value) {
		ProtocolStep ps = new ProtocolStep();

		Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(CardVersion.class,
				new CardVersionDeserializer()).create();

		CardVersion cv = gson.fromJson(value, CardVersion.class);

		IdemixCredentials ic = new IdemixCredentials(null);
		ps.commandsSets = new HashMap< Short, List<ProtocolCommand> >();
		for (VerificationDescription vd : specs) {
			VerifyCredentialInformation vci;
			try {
				vci = new VerifyCredentialInformation(vd.getVerifierID(), vd.getVerificationID());
				IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
				vspec.setCardVersion(cv);
				Nonce nonce = ic.generateNonce(vspec);
				ProtocolState.putVerificationNonce(id, vd.getID(), ((IdemixNonce) nonce).getNonce());
				ps.commandsSets.put(vd.getID(), ic.requestProofCommands(vspec, nonce));
			} catch (InfoException e) {
				e.printStackTrace();
			} catch (CredentialsException e) {
				e.printStackTrace();
			}
		}

		return ps;
	}

	public static Map<String, Attributes> processVerificationResponse(String id, List<VerificationDescription> specs, String value) {
		
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolResponse.class,
				new ProtocolResponseDeserializer()).create();

		Map<String, ProtocolResponses> responsesMap = gson.fromJson(value, protocolResponsesType);
		IdemixCredentials ic = new IdemixCredentials(null);
		
		// TODO: this crashes if IRMATube credential is not present,
		// responseMap is never checked for non-null.

		Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();
		for (VerificationDescription vd : specs) {
			ProtocolResponses responses = responsesMap.get(Short.toString(vd.getID()));
			IdemixNonce nonce = new IdemixNonce(ProtocolState.getVerificationNonce(id,vd.getID()));
			VerifyCredentialInformation vci = null;
			try {
				vci = new VerifyCredentialInformation(vd.getVerifierID(), vd.getVerificationID());
			} catch (InfoException e) {
				e.printStackTrace();
			}
			IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
			try {
				attributesMap.put(vd.getVerificationID(), ic.verifyProofResponses(vspec, nonce, responses));
			} catch (CredentialsException e) {
				// TODO: Expiry is one of the cases that is handled here, should do this better
				e.printStackTrace();
			}
		}

		return attributesMap;
	}
}
