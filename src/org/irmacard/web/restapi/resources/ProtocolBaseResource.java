package org.irmacard.web.restapi.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.web.restapi.ProtocolState;
import org.irmacard.web.restapi.util.QRResponse;
import org.restlet.data.MediaType;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A simple base class for HTTP based credential protocols.
 * @author Maarten Everts (TNO)
 *
 */
public abstract class ProtocolBaseResource extends ServerResource  {

    @Post("json")
    public String handlePost (String value) throws InfoException {
        String id = (String) getRequestAttributes().get("id");
        String step = (String) getRequestAttributes().get("step");

        if (id == null) {
            // Create a new protocol session
            id = UUID.randomUUID().toString();
            ProtocolState.putStatus(id, "start");
            if (isOptionSet("qr")) {
                return createQRResponse(id);
            } else {
                step = "0";
            }
        }

        if (step == null) {
            step = "0";
        }

        int istep = Integer.parseInt(step);
        return handleProtocolStep(id, istep, value);
    }

    @Get
    public Representation handleGet() {
        String id = (String) getRequestAttributes().get("id");
        String step = (String) getRequestAttributes().get("step");
        if (id == null) {
            return null;
        }

        if (step != null) {
            if (step.equals("qr")) {
                String qrURL = getBaseURL() + getBasePath().substring(0, getBasePath().lastIndexOf('/')+1) + "0";
                return generateQRImage(qrURL);
            } else if (step.equals("status")) {
                return getStatus(id);
            } else if (step.equals("attributes")) {
                return getAttributes(id);
            }

        }
        return null;
    }

    /**
     * Returns an image containing an QR image for the text in qrValue.
     * @param qrValue text to be placed in QR image.
     * @return
     */
    public Representation generateQRImage(String qrValue) {
         ByteArrayOutputStream out = QRCode.from(qrValue).to(
                    ImageType.PNG).withSize(300, 300).stream();
         byte[] data = out.toByteArray();
         ObjectRepresentation<byte[]> or=new ObjectRepresentation<byte[]>(data, MediaType.IMAGE_PNG) {
                @Override
                public void write(OutputStream os) throws IOException {
                    super.write(os);
                    os.write(this.getObject());
                }
            };
         return or;
    }

    public class ProtocolResourceStatus {
        public String status = null;
        public String result = null;
    }

    /**
     * Return a json message with the current status
     * @param id
     * @return
     */
    public Representation getStatus(String id) {
        String status = ProtocolState.getStatus(id);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        ProtocolResourceStatus protocolStatus = new ProtocolResourceStatus();
        protocolStatus.status = status;
        protocolStatus.result = ProtocolState.getResult(id);
        return new StringRepresentation(gson.toJson(protocolStatus));
    }

    public Representation getAttributes(String id) {
        Attributes attributes = ProtocolState.getAttributes(id);
        Map<String,String> attributesReadable = new HashMap<String,String>();
        for(String k : attributes.getIdentifiers()) {
            attributesReadable.put(k, new String(attributes.get(k)));
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return new StringRepresentation(gson.toJson(attributesReadable));
    }

    public String createQRResponse(String id) {
        QRResponse qrr = new QRResponse();
        qrr.qr_url = getBaseURL() + getBasePath() + "/" + id + "/qr";
        qrr.status_url = getBaseURL() + getBasePath() + "/" + id + "/status";
        Gson gson = new GsonBuilder().
                setPrettyPrinting().
                create();
        return gson.toJson(qrr);
    }

    /**
     * Method to handle a single protocol step, to be overriden by a subclass.
     *
     * @param id
     * @param step
     * @param value
     * @return
     * @throws InfoException 
     */
    public abstract String handleProtocolStep(String id, int step, String value) throws InfoException;

    /**
     * Check whether a boolean query parameter was set to true.
     * For example, whether "?qr=true" is appended to the url.
     * @param optionName
     * @return
     */
    private boolean isOptionSet(String optionName) {
        try {
            return getReference().getQueryAsForm().getFirstValue(optionName).equals("true");
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Returns the full base URL (hostname) for the current resource.
     * @return
     */
    public String getBaseURL() {
        String baseURL = null;

        HttpServletRequest request = ServletUtils.getRequest(getRequest());
        String fwdURL = request.getHeader("X-Forwarded-URL");
        if (fwdURL != null) {
            System.out.println("Behind proxy, accessed using URL: " + fwdURL + " (stripping " + request.getServletPath() + " for baseURL).");
            baseURL = fwdURL.substring(0, fwdURL.indexOf(request.getServletPath()));
        } else {
            baseURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }

        return baseURL;
    }

    public String getBasePath() {
        HttpServletRequest request = ServletUtils.getRequest(getRequest());

        String path = getReference().getPath();
        if (path.startsWith(request.getContextPath())) {
            path = path.substring(request.getContextPath().length());
        }

        return path;
    }

    public String makeResponseURL(String id, int step) {
        if (getRequestAttributes().get("id") == null) {
            return getBaseURL() + getBasePath() + '/' + id + '/' + Integer.toString(step);
        } else {
            return getBaseURL() + getBasePath().substring(0, getBasePath().lastIndexOf('/')+1) + Integer.toString(step);
        }
    }
}
