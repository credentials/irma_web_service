package org.irmacard.web.restapi.resources.irmaTube;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMATubeRegistrationVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMATube";
	final static String VERIFICATION_ID_AGE = "ageLowerNone";

	VerificationDescription ageDesc;

	public IRMATubeRegistrationVerificationResource() {
		try {
			ageDesc = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATION_ID_AGE);
			System.out.println("Age credential: " + ageDesc);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes age = attrMap.get(VERIFICATION_ID_AGE);

		if (age == null) {
			return ProtocolStep.newError(ageDesc.getName()
					+ " credential is invalid/expired");
		}

		ProtocolState.putIRMATubeAge(id, "");

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + "/protocols/issue/IRMATubeRegistration/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		System.out.println("Age dsescription: " + ageDesc);
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(ageDesc);
		return result;
	}
}
