package org.irmacard.web.restapi;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.irmacard.web.restapi.resources.IssueStudentCredResource;
import org.irmacard.web.restapi.resources.VerificationProtocolResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import credentials.Attributes;
import credentials.idemix.util.CredentialInformation;

public class IRMAProtocolApplication extends Application {
    /**
    * Starting point for the IRMA REST interface for smartcard protocols. Cointains
    * the URL router.
    * 
    * @author Maarten Everts
    */
   @Override
   public synchronized Restlet createInboundRoot() {
       Router router = new Router(getContext());
       
		URI CORE_LOCATION;
		try {
			CORE_LOCATION = VerificationProtocolResource.class.getClassLoader()
					.getResource("/resources/").toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}
		CredentialInformation.setCoreLocation(CORE_LOCATION);


       // This is a quick 'hack' to have some state, there is most probably
       // a better way :)
       Map<String,BigInteger> noncemap = new HashMap<String, BigInteger>();
       getContext().getAttributes().put("noncemap", noncemap);
       Map<String,Attributes> attributemap = new HashMap<String, Attributes>();
       getContext().getAttributes().put("attributemap", attributemap);

       router.attach("/verification/{crednr}", VerificationProtocolResource.class); 
       router.attach("/verification/{crednr}/{id}/{round}", VerificationProtocolResource.class);

       router.attach("/issue/studentCred", IssueStudentCredResource.class);
       router.attach("/issue/studentCred/{id}/{round}", IssueStudentCredResource.class);
       return router;
   }   
}
