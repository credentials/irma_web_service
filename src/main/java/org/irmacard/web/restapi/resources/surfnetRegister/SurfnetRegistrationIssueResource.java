package org.irmacard.web.restapi.resources.surfnetRegister;

import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.idemix.descriptions.IdemixCredentialDescription;
import org.irmacard.credentials.info.CredentialDescription;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.resources.IssueBaseResource;
import org.irmacard.web.restapi.util.IssueCredentialInfo;

public class SurfnetRegistrationIssueResource extends IssueBaseResource {
    final String ISSUER = "Surfnet";
    final String CREDENTIAL = "eventRegistration";

    @Override
    public Map<String, IssueCredentialInfo> getIssueCredentialInfos(String id,
            String value) {
        Map<String, IssueCredentialInfo> map = new HashMap<String, IssueCredentialInfo>();

        // Test if user with this id is indeed known
        RegistrationStore store;
        try {
            store = RegistrationStore.getStore();
            if (!store.isRegistrationTokenKnown(id)) {
                return map;
            }
        } catch (RegistrationException e) {
            e.printStackTrace();
            return map;
        }

        IssueCredentialInfo ici = new IssueCredentialInfo();
        Map<String, String> attributes = new HashMap<String, String>();

        ici.name = "Event Registration";
        attributes.put("event", "IRMA meeting");
        attributes.put("date", "November 6, 2015");
        ici.attributes = attributes;

        map.put(CREDENTIAL, ici);

        return map;
    }

    public IdemixCredentialDescription getIdemixCredentialDescription(String cred)
            throws InfoException {
        DescriptionStore ds = DescriptionStore.getInstance();
        CredentialDescription cd = ds.getCredentialDescriptionByName(ISSUER, cred);
        return new IdemixCredentialDescription(cd);
    }
}
