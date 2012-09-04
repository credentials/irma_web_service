package org.irmacard.web.restapi.resources;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.login.CredentialException;

import net.sourceforge.scuba.smartcards.IResponseAPDU;

import org.irmacard.web.restapi.IRMASetup;

import org.irmacard.web.restapi.util.CommandSet;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ResponseAPDUDeserializer;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.zurich.idmx.dm.Values;
import com.ibm.zurich.idmx.issuance.Issuer;
import com.ibm.zurich.idmx.utils.SystemParameters;

import credentials.CredentialsException;
import credentials.Nonce;
import credentials.idemix.IdemixCredentials;
import credentials.idemix.IdemixNonce;
import credentials.idemix.IdemixPrivateKey;
import credentials.idemix.spec.IdemixIssueSpecification;
import credentials.idemix.spec.IdemixVerifySpecification;

import credentials.Attributes;

import service.ProtocolCommand;
import service.ProtocolResponses;

public class IssueStudentCredResource extends ProtocolResource {
	private class IssuanceCommandSet {
	    public List<ProtocolCommand> commands;
	    public String responseurl;
	    public Attributes attributes;
	}
	
    /** Attribute values */
    public static final BigInteger ATTRIBUTE_VALUE_1 = BigInteger.valueOf(1313);
    public static final BigInteger ATTRIBUTE_VALUE_2 = BigInteger.valueOf(1314);
    public static final BigInteger ATTRIBUTE_VALUE_3 = BigInteger.valueOf(1315);
    public static final BigInteger ATTRIBUTE_VALUE_4 = BigInteger.valueOf(1316);
	
	@Post("json")
	public String handlePost (String value) {
		String id = (String) getRequestAttributes().get("id");
		String round = (String) getRequestAttributes().get("round");
		int iround;
		
		if (id == null) {
			iround = 0;
			id = UUID.randomUUID().toString();
		} else {
			iround = Integer.parseInt(round);
		} 
		
		switch (iround) {
		case 0:
			return startVerify(getProofSpec(), value, id, "1");
		case 1:
			return processVerify(value, id);
		case 2:
			return processIssuance(value, id);
		}
		
		return null;
	}
	
	private IdemixVerifySpecification getProofSpec() {
		// FIXME: proper credential
		return IdemixVerifySpecification
				.fromIdemixProofSpec(IRMASetup.PROOF_SPEC_LOCATION, (short) 4);
	}
	
	public String processVerify(String value, String id) {
		Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(ProtocolCommand.class,
				new ProtocolCommandSerializer()).create();
		
		Attributes attr;
		BigInteger nonce1 = null;
		
		try {
			attr = verifyResponses(getProofSpec(), value, id);
		} catch(CredentialsException e) {
			e.printStackTrace();
			return "{\"response\": \"invalid\"}";
		}
		if( attr == null ) {
			return "{\"response\": \"invalid\"}";
		}

		// FIXME: setup the actual idemix issue specification
		IdemixIssueSpecification spec = IdemixIssueSpecification
				.fromIdemixIssuanceSpec(
						IRMASetup.ISSUER_PK_LOCATION,
						IRMASetup.CRED_STRUCT_ID,
						(short) 7);

		IdemixPrivateKey isk = new IdemixPrivateKey(IRMASetup.setupIssuerPrivateKey());

		// FIXME: retrieve proper attributes
		Attributes attributes = getIssuanceAttributes();
		IdemixCredentials ic = new IdemixCredentials();

		// Initialize the issuer
		Issuer issuer = new Issuer(isk.getIssuerKeyPair(), spec.getIssuanceSpec(),
				null, null, spec.getValues(attributes));

		// Run part one of protocol
		List<ProtocolCommand> commands;
		try {
			commands = ic.requestIssueRound1Commands(spec, attributes, issuer);
		} catch (CredentialsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"response\": \"invalid\", \"error\": \"}" + e.toString() + "\"";
		}

		// Save state, this is the nasty part
		try {
			Field nonce1Field = Issuer.class.getDeclaredField("nonce1");
			nonce1Field.setAccessible(true);
			nonce1 = (BigInteger) nonce1Field.get(issuer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		@SuppressWarnings("unchecked")
		Map<String, BigInteger> noncemap = (Map<String, BigInteger>) getContext()
				.getAttributes().get("noncemap");
		@SuppressWarnings("unchecked")
		Map<String, Attributes> attributemap = (Map<String, Attributes>) getContext()
				.getAttributes().get("attributemap");
		noncemap.put(id, nonce1);
		attributemap.put(id, attributes);
		
		IssuanceCommandSet ics = new IssuanceCommandSet();
		ics.commands = commands;
		ics.attributes = attributes;
		String path = getReference().getPath();
		ics.responseurl = path.substring(0, path.length() - 1) + "2";
		
		return gson.toJson(ics);
	}
	
	public String processIssuance(String value, String id) {
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(ProtocolCommand.class,
						new ProtocolCommandSerializer())
				.registerTypeAdapter(IResponseAPDU.class,
						new ResponseAPDUDeserializer()).create();
		
		Attributes attr;

		// FIXME: setup the actual idemix issue specification
		IdemixIssueSpecification spec = IdemixIssueSpecification
				.fromIdemixIssuanceSpec(
						IRMASetup.ISSUER_PK_LOCATION,
						IRMASetup.CRED_STRUCT_ID,
						(short) 7);

		IdemixPrivateKey isk = new IdemixPrivateKey(IRMASetup.setupIssuerPrivateKey());

		IdemixCredentials ic = new IdemixCredentials();

		@SuppressWarnings("unchecked")
		Map<String, BigInteger> noncemap = (Map<String, BigInteger>) getContext()
				.getAttributes().get("noncemap");
		@SuppressWarnings("unchecked")
		Map<String, Attributes> attributemap = (Map<String, Attributes>) getContext()
				.getAttributes().get("attributemap");
		BigInteger nonce1 = (BigInteger) noncemap.get(id);
		Attributes attributes = attributemap.get(id);

		// Initialize the issuer
		Issuer issuer = new Issuer(isk.getIssuerKeyPair(), spec.getIssuanceSpec(),
				null, null, spec.getValues(attributes));

		// Restore the state, this is the nasty part
		try {
			Field nonce1Field = Issuer.class.getDeclaredField("nonce1");
			nonce1Field.setAccessible(true);
			nonce1Field.set(issuer, nonce1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ProtocolResponses responses = gson.fromJson(value,
				ProtocolResponses.class);

		// Run part one of protocol
		List<ProtocolCommand> commands;
		try {
			// Run next part of protocol
			commands = ic.requestIssueRound3Commands(spec, attributes, issuer, responses);
		} catch (CredentialsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"response\": \"invalid\", \"error\": \"}" + e.toString() + "\"";
		}
		
		CommandSet cs = new CommandSet();
		cs.commands = commands;
		cs.responseurl = "";
		
		return gson.toJson(cs);
	}
	
    private Attributes getIssuanceAttributes() {
        // Return the attributes that have been revealed during the proof
        Attributes attributes = new Attributes();

        attributes.add("attr1", ATTRIBUTE_VALUE_1.toByteArray());
        attributes.add("attr2", ATTRIBUTE_VALUE_2.toByteArray());
        attributes.add("attr3", ATTRIBUTE_VALUE_3.toByteArray());
        attributes.add("attr4", ATTRIBUTE_VALUE_4.toByteArray());
        
        return attributes;
    }
	
}
