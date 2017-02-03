package org.xhtmlrenderer.demo.browser;

import org.xhtmlrenderer.demo.browser.actions.ZoomAction;
import org.xhtmlrenderer.swing.*;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.Uu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class BrowserMenuBar extends JMenuBar {
	BrowserStartup root;

	JMenu file;
	JMenu edit;
	JMenu view;
	JMenu go;
	JMenuItem view_source;
	JMenu debug;
	JMenu demos;
	private String lastDemoOpened;

	private Map allDemos;
	private JMenu help;

	public BrowserMenuBar(BrowserStartup root) {
		this.root = root;
	}

	public void init() {
		file = new JMenu("Browser");
		file.setMnemonic('B');

		debug = new JMenu("Debug");
		debug.setMnemonic('U');

		demos = new JMenu("Demos");
		demos.setMnemonic('D');

		view = new JMenu("View");
		view.setMnemonic('V');

		help = new JMenu("Help");
		help.setMnemonic('H');

		view_source = new JMenuItem("Page Source");
		view_source.setEnabled(false);
		view.add(root.actions.stop);
		view.add(root.actions.refresh);
		view.add(root.actions.reload);
		view.add(new JSeparator());
		JMenu text_size = new JMenu("Text Size");
		text_size.setMnemonic('T');
		text_size.add(root.actions.increase_font);
		text_size.add(root.actions.decrease_font);
		text_size.add(new JSeparator());
		text_size.add(root.actions.reset_font);
		view.add(text_size);

		go = new JMenu("Go");
		go.setMnemonic('G');
	}

	public void createLayout() {
		final ScalableXHTMLPanel panel = root.panel.view;

		file.add(root.actions.open_file);
		file.add(new JSeparator());
		file.add(root.actions.export_pdf);
		file.add(new JSeparator());
		file.add(root.actions.quit);
		add(file);

		/*
		// TODO: we can get the document and format it, but need syntax highlighting
		// and a tab or separate window, dialog, etc.
		view_source.setAction(new ViewSourceAction(panel));
		view.add(view_source);
		*/

		JMenu zoom = new JMenu("Zoom");
		zoom.setMnemonic('Z');
		ScaleFactor[] factors = this.initializeScales();
		ButtonGroup zoomGroup = new ButtonGroup();
		for (int i = 0; i < factors.length; i++) {
			ScaleFactor factor = factors[i];
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(new ZoomAction(panel, factor));

			if (factor.isNotZoomed()) item.setSelected(true);

			zoomGroup.add(item);
			zoom.add(item);
		}
		view.add(new JSeparator());
		view.add(zoom);
		view.add(new JSeparator());
		view.add(new JCheckBoxMenuItem(root.actions.print_preview));
		add(view);

		go.add(root.actions.forward);
		go.add(root.actions.backward);

		add(go);

		demos.add(new NextDemoAction());
		demos.add(new PriorDemoAction());
		demos.add(new JSeparator());
		allDemos = new LinkedHashMap();

		populateDemoList();

		for (Iterator iter = allDemos.keySet().iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			demos.add(new LoadAction(s, (String) allDemos.get(s)));
		}

		add(demos);

		JMenu debugShow = new JMenu("Show");
		debug.add(debugShow);
		debugShow.setMnemonic('S');

		debugShow.add(new JCheckBoxMenuItem(new BoxOutlinesAction()));
		debugShow.add(new JCheckBoxMenuItem(new LineBoxOutlinesAction()));
		debugShow.add(new JCheckBoxMenuItem(new InlineBoxesAction()));
		debugShow.add(new JCheckBoxMenuItem(new FontMetricsAction()));

		JMenu anti = new JMenu("Anti Aliasing");
		ButtonGroup anti_level = new ButtonGroup();
		addLevel(anti, anti_level, "None", -1);
		addLevel(anti, anti_level, "Low", 25).setSelected(true);
		addLevel(anti, anti_level, "Medium", 12);
		addLevel(anti, anti_level, "High", 0);
		debug.add(anti);


		debug.add(new ShowDOMInspectorAction());
		debug.add(new AbstractAction("Validation Console") {
			public void actionPerformed(ActionEvent evt) {
				if (root.validation_console == null) {
					root.validation_console = new JFrame("Validation Console");
					JFrame frame = root.validation_console;
					JTextArea jta = new JTextArea();

					root.error_handler.setTextArea(jta);

					jta.setEditable(false);
					jta.setLineWrap(true);
					jta.setText("Validation Console: XML Parsing Error Messages");

					frame.getContentPane().setLayout(new BorderLayout());
					frame.getContentPane().add(new JScrollPane(jta), "Center");
					JButton close = new JButton("Close");
					frame.getContentPane().add(close, "South");
					close.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							root.validation_console.setVisible(false);
						}
					});

					frame.pack();
					frame.setSize(400, 300);
				}
				root.validation_console.setVisible(true);
			}
		});

		debug.add(root.actions.generate_diff);
		add(debug);

		help.add(root.actions.usersManual);
		help.add(new JSeparator());
		help.add(root.actions.aboutPage);
		add(help);
	}

	private void populateDemoList() {
		List demoList = new ArrayList();
		URL url = BrowserMenuBar.class.getResource("/demos/file-list.txt");
		InputStream is = null;
		LineNumberReader lnr = null;
		if (url != null) {
			try {
				is = url.openStream();
				InputStreamReader reader = new InputStreamReader(is);
				lnr = new LineNumberReader(reader);
				try {
					String line;
					while ((line = lnr.readLine()) != null) {
						demoList.add(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						lnr.close();
					} catch (IOException e) {
						// swallow
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// swallow
					}
				}
			}

			for (Iterator itr = demoList.iterator(); itr.hasNext();) {
				String s = (String) itr.next();
				String s1[] = s.split(",");
				allDemos.put(s1[0], s1[1]);
			}
		}
	}

	private JRadioButtonMenuItem addLevel(JMenu menu, ButtonGroup group, String title, int level) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AntiAliasedAction(title, level));
		group.add(item);
		menu.add(item);
		return item;
	}

	public void createActions() {
		if (Configuration.isTrue("xr.use.listeners", true)) {
			List l = root.panel.view.getMouseTrackingListeners();
			for (Iterator i = l.iterator(); i.hasNext(); ) {
				FSMouseListener listener = (FSMouseListener)i.next();
				if ( listener instanceof LinkListener ) {
					root.panel.view.removeMouseTrackingListener(listener);
				}
			}

			root.panel.view.addMouseTrackingListener(new LinkListener() {
			   public void linkClicked(BasicPanel panel, String uri) {
				   if (uri.startsWith("demoNav")) {
					   String pg = uri.split(":")[1];
					   if (pg.equals("back")) {
						   navigateToPriorDemo();
					   } else {
						   navigateToNextDemo();
					   }
				   } else {
					   super.linkClicked(panel, uri);
				   }
			   } 
			});
		}
	}

	private ScaleFactor[] initializeScales() {
		ScaleFactor[] scales = new ScaleFactor[11];
		int i = 0;
		scales[i++] = new ScaleFactor(1.0d, "Normal (100%)");
		scales[i++] = new ScaleFactor(2.0d, "200%");
		scales[i++] = new ScaleFactor(1.5d, "150%");
		scales[i++] = new ScaleFactor(0.85d, "85%");
		scales[i++] = new ScaleFactor(0.75d, "75%");
		scales[i++] = new ScaleFactor(0.5d, "50%");
		scales[i++] = new ScaleFactor(0.33d, "33%");
		scales[i++] = new ScaleFactor(0.25d, "25%");
		scales[i++] = new ScaleFactor(ScaleFactor.PAGE_WIDTH, "Page width");
		scales[i++] = new ScaleFactor(ScaleFactor.PAGE_HEIGHT, "Page height");
		scales[i++] = new ScaleFactor(ScaleFactor.PAGE_WHOLE, "Whole page");
		return scales;
	}

	class ShowDOMInspectorAction extends AbstractAction {
		private DOMInspector inspector;
		private JFrame inspectorFrame;

		ShowDOMInspectorAction() {
			super("DOM Tree Inspector");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		}

		public void actionPerformed(ActionEvent evt) {
			if (inspectorFrame == null) {
				inspectorFrame = new JFrame("DOM Tree Inspector");
			}
			if (inspector == null) {
				inspector = new DOMInspector(root.panel.view.getDocument(), root.panel.view.getSharedContext(), root.panel.view.getSharedContext().getCss());

				inspectorFrame.getContentPane().add(inspector);

				inspectorFrame.pack();
				inspectorFrame.setSize(500, 600);
				inspectorFrame.show();
			} else {
				inspector.setForDocument(root.panel.view.getDocument(), root.panel.view.getSharedContext(), root.panel.view.getSharedContext().getCss());
			}
			inspectorFrame.show();
		}
	}

	class BoxOutlinesAction extends AbstractAction {
		BoxOutlinesAction() {
			super("Show Box Outlines");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
		}

		public void actionPerformed(ActionEvent evt) {
			root.panel.view.getSharedContext().setDebug_draw_boxes(!root.panel.view.getSharedContext().debugDrawBoxes());
			root.panel.view.repaint();
		}
	}

	class LineBoxOutlinesAction extends AbstractAction {
		LineBoxOutlinesAction() {
			super("Show Line Box Outlines");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		}

		public void actionPerformed(ActionEvent evt) {
			root.panel.view.getSharedContext().setDebug_draw_line_boxes(!root.panel.view.getSharedContext().debugDrawLineBoxes());
			root.panel.view.repaint();
		}
	}

	class InlineBoxesAction extends AbstractAction {
		InlineBoxesAction() {
			super("Show Inline Boxes");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		}

		public void actionPerformed(ActionEvent evt) {
			root.panel.view.getSharedContext().setDebug_draw_inline_boxes(!root.panel.view.getSharedContext().debugDrawInlineBoxes());
			root.panel.view.repaint();
		}
	}

	class FontMetricsAction extends AbstractAction {
		FontMetricsAction() {
			super("Show Font Metrics");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_F));
		}

		public void actionPerformed(ActionEvent evt) {
			root.panel.view.getSharedContext().setDebug_draw_font_metrics(!root.panel.view.getSharedContext().debugDrawFontMetrics());
			root.panel.view.repaint();
		}
	}

	class NextDemoAction extends AbstractAction {

		public NextDemoAction() {
			super("Next Demo Page");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			navigateToNextDemo();
		}
	}

	public void navigateToNextDemo() {
		String nextPage = null;
		for (Iterator iter = allDemos.keySet().iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			if (s.equals(lastDemoOpened)) {
				if (iter.hasNext()) {
					nextPage = (String) iter.next();
					break;
				}
			}
		}
		if (nextPage == null) {
			// go to first page
			Iterator iter = allDemos.keySet().iterator();
			nextPage = (String) iter.next();
		}

		try {
			root.panel.loadPage((String) allDemos.get(nextPage));
			lastDemoOpened = nextPage;
		} catch (Exception ex) {
			Uu.p(ex);
		}
	}

	class PriorDemoAction extends AbstractAction {

		public PriorDemoAction() {
			super("Prior Demo Page");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			navigateToPriorDemo();
		}
	}

	public void navigateToPriorDemo() {
		String priorPage = null;
		for (Iterator iter = allDemos.keySet().iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			if (s.equals(lastDemoOpened)) {
				break;
			}
			priorPage = s;
		}
		if (priorPage == null) {
			// go to last page
			Iterator iter = allDemos.keySet().iterator();
			while (iter.hasNext()) {
				priorPage = (String) iter.next();
			}
		}

		try {
			root.panel.loadPage((String) allDemos.get(priorPage));
			lastDemoOpened = priorPage;
		} catch (Exception ex) {
			Uu.p(ex);
		}
	}

	class LoadAction extends AbstractAction {
		protected String url;

		private String pageName;

		public LoadAction(String name, String url) {
			super(name);
			pageName = name;
			this.url = url;
		}

		public void actionPerformed(ActionEvent evt) {
			try {
				root.panel.loadPage(url);
				lastDemoOpened = pageName;
			} catch (Exception ex) {
				Uu.p(ex);
			}
		}

	}

	class AntiAliasedAction extends AbstractAction {
		int fontSizeThreshold;

		AntiAliasedAction(String text, int fontSizeThreshold) {
			super(text);
			this.fontSizeThreshold = fontSizeThreshold;
		}

		public void actionPerformed(ActionEvent evt) {
			root.panel.view.getSharedContext().getTextRenderer().setSmoothingThreshold(fontSizeThreshold);
			root.panel.view.repaint();
		}
	}

}

class EmptyAction extends AbstractAction {
	public EmptyAction(String name, Icon icon) {
		this(name, "", icon);
	}

	public EmptyAction(String name, String shortDesc, Icon icon) {
		super(name, icon);
		putValue(Action.SHORT_DESCRIPTION, shortDesc);
	}

	public EmptyAction(String name, int accel) {
		this(name);
		putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(accel,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public EmptyAction(String name) {
		super(name);
	}

	public void actionPerformed(ActionEvent evt) {
	}
}
