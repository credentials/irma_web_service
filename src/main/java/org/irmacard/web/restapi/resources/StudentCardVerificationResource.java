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

public class StudentCardVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "RU";
	final static String VERIFICATIONID = "rootID";

	VerificationDescription rootIDDesc;

	public StudentCardVerificationResource() {
		try {
			rootIDDesc = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes attr = attrMap.get(VERIFICATIONID);

		if (attr == null) {
			return ProtocolStep.newError(rootIDDesc.getName()
					+ " credential is invalid/expired");
		}

		String userID = new String(attr.get("userID"));
		
		// Check if eligible
		if(! eligibleForIssuance(userID)){
			return ProtocolStep.newError("ID " + userID + " is not eligible");
		}
		
		ProtocolState.putStudentCardUUID(id, userID);

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + "/protocols/issue/StudentCard/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(rootIDDesc);
		return result;
	}

    private boolean eligibleForIssuance(String id) {
    	return id.toLowerCase().startsWith("s");
    }
}
