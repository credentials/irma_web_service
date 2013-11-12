package org.irmacard.web.restapi.resources.irmaWiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMAWikiRegistrationVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMAWiki";
	final static String VERIFICATION_ID = "surfnetRootNone";

	VerificationDescription rootDesc;

	public IRMAWikiRegistrationVerificationResource() {
		try {
			rootDesc = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATION_ID);
			System.out.println("Root credential: " + rootDesc);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + "/protocols/issue/IRMAWikiRegistration/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		System.out.println("Root description: " + rootDesc);
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(rootDesc);
		return result;
	}
}
