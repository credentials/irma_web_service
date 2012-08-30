package org.irmacard.web.restapi;

import org.irmacard.web.restapi.resources.VerificationProtocolResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

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

       router.attach("/verification/{crednr}", VerificationProtocolResource.class); 
       router.attach("/verification/{crednr}/{nonce}/{round}", VerificationProtocolResource.class);
       return router;
   }   
}
