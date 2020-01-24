package com.terry;

import java.awt.Point;
import java.io.Serializable;

public class Widget extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = 7635399172801274223L;
	
	private static final char TYPE_BUTTON = 'b';
	private static final char TYPE_TEXTBOX = 't';
	private static final char TYPE_LABEL = 'l';
	private static final char TYPE_GRAPHIC = 'g';
	private static final char TYPE_RADIO = 'r';
	private static final char TYPE_CHECK = 'c';
	
	private char type;
	private String label;
	private Point bounds;
	private Appearance appearance;
	
	public Widget(String expr) {
		super(TYPE_WIDGET, expr);
		
		type = TYPE_BUTTON;
		label = null;
		bounds = null;
	}
	
	public void setType(char type) {
		this.type = type;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setBounds(int w, int h) {
		bounds = new Point(w,h);
	}
	
	private static class Appearance {
		/*
		 * TODO how will the visual representation of a widget be stored? It should be derived from screenshot images.
		 * 
		 * Check out compiled autoencoders (preferably java classes). One such network could be trained with a (very) limited dataset
		 * to be able to recognize a widget.
		 * 
		 * Check out visual copyright detection algorithms. These specialize in searching for unique objects in images.
		 * 
		 * Check out image classifiers. They'll be trained to recognize probabilities of certain objects being contained in the image,
		 * but I could use this list of recognized object probabilities as a signature for the widget.
		 * 
		 * Check out hough transforms for identifying characteristics.
		 * 
		 * Check out keypoints and descriptors for identifying image characteristics.
		 */
	}
}
