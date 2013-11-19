package org.irmacard.web.restapi.resources;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

public class MijnOverheidIssueResource extends IssueBaseResource {
	final String ISSUER = "MijnOverheid";

	@Override
	public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id, String value) {
		Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();
		Map<String, String> attr1 = new HashMap<String, String>();
		Map<String, String> attr2 = new HashMap<String, String>();

		IssueCredentialInfo ici1 = new IssueCredentialInfo();
		attr1.put("firstnames", "Johan Pieter");
		attr1.put("firstname", "Johan");
		attr1.put("familyname", "Stuivezand");
		attr1.put("prefix", "van");
		ici1.name = "Full Name";
		ici1.attributes = attr1;
		
		IssueCredentialInfo ici2 = new IssueCredentialInfo();
		attr2.put("dateofbirth", "29-2-2004");
		attr2.put("placeofbirth", "Stuivezand");
		attr2.put("countryofbirth", "Nederland");
		attr2.put("gender", "male");
		ici2.name = "Birth Certificate";
		ici2.attributes = attr2;
		
		map.put("fullName", ici1);
		map.put("birthCertificate", ici2);
		
		return map;
	}
	
	public IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
