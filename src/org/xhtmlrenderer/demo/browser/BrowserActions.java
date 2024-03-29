package org.xhtmlrenderer.demo.browser;

import org.xhtmlrenderer.demo.browser.actions.CopySelectionAction;
import org.xhtmlrenderer.demo.browser.actions.FontSizeAction;
import org.xhtmlrenderer.demo.browser.actions.GenerateDiffAction;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.util.Uu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import linoleum.application.FileChooser;

public class BrowserActions {
	public Action open_file, export_pdf , quit, print;
	public Action forward, backward, refresh, reload, load, stop, print_preview, goHome;
	public Action generate_diff, usersManual, aboutPage;
	public BrowserStartup root;
	public Action increase_font, decrease_font, reset_font;
	public Action goToPage;
	public static final Logger logger = Logger.getLogger("app.browser");
	private final FileChooser chooser = new FileChooser();

	public BrowserActions(final BrowserStartup root) {
		this.root = root;
	}

	public void init() {
		URL url = null;
		url = getImageUrl("images/process-stop.png");
		stop = new AbstractAction("Stop", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				// TODO: stop not coded
				System.out.println("stop called");
				// root.panel.view.stop();
			}
		};
		// TODO: need right API call for ESC
		//stop.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE));
		stop.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);

		open_file = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				openAndShowFile();
			}
		};
		open_file.putValue(Action.NAME, "Open File...");
		setAccel(open_file, KeyEvent.VK_O);
		setMnemonic(open_file, KeyEvent.VK_O);
		
		export_pdf = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				exportToPdf();
			}
		};
		export_pdf.putValue(Action.NAME, "Export PDF...");
		// is iText in classpath ? 
		try {
			Class.forName("com.lowagie.text.DocumentException");
		} catch(final ClassNotFoundException e) {
			export_pdf.setEnabled(false);
		}
		
		/*setAccel(export_pdf, KeyEvent.VK_E);
		setMnemonic(export_pdf, KeyEvent.VK_E);*/

		/* printing disabled for R6
		url = getImageUrl("images/document-print.png");
		print = new PrintAction(root, new ImageIcon(url));
		setAccel(print, KeyEvent.VK_P);
		setMnemonic(print, KeyEvent.VK_P);
		*/

		quit = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				root.doDefaultCloseAction();
			}
		};

		setName(quit, "Quit");
		setAccel(quit, KeyEvent.VK_Q);
		setMnemonic(quit, KeyEvent.VK_Q);
		
		url = getImageUrl("images/go-previous.png");
		backward = new EmptyAction("Back", "Go back one page", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.goBack();
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		backward.setEnabled(false);
		backward.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
						KeyEvent.ALT_MASK));


		url = getImageUrl("images/go-next.png");
		forward = new EmptyAction("Forward", "Go forward one page", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.goForward();
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};
		forward.setEnabled(false);
		forward.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
						KeyEvent.ALT_MASK));

		url = getImageUrl("images/view-refresh.png");
		refresh = new EmptyAction("Refresh", "Refresh page", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.view.invalidate();
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};
		refresh.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke("F5"));

		url = getImageUrl("images/view-refresh.png");
		reload = new EmptyAction("Reload", "Reload page", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.reloadPage();
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};
		reload.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F5,
						InputEvent.SHIFT_MASK));
		reload.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);

		print_preview = new EmptyAction("Print Preview", "Print preview mode", null) {
			public void actionPerformed(ActionEvent evt) {
				togglePrintPreview();
			}
		};
		print_preview.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);

		load = new AbstractAction("Load") {
			public void actionPerformed(ActionEvent evt) {
				try {
					String url_text = root.panel.url.getText();
					root.panel.loadPage(url_text);
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		url = getImageUrl("images/media-playback-start_16x16.png");
		goToPage = new EmptyAction("Go", "Go to URL in address bar", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					String url_text = root.panel.url.getText();
					root.panel.loadPage(url_text);
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		url = getImageUrl("images/go-home.png");
		goHome = new EmptyAction("Go Home", "Browser homepage", new ImageIcon(url)) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.loadPage(root.startPage);
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		usersManual = new EmptyAction("FS User's Guide", "Flying Saucer User's Guide", null) {
			public void actionPerformed(ActionEvent evt) {
				try {
					root.panel.loadPage("/users-guide-r8.html");
					root.panel.view.repaint();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		aboutPage = new EmptyAction("About", "About the Browser Demo", null) {
			public void actionPerformed(ActionEvent evt) {
				try {
					showAboutDialog();
				} catch (Exception ex) {
					Uu.p(ex);
				}
			}
		};

		generate_diff = new GenerateDiffAction(root);

		increase_font = new FontSizeAction(root, FontSizeAction.INCREMENT);
		increase_font.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		increase_font.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);

		reset_font = new FontSizeAction(root, FontSizeAction.RESET);
		reset_font.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_0,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		reset_font.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);

		decrease_font = new FontSizeAction(root, FontSizeAction.DECREMENT);
		decrease_font.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		decrease_font.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

		setName(increase_font, "Increase");
		setName(reset_font, "Normal");
		setName(decrease_font, "Decrease");
	}

	private void showAboutDialog() {
		final PanelManager uac = new PanelManager();
		final XHTMLPanel panel = new XHTMLPanel(uac);
		uac.setRepaintListener(panel);
		panel.setOpaque(false);
		panel.setDocument("demo:/demos/about.xhtml");
		panel.setPreferredSize(new Dimension(500, 400));
		JOptionPane.showInternalMessageDialog(root, panel, "About the Browser Demo", JOptionPane.PLAIN_MESSAGE);
	}

	private void togglePrintPreview() {
		try {
			SharedContext sharedContext = root.panel.view.getSharedContext();

			// flip status--either we are in "print" mode (print media) or non-print (screen media)
			if (sharedContext.isPrint()) {
				sharedContext.setPrint(false);
				sharedContext.setInteractive(true);
			} else {
				sharedContext.setPrint(true);
				sharedContext.setInteractive(false);
			}
			print_preview.putValue(Action.SHORT_DESCRIPTION,
					! sharedContext.isPrint() ? "Print preview" : "Normal view");
			root.panel.reloadPage();
			root.panel.view.repaint();
		} catch (Exception ex) {
			Uu.p(ex);
		}
	}

	private void openAndShowFile() {
		switch (chooser.showInternalOpenDialog(root)) {
		case JFileChooser.APPROVE_OPTION:
			try {
				final String url = chooser.getSelectedFile().toURI().toURL().toString();
				root.panel.loadPage(url);
			} catch (final MalformedURLException ex) {
				logger.info("error:" + ex);
			}
			break;
		default:
		}
	}

	private void exportToPdf() {
		switch (chooser.showInternalSaveDialog(root)) {
		case JFileChooser.APPROVE_OPTION:
			final File outTarget = chooser.getSelectedFile();
			root.panel.exportToPdf(outTarget.getAbsolutePath());
			break;
		default:
		}
	}

	public static void setName(Action act, String name) {
		act.putValue(Action.NAME, name);
	}

	public static void setAccel(Action act, int key) {
		act.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(key,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public static void setMnemonic(Action act, Integer mnem) {
		act.putValue(Action.MNEMONIC_KEY, mnem);
	}

	public static URL getImageUrl(String url) {
		return BrowserActions.class.getClassLoader().getResource(url);
	}
}
