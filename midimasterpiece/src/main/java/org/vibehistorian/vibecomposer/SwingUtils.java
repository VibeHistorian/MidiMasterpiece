package org.vibehistorian.vibecomposer;

import org.apache.commons.lang3.tuple.Pair;
import org.vibehistorian.vibecomposer.Panels.InstPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SwingUtils {

	public static List<JPopupMenu> popupMenus = new ArrayList<>();

	public static double getScrolledPosition(JScrollPane pane, boolean horizontal) {
		//LG.i("Get scrl pos: " + pane.getHorizontalScrollBar().getVisibleAmount() / 2.0);
		if (horizontal) {
			return (pane.getHorizontalScrollBar().getValue())
					/ (double) pane.getHorizontalScrollBar().getMaximum();
		} else {
			return (pane.getVerticalScrollBar().getValue())
					/ (double) pane.getVerticalScrollBar().getMaximum();
		}
	}

	public static void setScrolledPosition(JScrollPane pane, boolean horizontal,
			double percentage) {
		//LG.i("Set scrl pos: " + pane.getHorizontalScrollBar().getMaximum() / 2.0);
		if (horizontal) {
			pane.getHorizontalScrollBar().setValue(
					Math.max(0, (int) (percentage * pane.getHorizontalScrollBar().getMaximum())));
		} else {
			pane.getVerticalScrollBar().setValue(
					Math.max(0, (int) (percentage * pane.getVerticalScrollBar().getMaximum())));
		}
	}

	public static void setupScrollpanePriorityScrolling(JScrollPane pane) {
		if (pane.getMouseWheelListeners() != null) {
			for (MouseWheelListener mwl : pane.getMouseWheelListeners()) {
				pane.removeMouseWheelListener(mwl);
			}
		}

		pane.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				int scrollableVerticalGap = pane.getVerticalScrollBar().getMaximum()
						- pane.getVerticalScrollBar().getVisibleAmount();
				//LG.i("Scrollable gap: " + scrollableVerticalGap);
				if (scrollableVerticalGap < 15 || e.isShiftDown()) {
					// Horizontal scrolling
					Adjustable adj = pane.getHorizontalScrollBar();
					int scroll = e.getUnitsToScroll() * adj.getBlockIncrement() * 6;
					adj.setValue(adj.getValue() + scroll);
				} else {
					// Vertical scrolling
					Adjustable adj = pane.getVerticalScrollBar();
					int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
					adj.setValue(adj.getValue() + scroll);
				}
			}
		});
	}

	public static int getDrawStringWidth(String text) {
		AffineTransform affinetransform = new AffineTransform();
		FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
		Font font = new Font("Tahoma", Font.PLAIN, 12);
		int textwidth = (int) (font.getStringBounds(text, frc).getWidth());
		//int textheight = (int)(font.getStringBounds(text, frc).getHeight());
		return textwidth;
	}

	public static void flashComponentCustom(final JComponent field, BiConsumer<JComponent, Boolean> customFlasher,
									  final int timerDelay, int totalTime) {
		final int totalCount = totalTime / timerDelay;
		javax.swing.Timer timer = new javax.swing.Timer(timerDelay, new ActionListener() {
			int count = 0;

			public void actionPerformed(ActionEvent evt) {
				if (count % 2 == 0) {
					customFlasher.accept(field, true);
				} else {
					customFlasher.accept(field, false);
					if (count >= totalCount) {
						((Timer) evt.getSource()).stop();
					}
				}
				count++;
			}
		});
		timer.start();
	}

	public static void flashComponent(final JComponent field, Color flashColor,
			final int timerDelay, int totalTime) {
		flashComponentCustom(field, (f, state) -> {
			f.setBackground(state ? flashColor : null);
		}, timerDelay, totalTime);
	}

	public static Pair<JPopupMenu, MouseListener> addPopupMenu(JComponent comp, BiConsumer<ActionEvent, String> actionOnSelect,
			Function<MouseEvent, Boolean> actionOnMousePress, List<String> displayedPopupItems,
			List<Color> popupItemColors) {
		return addPopupMenu(comp, actionOnSelect, actionOnMousePress, displayedPopupItems, popupItemColors,
				1);
	}

	public static Pair<JPopupMenu, MouseListener> addPopupMenu(JComponent comp, BiConsumer<ActionEvent, String> actionOnSelect,
			Function<MouseEvent, Boolean> actionOnMousePress, List<String> displayedPopupItems,
			List<Color> popupItemColors, int columns) {
		final JPopupMenu popup = new JPopupMenu();
		popup.setOpaque(true);
		popup.setLayout(new GridLayout(0, columns));
		popupMenus.add(popup);
		for (int i = 0; i < displayedPopupItems.size(); i++) {
			String e = displayedPopupItems.get(i);
			final int fI = i;

			JMenuItem newE;
			if (popupItemColors != null) {
				newE = new JMenuItem(e) {
					private static final long serialVersionUID = -2776813999053048654L;

					@Override
					protected void paintComponent(Graphics g) {
						boolean sel = this.isArmed();
						int w = getWidth();
						int h = getHeight();
						g.setColor(new Color(60, 60, 60));
						g.fillRect(0, 0, w, h);
						Color newColor = sel
								? OMNI.mixColor(popupItemColors.get(fI), Color.white, 0.2)
								: popupItemColors.get(fI);
						g.setColor(OMNI.alphen(newColor, newColor.getAlpha() * 8 / 10));
						g.fillRect(0, 0, w, h);
						g.setColor(new Color(210, 210, 210));
						g.drawString(getText(), 3, h - 3);
					}
				};
			} else {
				newE = new JMenuItem(e);
			}
			newE.setOpaque(true);
			if (popupItemColors != null) {
				newE.setBackground(popupItemColors.get(i));
			}
			newE.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					actionOnSelect.accept(evt, e);
				}
			});
			popup.add(newE);
		}
		MouseListener listener = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent evt) {
				Boolean goodPress = actionOnMousePress.apply(evt);
				if (goodPress != null && goodPress) {
					popup.show(evt.getComponent(), evt.getX() - popup.getWidth() / 4, evt.getY());
				}
			}

		};
		comp.addMouseListener(listener);
		return Pair.of(popup, listener);
	}

	public static InstPanel getInstParent(Component comp) {
		Container maybeParent = comp.getParent();
		int depth = 5;
		while (depth >= 0) {
			if (maybeParent instanceof InstPanel) {
				return (InstPanel) maybeParent;
			} else {
				maybeParent = maybeParent.getParent();
				depth--;
			}
		}
		return (maybeParent instanceof InstPanel) ? (InstPanel) maybeParent : null;
	}

	public static Point getMouseLocation() {
		//Point mousePointFixed = new Point(mp);
		/*if (VibeComposerGUI.vibeComposerGUI.getLocation().x < 0) {
			//mp.x *= -1;
		}
		LG.i("Mouse: " + mp.toString());*/
		return MouseInfo.getPointerInfo().getLocation();
	}

	public static void setFrameLocation(JDialog frame, Point loc) {
		loc.x = Math.max(VibeComposerGUI.vibeComposerGUI.getLocation().x + 50, loc.x);
		loc.y = Math.max(VibeComposerGUI.vibeComposerGUI.getLocation().y + 50, loc.y);
		if (loc.x < 0) {
			Timer tmr = new Timer(50, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.setLocation(loc);
				}
			});
			tmr.setRepeats(false);
			tmr.start();
		} else {
			frame.setLocation(loc);
		}
	}
}
