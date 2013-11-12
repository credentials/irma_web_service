package org.irmacard.web.restapi;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.idemix.util.CardVersion;
import org.irmacard.web.restapi.resources.irmaWiki.data.IRMAWikiIssuanceData;

import com.ibm.zurich.idmx.issuance.Issuer;

/**
 * Very simple class for short-lived state for the protocols.
 * In time this may be replaced by something else.
 * @author Maarten Everts (TNO)
 *
 */
public class ProtocolState {
	private static Map<String,BigInteger> nonceMap = new HashMap<String, BigInteger>();
	private static Map<String,BigInteger> verificationNonceMap = new HashMap<String, BigInteger>();
	private static Map<String,Attributes> attributesMap = new HashMap<String, Attributes>();
	private static Map<String,Issuer> issuerMap = new HashMap<String, Issuer>();
	private static Map<String,String> stateMap = new HashMap<String, String>();
	private static Map<String,String> resultMap = new HashMap<String, String>();
	private static Map<String,CardVersion> cardMap = new HashMap<String, CardVersion>();

	// State for IRMATube, we store the age that we are verifying
	private static Map<String, String> irmaTubeAgeMap = new HashMap<String, String>();

	// State for StudentCard issuing, keep track of the verified UUID
	private static Map<String,String> studentCardUUIDMap = new HashMap<String, String>();

	// State for IRMAWiki issuance.
	private static Map<String, IRMAWikiIssuanceData> irmaWikiMap = new HashMap<String, IRMAWikiIssuanceData>();

	public static BigInteger getNonce(String id) {
		return nonceMap.get(id);
	}

	public static void putNonce(String id, BigInteger nonce) {
		nonceMap.put(id, nonce);
	}

	public static void putCardVersion(String id, CardVersion cv) {
		cardMap.put(id, cv);
	}

	public static CardVersion getCardVersion(String id) {
		return cardMap.get(id);
	}

	public static BigInteger getVerificationNonce(String id, short vId) {
		return verificationNonceMap.get(id + vId);
	}

	public static void putVerificationNonce(String id, short vId, BigInteger nonce) {
		verificationNonceMap.put(id + vId, nonce);
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

	public static void putIRMATubeAge(String id, String age) {
		irmaTubeAgeMap.put(id, age);
	}

	public static String getIRMATubeAge(String id) {
		return irmaTubeAgeMap.get(id);
	}

	public static void putStudentCardUUID(String id, String uuid) {
		studentCardUUIDMap.put(id, uuid);
	}

	public static String getStudentCardUUID(String id) {
		return studentCardUUIDMap.get(id);
	}

	public static void putIRMAWikiData(String id, IRMAWikiIssuanceData data) {
		irmaWikiMap.put(id, data);
	}

	public static IRMAWikiIssuanceData getIRMAWikiData(String id) {
		return irmaWikiMap.get(id);
	}
}
