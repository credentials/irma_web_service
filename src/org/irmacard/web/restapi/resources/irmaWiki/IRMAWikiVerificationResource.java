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

public class IRMAWikiVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMAWiki";
	final static String VERIFICATIONID = "memberAll";

	VerificationDescription memberDescription;

	public IRMAWikiVerificationResource() {
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

		Attributes member = attrMap.get(VERIFICATIONID);

		if (member == null) {
			return ProtocolStep.newError(memberDescription.getName()
					+ " credential is invalid/expired");
		}

		if (!(new String(member.get("type"))).equals("user")) {
			return ProtocolStep.newError("You need to be a member to modify this content");
		}
		
		// FIXME: store nickName + type + OK in session.

		String servletURL = getRootRef().getPath();
		String serviceURL = servletURL.substring(0,servletURL.lastIndexOf('/'));

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + serviceURL + "/resources/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		System.out.println("Member description: " + memberDescription);
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(memberDescription);
		return result;
	}
}
