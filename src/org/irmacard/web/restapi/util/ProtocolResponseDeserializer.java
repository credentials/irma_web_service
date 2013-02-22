package org.irmacard.web.restapi.util;

import java.lang.reflect.Type;

import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Helper class to deserialize a ProtocolResponse from json
 *
 */
public class ProtocolResponseDeserializer implements JsonDeserializer<ProtocolResponse> {
	@Override
	public ProtocolResponse deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return new ProtocolResponse(
				json.getAsJsonObject().get("key").getAsString(), 
				new ResponseAPDU(Hex.hexStringToBytes(json.getAsJsonObject().get("apdu").getAsString())));
	}
}
