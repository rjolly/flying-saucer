package org.xhtmlrenderer.demo.browser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.sourceforge.jeuclid.DOMBuilder;
import net.sourceforge.jeuclid.MathMLParserSupport;
import net.sourceforge.jeuclid.MutableLayoutContext;
import net.sourceforge.jeuclid.context.LayoutContextImpl;
import net.sourceforge.jeuclid.context.Parameter;
import net.sourceforge.jeuclid.layout.JEuclidView;

public class JEuclidReplacedElementFactory extends IconReplacedElementFactory {
	private final Transformer transformer;

	public JEuclidReplacedElementFactory() {
		super("math");
		try {
			transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResource("/net/sourceforge/jeuclid/content/mathmlc2p.xsl").toString()));
		} catch (final TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private String c2p(final Document doc) {
		final Writer writer = new StringWriter();
		try {
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
		} catch (final TransformerException e) {
			e.printStackTrace();
		}
		return writer.toString().replaceAll("\u2148", "i");
	}

	public Image createImage(final Document doc) throws IconReplacedException {
		try {
			return createMathImage(MathMLParserSupport.parseString(c2p(doc)));
		} catch (final SAXException e) {
			throw new IconReplacedException(e);
		} catch (final ParserConfigurationException e) {
			throw new IconReplacedException(e);
		} catch (final IOException e) {
			throw new IconReplacedException(e);
		}
	}

	private Image createMathImage(final Node node) {
		((MutableLayoutContext) LayoutContextImpl.getDefaultLayoutContext()).setParameter(Parameter.SCRIPTMINSIZE, new Float(10f));
		final JEuclidView view = DOMBuilder.getInstance().createJeuclidDom(node).getDefaultView();
		final int width = (int) Math.ceil(view.getWidth());
		final int height = (int) (Math.ceil(view.getAscentHeight()) + Math.ceil(view.getDescentHeight()));

		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final Color transparency = new Color(255, 255, 255, 0);

		g.setColor(transparency);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.black);

		view.draw(g, 0, (float) Math.ceil(view.getAscentHeight()));
		return image;
	}
}
