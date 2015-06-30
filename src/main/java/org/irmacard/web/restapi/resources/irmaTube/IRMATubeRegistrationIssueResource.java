package org.irmacard.web.restapi.resources.irmaTube;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.resources.IssueBaseResource;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

public class IRMATubeRegistrationIssueResource extends IssueBaseResource {
	final String ISSUER = "IRMATube";
	final String CREDENTIAL = "member";
	
	private static final long ID_SIZE = 10000000000l;

	@Override
	public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id, String value) {
		String age = ProtocolState.getIRMATubeAge(id);
		String userID = (new Long((long) (Math.random()*ID_SIZE))).toString();
		System.out.println("Some randomness: " + Math.random() + " " + Math.random());
		System.out.println("Some randomness: " + Math.random() * ID_SIZE + " " + Math.random());
		System.out.println("Some more randomness testing: " + 0.1 * ID_SIZE);
		System.out.println("New userId generated: " + userID);
		
		if(age == null) {
			return null;
		}

		Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();
		
		IssueCredentialInfo ici = new IssueCredentialInfo();
		Map<String,String> attributes = new HashMap<String,String>();

		ici.name = "Member Credential";
		attributes.put("type", "regular");
		attributes.put("id", userID);
		ici.attributes = attributes;
		
		System.out.println("IRMATube: registered new user with id " + userID);

		map.put(CREDENTIAL, ici);

		return map;
	}

	public IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
