package org.irmacard.web.restapi.util;

import java.lang.reflect.Type;

import net.sf.scuba.smartcards.ResponseAPDU;
import net.sf.scuba.util.Hex;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


/**
 * Helper class to deserialize a ResponseAPDU from json
 *
 */
public class ResponseAPDUDeserializer implements JsonDeserializer<ResponseAPDU> {
	@Override
	public ResponseAPDU deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return new ResponseAPDU(Hex.hexStringToBytes(json.getAsString()));
	}
}
