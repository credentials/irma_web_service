package org.irmacard.web.irmaWiki;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.irmacard.credentials.Attributes;
import org.irmacard.web.restapi.resources.irmaWiki.IRMAWikiVerificationResource;
import org.irmacard.web.restapi.resources.irmaWiki.data.IRMAWikiVerificationData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IRMAWikiAttributeStoreServlet extends HttpServlet {
	
	private static final long serialVersionUID = 5535714536228123508L;

	/**
     * Process HEAD request. This returns the same headers as GET request, but without content.
	 * @throws IOException 
     * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
     */
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Process request without content.
        processRequest(request, response, false);
    }

    /**
     * Process GET request.
     * @throws IOException 
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Process request with content.
        processRequest(request, response, true);
    }

    /**
     * Process the actual request.
     * @param request The request to be processed.
     * @param response The response to be created.
     * @param content Whether the request body should be written (GET) or not (HEAD).
     * @throws IOException If something fails at I/O level.
     */
    private void processRequest
        (HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException
    {
        // Get requested id by path info. Also, strip leading '/'
        String id = request.getPathInfo().substring(1);
    	if (id == null) {
    		// If the "id" parameter is not present you are not allowed to access any attributes.
    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
    		return;
    	}

    	Attributes attributes = getAttributeStore().get(id);
    	if (attributes == null) {
    		// User hasn't authenticated with IRMA, so not allowed to access attributes.
    		System.out.println("Attributes not registered: access prohibited");
    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
    		return;
    	}

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(new IRMAWikiVerificationData(attributes));
        response.getWriter().write(json);
    }

    private Map<String, Attributes> getAttributeStore() {
		ServletContext servletctx = getServletContext();

		@SuppressWarnings("unchecked")
		Map<String, Attributes> store = (Map<String, Attributes>) servletctx
				.getAttribute(IRMAWikiVerificationResource.ATTRIBUTE_STORE_NAME);
		if (store == null) {
			System.out.println("Resetting store in IRMAWikiAttributeStoreServlet");
			store = new HashMap<String, Attributes>();
			servletctx.setAttribute(IRMAWikiVerificationResource.ATTRIBUTE_STORE_NAME, store);
		}

		return store;
	}
}
