/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.SwingPropertyChangeSupport;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.tools.DataTool;

/**
 * This is the abstract base class for all image filters. Note: subclasses
 * should always provide a no-argument constructor.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public abstract class Filter {

	protected static final int ROTATE_NONE = -1, ROTATE_CCW_90 = 0, ROTATE_CW_90 = 1, ROTATE_180 = 2;

	public static final String PROPERTY_FILTER_VISIBLE = "filter_visible";
	public static final String PROPERTY_FILTER_COLOR = "filter_color";
	public static final String PROPERTY_FILTER_TAB = "tab";
	public static final String PROPERTY_FILTER_IMAGE = "image";
	public static final String PROPERTY_FILTER_FILTER = "filter";
	public static final String PROPERTY_FILTER_BRIGHTNESS = "brightness";
	public static final String PROPERTY_FILTER_MEAN = "mean";
	public static final String PROPERTY_FILTER_RESET = "reset";


	
	protected int rotationType = ROTATE_NONE;
	protected boolean autoScale720x480 = false;


	protected int[] pixelsIn, pixelsOut;

	protected double widthFactor = 1.0, heightFactor = 1.0;

// instance fields
	/** true if the filter inspector is visible */
	public boolean inspectorVisible;

	/** the x-component of inspector position */
	public int inspectorX = Integer.MIN_VALUE;

	/** the y-component of inspector position */
	public int inspectorY;

	protected BufferedImage source, input, output;
	protected int w, h;

	private boolean enabled = true;
	private String name;
	protected boolean changed = false;
	protected String previousState;
	protected VideoPanel vidPanel;
	protected PropertyChangeSupport support;
	protected Action enabledAction;
	protected JCheckBoxMenuItem enabledItem;
	protected JMenuItem deleteItem, propertiesItem, copyItem;
	protected boolean hasInspector;
	protected Frame frame;
	protected JButton closeButton;
	protected JButton ableButton;
	protected JButton clearButton;
	protected FilterStack stack; // set by stack when filter added
	protected boolean doCreateOutput = true;

	protected int nPixelsIn;

	/**
	 * Constructs a Filter object.
	 */
	@SuppressWarnings("serial")
	protected Filter() {
		support = new SwingPropertyChangeSupport(this);
		// get the name of this filter from the simple class name
		name = getClass().getSimpleName();
		int i = name.indexOf("Filter"); //$NON-NLS-1$
		if ((i > 0) && (i < name.length() - 1)) {
			name = name.substring(0, i);
		}

		// set up menu items
		enabledAction = new AbstractAction(MediaRes.getString("Filter.MenuItem.Enabled")) { //$NON-NLS-1$
			@Override
			public void actionPerformed(ActionEvent e) {
				Filter.this.setEnabled(enabledItem.isSelected());
				refresh();
			}

		};
		enabledItem = new JCheckBoxMenuItem(enabledAction);
		enabledItem.setSelected(isEnabled());
		propertiesItem = new JMenuItem(MediaRes.getString("Filter.MenuItem.Properties")); //$NON-NLS-1$
		propertiesItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog inspector = getInspector();
				if (inspector != null) {
					inspector.setVisible(true);
				}
			}

		});
		copyItem = new JMenuItem(MediaRes.getString("Filter.MenuItem.Copy")); //$NON-NLS-1$
		copyItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copy();
			}
		});
		closeButton = new JButton();
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog inspector = getInspector();
				if (inspector != null) {
					if (isChanged() && previousState != null) {
						changed = false;
						support.firePropertyChange("filterChanged", previousState, Filter.this); //$NON-NLS-1$
						previousState = null;
					}
					inspector.setVisible(false);
				}
			}

		});
		ableButton = new JButton();
		ableButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledItem.setSelected(!enabledItem.isSelected());
				enabledAction.actionPerformed(null);
			}

		});
		clearButton = new JButton();
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
			}

		});
	}

	/**
	 * Returns a JDialog inspector for controlling filter properties.
	 *
	 * @return the inspector
	 */
	public abstract JDialog getInspector();

	/**
	 * Clears the filter. This default method does nothing.
	 */
	public void clear() {
		/** empty block */
	}

	/**
	 * Determines if the filter settings have changed.
	 * 
	 * @return true if changed
	 */
	public boolean isChanged() {
		return changed;
	}

	/**
	 * Sets the video panel.
	 * 
	 * @param panel the video panel
	 */
	public void setVideoPanel(VideoPanel panel) {
		vidPanel = panel;
		frame = vidPanel == null ? null : JOptionPane.getFrameForComponent(vidPanel);
	}

	/**
	 * Refreshes this filter's GUI
	 */
	public void refresh() {
		enabledItem.setText(MediaRes.getString("Filter.MenuItem.Enabled")); //$NON-NLS-1$
		propertiesItem.setText(MediaRes.getString("Filter.MenuItem.Properties")); //$NON-NLS-1$
		closeButton.setText(MediaRes.getString("Filter.Button.Close")); //$NON-NLS-1$
		ableButton.setText(isEnabled() ? MediaRes.getString("Filter.Button.Disable") : //$NON-NLS-1$
				MediaRes.getString("Filter.Button.Enable")); //$NON-NLS-1$
		clearButton.setText(MediaRes.getString("Filter.Button.Clear")); //$NON-NLS-1$
		clearButton.setEnabled((isEnabled()));
	}

	@Override
	public void finalize() {
		OSPLog.finer(getClass().getSimpleName() + " resources released by garbage collector"); //$NON-NLS-1$
	}

	/**
	 * Sets whether this filter is enabled.
	 *
	 * @param enabled <code>true</code> if this is enabled.
	 */
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		support.firePropertyChange("enabled", null, new Boolean(enabled)); //$NON-NLS-1$
	}

	/**
	 * Gets whether this filter is enabled.
	 *
	 * @return <code>true</code> if this is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Copies this filter to the clipboard.
	 */
	public void copy() {
		XMLControl control = new XMLControlElement(this);
		DataTool.copy(control.toXML());
	}

	/**
	 * Disposes of this filter.
	 */
	public void dispose() {
		removePropertyChangeListener(stack);
		stack = null;
		JDialog inspector = getInspector();
		if (inspector != null) {
			inspector.setVisible(false);
			inspector.dispose();
		}
		setVideoPanel(null);
		if (source != null)
			source.flush();
		if (input != null)
			input.flush();
		if (output != null)
			output.flush();
		pixelsIn = pixelsOut = null;
	}

	/**
	 * Adds a PropertyChangeListener to this filter.
	 *
	 * @param listener the object requesting property change notification
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	/**
	 * Adds a PropertyChangeListener to this filter.
	 *
	 * @param property the name of the property of interest to the listener
	 * @param listener the object requesting property change notification
	 */
	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		support.addPropertyChangeListener(property, listener);
	}

	/**
	 * Removes a PropertyChangeListener from this filter.
	 *
	 * @param listener the listener requesting removal
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	/**
	 * Removes a PropertyChangeListener for a specified property.
	 *
	 * @param property the name of the property
	 * @param listener the listener to remove
	 */
	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		support.removePropertyChangeListener(property, listener);
	}

	/**
	 * Returns a menu with items that control this filter. Subclasses should
	 * override this method and add filter-specific menu items.
	 *
	 * @param video the video using the filter (may be null)
	 * @return a menu
	 */
	public JMenu getMenu(Video video) {
		JMenu menu = new JMenu(MediaRes.getString("VideoFilter." + name)); //$NON-NLS-1$
		if (hasInspector) {
			menu.add(propertiesItem);
			menu.addSeparator();
		}
		menu.add(enabledItem);
		menu.addSeparator();
		menu.add(copyItem);
		if (video != null) {
			menu.addSeparator();
			deleteItem = new JMenuItem(MediaRes.getString("Filter.MenuItem.Delete")); //$NON-NLS-1$
			final FilterStack filterStack = video.getFilterStack();
			deleteItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					filterStack.removeFilter(Filter.this);
				}

			});
			menu.add(deleteItem);
		}
		return menu;
	}

	
	/**
	 * The issue here is that in SwingJS we may or may not have an actual raster. The act of getting the 
	 * raster does NOT ensure that it is actually filled with data. 
	 * 
	 * @param image
	 * @return
	 */
	protected Raster getRaster(BufferedImage image) {
		return image.getRaster();
	}
	
	
	protected void getPixels(BufferedImage image, int[] pixels) {
		image.getRaster().getDataElements(0, 0, w, h, pixels);
	}

	public void getPixelsIn() {
		pixelsIn = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
	}

	public void getPixelsOut() {
		pixelsOut = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
	}
	
	protected void initializeSource(BufferedImage image) {
		source = image;
		w = source.getWidth();
		h = source.getHeight();
		nPixelsIn = w * h;
		if (source.getType() == BufferedImage.TYPE_INT_RGB) {
			input = source;
		} else {
			input = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D gIn = input.createGraphics();
			gIn.drawImage(source, 0, 0, null);
			gIn.dispose();
		}
		if (doCreateOutput ) {
			if (autoScale720x480 && w == 720 && h == 480 && widthFactor == 1.0 && heightFactor == 1.0) {
			    // look for DV format and resize for square pixels by default
				widthFactor = 0.889;
			}
			int wOut = (int) (w * widthFactor);
			int hOut = (int) (h * heightFactor);
			if (rotationType == ROTATE_CW_90 || rotationType == ROTATE_CCW_90) {
				int w0 = wOut;
				wOut = hOut;
				hOut = w0;
			}
			output = new BufferedImage(wOut, hOut, BufferedImage.TYPE_INT_RGB);
		}
	}


	/**
	 * Applies the filter to a source image and returns the result. If the filter is
	 * not enabled, the source image should be returned.
	 *
	 * @param sourceImage the source image
	 * @return the filtered image
	 */
	protected BufferedImage getFilteredImage(BufferedImage sourceImage) {
		if (!isEnabled()) {
			return sourceImage;
		}
		if (sourceImage != source) {
			initializeSource(sourceImage);
		}
		setOutputPixels();
		return output;
	}

	abstract protected void initializeSubclass();

	abstract protected void setOutputPixels();
}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
