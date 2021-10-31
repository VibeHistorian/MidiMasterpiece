package org.vibehistorian.vibecomposer.Helpers;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.vibehistorian.vibecomposer.Arrangement;
import org.vibehistorian.vibecomposer.VibeComposerGUI;

public class SectionDropDownCheckButton extends CheckButton {

	private static final long serialVersionUID = -5179430651277878280L;
	private List<String> dropDownOptions = null;
	public static int popupIndex = 0;

	public SectionDropDownCheckButton(String name, boolean sel, Color alphen) {
		super(name, sel, alphen);

		dropDownOptions = new ArrayList<>();
		Arrangement.defaultSections.keySet().forEach(e -> dropDownOptions.add(e));
		final JPopupMenu popup = new JPopupMenu();
		for (int i = 0; i < dropDownOptions.size(); i++) {
			String e = dropDownOptions.get(i);
			JMenuItem newE = new JMenuItem(e);
			newE.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					VibeComposerGUI.vibeComposerGUI
							.handleArrangementAction("ArrangementAddNewSection," + e, 0, 0);
					System.out.println("popupindex: " + popupIndex);
				}
			});
			popup.add(newE);
		}

		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent evt) {
				if (SwingUtilities.isRightMouseButton(evt) && dropDownOptions != null) {
					if (Character.isDigit(SectionDropDownCheckButton.this.getText().charAt(0))) {
						// not global
						String digits = SectionDropDownCheckButton.this.getText().split(":")[0];
						Integer index = Integer.valueOf(digits);
						popupIndex = index;
					} else {
						// global/first
						popupIndex = 0;
					}
					popup.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}

		});
	}

	public SectionDropDownCheckButton(String name, boolean sel) {
		this(name, sel, null);
	}
}
