package org.xhtmlrenderer.demo.browser;

import org.xhtmlrenderer.util.GeneralUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;

public class DirectoryLister {
	public static String list(File file) {
		StringBuffer sb = new StringBuffer();

		sb.append("<html>");
		sb.append("<head>");
		sb.append("<title>Directory listing for ");
		sb.append(file.getPath());
		sb.append("</title>");
		sb.append("<style>");
		sb.append("body { font-family: monospaced; }");
		sb.append("ul { background-color: #ddffdd; }");
		sb.append("li { list-style-type: none; }");
		sb.append("a { text-decoration: none; }");
		sb.append(".dir { font-weight: bold; color: #ff9966; }");
		sb.append(".file { font-weight: normal; color: #003333; }");
		sb.append("</style>");
		sb.append("</head>");
		sb.append("<body>");
		sb.append("<h2>Index of ");
		sb.append(file.toString());
		sb.append("</h2>");
		sb.append("<hr />");

		if (file.isDirectory()) {
			String loc = null;
			try {
				File parent = file.getParentFile();
				if ( parent != null ) {
					loc = GeneralUtil.htmlEscapeSpace(file.getAbsoluteFile().getParentFile().toURL().toExternalForm()).toString();
					sb.append("<a class='dir' href='" + loc + "'>Up to higher level directory</a>");
				}
			} catch (MalformedURLException e) {
				// skip
			}
			sb.append("<table style='width: 75%'>");
			File[] files = file.listFiles();
			String cls = "";
			String img = "";
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				if ( f.isHidden() ) continue;
				long len = f.length();
				String lenDesc = ( len > 1024 ? new DecimalFormat("#,###KB").format(len / 1024) : "");
				String lastMod = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(new Date(f.lastModified()));
				sb.append("<tr>");
				if (files[i].isDirectory()) {
					cls = "dir";
				} else {
					cls = "file";
				}
				try {
					loc = GeneralUtil.htmlEscapeSpace(files[i].toURL().toExternalForm()).toString();
					sb.append("<td><a class='" + cls + "' href='" + loc + "'>" +
							files[i].getName() +
							"</a></td>" +
							"<td>" + lenDesc + "</td>" +
							"<td>" + lastMod + "</td>"
					);
				} catch (MalformedURLException e) {
					sb.append(files[i].getAbsolutePath());
				}
				sb.append("</tr>");
			}
			sb.append("</table>");
		}

		sb.append("<hr />");
		sb.append("</body></html>");

		return sb.toString();
	}
}
