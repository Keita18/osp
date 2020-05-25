/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

import java.awt.*; // uses Abstract Window Toolkit (awt)
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JViewport;

import org.opensourcephysics.display3d.simple3d.DrawingPanel3D;
import org.opensourcephysics.tools.FontSizer;

/**
 * PixelRectangle demonstrates how to draw an object using the AWT drawing API.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0 05/16/05
 */
public class MessageDrawable implements Drawable {
	String tlStr = null; // "top left";
	String trStr = null; // "top right";
	String blStr = null; // "bottom left";
	String brStr = null; // "bottom right";

	protected Font font;
	protected String fontname = "TimesRoman"; // The logical name of the font to use //$NON-NLS-1$
	protected int fontsize = 12; // The font size
	protected int fontstyle = Font.PLAIN; // The font style
	protected boolean ignoreRepaint=false;

	protected PropertyChangeListener guiChangeListener;

	/**
	 * Constructs a MessageDrawable.
	 *
	 */
	public MessageDrawable() {
		font = new Font(fontname, fontstyle, fontsize);
		guiChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				System.err.println("Property changed ="+e);
				if (e.getPropertyName().equals(FontSizer.PROPERTY_LEVEL)) { //$NON-NLS-1$
					int level = ((Integer) e.getNewValue()).intValue();
					setFontLevel(level);
				}
			}
		};
		FontSizer.addPropertyChangeListener(FontSizer.PROPERTY_LEVEL, guiChangeListener); //$NON-NLS-1$
	}
	
	public void setIgnoreRepaint(boolean ignore){
		ignoreRepaint=ignore;
	}

	/**
	 * Sets the font factor.
	 *
	 * @param factor the factor
	 */
	public void setMessageFont(Font aFont) {
		if (aFont!=null) font = aFont;
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	protected void setFontLevel(int level) {
		font = FontSizer.getResizedFont(font, level);
		System.err.println("Setting front level ="+level);
	}

	/**
	 * Sets the font factor.
	 *
	 * @param factor the factor
	 */
	public void setFontFactor(double factor) {
		font = FontSizer.getResizedFont(font, factor);
	}

	/**
	 * Shows a message in a yellow text box in the lower right hand corner.
	 *
	 * @param msg
	 */
	public void setMessage(String msg) {
		setMessage(msg, DrawingPanel.BOTTOM_RIGHT);
	}

	/**
	 * Shows a message in a yellow text box.
	 *
	 * location 0=bottom left location 1=bottom right location 2=top right location
	 * 3=top left
	 *
	 * @param msg
	 * @param location
	 */
	public void setMessage(String msg, int location) {
		if (msg != null) {
			if (msg.length() == 0)
				msg = null;
			else
				msg = TeXParser.parseTeX(msg);
		}
		switch (location) {
		case DrawingPanel.BOTTOM_LEFT: // usually used for mouse coordinates
			blStr = msg;
			break;
		case DrawingPanel.BOTTOM_RIGHT:
			brStr = msg;
			break;
		case DrawingPanel.TOP_RIGHT:
			trStr = msg;
			break;
		case DrawingPanel.TOP_LEFT:
			tlStr = msg;
			break;
		}
	}
	
	/**
	 * Draws this message boxes on a DrawingPanel3D. Required to implement the
	 * Drawable interface.
	 *
	 * @param panel DrawingPanel
	 * @param g     Graphics
	 */
	//public void drawOn3D(DrawingPanel3D panel, Graphics g) {
	public void drawOn3D(Component panel, Graphics g) {
    if(ignoreRepaint) return;
		// DB if DrawingPanel is in a scrollpane then use view rect for positioning
		Rectangle port = null;
		if (panel.getParent() instanceof JViewport) {
			port = ((JViewport)panel.getParent()).getViewRect();
		}		
		g = g.create();
//		/** @j2sNative g.unclip$I(-3); */
		Font oldFont = g.getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int vertOffset = fm.getDescent();
		int height = fm.getAscent() + 1 + vertOffset; // string height
		int width = 0; // string width
		g.setClip(0, 0, panel.getWidth(), panel.getHeight());
		// this method implements the Drawable interface
		if (tlStr != null && !tlStr.equals("")) { // draw tl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(tlStr) + 6; // current string width
			int x = port==null? 0: port.x;
			int y = port==null? 0: port.y;
			g.fillRect(x, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width, height);
			g.drawString(tlStr, x + 4, y + height - vertOffset);
		}

		if (trStr != null) { // draw tr message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(trStr) + 8; // current string width
			int x = port==null? panel.getWidth() - width: port.x + port.width - width;
			int y = port==null? 0: port.y;
			g.fillRect(x - 1, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y, width, height); // fills rectangle
			g.drawString(trStr, x + 4, y + height - vertOffset);
		}
		if (blStr != null) { // draw bl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(blStr) + 6; // current string width
			int x = port==null? 0: port.x;
			int y = port==null? panel.getHeight() - height: port.y + port.height - height;
			g.fillRect(x, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y - 1, width, height);
			g.drawString(blStr, x + 4, y + height - vertOffset - 1);
		}
		if (brStr != null) { // draw br message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(brStr) + 8; // current string width
			int x = port==null? panel.getWidth() - width: port.x + port.width - width;
			int y = port==null? panel.getHeight() - height: port.y + port.height - height;
			g.fillRect(x - 1, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y - 1, width, height); // outlines rectangle
			g.drawString(brStr, x + 4, y + height - vertOffset - 1);
		}
//		/** @j2sNative g.unclip$I(3); */
		g.setFont(oldFont);
		g.dispose();
	
	}

	/**
	 * Draws this rectangle using the AWT drawing API. Required to implement the
	 * Drawable interface.
	 *
	 * @param panel DrawingPanel
	 * @param g     Graphics
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		if(ignoreRepaint) return;
		// DB if DrawingPanel is in a scrollpane then use view rect for positioning
		Rectangle port = null;
		if (panel.getParent() instanceof JViewport) {
			port = ((JViewport)panel.getParent()).getViewRect();
		}		
		g = g.create();
//		/** @j2sNative g.unclip$I(-3); */
		Font oldFont = g.getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int vertOffset = fm.getDescent();
		int height = fm.getAscent() + 1 + vertOffset; // string height
		int width = 0; // string width
		g.setClip(0, 0, panel.getWidth(), panel.getHeight());
		// this method implements the Drawable interface
		if (tlStr != null) { // draw tl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(tlStr) + 6; // current string width
			int x = port==null? 0: port.x;
			int y = port==null? 0: port.y;
			g.fillRect(x, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width, height);
			g.drawString(tlStr, x + 4, y + height - vertOffset);
		}

		if (trStr != null) { // draw tr message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(trStr) + 8; // current string width
			int x = port==null? panel.getWidth() - width: port.x + port.width - width;
			int y = port==null? 0: port.y;
			g.fillRect(x - 1, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y, width, height); // fills rectangle
			g.drawString(trStr, x + 4, y + height - vertOffset);
		}
		if (blStr != null) { // draw bl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(blStr) + 14; // current string width
			int x = port==null? 0: port.x;
			int y = port==null? panel.getHeight() - height: port.y + port.height - height;
			g.fillRect(x, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y - 1, width, height);
			g.drawString(blStr, x + 4, y + height - vertOffset - 1);
		}
		if (brStr != null) { // draw br message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(brStr) + 8; // current string width
			int x = port==null? panel.getWidth() - width: port.x + port.width - width;
			int y = port==null? panel.getHeight() - height: port.y + port.height - height;
			g.fillRect(x - 1, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y - 1, width, height); // outlines rectangle
			g.drawString(brStr, x + 4, y + height - vertOffset - 1);
		}
//		/** @j2sNative g.unclip$I(3); */
		g.setFont(oldFont);
		g.dispose();
	}
}
