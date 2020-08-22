package org.xhtmlrenderer.demo.browser;

import java.awt.Image;
import org.w3c.dom.Document;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscoderException;

public class SVGBatikReplacedElementFactory extends IconReplacedElementFactory {
	public SVGBatikReplacedElementFactory() {
		super("svg");
	}

	public Image createImage(final Document doc) throws IconReplacedException {
		final MemoryTranscoder transcoder = new MemoryTranscoder();
		try {
			transcoder.transcode(new TranscoderInput(doc), new TranscoderOutput());
			return transcoder.getImage();
		} catch (final TranscoderException e) {
			throw new IconReplacedException(e);
		} catch (final ClassCastException e) {
			throw new IconReplacedException(e);
		}
	}
}
