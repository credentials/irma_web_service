package org.irmacard.web.restapi.resources;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.List;

import net.sourceforge.scuba.smartcards.IResponseAPDU;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import org.irmacard.web.restapi.IRMASetup;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import service.ProtocolCommand;
import service.ProtocolResponses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import credentials.Attributes;
import credentials.CredentialsException;
import credentials.Nonce;
import credentials.idemix.IdemixCredentials;
import credentials.idemix.IdemixNonce;
import credentials.idemix.spec.IdemixVerifySpecification;

/**
 * Resource for the verification protocol.
 * @author Maarten Everts
 *
 */
public class VerificationProtocolResource extends ServerResource {

    private class CommandSet {
    	public List<ProtocolCommand> commands;
    	public String responseurl;
    }
    
	@Post("json")
	public String handlePost (String value) {
		Integer crednr = Integer.parseInt((String) getRequestAttributes().get("crednr"));
		String nonce = (String) getRequestAttributes().get("nonce");
		String round = (String) getRequestAttributes().get("round");
		if (nonce == null) {
			return step0(crednr,value);
		} else if (round != null && round.equals("1")) {
			return step1(crednr,value,nonce);
		}
		return null;
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

		IdemixCredentials ic = new IdemixCredentials();
		
		IdemixVerifySpecification vspec = IdemixVerifySpecification
				.fromIdemixProofSpec(IRMASetup.PROOF_SPEC_LOCATION, (short)crednr);
		try {
			CommandSet cs = new CommandSet();
			Nonce nonce = ic.generateNonce(vspec);
			cs.commands = ic.requestProofCommands(vspec, nonce);
			// OK, this is kind of dirty, but I need a representation of the nonce :)
			String strnonce = Hex.bytesToHexString(((IdemixNonce)nonce).getNonce().toByteArray());
			cs.responseurl = getReference().getPath() + "/" + strnonce + "/1";
			return gson.toJson(cs);
		} catch (CredentialsException e) {
			e.printStackTrace();
		}

		return null; 		
	}
	
	/**
	 * Handle the next step of the verification protocol.
	 * @param crednr credential number
	 * @param value request body (with the card responses)
	 * @param strNonce nonce, hex-encoded.
	 * @return
	 */
	public String step1(int crednr, String value, String strNonce) {
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				registerTypeAdapter(IResponseAPDU.class, new ResponseAPDUDerializer()).
				create();
		ProtocolResponses responses = gson.fromJson(value, ProtocolResponses.class);		
		IdemixCredentials ic = new IdemixCredentials();
		IdemixVerifySpecification vspec = IdemixVerifySpecification
				.fromIdemixProofSpec(IRMASetup.PROOF_SPEC_LOCATION, (short)crednr);
		IdemixNonce nonce = new IdemixNonce(new BigInteger(Hex.hexStringToBytes(strNonce)));

		try {
			Attributes attr = ic.verifyProofResponses(vspec, nonce, responses);
			// TODO: do something with the results!
			if (attr == null) {
				return "{\"response\": \"invalid\"}";
			} else {
				return "{\"response\": \"valid\"}";
			}
		} catch (CredentialsException e) {
			e.printStackTrace();
			return "{\"response\": \"invalid\"}";
		}
	}

	/**
	 * Helper class to deserialize a ResponseAPDU from json
	 *
	 */
	private class ResponseAPDUDerializer implements JsonDeserializer<ResponseAPDU> {
		@Override
		public ResponseAPDU deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return new ResponseAPDU(Hex.hexStringToBytes(json.getAsString()));
		}
	}
	
	/**
	 * Helper class to serialize ProtocolCommand to JSON.
	 *
	 */
	private class ProtocolCommandSerializer implements JsonSerializer<ProtocolCommand> {
		@Override
		public JsonElement serialize(ProtocolCommand src, Type typeOfSrc,
				JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("key", src.key);
			obj.addProperty("command", Hex.bytesToHexString(src.command.getBytes()));
			return obj;
		}
	}
}
