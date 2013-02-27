package org.irmacard.web.restapi.resources;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ProtocolResponses;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.spec.IdemixIssueSpecification;
import org.irmacard.credentials.idemix.spec.IdemixVerifySpecification;
import org.irmacard.credentials.idemix.util.VerifyCredentialInformation;
import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.ProtocolStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.zurich.idmx.issuance.Issuer;

public class IssueStudentCredResource  extends ProtocolBaseResource {
	private VerifyCredentialInformation vci = new VerifyCredentialInformation("Surfnet", "root", "RU", "rootID");
	
	@Override
	public String handleProtocolStep(String id, int step, String value) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ProtocolCommand.class,
					new ProtocolCommandSerializer()).create();
		
		ProtocolStep ps = null;
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
		
		switch (step) {
		case 0:
			ps = VerificationProtocolResource.createVerificationProtocolStep(id, vspec);
			ps.responseurl = makeResponseURL(step+1);
			break;
		case 1:
			ps = createIssuanceProtocolStep1(id, value);
			ps.responseurl = makeResponseURL(step + 1);
			break;
		case 2:
			ps = createIssuanceProtocolStep2(id, value);
			ps.responseurl = makeResponseURL(step + 1);
			break;
		case 3:
			ps = createIssuanceProtocolStepEnd(id, value);
			break;			
		default:
			break;
		}
		return gson.toJson(ps);
	}
	
	private ProtocolStep createIssuanceProtocolStep1(String id, String value) {
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
		Attributes attr;
		try {
			attr = VerificationProtocolResource.processVerificationResponse(id, vspec, value);
		} catch (CredentialsException e) {
			e.printStackTrace();
			return ProtocolStep.newError("Invalid root credential.");
		}
		if (attr == null) {
			return ProtocolStep.newError("Invalid root credential.");
		}
		
		String userID = new String(attr.get("http://www.irmacard.org/credentials/phase1/Surfnet/root/structure.xml;someRandomName;userID"));
		
		// Check if eligible
		if(! eligibleForIssuance(userID)){
			return ProtocolStep.newError("ID " + userID + " is not eligible");
		}

		
		// FIXME: retrieve proper attributes
		Attributes attributes = getIssuanceAttributes(userID);
		
		ProtocolState.putStatus(id, "issueready");
		
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
			return ProtocolStep.newError("Error while issuing.");
		}
		
		// Save state, this is the nasty part
		BigInteger nonce1 = null;
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
		
		ProtocolStep ps = new ProtocolStep();
		ps.commands = commands;
		ps.confirmationMessage = "Are you sure you want this credential to be issued to your IRMA card?";
		ps.askConfirmation = true;
		ps.usePIN = true;
		ps.protocolDone = false;
		ps.feedbackMessage = "Issuing credential (1)";
		
		return ps;
		
	}

	private ProtocolStep createIssuanceProtocolStep2(String id, String value) {
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
			e.printStackTrace();
			return ProtocolStep.newError("Error while issuing.");
		}

		ProtocolState.putStatus(id, "issuing");

		ProtocolStep ps = new ProtocolStep();
		ps.commands = commands;
		ps.protocolDone = false;
		ps.feedbackMessage = "Issuing credential (2)";

		return ps;
	}
	
	private ProtocolStep createIssuanceProtocolStepEnd(String id, String value) {
		// TODO: actually check what is sent
		ProtocolState.putStatus(id, "success");
		ProtocolStep ps = new ProtocolStep();
		ps.feedbackMessage = "Issuance successful";
		ps.protocolDone = true;
		ps.status = "success";
		return ps;
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
