package org.xhtmlrenderer.demo.browser;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class ValidationConsole extends JInternalFrame {
	private final ValidationHandler error_handler = new ValidationHandler();

	public ValidationConsole(final BrowserStartup root) {
		super("Validation Console");
		root.getDesktopPane().add(this);
		final JTextArea jta = new JTextArea();
		error_handler.setTextArea(jta);
		jta.setEditable(false);
		jta.setLineWrap(true);
		jta.setText("Validation Console: XML Parsing Error Messages");
		final Container panel = getContentPane();
		panel.setLayout(new BorderLayout());
		panel.add(new JScrollPane(jta), "Center");
		final JButton close = new JButton("Close");
		panel.add(close, "South");
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				doDefaultCloseAction();
			}
		});
		root.addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(final InternalFrameEvent event) {
				doDefaultCloseAction();
			}
		});
		pack();
		setSize(400, 300);
		setClosable(true);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}
}
