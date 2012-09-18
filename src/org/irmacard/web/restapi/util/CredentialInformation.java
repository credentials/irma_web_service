package org.irmacard.web.restapi.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import org.irmacard.web.restapi.resources.VerificationProtocolResource;

import com.ibm.zurich.credsystem.utils.Locations;

public class CredentialInformation {
	URI CORE_LOCATION;
	
	URI baseLocation;
	URI issuerPKLocation;

	URI credStructBaseLocation;
	URI credStructLocation;
	
	URI issuerBaseID;
	URI credStructID;
	
	short credNr;
	
	public CredentialInformation(String issuer, String credName) {
		try {
			CORE_LOCATION = VerificationProtocolResource.class.getClassLoader()
					.getResource("/resources/").toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}

		baseLocation = CORE_LOCATION.resolve(issuer + "/");
		issuerPKLocation = baseLocation.resolve("ipk.xml");

		credStructBaseLocation = baseLocation.resolve("Issues/" + credName + "/");
		credStructLocation = credStructBaseLocation.resolve("structure.xml");

		readBaseURL();
		readCredID();

		credStructID = issuerBaseID.resolve(credName + "/structure.xml");
		
		setupSystem();
		setupCredentialStructure();
	}
	
	public void readBaseURL() {
		Scanner sc = null;
		try {
			sc = new Scanner(baseLocation.resolve("baseURL.txt").toURL().openStream());
			issuerBaseID = new URI(sc.nextLine());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}
	}
	
	public void readCredID() {
		Scanner sc = null;
		try {
		sc = new Scanner(credStructBaseLocation.resolve("id.txt").toURL().openStream());
		credNr = (short) sc.nextInt();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}
	}

	public void setupSystem() {
	    Locations.initSystem(baseLocation, issuerBaseID.toString());
	    Locations.init(issuerBaseID.resolve("ipk.xml"), issuerPKLocation);
	}
    
    public void setupCredentialStructure() {
    	Locations.init(credStructID, credStructLocation);
    }
    
    /**
     * You should use this. It is here only for testing. TODO
     * @param nr
     */
    public void setCredentialNr(short nr) {
    	credNr = nr;
    }
}
