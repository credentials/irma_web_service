package org.irmacard.web.restapi.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.spec.IdemixIssueSpecification;
import org.irmacard.credentials.idemix.spec.IdemixVerifySpecification;
import org.irmacard.credentials.idemix.util.VerifyCredentialInformation;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolStep;
import org.irmacard.web.restapi.util.IssueCredentialInformation;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.QRResponse;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ProtocolResponses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.zurich.idmx.issuance.Issuer;


public class QRIssueStudentCredResource extends ProtocolResource {
	public static final String VERIFY_ISSUER = "IdemixLib";
	public static final String VERIFY_CRED_NAME = "CredStructCard4";
	public static final String VERIFY_SPEC_NAME = "default4";
	
    @SuppressWarnings("unused")
	private class IssuanceCommandSet {
		public List<ProtocolCommand> commands;
	    public String responseurl;
	    public Map<String,String> attributes;
	}
	
    @SuppressWarnings("unused")
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
		String baseURL;
		
		boolean useQR = isQROptionSet();

		if (id == null) {
			iround = 0;
			id = UUID.randomUUID().toString();

			ProtocolState.putState(id.toString(), "start");
			
			baseURL = getBaseURL() + getReference().getPath() + "/" + id.toString() + "/";
		} else {
			iround = Integer.parseInt(round);
			String path = getReference().getPath();
			baseURL = getBaseURL() + path.substring(0, path.lastIndexOf('/')+1);
		} 
		
		
		switch (iround) {
		case 0:
			if (useQR) {
				return createQRResponse(id);
			} else {
				ProtocolState.putState(id.toString(), "step1");
				return startVerify(getProofSpec(), value, id, baseURL + "1");
			}
		case 1:
			return processVerify(value, id);
		case 2:
			return processIssuance(value, id);
		case 3:
			return processIssuancEnd(value, id);
		}
		
		return null;
	}
	
	private boolean isQROptionSet() {
		try {
			return getReference().getQueryAsForm().getFirstValue("qr").equals("true");
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	@Get
	public Representation handleGet() {
		String id = (String) getRequestAttributes().get("id");
		String round = (String) getRequestAttributes().get("round");
		if (id == null) {
			return null;
		}
		if (round != null) {
			if (round.equals("qr")) {
				return generateQRImage(id,"0");
			} else if (round.equals("state")) {
				return generateState(id);
			} else if (round.equals("attributes")) {
				return getAttributes(id);
			}
			
		}
		return null;
	}
	
	public Representation getAttributes(String id) {
		Attributes attributes = ProtocolState.getAttributes(id);
		Map<String,String> attributesReadable = new HashMap<String,String>();
		for(String k : attributes.getIdentifiers()) {
			attributesReadable.put(k, new String(attributes.get(k)));
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return new StringRepresentation(gson.toJson(attributesReadable));
	}
	
	public String createQRResponse(String id) {
		QRResponse qrr = new QRResponse();
		qrr.qr_url = getReference().getPath() + "/" + id + "/qr";
		qrr.state_url = getReference().getPath() + "/" + id + "/state";
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				create();
		return gson.toJson(qrr);
	}
	
	public Representation generateState(String id) {
		String state = ProtocolState.getState(id);
		if (state != null) {
			if (state.equals("valid")) {
				return new StringRepresentation("{\"state\": \"" + state + "\", \"url\": \"http://spuitenenslikken.bnn.nl/\"}");
			} else {
				return new StringRepresentation("{\"state\": \"" + state + "\"}");
			}
		}
		return null;
	}
	
	
	public Representation generateQRImage(String id, String step) {
		String path = getReference().getPath();
		String qrURL = getBaseURL() + path.substring(0, path.lastIndexOf('/')+1) + step;
		 
		 ByteArrayOutputStream out = QRCode.from(qrURL).to(
	                ImageType.PNG).withSize(300, 300).stream();
		 byte[] data = out.toByteArray();
		 ObjectRepresentation<byte[]> or=new ObjectRepresentation<byte[]>(data, MediaType.IMAGE_PNG) {
		        @Override
		        public void write(OutputStream os) throws IOException {
		            super.write(os);
		            os.write(this.getObject());
		        }
		    };

		 return or; 		 
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

		ProtocolState.putState(id, "issueready");
		
		// FIXME: retrieve proper attributes
		Attributes attributes = getIssuanceAttributes(userID);
		
		IdemixCredentials ic = new IdemixCredentials(null);
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
		
		ProtocolState.putIssuer(id, issuer);
		ProtocolState.putNonce(id, nonce1);
		ProtocolState.putAttributes(id, attributes);
		
		String path = getReference().getPath();
		
		ProtocolStep ps = new ProtocolStep();
		ps.commands = commands;
		ps.responseurl = getBaseURL() + path.substring(0, path.length() - 1) + "2";
		ps.confirmationMessage = "Are you sure you want this credential to be issued to your IRMA card?";
		ps.askConfirmation = true;
		ps.usePIN = true;
		ps.protocolDone = false;
		ps.feedbackMessage = "Issuing credential (1)";
		
		return gson.toJson(ps);
	}
	
	public String processIssuance(String value, String id) {
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(ProtocolCommand.class,
						new ProtocolCommandSerializer())
				.registerTypeAdapter(ProtocolResponse.class,
						new ProtocolResponseDeserializer()).create();

		// FIXME: setup the actual idemix issue specification
		System.out.println("==== Setting up credential infromation ===");
		IdemixCredentials ic = new IdemixCredentials(null);
		IssueCredentialInformation ici = new IssueCredentialInformation("RU", "studentCard");

		System.out.println("=== Getting issuance information ===");
		IdemixIssueSpecification spec = ici.getIdemixIssueSpecification();

		
		BigInteger nonce1 = ProtocolState.getNonce(id);
		Attributes attributes = ProtocolState.getAttributes(id);

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
		issuer = ProtocolState.getIssuer(id);
		
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

		ProtocolState.putState(id, "issuing");
		
		String path = getReference().getPath();
		ProtocolStep cs = new ProtocolStep();
		cs.commands = commands;
		cs.responseurl = getBaseURL() + path.substring(0, path.length() - 1) + "3";
		cs.protocolDone = false;
		cs.feedbackMessage = "Issuing credential (2)";
		
		return gson.toJson(cs);
	}
	
	private String processIssuancEnd(String value, String id) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
		ProtocolState.putState(id, "issuingdone");
		ProtocolStep ps = new ProtocolStep();
		ps.feedbackMessage = "Issuance successful";
		ps.protocolDone = true;
		return gson.toJson(ps);
	}
	
    private Attributes getIssuanceAttributes(String id) {
        // Return the attributes that have been revealed during the proof
        Attributes attributes = new Attributes();
        
        if(id.equals("s112233@ru.nl")) {
    		attributes.add("university", "Radboud University".getBytes());
    		attributes.add("studentCardNumber", "081122337".getBytes());
    		attributes.add("studentID", "s112233".getBytes());
    		attributes.add("level", "PhD".getBytes());
    		attributes.add("expiry", "halfyear".getBytes());
        } else if(id.toLowerCase().equals("u012147@ru.nl")) {
        	attributes.add("university", "Radboud University".getBytes());
    		attributes.add("studentCardNumber", "081122336".getBytes());
    		attributes.add("studentID", "u012147".getBytes());
    		attributes.add("level", "PhD".getBytes());
    		attributes.add("expiry", "halfyear".getBytes());
        } else if(id.toLowerCase().equals("u921154@ru.nl")) {
        	attributes.add("university", "Radboud University".getBytes());
    		attributes.add("studentCardNumber", "2300921154".getBytes());
    		attributes.add("studentID", "u921154".getBytes());
    		attributes.add("level", "PhD".getBytes());
    		attributes.add("expiry", "halfyear".getBytes());
        } else {
			attributes.add("university", "Radboud University".getBytes());
			attributes.add("studentCardNumber", "0813371337".getBytes());
			attributes.add("studentID", "s1234567".getBytes());
			attributes.add("level", "PhD".getBytes());
    		attributes.add("expiry", "halfyear".getBytes());
        }
		
		return attributes;
	}
    
    private boolean eligibleForIssuance(String id) {
    	return id.toLowerCase().substring(0, 1).equals("s") ||
    			id.toLowerCase().equals("u012147@ru.nl") ||
    			id.toLowerCase().equals("u921154@ru.nl");
    }
}
