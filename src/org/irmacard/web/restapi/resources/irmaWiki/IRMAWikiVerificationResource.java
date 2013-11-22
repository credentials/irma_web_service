package org.irmacard.web.restapi.resources.irmaWiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.VerificationDescription;
import org.irmacard.web.restapi.resources.VerificationBaseResource;
import org.irmacard.web.restapi.util.ProtocolStep;

public class IRMAWikiVerificationResource extends
		VerificationBaseResource {
	final static String VERIFIER = "IRMAWiki";
	final static String VERIFICATIONID = "memberAll";
	public static final String ATTRIBUTE_STORE_NAME = "IRMAWiki.attribute.store";

	VerificationDescription memberDescription;

	public IRMAWikiVerificationResource() {
		try {
			memberDescription = DescriptionStore.getInstance()
					.getVerificationDescriptionByName(VERIFIER, VERIFICATIONID);
		} catch (InfoException e) {
			e.printStackTrace();
		}

	}

	@Override
	public ProtocolStep onSuccess(String id, Map<String, Attributes> attrMap) {
		ProtocolStep ps = new ProtocolStep();

		Attributes member = attrMap.get(VERIFICATIONID);

		if (member == null) {
			return ProtocolStep.newError(memberDescription.getName()
					+ " credential is invalid/expired");
		}
		
		getAttributeStore().put(id, member);
		
		System.out.println("refPath: " + getReference().getPath() + "\n refQuery: " + getReference().getQuery());
		System.err.println("refPath: " + getReference().getPath() + "\n refQuery: " + getReference().getQuery());
		ps.protocolDone = true;
		ps.status = "success";
		ps.result = "http://wiki.pilot.irmacard.org/index.php?title=Special:IRMALogin/" + id;
		return ps;
	}

	@Override
	public List<VerificationDescription> getVerifications(String id) {
		System.out.println("Member description: " + memberDescription);
		List<VerificationDescription> result = new ArrayList<VerificationDescription>();
		result.add(memberDescription);
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Attributes> getAttributeStore() {
		Map<String, Attributes> store = null;
		ServletContext ctxt = null;
		ServletContext other_ctxt = null;

		try {
			ctxt = (ServletContext) getContext().getServerDispatcher().getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
			other_ctxt = (ServletContext) getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
		} catch (ClassCastException e) {
			e.printStackTrace();
		}

		// Try first context
		if(ctxt != null) {
			store = (Map<String, Attributes>) ctxt.getAttribute(ATTRIBUTE_STORE_NAME);
		}

		// Try second context
		if(store == null && other_ctxt != null) {
			store = (Map<String, Attributes>) other_ctxt.getAttribute(ATTRIBUTE_STORE_NAME);
		}

		// Still no store found, create it and put it in first context.
		if( store == null) {
			System.out.println("Store not found, generating new store");
			store = new HashMap<String, Attributes>();
			ctxt.setAttribute(ATTRIBUTE_STORE_NAME, store);
		}

		return store;
	}
}
