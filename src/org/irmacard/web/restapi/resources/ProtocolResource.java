package org.irmacard.web.restapi.resources;

import java.math.BigInteger;
import java.util.Map;

import org.irmacard.web.restapi.util.CommandSet;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.restlet.resource.ServerResource;

import service.ProtocolCommand;
import service.ProtocolResponse;
import service.ProtocolResponses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import credentials.Attributes;
import credentials.CredentialsException;
import credentials.Nonce;
import credentials.idemix.IdemixCredentials;
import credentials.idemix.IdemixNonce;
import credentials.idemix.spec.IdemixVerifySpecification;

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
		IdemixCredentials ic = new IdemixCredentials();

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
		IdemixCredentials ic = new IdemixCredentials();

		return ic.verifyProofResponses(vspec, nonce, responses);
	}

}
