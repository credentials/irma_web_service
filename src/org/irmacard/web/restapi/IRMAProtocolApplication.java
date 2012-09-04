package org.irmacard.web.restapi;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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
       
       // This is a quick 'hack' to have some state, there is most probably
       // a better way :)
       Map<String,BigInteger> noncemap = new HashMap<String, BigInteger>();
       getContext().getAttributes().put("noncemap", noncemap);
       router.attach("/verification/{crednr}", VerificationProtocolResource.class); 
       router.attach("/verification/{crednr}/{id}/{round}", VerificationProtocolResource.class);
       return router;
   }   
}
