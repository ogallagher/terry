package com.terry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.scene.image.ImageView;

public class IntercomIcon extends ImageView {
	private int width;
	
	public IntercomIcon(String url, int w) {
		super(url);
		
		width = w;
		
		setPreserveRatio(true);
		setFitWidth(width);
	}
	
	public void animate(double tx, double ty, double s, double r) {
		AnimateThread animator = new AnimateThread(tx,ty,s,r,this);
		animator.start();
	}
	
	public void animate(double s, double r) {
		AnimateThread animator = new AnimateThread(-1,-1,s,r,this);
		animator.start();
	}
	
	private static class AnimateThread extends Thread {
		private boolean translates = false;
		private boolean scales = false;
		private boolean rotates = false;
		
		private DoubleProperty ix;
		private DoubleProperty iy;
		private DoubleProperty iw;
		private DoubleProperty ih;
		private DoubleProperty ir;
		private BooleanProperty iv;
		
		private double tx,ty,s,r;
		private double d;
		
		private static final double EASING = 0.1;
		private static final double MIN_DIFF = 5; //px difference between current transform and dest transform when animation is considered finished
		
		public AnimateThread(double tx, double ty, double s, double r, IntercomIcon i) {
			if (tx != -1) {
				translates = true;
				
				ix = i.translateXProperty();
				iy = i.translateYProperty();
				
				this.tx = tx;
				this.ty = ty;
			}
			
			if (s != -1) {
				scales = true;
				
				iw = i.scaleXProperty();
				ih = i.scaleYProperty();
				
				this.s = s;
				
				i.setVisible(true);
				iv = i.visibleProperty();
			}
			
			if (r != -1) {
				rotates = true;
				
				ir = i.rotateProperty(); //in degrees
				
				this.r = r;
			}
		}
		
		@Override
		public void run() {
			boolean got = true, gos = true, gor = true;
			
			while (!isInterrupted() && (got || gos || gor)) {
				if (translates) {
					//TODO tx,ty
				}
				else {
					got = false;
				}
				
				if (scales) {
					//sx,sy
					d = iw.get();
					d = d + EASING * (s - d);
					
					if (d < MIN_DIFF) {
						if (s == 0) {
							iv.set(false);
						}
						else {
							iw.set(s);
							iy.set(s);
						}
						
						gos = false;
					}
					else {
						iw.set(d);
						iy.set(d);
					}
				}
				else {
					gos = false;
				}
				
				if (rotates) {
					d = ir.get();
					d = d + EASING * (r - d);
					
					if (d < MIN_DIFF) {
						ir.set(r);
						gor = false;
					}
					else {
						ir.set(d);
					}
				}
				else {
					gor = false;
				}
			}
		}
	}
}
