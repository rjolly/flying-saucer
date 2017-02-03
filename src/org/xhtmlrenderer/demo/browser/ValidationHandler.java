package org.xhtmlrenderer.demo.browser;

import java.util.logging.*;
import javax.swing.JTextArea;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class ValidationHandler implements ErrorHandler {
	protected JTextArea jta;
	public static Logger logger = Logger.getLogger( "app.browser" );

	public void error( SAXParseException ex ) {
		print( "error: " + print( ex ) );
	}

	public void fatalError( SAXParseException ex ) {
		print( "fatal error: " + print( ex ) );
	}

	public void warning( SAXParseException ex ) {
		print( "warning: " + print( ex ) );
	}

	public String print( SAXParseException ex ) {
		StringBuffer sb = new StringBuffer();
		sb.append( "Exception: " + ex.getMessage() );
		sb.append( "failed at column : " + ex.getColumnNumber() +
				" on line " + ex.getLineNumber() );
		sb.append( "entity:\n" + ex.getPublicId() + "\n" + ex.getSystemId() );
		return sb.toString();
	}

	public void setTextArea( JTextArea jta ) {
		this.jta = jta;
	}

	protected void print( String str ) {
		if ( jta != null ) {
			jta.append( str );
		}
	}
}
