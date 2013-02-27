package org.irmacard.web.restapi;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.Attributes;

import com.ibm.zurich.idmx.issuance.Issuer;

/**
 * Very simple class for short-lived state for the protocols.
 * In time this may be replaced by something else.
 * @author Maarten Everts (TNO)
 *
 */
public class ProtocolState {
	private static Map<String,BigInteger> nonceMap = new HashMap<String, BigInteger>();
	private static Map<String,Attributes> attributesMap = new HashMap<String, Attributes>();
	private static Map<String,Issuer> issuerMap = new HashMap<String, Issuer>();
	private static Map<String,String> stateMap = new HashMap<String, String>();
	private static Map<String,String> resultMap = new HashMap<String, String>();
	
	public static BigInteger getNonce(String id) {
		return nonceMap.get(id);
	}
	public static void putNonce(String id, BigInteger nonce) {
		nonceMap.put(id, nonce);
	}
	
	public static Attributes getAttributes(String id) {
		return attributesMap.get(id);
	}
	public static void putAttributes(String id, Attributes attributes) {
		attributesMap.put(id, attributes);
	}
	
	public static Issuer getIssuer(String id) {
		return issuerMap.get(id);
	}
	public static void putIssuer(String id, Issuer issuer) {
		issuerMap.put(id, issuer);
	}
	
	public static String getStatus(String id) {
		return stateMap.get(id);
	}
	public static void putStatus(String id, String state) {
		stateMap.put(id, state);
	}
	
	public static String getResult(String id) {
		return resultMap.get(id);
	}
	public static void putResult(String id, String state) {
		resultMap.put(id, state);
	}
}
