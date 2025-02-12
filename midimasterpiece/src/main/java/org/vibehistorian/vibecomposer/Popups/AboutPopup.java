package org.vibehistorian.vibecomposer.Popups;

import org.vibehistorian.vibecomposer.LG;

import javax.swing.*;
import java.awt.*;

public class AboutPopup extends CloseablePopup {
	JTextArea textArea;
	JScrollPane scroll;

	public AboutPopup() {
		super("About", 1, new Point(-400, 30));
		textArea = new JTextArea(24, 80);
		textArea.setBackground(Color.BLACK);
		textArea.setForeground(Color.LIGHT_GRAY);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		scroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		textArea.append("Copyright \u00a9 Vibe Historian 2023");
		textArea.append("" + "\r\n"
				+ "This program is free software; you can redistribute it and/or modify\r\n"
				+ "it under the terms of the GNU General Public License as published by\r\n"
				+ "the Free Software Foundation; either version 2 of the License, or any\r\n"
				+ "later version.\r\n" + "\r\n"
				+ "This program is distributed in the hope that it will be useful, but\r\n"
				+ "WITHOUT ANY WARRANTY; without even the implied warranty of\r\n"
				+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the\r\n"
				+ "GNU General Public License for more details.\r\n" + "\r\n"
				+ "You should have received a copy of the GNU General Public License\r\n"
				+ "along with this program; if not,\r\n" + "see <https://www.gnu.org/licenses/>.");
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		frame.add(scroll);
		frame.pack();
		frame.setVisible(true);

		LG.d("Opened About page!");
	}

	@Override
	protected void addFrameWindowOperation() {
		// do nothing

	}
}
