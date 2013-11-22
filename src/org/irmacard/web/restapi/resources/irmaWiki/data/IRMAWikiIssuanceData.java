package org.irmacard.web.restapi.resources.irmaWiki.data;

public class IRMAWikiIssuanceData {
	public String nickname;
	public String realname;
	public String email;
	public String toString() {
		return "nickname: " + nickname + "; realname: " + realname + "; email: " + email; 
	}
}
