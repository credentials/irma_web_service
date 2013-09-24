package org.irmacard.web.restapi.resources.irmaWiki;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.web.restapi.resources.IssueBaseResource;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

public class IRMAWikiRegistrationIssueResource extends IssueBaseResource {
	final String ISSUER = "IRMAWiki";
	final String CREDENTIAL = "member";
	
	@Override
	public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id) {
		String nickName = null; // FIXME: get this from session
		
		Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();
		
		IssueCredentialInfo ici = new IssueCredentialInfo();
		Map<String,String> attributes = new HashMap<String,String>();

		ici.name = "Member Credential";
		attributes.put("type", "user");
		attributes.put("nick", nickName);
		ici.attributes = attributes;
		
		System.out.println("IRMAWiki: registered new user with nickname " + nickName);

		map.put(CREDENTIAL, ici);

		return map;
	}

	public IssueCredentialInformation getIssueCredentialInformation(String cred) {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
