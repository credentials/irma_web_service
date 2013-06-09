package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMATubeVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMATube";
	VerificationDescription memberDescription;

	public IRMATubeVerificationResource() {
		try {
			
			memberDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, "memberType");
		} catch (InfoException e) {
			e.printStackTrace();
		}

	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();
		String age_str = ProtocolState.getIRMATubeAge(id);

		Attributes memberType = attrMap.get("memberType");

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
			}
		}

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = "http://spuitenenslikken.bnn.nl";
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
}
