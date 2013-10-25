package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.util.ProtocolStep;

public class MijnOverheidVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "MijnOverheid";
	final static String VERIFICATIONID = "rootAll";

	VerificationDescription bsnDesc;

	public MijnOverheidVerificationResource() {
		try {
			bsnDesc = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes bsn = attrMap.get("rootAll");

		if (bsn == null) {
			return ProtocolStep.newError(bsnDesc.getName()
					+ " credential is invalid/expired");
		}

		// This is where you would verify BSN and link it to what you already have in this session.
		// TODO: provide proper implementation of the following
		if ((new String(bsn.get("BSN"))).equals("1234567")) {
			return ProtocolStep.newError("You need to be over 16 to view this content");
		}

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = getBaseURL() + "/protocols/issue/MijnOverheid/" + (String) getRequestAttributes().get("id");
		//ps.result = makeResponseURL((String) getRequestAttributes().get("id"), 0);
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(bsnDesc);
		return result;
	}

}
