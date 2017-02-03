package org.xhtmlrenderer.demo.browser;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

import org.xhtmlrenderer.util.GeneralUtil;
import org.xhtmlrenderer.util.XRLog;

public class BrowserStartup {
	public BrowserPanel panel;
	protected BrowserMenuBar menu;
	protected JFrame frame;
	protected JFrame validation_console = null;
	protected BrowserActions actions;
	protected String startPage;

	protected ValidationHandler error_handler = new ValidationHandler();
	public static final Logger logger = Logger.getLogger("app.browser");

	public BrowserStartup() {
		this("demo:demos/splash/splash.html");
	}

	public BrowserStartup(String startPage) {
		logger.info("starting up");
		this.startPage = startPage;
	}

	public void initUI() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame = frame;
		logger.info("creating UI");
		actions = new BrowserActions(this);
		actions.init();

		panel = new BrowserPanel(this, new FrameBrowserPanelListener());
		panel.init();
		panel.createActions();

		menu = new BrowserMenuBar(this);
		menu.init();
		menu.createLayout();
		menu.createActions();

		frame.setJMenuBar(menu);

		frame.getContentPane().add(panel.toolbar, BorderLayout.PAGE_START);
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		frame.getContentPane().add(panel.status, BorderLayout.PAGE_END);
		frame.pack();
		frame.setSize(1024, 768);
	}

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				final BrowserStartup bs = new BrowserStartup();
				bs.initUI();
				bs.launch();
			}
		});
	}

	public void launch() {
		try {
			panel.loadPage(startPage);

			frame.setVisible(true);
		} catch (Exception ex) {
			XRLog.general(Level.SEVERE, ex.getMessage(), ex);
		}
	}

	class FrameBrowserPanelListener implements BrowserPanelListener {
		public void pageLoadSuccess(String url, String title) {
			frame.setTitle(title + (title.length() > 0 ? " - " : "") + "Flying Saucer");
		}
	}
}
