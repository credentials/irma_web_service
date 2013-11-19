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
        
        if(id.equals("s112233@ru.nl")) {
    		attributes.put("university", "Radboud University");
    		attributes.put("studentCardNumber", "081122337");
    		attributes.put("studentID", "s112233");
    		attributes.put("level", "PhD");
    		attributes.put("expiry", "halfyear");
        } else if(id.toLowerCase().equals("u012147@ru.nl")) {
        	attributes.put("university", "Radboud University");
    		attributes.put("studentCardNumber", "081122336");
    		attributes.put("studentID", "u012147");
    		attributes.put("level", "PhD");
    		attributes.put("expiry", "halfyear");
        } else if(id.toLowerCase().equals("u921154@ru.nl")) {
        	attributes.put("university", "Radboud University");
    		attributes.put("studentCardNumber", "2300921154");
    		attributes.put("studentID", "u921154");
    		attributes.put("level", "PhD");
    		attributes.put("expiry", "halfyear");
        } else {
			attributes.put("university", "Radboud University");
			attributes.put("studentCardNumber", "0813371337");
			attributes.put("studentID", "s1234567");
			attributes.put("level", "PhD");
    		attributes.put("expiry", "halfyear");
        }
		
		return attributes;
	}
	
	public IssueCredentialInformation getIssueCredentialInformation(String cred) throws InfoException {
		return new IssueCredentialInformation(ISSUER, cred);
	}
}
