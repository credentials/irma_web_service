package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMATubeVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMATube";
	VerificationDescription ageDescription;
	VerificationDescription memberDescription;

	public IRMATubeVerificationResource() {
		try {
			ageDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, "ageLowerOver16");
			memberDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, "memberType");
		} catch (InfoException e) {
			e.printStackTrace();
		}

	}

	@Override
	public ProtocolStep onSuccess(Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes age = attrMap.get("ageLowerOver16");
		Attributes memberType = attrMap.get("memberType");

		if (age == null) {
			return ProtocolStep.newError(ageDescription.getName()
					+ " credential is invalid/expired");
		}

		if (memberType == null) {
			return ProtocolStep.newError(memberDescription.getName()
					+ " credential is invalid/expired");
		}

		if (!(new String(age.get("over16"))).equals("yes")) {
			return ProtocolStep.newError("You need to be over 16 to view this content");
		}

		if (!(new String(memberType.get("type"))).equals("regular")) {
			return ProtocolStep.newError("You need to be a member to view this content");
		}

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = "http://spuitenenslikken.bnn.nl";
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications() {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(ageDescription);
		result.add(memberDescription);
		return result;
	}

}
