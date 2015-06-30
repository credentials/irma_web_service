package org.irmacard.web.restapi.resources.irmaWiki.data;

import org.irmacard.credentials.Attributes;

public class IRMAWikiVerificationData {
	public String nickname;
	public String realname;
	public String email;
	public String type = "user";
	public IRMAWikiVerificationData(Attributes attributes) {
		byte[] attr = attributes.get("nickname");
		if (attr != null) nickname = new String(attr);
		attr = attributes.get("realname");
		if (attr != null) realname = new String(attr);
		attr = attributes.get("type");
		if (attr != null) type = new String(attr);
		attr = attributes.get("email");
		if (attr != null) email = new String(attr);
	}
	public String toString() {
		return "type: " + type + "; nickname: " + nickname + "; realname: " + realname + "; email: " + email; 
	}
}
