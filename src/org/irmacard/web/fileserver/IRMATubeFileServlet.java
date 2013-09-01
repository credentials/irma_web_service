/*
 * org/irmacard/web/fileserver/FileServlet.java
 *
 * Copyright (C) 2009 BalusC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 * If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * This server is graciously provided by BalusC. It is used in the irma_web_service
 * to stream some files, only when prior authentication using and IRMACard has taken
 * place.
 */

package org.irmacard.web.fileserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.irmacard.web.restapi.resources.irmaTube.IRMATubeVerificationResource;

/**
 * A file servlet supporting resume of downloads and client-side caching and GZIP of text content.
 * This servlet can also be used for images, client-side caching would become more efficient.
 * This servlet can also be used for text files, GZIP would decrease network bandwidth.
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
public class IRMATubeFileServlet extends HttpServlet {

    // Constants ----------------------------------------------------------------------------------

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
	
	/**
	 * Argument 
	 */
	private final static String ACCESS_SUFFIX = "access";

    // Properties ---------------------------------------------------------------------------------

    private URI basePath;

    // Actions ------------------------------------------------------------------------------------

    /**
     * Initialize the servlet.
     * @see HttpServlet#init().
     */
    public void init() throws ServletException {
		try {
			this.basePath = IRMATubeFileServlet.class.getClassLoader()
					.getResource("/resources/irmaTube/").toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new ServletException(e.toString());
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw new ServletException("Directory missing");
		}
		System.out.println("Basepath is now: " + this.basePath);

        // Validate base path.
		// FIXME: all the following comments are rubbish
        if (this.basePath == null) {
            throw new ServletException("FileServlet init param 'basePath' is required.");
        } else {
            File path = new File(this.basePath);
            if (!path.exists()) {
                throw new ServletException("FileServlet init param 'basePath' value '"
                    + this.basePath + "' does actually not exist in file system.");
            } else if (!path.isDirectory()) {
                throw new ServletException("FileServlet init param 'basePath' value '"
                    + this.basePath + "' is actually not a directory in file system.");
            } else if (!path.canRead()) {
                throw new ServletException("FileServlet init param 'basePath' value '"
                    + this.basePath + "' is actually not readable in file system.");
            }
        }
    }

    /**
     * Process HEAD request. This returns the same headers as GET request, but without content.
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
        (HttpServletRequest request, HttpServletResponse response, boolean content)
            throws IOException
    {
        // Get requested file by path info.
    	// also, strip leading '/'
        String requestString = request.getPathInfo().substring(1);
        
        // Parse requestString as /session-id/path/to/file
        int separatorIdx = requestString.indexOf('/');
        String sessionId = requestString.substring(0, separatorIdx);
        String requestedFile = requestString.substring(separatorIdx + 1);
        System.out.println("Session id: " + sessionId + " requested: " + requestedFile);
    	
    	// Validate whether user has access to this file
    	Integer userAge;
    	
    	if (sessionId == null) {
    		// If the "id" parameter is not present you are not
    		// allowed to access any file.
    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
    		return;
    	}

    	userAge = getAgeStore().get(sessionId);

    	if (userAge == null) {
    		// User hasn't authenticated with IRMA, so not allowed
    		// to access file.
    		System.out.println("Age not registered: access prohibited");
    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
    		return;
    	}

        // Validate the requested file ------------------------------------------------------------
        
        // Check if file is actually supplied to the request URL.
        if (requestedFile == null) {
            // Do your thing if the file is not supplied to the request URL.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // URL-decode the file name (might contain spaces and on) and prepare file object.
        File file = new File(basePath.resolve(URLDecoder.decode(requestedFile, "UTF-8")));

        // Check if file actually exists in filesystem.
        if (!file.exists()) {
            // Do your thing if the file appears to be non-existing.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
        	System.out.println("Requested file: " + file.getAbsolutePath() + " not found (requested: " + requestedFile + ")");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare some variables. The ETag is an unique identifier of the file.
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;
        
        // Validate whether requester is allowed to access
        String baseRequestedFile = requestedFile.substring(0, requestedFile.lastIndexOf('.') + 1);
        System.out.println("Base file: " + baseRequestedFile);
        File permfile = new File(basePath.resolve(URLDecoder.decode(baseRequestedFile + ACCESS_SUFFIX, "UTF-8")));
        System.out.println("Accessing permission file: " + permfile.getAbsolutePath());
        
        if(!permfile.exists()) {
        	// No permissions granted, refusing request
        	System.out.println("Permission file doesn't exist");
        	response.sendError(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }
        
        BufferedReader permReader = new BufferedReader(new FileReader(permfile));
        Integer minAge = null;
        try {
        	minAge = Integer.valueOf(permReader.readLine());
        	System.out.println("Found minimum age: " + minAge);
        } catch (Exception e) {
        	e.printStackTrace();
        	response.sendError(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }

        if(minAge > userAge) {
        	response.sendError(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }

        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }


        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }


        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<Range>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }


        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = getServletContext().getMimeType(fileName);
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the browser and expand content type with the one and right character encoding.
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        } 

        // Else, expect for images, determine content disposition. If content type is supported by
        // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
        else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }

        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);


        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        RandomAccessFile input = null;
        OutputStream output = null;

        try {
            // Open streams.
            input = new RandomAccessFile(file, "r");
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                Range r = full;
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);

                if (content) {
                    if (acceptsGzip) {
                        // The browser accepts GZIP, so GZIP the content.
                        response.setHeader("Content-Encoding", "gzip");
                        output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
                    } else {
                        // Content length is not directly predictable in case of GZIP.
                        // So only add it if there is no means of GZIP, else browser will hang.
                        response.setHeader("Content-Length", String.valueOf(r.length));
                    }

                    // Copy full range.
                    copy(input, output, r.start, r.length);
                }

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Copy single part range.
                    copy(input, output, r.start, r.length);
                }

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Cast back to ServletOutputStream to get the easy println methods.
                    ServletOutputStream sos = (ServletOutputStream) output;

                    // Copy multi part range.
                    for (Range r : ranges) {
                        // Add multipart boundary and header fields for every range.
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        sos.println("Content-Type: " + contentType);
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                        // Copy single part range of multi part range.
                        copy(input, output, r.start, r.length);
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
        } finally {
            // Gently close streams.
            close(output);
            close(input);
        }
    }

    // Helpers (can be refactored to public utility class) ----------------------------------------

    /**
     * Returns true if the given accept header accepts the given value.
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
            || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
            || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
            || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private static void copy(RandomAccessFile input, OutputStream output, long start, long length)
        throws IOException
    {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;

        if (input.length() == length) {
            // Write full range.
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
        } else {
            // Write partial range.
            input.seek(start);
            long toRead = length;

            while ((read = input.read(buffer)) > 0) {
                if ((toRead -= read) > 0) {
                    output.write(buffer, 0, read);
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    break;
                }
            }
        }
    }

    /**
     * Close the given resource.
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
                // Ignore IOException. If you want to handle this anyway, it might be useful to know
                // that this will generally only be thrown when the client aborted the request.
            }
        }
    }
    
    private Map<String, Integer> getAgeStore() {
		ServletContext servletctx = getServletContext();

		@SuppressWarnings("unchecked")
		Map<String, Integer> store = (Map<String, Integer>) servletctx
				.getAttribute(IRMATubeVerificationResource.AGE_STORE_NAME);
		if (store == null) {
			System.out.println("Resetting store in IRMATubeFileServlet");
			store = new HashMap<String, Integer>();
			servletctx.setAttribute(
					IRMATubeVerificationResource.AGE_STORE_NAME, store);
		}

		System.out.println("Keys in AgeStore: " + store);
		for(String k : store.keySet()) {
			System.out.println("Key: " + k + ", value: " + store.get(k));
		}

		return store;
	}

    // Inner classes ------------------------------------------------------------------------------

    /**
     * This class represents a byte range.
     */
    protected class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

    }

}
