package org.irmacard.web.restapi.resources.irmaWiki;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.resources.IssueBaseResource;
import org.irmacard.web.restapi.resources.irmaWiki.data.IRMAWikiIssuanceData;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IRMAWikiRegistrationIssueResource extends IssueBaseResource {
	final String ISSUER = "IRMAWiki";
	final String CREDENTIAL = "member";
	
	public class Names {
		public String nickname;
		public String realname;
		public String email;
		public Names() {}
	}

	@Override
	public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id, String value) {
		IRMAWikiIssuanceData data = ProtocolState.getIRMAWikiData(id);
		if (data == null) {
			data = new IRMAWikiIssuanceData();
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (value != null) {
			Names names = gson.fromJson(value, Names.class);
			if (names != null && names.nickname != null) {
				data.nickname = names.nickname;
				data.realname = names.realname;
				data.email = names.email;
			}
		}
		
		if (data.nickname == null) {
			return null;
		}
		if (data.realname == null || data.realname.isEmpty()) {
			data.realname = " ";
		}
		if (data.email == null || data.email.isEmpty()) {
			data.email = " ";
		}
		Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();
		
		IssueCredentialInfo ici = new IssueCredentialInfo();
		Map<String,String> attributes = new HashMap<String,String>();

		ici.name = "Member Credential";
		attributes.put("type", "user");
		attributes.put("nickname", data.nickname);
		attributes.put("realname", data.realname);
		attributes.put("email", data.email);
		ici.attributes = attributes;
		
		System.out.println("IRMAWiki: registered new user with nickname " + data.nickname + "(" + data.realname + ")");

		map.put(CREDENTIAL, ici);

		ProtocolState.putIRMAWikiData(id, data);
		return map;
	}

	public IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
