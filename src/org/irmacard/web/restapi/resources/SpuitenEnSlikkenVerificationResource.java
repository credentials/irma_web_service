package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.util.ProtocolStep;

public class SpuitenEnSlikkenVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "UitzendingGemist";
	final static String VERIFICATIONID = "ageLowerOver16";
	

	@Override
	public ProtocolStep onSuccess(Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();
		ps.protocolDone = true;
		ps.status = "success";
		ps.result = "http://spuitenenslikken.bnn.nl";
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications() {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		try {
			result.add(DescriptionStore.getInstance().getVerificationDescriptionByName(VERIFIER, VERIFICATIONID));
		} catch (InfoException e) {
			e.printStackTrace();
		}
		return result;
	}

}
