package org.vibehistorian.vibecomposer.Popups;

import org.vibehistorian.vibecomposer.Components.RandomIntegerListButton;
import org.vibehistorian.vibecomposer.LG;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

public class ButtonIntegerListValuePopup extends CloseablePopup {
	private RandomIntegerListButton butt = null;
	private String customInput = null;
	private JTextField intListField = new JTextField("", 10);
	public int randomNum = Integer.MIN_VALUE;

	public ButtonIntegerListValuePopup(RandomIntegerListButton butt) {
		super("Button Value Setting", 0);
		this.butt = butt;
		Random rand = new Random();
		randomNum = rand.nextInt();
		intListField.setText(butt.getText());
		intListField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					close();
				}
			}
		});
		frame.add(intListField);
		frame.pack();
		frame.setVisible(true);

	}

	protected void addFrameWindowOperation() {
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent e) {
				if (frame.isVisible()) {
					try {
						customInput = intListField.getText();
					} catch (NumberFormatException ex) {
						LG.d("Invalid value: " + intListField.getText());
					}
					if (customInput != null) {
						butt.setValue(customInput);
					}
				}


				/*if (RandomValueButton.singlePopup != null) {
					if (RandomValueButton.singlePopup.randomNum == randomNum) {
						RandomValueButton.singlePopup = null;
					}
				}*/

			}

			@Override
			public void windowClosed(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowActivated(WindowEvent e) {
				// Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// Auto-generated method stub

			}

		});

	}
}
