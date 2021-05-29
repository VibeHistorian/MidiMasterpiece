package org.vibehistorian.midimasterpiece.midigenerator.Panels;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;

public class NumPanel extends JPanel {

	private static final long serialVersionUID = -2145278227995141172L;

	private JTextField text = null;
	private JLabel label = null;
	private JSlider slider = null;
	boolean needToReset = false;
	private int naturalMax = 100;
	private int naturalMin = 0;

	public NumPanel(String name, int value) {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		label = new JLabel(name);
		text = new JTextField(String.valueOf(value), 2);
		slider = new JSlider();
		initSlider(0, 100, value);
		initText();
		add(label);
		add(text);
		add(slider);
	}

	public NumPanel(String name, int value, int minimum, int maximum) {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		label = new JLabel(name);
		text = new JTextField(String.valueOf(value), maximum > 999 ? 3 : 2);
		slider = new JSlider();
		initSlider(minimum, maximum, value);
		initText();
		add(label);
		add(text);
		add(slider);
		naturalMax = maximum;
		naturalMin = minimum;
	}

	private void initSlider(int minimum, int maximum, int value) {
		slider.setMinimum(minimum);
		slider.setMaximum(maximum);
		slider.setValue(value);
		slider.setOrientation(JSlider.VERTICAL);
		slider.setPreferredSize(new Dimension(20, 40));
		//slider.setPaintTicks(true);

		slider.addMouseListener(new MouseAdapter() {
			boolean dragging = false;
			Thread numCycle = null;

			@Override
			public void mousePressed(MouseEvent me) {
				if (me.isShiftDown()) {
					needToReset = true;
					int potentialMax = slider.getValue() + slider.getMaximum() / 10;
					int potentialMin = slider.getValue() - slider.getMaximum() / 10;
					slider.setMaximum((potentialMax > slider.getMaximum()) ? slider.getMaximum()
							: potentialMax);
					slider.setMinimum((potentialMin < slider.getMinimum()) ? slider.getMinimum()
							: potentialMin);
				}
				dragging = true;
				startNumSliderThread(me);
			}

			@Override
			public void mouseReleased(MouseEvent me) {
				dragging = false;
				if (needToReset) {
					needToReset = false;
					slider.setMaximum(naturalMax);
					slider.setMinimum(naturalMin);
				}
			}

			public void startNumSliderThread(MouseEvent me) {
				if (numCycle != null && numCycle.isAlive()) {
					System.out.println("Label slider thread already exists! " + label.getText());
					return;
				}
				System.out.println("Starting new label slider thread..! " + label.getText());
				numCycle = new Thread() {

					public void run() {
						while (dragging) {
							updateToolTip();
							try {
								sleep(25);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				};
				numCycle.start();

			}

			public void updateToolTip() {
				text.setText(String.valueOf(slider.getValue()));
			}
		});
	}

	private void initText() {
		//text.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		text.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				tryUpdate();

			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				tryUpdate();

			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				tryUpdate();

			}

		});
	}

	private void tryUpdate() {
		if (StringUtils.isEmpty(text.getText())) {
			return;
		}
		int tryValue = 0;
		try {
			tryValue = Integer.valueOf(text.getText());
			if (tryValue > slider.getMaximum()) {
				slider.setValue(slider.getMaximum());
				updateTextLater();
			} else {
				slider.setValue(tryValue);
			}

		} catch (NumberFormatException ex) {
			System.out.println("Invalid value: " + text.getText());
		}
	}

	private void updateTextLater() {
		Runnable doAssist = new Runnable() {
			@Override
			public void run() {
				text.setText(slider.getMaximum() + "");
			}
		};
		SwingUtilities.invokeLater(doAssist);
	}

	public String getName() {
		return label.getText();
	}

	public int getInt() {
		return Integer.valueOf(text.getText());
	}

	public void setInt(int val) {
		text.setText(val + "");
	}
}
