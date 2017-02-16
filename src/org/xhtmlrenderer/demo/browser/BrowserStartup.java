package org.xhtmlrenderer.demo.browser;

import java.awt.BorderLayout;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.ImageIcon;
import javax.swing.border.EtchedBorder;

import linoleum.application.Frame;

public class BrowserStartup extends Frame {
	public BrowserPanel panel;
	BrowserMenuBar menu;
	BrowserActions actions;
	final String startPage = "demo:demos/splash/splash.html";

	static {
		System.setProperty("xr.css.user-agent-default-css", "/org/xhtmlrenderer/demo/browser/");
		System.setProperty("xr.use.listeners", "false");
	}

	public BrowserStartup() {
		this(null);
	}

	public BrowserStartup(final Frame parent) {
		super(parent);
		initUI();
		setIcon(new ImageIcon(getClass().getResource("flyingsaucer24.png")));
		setMimeType("text/html:application/xhtml+xml");
	}

	private void initUI() {
                setClosable(true);
                setIconifiable(true);
                setMaximizable(true);
                setResizable(true);

		setName("FlyingSaucer");

		actions = new BrowserActions(this);
		actions.init();

		panel = new BrowserPanel(this, new FrameBrowserPanelListener());
		panel.init();
		panel.createActions();

		menu = new BrowserMenuBar(this);
		menu.init();
		menu.createLayout();

		setJMenuBar(menu);

		getContentPane().add(panel.toolbar, BorderLayout.PAGE_START);
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		getContentPane().add(panel.status, BorderLayout.PAGE_END);
		pack();
		setSize(1024, 768);
	}

	@Override
	public Frame getFrame(final Frame parent) {
		return new BrowserStartup(parent);
	}

	@Override
	public void setURI(final URI uri) {
		panel.loadPage(uri.toString());
	}

	@Override
	public URI getURI() {
		final String str = panel.view.getSharedContext().getBaseURL();
		if (str != null) try {
			return new URI(str);
		} catch (final URISyntaxException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void open() {
		final String str = panel.view.getSharedContext().getBaseURL();
		if (str == null) {
			panel.loadPage(startPage);
		}
	}

	class FrameBrowserPanelListener implements BrowserPanelListener {
		public void pageLoadSuccess(final String url, final String title) {
			setTitle(title + (title.length() > 0 ? " - " : "") + "Flying Saucer");
		}
	}
}
