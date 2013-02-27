package org.irmacard.web.restapi;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.idemix.util.CredentialInformation;
import org.irmacard.web.restapi.resources.IssueStudentCredResource;
import org.irmacard.web.restapi.resources.QRIssueStudentCredResource;
import org.irmacard.web.restapi.resources.QRVerificationProtocolResource;
import org.irmacard.web.restapi.resources.VerificationProtocolResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import com.ibm.zurich.idmx.issuance.Issuer;


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

       
       router.attach("/verification/{crednr}", VerificationProtocolResource.class); 
       router.attach("/verification/{crednr}/{id}/{round}", VerificationProtocolResource.class);

       router.attach("/qrverification/{crednr}", QRVerificationProtocolResource.class); 
       router.attach("/qrverification/{crednr}/{id}/{round}", QRVerificationProtocolResource.class);
       
       router.attach("/issue/studentCred", IssueStudentCredResource.class);
       router.attach("/issue/studentCred/{id}/{round}", IssueStudentCredResource.class);
       
       router.attach("/qrissue/studentCred", QRIssueStudentCredResource.class);
       router.attach("/qrissue/studentCred/{id}/{round}", QRIssueStudentCredResource.class);
       
       return router;
   }   
}
