package org.irmacard.web.restapi.resources.surfnetRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SurfnetRegistrationVerificationResource extends VerificationBaseResource {
    final static String VERIFIER = "Surfnet";
    final static String VERIFICATIONID = "registerName";

    VerificationDescription registerNameDesc;

    public SurfnetRegistrationVerificationResource() {
        try {
            registerNameDesc = DescriptionStore.getInstance()
                    .getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
        } catch (InfoException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
        ProtocolStep ps = new ProtocolStep();

        Attributes attr = attrMap.get(VERIFICATIONID);

        if (attr == null) {
            return ProtocolStep.newError(
                    registerNameDesc.getName() + " credential is invalid/expired");
        }

        String firstName = new String(attr.get("firstname"));
        String prefix = new String(attr.get("prefix"));
        String familyName = new String(attr.get("familyname"));

        String name;
        if (prefix.equals(" ")) {
            name = firstName + " " + familyName;
        } else {
            name = firstName + " " + prefix + " " + familyName;
        }

        boolean lunch = ((String) getRequestAttributes().get("lunch")).equals("yes");

        System.out.println("Gotton: " + name);
        System.out.println("Lunch request: " + lunch);

        String uuid = "";
        try {
            RegistrationStore store = RegistrationStore.getStore();
            uuid = store.getUserRegistrationToken(name);

            if (uuid != null) {
                System.out.println("User already registered, updating");
                store.updateUser(uuid, name, lunch);
            } else {
                System.out.println("User will now be registered");
                uuid = store.registerUser(name, lunch);
            }
        } catch (RegistrationException e) {

        }

        ps.protocolDone = true;
        ps.status = "success";
        ps.feedbackMessage = name;

        HashMap<String, String> results = new HashMap<String, String>();
        results.put("name", name);
        results.put("registrationToken", uuid);
        results.put("lunch", lunch ? "yes" : "no");
        Gson gson = new GsonBuilder().create();
        ps.result = gson.toJson(results);

        return ps;
    }

    @Override
    public List<VerificationDescription> getVerifications(String id) {
        List<VerificationDescription> result = new ArrayList<VerificationDescription>();
        result.add(registerNameDesc);
        return result;
    }
}
