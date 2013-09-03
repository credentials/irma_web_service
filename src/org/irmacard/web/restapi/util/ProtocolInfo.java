package org.irmacard.web.restapi.util;

import java.util.Map;

public class ProtocolInfo {
	public String qr_url;
	public String status_url;
	public Map<Short, String> verification_names;
	public Map<String, IssueCredentialInfo> issue_information;
}
