package org.irmacard.web.restapi.util;

import java.lang.reflect.Type;

import org.irmacard.idemix.util.CardVersion;

import net.sourceforge.scuba.util.Hex;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Helper class to deserialize a ProtocolResponse from json
 *
 */
public class CardVersionDeserializer implements JsonDeserializer<CardVersion> {
	@Override
	public CardVersion deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return new CardVersion(Hex.hexStringToBytes(json.getAsJsonObject().get("cardVersion").getAsString()));
	}
}
