package org.irmacard.web.restapi.resources.irmaTube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMATubeVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMATube";
	final static String VERIFICATIONID = "memberType";

	public static final String AGE_STORE_NAME = "IRMATube.Age.Store";
	public static final int NO_AGE_VERIFIED = 0;
	VerificationDescription memberDescription;

	public IRMATubeVerificationResource() {
		try {
			memberDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();
		String age_str = ProtocolState.getIRMATubeAge(id);

		Attributes memberType = attrMap.get(VERIFICATIONID);

		if (memberType == null) {
			return ProtocolStep.newError(memberDescription.getName()
					+ " credential is invalid/expired");
		}

		if (!(new String(memberType.get("type"))).equals("regular")) {
			return ProtocolStep.newError("You need to be a member to view this content");
		}
		
		if (!age_str.equals("")) {
			Attributes age = attrMap.get("ageLowerOver" + age_str);
			VerificationDescription ageDescription = getVerificationDescription("ageLowerOver"
					+ age_str);

			if (age == null) {
				return ProtocolStep.newError(ageDescription.getName()
						+ " credential is invalid/expired");
			}
			if (!(new String(age.get("over" + age_str))).equals("yes")) {
				return ProtocolStep.newError("You need to be over " + age_str
						+ " to view this content");
			} else {
				// Verification successful
				System.out.println("Added something tot the age-store");
				getAgeStore().put(id, Integer.valueOf(age_str));
			}
		} else {
			System.out.println("No age necessary");
			getAgeStore().put(id, NO_AGE_VERIFIED);
		}

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + "/resources/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		String age_str = ProtocolState.getIRMATubeAge(id);
		if(age_str == null) {
			age_str = (String) getRequestAttributes().get("age");
			if (age_str == null
					|| (!age_str.equals("12") && 
						!age_str.equals("16") && 
						!age_str.equals("18")))
				age_str = "";
			ProtocolState.putIRMATubeAge(id, age_str);
		} else {
			System.out.println("Recovered age " + age_str + " for ID " + id);
		}
		System.out.println("Recovered age " + age_str + " for ID " + id);
		
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		if(!age_str.equals("")) {
			result.add(getVerificationDescription("ageLowerOver" + age_str));
			if(getVerificationDescription("ageLowerOver" + age_str) == null) {
				System.out.println("Could not load credential for... " + age_str);
			}
		}
		result.add(memberDescription);
		return result;
	}
	
	private VerificationDescription getVerificationDescription(String cred) {
		try {
			return DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, cred);
		} catch (InfoException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * After verification of age, the verified age is stored in this servlet
	 * so that it can be accessed by the FileServlet to verify whether it is
	 * allowed to serve this file.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Integer> getAgeStore() {
		Map<String, Integer> store = null;
		ServletContext ctxt = null;
		ServletContext other_ctxt = null;

		try {
			ctxt = (ServletContext) getContext().getServerDispatcher().getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		try {
			other_ctxt = (ServletContext) getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
		} catch (ClassCastException e) {
			e.printStackTrace();
		}

		// Try first context
		if(ctxt != null) {
			store = (Map<String, Integer>) ctxt.getAttribute(AGE_STORE_NAME);
		}

		// Try second context
		if(store == null && other_ctxt != null) {
			store = (Map<String, Integer>) other_ctxt.getAttribute(AGE_STORE_NAME);
		}

		// Still no store found, create it and put it in first context.
		if( store == null) {
			System.out.println("Store not found, generating new store");
			store = new HashMap<String, Integer>();
			ctxt.setAttribute(AGE_STORE_NAME, store);
		}

		return store;
	}
}
