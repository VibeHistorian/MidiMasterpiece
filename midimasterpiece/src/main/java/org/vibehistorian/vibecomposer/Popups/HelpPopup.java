package org.vibehistorian.vibecomposer.Popups;

import org.vibehistorian.vibecomposer.LG;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HelpPopup {

	public HelpPopup() {
		/*textArea = new JTextArea(24, 80);
		textArea.setBackground(Color.BLACK);
		textArea.setForeground(Color.LIGHT_GRAY);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		scroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		
		frame.add(scroll);
		frame.pack();
		frame.setVisible(true);*/

		try {
			Desktop.getDesktop().browse(new URI(
					"https://github.com/VibeHistorian/VibeComposer/blob/development_master/midimasterpiece/VibeComposer_UserManual.pdf"));
		} catch (IOException | URISyntaxException e) {
			// Auto-generated catch block
			LG.e("Error opening GitHub link!", e);
		}

		LG.d("Opened Help page!");
	}
}
