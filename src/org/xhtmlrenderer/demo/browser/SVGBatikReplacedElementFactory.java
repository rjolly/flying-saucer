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

	public Image createImage(final Document doc) {
		final MemoryTranscoder transcoder = new MemoryTranscoder();
		try {
			transcoder.transcode(new TranscoderInput(doc), new TranscoderOutput());
		} catch (final TranscoderException e) {
			e.printStackTrace();
		}
		return transcoder.getImage();
	}
}
