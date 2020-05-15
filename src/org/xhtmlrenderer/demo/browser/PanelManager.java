package org.xhtmlrenderer.demo.browser;

import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.swing.DelegatingUserAgent;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.GeneralUtil;
import org.xml.sax.InputSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import javax.xml.transform.sax.SAXSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class PanelManager extends DelegatingUserAgent {
	private int index = -1;
	private ArrayList history = new ArrayList();

	public String resolveURI(String uri) {
		final String burl = getBaseURL();

		URL ref = null;

		if (uri == null) return burl;
		if (uri.trim().equals("")) return burl; //jar URLs don't resolve this right

		if (uri.startsWith("demo:")) {
			DemoMarker marker = new DemoMarker();
			String short_url = uri.substring(5);
			if (!short_url.startsWith("/")) {
				short_url = "/" + short_url;
			}
			ref = marker.getClass().getResource(short_url);
			Uu.p("ref = " + ref);
		} else if (uri.startsWith("demoNav:")) {
			DemoMarker marker = new DemoMarker();
			String short_url = uri.substring("demoNav:".length());
			if (!short_url.startsWith("/")) {
				short_url = "/" + short_url;
			}
			ref = marker.getClass().getResource(short_url);
			Uu.p("Demo navigation URI, ref = " + ref);
		} else if (uri.startsWith("javascript")) {
			Uu.p("Javascript URI, ignoring: " + uri);
		} else if (uri.startsWith("news")) {
			Uu.p("News URI, ignoring: " + uri);
		} else {
			try {
				URL base;
				if (burl == null || burl.length() == 0) {
					base = new File(".").toURL();
				} else {
					base = new URL(burl);
				}
				ref = new URL(base, uri);
			} catch (MalformedURLException e) {
				Uu.p("URI/URL is malformed: " + burl + " or " + uri);
			}
		}

		if (ref == null) {
			return null;
		} else {
			return ref.toExternalForm();
		}
	}

	public XMLResource getXMLResource(String uri) {
		uri = resolveURI(uri);
		if (uri != null && uri.startsWith("file:")) {
			File file = null;
			try {
				StringBuffer sbURI = GeneralUtil.htmlEscapeSpace(uri);

				XRLog.general("Encoded URI: " + sbURI);
				file = new File(new URI(sbURI.toString()));
			} catch (URISyntaxException e) {
				XRLog.exception("Invalid file URI " + uri, e);
				return getNotFoundDocument(uri);
			}
			if (file.isDirectory()) {
				String dirlist = DirectoryLister.list(file);
				return XMLResource.load(new StringReader(dirlist));
			}
		}
		XMLResource xr = null;
		URLConnection uc = null;
		InputStream inputStream = null;
		try {
			uc = new URL(uri).openConnection();
			uc.connect();
			String contentType = uc.getContentType();
			//Maybe should popup a choice when content/unknown!
			if (contentType == null) contentType = "content/unknown";
			if (contentType.equals("text/plain") || contentType.equals("content/unknown")) {
				inputStream = uc.getInputStream();
				SAXSource source = new SAXSource(new PlainTextXMLReader(inputStream), new InputSource());
				xr = XMLResource.load(source);
			} else if (contentType.startsWith("text/html")) {
				inputStream = uc.getInputStream();
				final String charsetName = new MimeType(contentType).getParameter("charset");
				final Document document = Jsoup.parse(inputStream, charsetName, uri);
				document.select("script").remove();
				document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
				document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
				xr = XMLResource.load(new StringReader(document.toString()));
			} else if (contentType.startsWith("image")) {
				String doc = "<img src='" + uri + "'/>";
				xr = XMLResource.load(new StringReader(doc));
			} else if (contentType.startsWith("text/xml") || contentType.startsWith("application/xml") || contentType.startsWith("application/xhtml+xml")) {
				inputStream = uc.getInputStream();
				xr = XMLResource.load(inputStream);
			} else {
				String doc = "<html><h1>Unsupported content type</h1><p><pre>" + contentType + "</pre></p></html>";
				xr = XMLResource.load(new StringReader(doc));
			}
		} catch (MalformedURLException e) {
			XRLog.exception("bad URL given: " + uri, e);
		} catch (IOException e) {
			XRLog.exception("IO problem for " + uri, e);
		} catch (MimeTypeParseException e) {
			XRLog.exception("Mime type problem for " + uri, e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// swallow
				}
			}
		}

		if (xr == null) {
			xr = getNotFoundDocument(uri);
		}
		return xr;
	}

	private XMLResource getNotFoundDocument(String uri) {
		XMLResource xr;

		// URI may contain & symbols which can "break" the XHTML we're creating
		String cleanUri = GeneralUtil.escapeHTML(uri);
		String notFound = "<html><h1>Document not found</h1><p>Could not access URI <pre>" + cleanUri + "</pre></p></html>";

		xr = XMLResource.load(new StringReader(notFound));
		return xr;
	}

	public boolean isVisited(String uri) {
		if (uri == null) return false;
		uri = resolveURI(uri);
		return history.contains(uri);
	}

	public void setBaseURL (String url) {
		String burl = super.getBaseURL();
		if(burl !=null &&  burl.startsWith("error:")) burl = null;
		
		burl = resolveURI(url);
		if (burl == null) burl = "error:FileNotFound";

		super.setBaseURL(burl);

		// setBaseURL is called by view when document is loaded
		if (index >= 0) {
			String historic = (String) history.get(index);
			if (historic.equals(burl)) return; //moved in history
		}
		index++;
		for (int i = index; i < history.size(); history.remove(i)) ;
		history.add(index, burl);
	}

	public String getForward() {
		index++;
		return (String) history.get(index);
	}

	public String getBack() {
		index--;
		return (String) history.get(index);
	}

	public boolean hasForward() {
		if (index + 1 < history.size() && index >= 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean hasBack() {
		if (index > 0) {
			return true;
		} else {
			return false;
		}
	}
}
