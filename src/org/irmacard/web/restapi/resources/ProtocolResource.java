package org.irmacard.web.restapi.resources;

import java.math.BigInteger;
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
import org.irmacard.web.restapi.util.CommandSet;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ProtocolResource extends ServerResource {
	/**
	 * Start new verification protocol
	 * 
	 * @param crednr
	 *            credential number
	 * @param value
	 *            request body
	 * @return
	 */
	public String startVerify(IdemixVerifySpecification vspec, String value,
			String id, String step) {
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(ProtocolCommand.class,
						new ProtocolCommandSerializer()).create();
		IdemixCredentials ic = new IdemixCredentials(null);

		try {
			CommandSet cs = new CommandSet();
			Nonce nonce = ic.generateNonce(vspec);
			cs.commands = ic.requestProofCommands(vspec, nonce);

			@SuppressWarnings("unchecked")
			Map<String, BigInteger> noncemap = (Map<String, BigInteger>) getContext()
					.getAttributes().get("noncemap");
			noncemap.put(id, ((IdemixNonce) nonce).getNonce());

			cs.responseurl = getReference().getPath() + "/" + id.toString()
					+ "/" + step;
			return gson.toJson(cs);
		} catch (CredentialsException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public Attributes verifyResponses(IdemixVerifySpecification vspec,
			String value, String id) throws CredentialsException {
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(ProtocolResponse.class,
						new ProtocolResponseDeserializer()).create();

		// Get the nonce based on the id
		@SuppressWarnings("unchecked")
		Map<String, BigInteger> noncemap = (Map<String, BigInteger>) getContext()
				.getAttributes().get("noncemap");
		BigInteger intNonce = noncemap.get(id);
		IdemixNonce nonce = new IdemixNonce(intNonce);

		ProtocolResponses responses = gson.fromJson(value,
				ProtocolResponses.class);
		IdemixCredentials ic = new IdemixCredentials(null);

		return ic.verifyProofResponses(vspec, nonce, responses);
	}

}
