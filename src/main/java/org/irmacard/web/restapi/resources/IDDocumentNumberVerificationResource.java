package org.irmacard.web.restapi.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IDDocumentNumberVerificationResource extends
		VerificationBaseResource {
    final static String VERIFIER = "MijnOverheid";
    final static String VERIFICATIONID = "idDocumentNumber";

    VerificationDescription rootDescription;

	public IDDocumentNumberVerificationResource() {
		try {
			rootDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();
        Attributes document = attrMap.get(VERIFICATIONID);

        if (document == null) {
			return ProtocolStep.newError(rootDescription.getName()
					+ " credential is invalid/expired");
		}

        String documentNr = new String(document.get("number"));

        ps.protocolDone = true;
        ps.status = "success";
        ps.feedbackMessage = "Document succesfully verified";
        ps.result = documentNr;
        return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(rootDescription);

		return result;
	}
}
