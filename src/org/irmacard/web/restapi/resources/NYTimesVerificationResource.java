package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.util.ProtocolStep;

public class NYTimesVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "NYTimes";
	final static String VERIFICATIONID = "ageLowerOver12";

	VerificationDescription ageDescription;

	public NYTimesVerificationResource() {
		try {
			ageDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes age = attrMap.get(VERIFICATIONID);

		if (age == null) {
			return ProtocolStep.newError(ageDescription.getName()
					+ " credential is invalid/expired");
		}

		if (!(new String(age.get("over12"))).equals("yes")) {
			return ProtocolStep.newError("You need to be over 12 to view this content");
		}

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = "http://www.nytimes.com";
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(ageDescription);
		return result;
	}

}
