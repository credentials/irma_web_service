package org.irmacard.web.restapi.util;

import java.net.URI;

import org.irmacard.credentials.idemix.spec.IdemixVerifySpecification;


public class VerifyCredentialInformation extends CredentialInformation {
	private URI proofSpecLocation;
	
	public VerifyCredentialInformation(String issuer, String credName, String verifySpecName) {
		super(issuer, credName);
		
		proofSpecLocation = baseLocation.resolve("Verifies/" + verifySpecName + "/specification.xml");
	}
	
	public IdemixVerifySpecification getIdemixVerifySpecification() {
		return IdemixVerifySpecification.fromIdemixProofSpec(proofSpecLocation, credNr);
	}
}
