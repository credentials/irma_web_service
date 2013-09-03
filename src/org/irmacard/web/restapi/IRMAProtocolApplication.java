package org.irmacard.web.restapi;

import java.net.URI;
import java.net.URISyntaxException;

import org.irmacard.credentials.idemix.util.CredentialInformation;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.web.restapi.resources.MijnOverheidIssueResource;
import org.irmacard.web.restapi.resources.MijnOverheidVerificationResource;
import org.irmacard.web.restapi.resources.NYTimesVerificationResource;
import org.irmacard.web.restapi.resources.SpuitenEnSlikkenVerificationResource;
import org.irmacard.web.restapi.resources.StudentCardIssueResource;
import org.irmacard.web.restapi.resources.StudentCardVerificationResource;
import org.irmacard.web.restapi.resources.VerificationProtocolResource;
import org.irmacard.web.restapi.resources.irmaTube.IRMATubeRegistrationIssueResource;
import org.irmacard.web.restapi.resources.irmaTube.IRMATubeRegistrationVerificationResource;
import org.irmacard.web.restapi.resources.irmaTube.IRMATubeVerificationResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;


public class IRMAProtocolApplication extends Application {
	/**
	 * Starting point for the IRMA REST interface for smartcard protocols. Contains
	 * the URL router.
	 * 
	 * @author Maarten Everts
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		Router router = new Router(getContext());

		URI CORE_LOCATION;
		try {
			CORE_LOCATION = IRMAProtocolApplication.class.getClassLoader()
					.getResource("/resources/irma_configuration/").toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}
		CredentialInformation.setCoreLocation(CORE_LOCATION);
		DescriptionStore.setCoreLocation(CORE_LOCATION);

		router.attach("/verification/SpuitenEnSlikken", SpuitenEnSlikkenVerificationResource.class);
		router.attach("/verification/SpuitenEnSlikken/{id}/{step}", SpuitenEnSlikkenVerificationResource.class);

		router.attach("/verification/NYTimes", NYTimesVerificationResource.class);
		router.attach("/verification/NYTimes/{id}/{step}", NYTimesVerificationResource.class);

		router.attach("/verification/IRMATube/{age}", IRMATubeVerificationResource.class);
		router.attach("/verification/IRMATube/{age}/{id}/{step}", IRMATubeVerificationResource.class);

		router.attach("/verification/MijnOverheid", MijnOverheidVerificationResource.class);
		router.attach("/verification/MijnOverheid/{id}/{step}", MijnOverheidVerificationResource.class);
		router.attach("/issue/MijnOverheid/{id}", MijnOverheidIssueResource.class);
		router.attach("/issue/MijnOverheid/{id}/{cred}/{step}", MijnOverheidIssueResource.class);

		router.attach("/verification/StudentCard", StudentCardVerificationResource.class);
		router.attach("/verification/StudentCard/{id}/{step}", StudentCardVerificationResource.class);
		router.attach("/issue/StudentCard/{id}", StudentCardIssueResource.class);
		router.attach("/issue/StudentCard/{id}/{cred}/{step}", StudentCardIssueResource.class);

		router.attach("/verification/IRMATubeRegistration", IRMATubeRegistrationVerificationResource.class);
		router.attach("/verification/IRMATubeRegistration/{id}/{step}", IRMATubeRegistrationVerificationResource.class);
		router.attach("/issue/IRMATubeRegistration/{id}", IRMATubeRegistrationIssueResource.class);
		router.attach("/issue/IRMATubeRegistration/{id}/{cred}/{step}", IRMATubeRegistrationIssueResource.class);

		return router;
	}   
}
