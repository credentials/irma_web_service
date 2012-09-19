package org.irmacard.web.restapi.util;

import java.lang.reflect.Type;

import net.sourceforge.scuba.util.Hex;
import service.ProtocolCommand;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Helper class to serialize ProtocolCommand to JSON.
 *
 */
public class ProtocolCommandSerializer implements JsonSerializer<ProtocolCommand> {
	@Override
	public JsonElement serialize(ProtocolCommand src, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("key", src.getKey());
		obj.addProperty("command", Hex.bytesToHexString(src.getAPDU().getBytes()));
		return obj;
	}
}
