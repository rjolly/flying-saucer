package org.xhtmlrenderer.demo.browser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;
import org.xhtmlrenderer.pdf.util.XHtmlMetaToPdfInfoAdapter;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.LinkListener;
import org.xhtmlrenderer.swing.ScalableXHTMLPanel;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.GeneralUtil;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BrowserPanel extends JPanel implements DocumentListener {
	private final ChainedReplacedElementFactory cef = new ChainedReplacedElementFactory();
	private final Action copyLinkLocationAction = new CopyLinkLocationAction();
	private String uri;
	private JButton forward;
	private JButton backward;
	private JButton stop;
	private JButton reload;
	private JButton goHome;
	private JButton font_inc;
	private JButton font_rst;
	private JButton font_dec;
	private JButton print;
	JTextField url;
	BrowserStatus status;
	public ScalableXHTMLPanel view;
	private JScrollPane scroll;
	private BrowserStartup root;
	private BrowserPanelListener listener;
	private JPopupMenu popup;
	private JButton print_preview;
	private final Logger logger = Logger.getLogger("app.browser");
	private PanelManager manager;
	private JButton goToPage;
	public JToolBar toolbar;

	private class CopyLinkLocationAction extends AbstractAction {
		public CopyLinkLocationAction() {
			super("Copy link location");
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final StringSelection selection = new StringSelection(uri);
			getToolkit().getSystemClipboard().setContents(selection, selection);
		}
	}

	public BrowserPanel(BrowserStartup root, BrowserPanelListener listener) {
		super();
		this.root = root;
		this.listener = listener;
	}

	public void init() {
		forward = new JButton();
		backward = new JButton();
		stop = new JButton();
		reload = new JButton();
		goToPage = new JButton();
		goHome = new JButton();

		url = new JTextField();
		url.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				super.focusGained(e);
				url.selectAll();
			}

			public void focusLost(FocusEvent e) {
				super.focusLost(e);
				url.select(0, 0);
			}
		});

		manager = new PanelManager();
		view = new ScalableXHTMLPanel(manager);
		manager.setRepaintListener(view);
		ImageResourceLoader irl = new ImageResourceLoader();
		irl.setRepaintListener(view);
		manager.setImageResourceLoader(irl);
		cef.addFactory(new SwingReplacedElementFactory(view, irl));
		cef.addFactory(new JEuclidReplacedElementFactory());
		cef.addFactory(new SVGBatikReplacedElementFactory());
		view.getSharedContext().setReplacedElementFactory(cef);
		view.addMouseTrackingListener(new LinkListener() {
			@Override
			public void linkClicked(final BasicPanel panel, final String uri) {
				if (!popup.isVisible()) {
					onMouseOut(panel, null);
					try {
						final URI u = new URI(uri);
						if ("demoNav".equals(u.getScheme())) {
							final String pg = u.getSchemeSpecificPart();
							if (pg.equals("back")) {
								root.menu.navigateToPriorDemo();
							} else {
								root.menu.navigateToNextDemo();
							}
						} else if (u.isAbsolute()) {
							loadPage(uri);
						} else {
							super.linkClicked(panel, uri);
						}
					} catch (final URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onMouseOver(final BasicPanel panel, final Box box) {
				uri = findLink(panel, box.getElement());
				if (uri != null) {
					setStatus(uri);
					panel.setComponentPopupMenu(popup);
				}
			}

			@Override
			public void onMouseOut(final BasicPanel panel, final Box box) {
				if (uri != null) {
					panel.setComponentPopupMenu(null);
					setStatus("");
				}
			}
		});
		view.addDocumentListener(this);
		view.addDocumentListener(manager);
		view.setCenteredPagedView(true);
		view.setBackground(Color.LIGHT_GRAY);
		scroll = new FSScrollPane(view);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		print_preview = new JButton();
		print = new JButton();

		loadCustomFonts();

		status = new BrowserStatus();
		status.init();

		initPopup();
		initToolbar();

		int text_width = 200;
		view.setPreferredSize(new Dimension(text_width, text_width));

		setLayout(new BorderLayout());
		this.add(scroll, BorderLayout.CENTER);
	}

	private String findLink(final BasicPanel panel, final Element e) {
		String uri = null;
		for (Node node = e; node.getNodeType() == Node.ELEMENT_NODE; node = node.getParentNode()) {
			uri = panel.getSharedContext().getNamespaceHandler().getLinkUri((Element) node);
			if (uri != null) {
				break;
			}
		}
		return uri;
	}

	private void initPopup() {
		popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(final PopupMenuEvent evt) {
			}
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent evt) {
			}
			public void popupMenuWillBecomeVisible(final PopupMenuEvent evt) {
				prepare();
			}
                });
	}

	private void prepare() {
		popup.removeAll();
		boolean sep0 = false;
		if (uri != null) try {
			final URI u = new URI(uri);
			if (u.isAbsolute()) {
				sep0 = root.getApplicationManager().populate(u, popup);
			}
		} catch (final URISyntaxException ex) {
			ex.printStackTrace();
		}
		if (sep0) {
			popup.addSeparator();
		}
		popup.add(copyLinkLocationAction);
	}

	private void initToolbar() {
		toolbar = new JToolBar();
		toolbar.setRollover(true);
		toolbar.add(backward);
		toolbar.add(forward);
		toolbar.add(reload);
		toolbar.add(goHome);
		toolbar.add(url);
		toolbar.add(goToPage);
		// disabled for R6
		// toolbar.add(print);
		toolbar.setFloatable(false);
	}

	private void loadCustomFonts() {
		SharedContext rc = view.getSharedContext();
		try {
			rc.setFontMapping("Fuzz", Font.createFont(Font.TRUETYPE_FONT,
					new DemoMarker().getClass().getResourceAsStream("/demos/fonts/fuzz.ttf")));
		} catch (Exception ex) {
			Uu.p(ex);
		}
	}

	public void createLayout() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gbl);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(toolbar, c);
		add(toolbar);

		//c.gridx = 0;
		c.gridx++;
		c.gridy++;
		c.weightx = c.weighty = 0.0;
		c.insets = new Insets(5, 0, 5, 5);
		gbl.setConstraints(backward, c);
		add(backward);

		c.gridx++;
		gbl.setConstraints(forward, c);
		add(forward);

		c.gridx++;
		gbl.setConstraints(reload, c);
		add(reload);

		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		gbl.setConstraints(print_preview, c);
		add(print_preview);

		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 5;
		c.ipady = 5;
		c.weightx = 10.0;
		c.insets = new Insets(5, 0, 5, 0);
		gbl.setConstraints(url, c);
		url.setBorder(BorderFactory.createLoweredBevelBorder());
		add(url);

		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		c.insets = new Insets(0, 5, 0, 0);
		gbl.setConstraints(goToPage, c);
		add(goToPage);

		c.gridx = 0;
		c.gridy++;
		c.ipadx = 0;
		c.ipady = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 7;
		c.weightx = c.weighty = 10.0;
		gbl.setConstraints(scroll, c);
		add(scroll);

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0.1;
		gbl.setConstraints(status, c);
		add(status);
	}

	public void createActions() {
		// set text to "" to avoid showing action text in button--
		// we only want it in menu items
		backward.setAction(root.actions.backward);
		backward.setText("");
		forward.setAction(root.actions.forward);
		forward.setText("");
		reload.setAction(root.actions.reload);
		reload.setText("");
		goHome.setAction(root.actions.goHome);
		goHome.setText("");
		print_preview.setAction(root.actions.print_preview);
		print_preview.setText("");

		url.setAction(root.actions.load);
		goToPage.setAction(root.actions.goToPage);
		updateButtons();
	}

	public void goForward() {
		String uri = manager.getForward();
		view.setDocument(uri);
	}

	public void goBack() {
		String uri = manager.getBack();
		view.setDocument(uri);
	}

	public void reloadPage() {
		logger.config("Reloading Page: ");
		if (manager.getBaseURL() != null) {
			loadPage(manager.getBaseURL());
		}
	}

	//TODO: make this part of an implementation of UserAgentCallback instead
	public void loadPage(final String url_text) {
		root.setURI(url_text);
		root.open();
	}

	void doLoadPage(final String url_text) {
		try {
			logger.config("Loading Page: " + url_text);
			view.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			view.setDocument(url_text);
		} catch (XRRuntimeException ex) {
			XRLog.general(Level.SEVERE, "Runtime exception", ex);
			setStatus("Can't load document");
			handlePageLoadFailed(url_text, ex);
		} catch (Exception ex) {
			XRLog.general(Level.SEVERE, "Could not load page for display.", ex);
			ex.printStackTrace();
		}
	}

	public void exportToPdf( String path ) {
		if (manager.getBaseURL() != null) {
			setStatus( "Exporting to " + path + "..." );
			OutputStream os = null;
			try {
				os = new FileOutputStream(path);
				try {
				ITextRenderer renderer = new ITextRenderer();

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc =  db.parse(manager.getBaseURL());

				PDFCreationListener pdfCreationListener = new XHtmlMetaToPdfInfoAdapter( doc );
				renderer.setListener( pdfCreationListener );

				renderer.setDocument(manager.getBaseURL());
				renderer.layout();

				renderer.createPDF(os);
				setStatus( "Done export." );
			} catch (Exception e) {
				XRLog.general(Level.SEVERE, "Could not export PDF.", e);
				e.printStackTrace();
				setStatus( "Error exporting to PDF." );
				} finally {
					try {
						os.close();
					} catch (IOException e) {
						// swallow
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void handlePageLoadFailed(String url_text, XRRuntimeException ex) {
		final XMLResource xr;
		final String rootCause = getRootCause(ex);
		final String msg = GeneralUtil.escapeHTML(addLineBreaks(rootCause, 80));
		String notFound =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<!DOCTYPE html PUBLIC \" -//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
						"<body>\n" +
						"<h1>Document can't be loaded</h1>\n" +
						"<p>Could not load the page at \n" +
						"<pre>" + GeneralUtil.escapeHTML(url_text) + "</pre>\n" +
						"</p>\n" +
						"<p>The page failed to load; the error was </p>\n" +
						"<pre>" + msg + "</pre>\n" +
						"</body>\n" +
						"</html>";

		xr = XMLResource.load(new StringReader(notFound));
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				root.panel.view.setDocument(xr.getDocument(), null);
			}
		});
	}

	private String addLineBreaks(String _text, int maxLineLength) {
		StringBuffer broken = new StringBuffer(_text.length() + 10);
		boolean needBreak = false;
		for (int i = 0; i < _text.length(); i++) {
			if (i > 0 && i % maxLineLength == 0) needBreak = true;

			final char c = _text.charAt(i);
			if (needBreak && Character.isWhitespace(c)) {
				System.out.println("Breaking: " + broken.toString());
				needBreak = false;
				broken.append('\n');
			} else {
				broken.append(c);
			}
		}
		System.out.println("Broken! " + broken.toString());
		return broken.toString();  
	}

	private String getRootCause(Exception ex) {
		// FIXME
		Throwable cause = ex;
		while (cause != null) {
			cause = cause.getCause();
		}

		return cause == null ? ex.getMessage() : cause.getMessage();
	}

	public void documentStarted() {
		// TODO...
	}

	public void documentLoaded() {
		final String url_text = manager.getBaseURL();
		updateButtons();
		url.setText(url_text);
		setStatus("Successfully loaded: " + url_text);
		listener.pageLoadSuccess(url_text, view.getDocumentTitle());
		view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void setStatus(String txt) {
		status.text.setText(txt);
	}

	protected void updateButtons() {
		if (manager.hasBack()) {
			root.actions.backward.setEnabled(true);
		} else {
			root.actions.backward.setEnabled(false);
		}
		if (manager.hasForward()) {
			root.actions.forward.setEnabled(true);
		} else {
			root.actions.forward.setEnabled(false);
		}
	}

	public void onLayoutException(Throwable t) {
		// TODO: clean
		t.printStackTrace();
	}

	public void onRenderException(Throwable t) {
		// TODO: clean
		t.printStackTrace();
	}
}
