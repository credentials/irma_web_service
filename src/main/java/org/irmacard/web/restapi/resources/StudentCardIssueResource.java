package org.irmacard.web.restapi.resources;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.util.IssueCredentialInformation;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

public class StudentCardIssueResource extends IssueBaseResource {
	final String ISSUER = "RU";
	final String CREDENTIAL = "studentCard";

	@Override
	public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id, String value) {
		String userID = ProtocolState.getStudentCardUUID(id);

		Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();
		IssueCredentialInfo ici = new IssueCredentialInfo();
		ici.name = "Student Card";
		ici.attributes = getIssuanceAttributes(userID);
		map.put(CREDENTIAL, ici);

		return map;
	}
	
    private Map<String, String> getIssuanceAttributes(String id) {
        // Return the attributes that have been revealed during the proof
        Map<String,String> attributes = new HashMap<String,String>();

        int sep_idx = id.indexOf("@");
        String student_id = id.substring(1, sep_idx);
        String host = id.substring(sep_idx + 1);

        String university;

        System.out.println("Student: " + student_id + " hosted by " + host);
        if(host.equals("ru.nl")) {
        	university = "Radboud University";
        } else if (host.equals("student.tue.nl")) {
        	university = "TU Eindhoven";
        } else if (host.equals("utwente.nl")) {
        	university = "University of Twente";
        } else {
        	university = "Unknown";
        }

    	attributes.put("university", university);
		attributes.put("studentCardNumber", "Unknown");
		attributes.put("studentID", student_id);
		attributes.put("level", "master");
		return attributes;
	}

	public IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
