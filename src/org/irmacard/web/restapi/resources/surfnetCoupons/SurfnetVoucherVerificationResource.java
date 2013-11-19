package org.irmacard.web.restapi.resources.surfnetCoupons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class SurfnetVoucherVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "Surfnet";
	final static String VERIFICATIONID = "rootVoucher";

	public static final String AGE_STORE_NAME = "IRMATube.Age.Store";
	public static final int NO_AGE_VERIFIED = 0;
	VerificationDescription rootDescription;

	public SurfnetVoucherVerificationResource() {
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
		Attributes rootID = attrMap.get(VERIFICATIONID);

		if (rootID == null) {
			return ProtocolStep.newError(rootDescription.getName()
					+ " credential is invalid/expired");
		}

		// TODO: test here for eligible users
		String userID = new String(rootID.get("userID"));
		if (!userID.equals("u921154@ru.nl")) {
			return ProtocolStep.newError("Only the test-user can get a voucher");
		}
		
		// Verification successful
		String voucher = "";
		try {
			voucher = VoucherStore.getStore().getVoucherCode(
					userID);
		} catch (VoucherException e) {
			e.printStackTrace();
			ps.protocolDone = true;
			ps.status = "failure";
			ps.result = e.getMessage();
			return ps;
		}
		
		// No errors occurred
		System.out.println("Gotten voucher " + voucher + " from database for " + userID);

		ps.protocolDone = true;
		ps.status = "success";
		ps.result = voucher;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(rootDescription);
		
		return result;
	}
}
