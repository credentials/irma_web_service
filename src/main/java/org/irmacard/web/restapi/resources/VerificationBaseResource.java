package org.irmacard.web.restapi.resources;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.scuba.smartcards.ProtocolCommand;
import net.sf.scuba.smartcards.ProtocolCommands;
import net.sf.scuba.smartcards.ProtocolResponse;
import net.sf.scuba.smartcards.ProtocolResponses;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.descriptions.IdemixVerificationDescription;
import org.irmacard.credentials.idemix.irma.IRMAIdemixDisclosureProof;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.idemix.IdemixSmartcard;
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
			for (VerificationDescription vd : getVerifications(id)) {
				ps.info.verification_names.put(vd.getID(), vd.getName());
			}
			break;
		case 1:
			ps = createVerificationProtocolStep(id, getIdemixVerifications(id), value);
			ps.responseurl = makeResponseURL(id, step+1);
			ProtocolState.putStatus(id, "step1");
			break;
		case 2:
			ps = onSuccess(id,
					processVerificationResponse(id, getIdemixVerifications(id), value));
			ProtocolState.putResult(id, ps.result);
			break;
		default:
			break;
		}
		ProtocolState.putStatus(id, ps.status);
		System.out.println(gson.toJson(ps));
		return gson.toJson(ps);
	}

	private List<IdemixVerificationDescription> getIdemixVerifications(String id) {
		List<VerificationDescription> vds = getVerifications(id);

		LinkedList<IdemixVerificationDescription> ivds = new LinkedList<IdemixVerificationDescription>();

		for (VerificationDescription vd : vds) {
			try {
				ivds.add(new IdemixVerificationDescription(vd));
			} catch (InfoException e) {
				e.printStackTrace();
			}
		}

		return ivds;
	}

	public abstract List<VerificationDescription> getVerifications(String id);

	public abstract ProtocolStep onSuccess(String id, Map<String,Attributes> attrMap);

	public static ProtocolStep createVerificationProtocolStep(String id,
			List<IdemixVerificationDescription> vds,
			String value) {
		ProtocolStep ps = new ProtocolStep();

		Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(CardVersion.class,
				new CardVersionDeserializer()).create();

		CardVersion cv = gson.fromJson(value, CardVersion.class);
		ProtocolState.putCardVersion(id, cv);

		ps.commandsSets = new HashMap< Short, List<ProtocolCommand> >();
		for (IdemixVerificationDescription vd : vds) {
			short vid = vd.getVerificationDescription().getID();

			BigInteger nonce = vd.generateNonce();
			ProtocolState.putVerificationNonce(id, vid, nonce);

			ProtocolCommands commands = IdemixSmartcard.buildProofCommands(cv, nonce, vd);
			ps.commandsSets.put(vid, commands);
		}

		return ps;
	}

	public static Map<String, Attributes> processVerificationResponse(String id,
			List<IdemixVerificationDescription> vds, String value) {

		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolResponse.class,
				new ProtocolResponseDeserializer()).create();

		Map<String, ProtocolResponses> responsesMap = gson.fromJson(value, protocolResponsesType);

		CardVersion cv = ProtocolState.getCardVersion(id);

		// TODO: this crashes if IRMATube credential is not present,
		// responseMap is never checked for non-null.

		Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();
		for (IdemixVerificationDescription vd : vds) {
			short vid = vd.getVerificationDescription().getID();
			String verificationID = vd.getVerificationDescription().getVerificationID();

			ProtocolResponses responses = responsesMap.get(Short.toString(vid));
			BigInteger nonce = ProtocolState.getVerificationNonce(id, vid);

			try {
				IRMAIdemixDisclosureProof proof = IdemixSmartcard
						.processBuildProofResponses(cv, responses, vd);
				attributesMap.put(verificationID, proof.verify(vd, nonce));
			} catch (CredentialsException e) {
				// TODO: Expiry is one of the cases that is handled here, should do this better
				e.printStackTrace();
			}
		}

		return attributesMap;
	}
}
