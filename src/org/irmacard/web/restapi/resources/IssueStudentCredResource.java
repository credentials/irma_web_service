package org.irmacard.web.restapi.resources;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.login.CredentialException;

import net.sourceforge.scuba.smartcards.IResponseAPDU;

import org.irmacard.web.restapi.IRMASetup;

import org.irmacard.web.restapi.util.CommandSet;
import org.irmacard.web.restapi.util.IssueCredentialInformation;
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
import credentials.idemix.util.VerifyCredentialInformation;

import credentials.Attributes;

import service.ProtocolCommand;
import service.ProtocolResponses;

public class IssueStudentCredResource extends ProtocolResource {
	public static final String VERIFY_ISSUER = "IdemixLib";
	public static final String VERIFY_CRED_NAME = "CredStructCard4";
	public static final String VERIFY_SPEC_NAME = "default4";
	
	private class IssuanceCommandSet {
	    public List<ProtocolCommand> commands;
	    public String responseurl;
	    public Map<String,String> attributes;
	}
	
	private class IssueError {
		public String status;
		public String message;
		public IssueError(String message) {
			status = "error";
			this.message = message;
		}
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
		VerifyCredentialInformation vci = new VerifyCredentialInformation("Surfnet", "root", "RU", "rootID");
		return vci.getIdemixVerifySpecification();
	}
	
	public String processVerify(String value, String id) {
		Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(ProtocolCommand.class,
				new ProtocolCommandSerializer()).create();
		
		Attributes attr = null;
		BigInteger nonce1 = null;
		
		try {
			attr = verifyResponses(getProofSpec(), value, id);
		} catch(CredentialsException e) {
			e.printStackTrace();
			return gson.toJson(new IssueError("Invalid root credential"));
		}
		if( attr == null) {
			return gson.toJson(new IssueError("Invalid root credential"));
		}
		
		String userID = new String(attr.get("http://www.irmacard.org/credentials/phase1/Surfnet/root/structure.xml;someRandomName;userID"));
		
		// Check if eligible
		if(! eligibleForIssuance(userID)){
			return gson.toJson(new IssueError("ID " + userID + " is not eligible"));
		}

		// FIXME: retrieve proper attributes
		Attributes attributes = getIssuanceAttributes(userID);
		
		IdemixCredentials ic = new IdemixCredentials();
		IssueCredentialInformation ici = new IssueCredentialInformation("RU", "studentCard");
		IdemixIssueSpecification spec = ici.getIdemixIssueSpecification();

		// Initialize the issuer
		Issuer issuer = ici.getIssuer(attributes);

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
		@SuppressWarnings("unchecked")
		Map<String, Issuer> issuermap = (Map<String, Issuer>) getContext()
				.getAttributes().get("issuermap");
		issuermap.put(id, issuer);
		noncemap.put(id, nonce1);
		attributemap.put(id, attributes);
		
		Map<String,String> attributesReadable = new HashMap<String,String>();
		for(String k : attributes.getIdentifiers()) {
			attributesReadable.put(k, new String(attributes.get(k)));
		}
		
		IssuanceCommandSet ics = new IssuanceCommandSet();
		ics.commands = commands;
		ics.attributes = attributesReadable;
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

		// FIXME: setup the actual idemix issue specification
		System.out.println("==== Setting up credential infromation ===");
		IdemixCredentials ic = new IdemixCredentials();
		IssueCredentialInformation ici = new IssueCredentialInformation("RU", "studentCard");

		System.out.println("=== Getting issuance information ===");
		IdemixIssueSpecification spec = ici.getIdemixIssueSpecification();

		@SuppressWarnings("unchecked")
		Map<String, BigInteger> noncemap = (Map<String, BigInteger>) getContext()
				.getAttributes().get("noncemap");
		@SuppressWarnings("unchecked")
		Map<String, Attributes> attributemap = (Map<String, Attributes>) getContext()
				.getAttributes().get("attributemap");
		@SuppressWarnings("unchecked")
		Map<String, Issuer> issuermap = (Map<String, Issuer>) getContext()
				.getAttributes().get("issuermap");
		BigInteger nonce1 = (BigInteger) noncemap.get(id);
		Attributes attributes = attributemap.get(id);

		// Initialize the issuer
		System.out.println("=== Getting issuer ===");
		Issuer issuer = ici.getIssuer(attributes);

		// Restore the state, this is the nasty part
		try {
			Field nonce1Field = Issuer.class.getDeclaredField("nonce1");
			nonce1Field.setAccessible(true);
			nonce1Field.set(issuer, nonce1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// FIXME: superfluous?
		issuer = issuermap.get(id);
		
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
	
    private Attributes getIssuanceAttributes(String id) {
        // Return the attributes that have been revealed during the proof
        Attributes attributes = new Attributes();
        
        if(id.equals("s112233@ru.nl")) {
    		attributes.add("university", "Radboud University".getBytes());
    		attributes.add("studentCardNumber", "081122337".getBytes());
    		attributes.add("studentID", "s112233".getBytes());
    		attributes.add("level", "PhD".getBytes());
        } else {
			attributes.add("university", "Radboud University".getBytes());
			attributes.add("studentCardNumber", "0813371337".getBytes());
			attributes.add("studentID", "s1234567".getBytes());
			attributes.add("level", "PhD".getBytes());
        }
		
		return attributes;
	}
    
    private boolean eligibleForIssuance(String id) {
    	return id.toLowerCase().substring(0, 1).equals("s");
    }
}
