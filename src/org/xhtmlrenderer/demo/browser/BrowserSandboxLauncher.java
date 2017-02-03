package org.xhtmlrenderer.demo.browser;

public class BrowserSandboxLauncher {
	public static void main(String[] args) {
		BrowserStartup bs = new BrowserStartup();
		bs.initUI();

		bs.panel.url.setVisible(false);
		bs.panel.goToPage.setVisible(false);
		bs.actions.open_file.setEnabled(false);

		bs.launch();
	}
}
