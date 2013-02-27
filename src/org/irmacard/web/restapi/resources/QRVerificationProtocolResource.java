package org.irmacard.web.restapi.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.Nonce;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.IdemixNonce;
import org.irmacard.credentials.idemix.spec.IdemixVerifySpecification;
import org.irmacard.credentials.idemix.util.VerifyCredentialInformation;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolStep;
import org.irmacard.web.restapi.util.ProtocolCommandSerializer;
import org.irmacard.web.restapi.util.ProtocolResponseDeserializer;
import org.irmacard.web.restapi.util.QRResponse;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.smartcards.ProtocolResponses;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Resource for the verification protocol that uses QR for out-of-band 
 * authentication using a NFC smartphone. For now a separate resource to make sure existing 
 * things do not break.
 * 
 * @author Maarten Everts
 *
 */
public class QRVerificationProtocolResource extends ServerResource {
	private final String ISSUER = "MijnOverheid";
	private final String CRED_NAME = "ageLower";
	private final String VERIFIER = "UitzendingGemist";
	private final String SPEC_NAME = "ageLowerOver16";


	@Post("json")
	public String handlePost (String value) {
		Integer crednr = Integer.parseInt((String) getRequestAttributes().get("crednr"));
		String id = (String) getRequestAttributes().get("id");
		String round = (String) getRequestAttributes().get("round");
		if (id == null) {
			return step0qr(crednr,value);
		} else if (round != null && round.equals("1")) {
			return step1qr(crednr,value,id);
		} else if (round != null && round.equals("2")) {
			return step2qr(crednr,value,id);
		}
		return null;
	}
	
	@Get
	public Representation handleGet() {
		Integer crednr = Integer.parseInt((String) getRequestAttributes().get("crednr"));
		String id = (String) getRequestAttributes().get("id");
		String round = (String) getRequestAttributes().get("round");
		if (id == null) {
			return null;
		}
		if (round != null) {
			if (round.equals("qr")) {
				return generateQRImage(crednr,id,"1");
			} else if (round.equals("state")) {
				return generateState(crednr,id);
			}
			
		}
		return null;
	}

	public Representation generateState(int crednr, String id) {
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
	
	public String getBaseURL() {
		return getReference().getScheme() +"://" + getReference().getHostDomain() + ":" + getReference().getHostPort();
	}
	
	public Representation generateQRImage(int crednr, String id, String step) {
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

	
	/**
	 * Start new verification protocol
	 * @param crednr credential number
	 * @param value request body
	 * @return
	 */
	public String step0qr(int crednr, String value) {
		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();
		QRResponse qrr = new QRResponse();
		
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				create();

		try {
			Nonce nonce = ic.generateNonce(vspec);
			
			// Save the state, use random id as key
			UUID id = UUID.randomUUID();
			BigInteger intNonce = ((IdemixNonce)nonce).getNonce();
			
			ProtocolState.putNonce(id.toString(), intNonce);
			ProtocolState.putState(id.toString(), "start");
			
			qrr.qr_url = getReference().getPath() + "/" + id.toString() + "/qr";
			qrr.state_url = getReference().getPath() + "/" + id.toString() + "/state";
			return gson.toJson(qrr);
			
		} catch (CredentialsException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	

	/**
	 * Handle the next step of the verification protocol.
	 * @param crednr credential number
	 * @param value request body (with the card responses)
	 * @param verificationId 
	 * @return
	 */
	public String step1qr(int crednr, String value, String id) {
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				registerTypeAdapter(ProtocolCommand.class, new ProtocolCommandSerializer()).
				create();
		
		// Get the nonce based on the id
		BigInteger intNonce = ProtocolState.getNonce(id);
		
		ProtocolState.putState(id.toString(), "step1");
		
		IdemixNonce nonce = new IdemixNonce(intNonce);
		
		ProtocolResponses responses = gson.fromJson(value, ProtocolResponses.class);		

		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();

		ProtocolStep cs = new ProtocolStep();
		try {
			cs.commands = ic.requestProofCommands(vspec, nonce);
			String path = getReference().getPath();
			
			cs.responseurl = getBaseURL() + path.substring(0, path.lastIndexOf('/')+1) + "2";
			return gson.toJson(cs);
		} catch (CredentialsException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Handle the next step of the verification protocol.
	 * @param crednr credential number
	 * @param value request body (with the card responses)
	 * @param id 
	 * @return
	 */
	public String step2qr(int crednr, String value, String id) {
		Gson gson = new GsonBuilder().
				setPrettyPrinting().
				registerTypeAdapter(ProtocolResponse.class, new ProtocolResponseDeserializer()).
				create();
		
		// Get the nonce based on the id

		BigInteger intNonce = ProtocolState.getNonce(id);
		IdemixNonce nonce = new IdemixNonce(intNonce);
		
		ProtocolResponses responses = gson.fromJson(value, ProtocolResponses.class);		

		VerifyCredentialInformation vci = new VerifyCredentialInformation(
				ISSUER, CRED_NAME, VERIFIER, SPEC_NAME);
		IdemixCredentials ic = new IdemixCredentials(null);
		IdemixVerifySpecification vspec = vci.getIdemixVerifySpecification();

		ProtocolStep ps = new ProtocolStep();
		ps.protocolDone = true;
		ps.result = "invalid";
		
		try {
			Attributes attr = ic.verifyProofResponses(vspec, nonce, responses);
			// TODO: do something with the results!
			if (attr != null) {
				ps.result = "valid";
				ps.data = "http://spuitenenslikken.bnn.nl";
				attr.print();
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		ProtocolState.putState(id.toString(), ps.result);
		return gson.toJson(ps);

	}	
}
