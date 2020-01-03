package com.terry;

import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class IntercomIcon extends ImageView {
	private int width;
	
	private AnimateThread animator;
	
	public IntercomIcon(String url, int w) {
		this(url,w,1);
	}
	
	public IntercomIcon(String url, int w, int s) {
		super(url);
		
		width = w;
		
		setPreserveRatio(true);
		setFitWidth(width);
		
		setScaleX(s);
		setScaleY(s);
		if (s == 0) {
			setVisible(false);
		}
	}
	
	public void animate(double tx, double ty, double s, double r) {
		animator = new AnimateThread(tx,ty,s,r,this);
		animator.start();
	}
	
	public void animate(double s, double r) {
		animate(-1,-1,s,r);
	}
	
	public void animate(double s) {
		animate(-1,-1,s,-1);
	}
	
	private static class AnimateThread extends Thread {
		private TranslateTransition translater = null;
		private ScaleTransition scaler = null;
		private RotateTransition rotater = null;
		
		private BooleanProperty ivisible = null;
		
		private static final double DURATION = 500;
		
		public AnimateThread(double tx, double ty, double s, double r, IntercomIcon i) {
			if (tx != -1) {
				translater = new TranslateTransition(Duration.millis(DURATION),i);
				translater.setByX(tx);
				translater.setByY(ty);
			}
			
			if (s != -1) {
				i.setVisible(true);
				
				scaler = new ScaleTransition(Duration.millis(DURATION),i);
				scaler.setByX(s - i.getScaleX());
				scaler.setByY(s - i.getScaleY());
				
				if (s == 0) {
					ivisible = i.visibleProperty();
				}
			}
			
			if (r != -1) {
				rotater = new RotateTransition(Duration.millis(DURATION),i);
				rotater.setByAngle(r - i.getRotate());
			}
		}
		
		@Override
		public void run() {
			if (translater != null) {
				translater.play();
			}
			
			if (scaler != null) {
				scaler.play();
				
				scaler.setOnFinished(new EventHandler<ActionEvent>() {
					public void handle(ActionEvent event) {
						if (ivisible != null) {
							ivisible.setValue(false);
						}
					}
				});
			}
			
			if (rotater != null) {
				rotater.play();
			}
		}
		
		public void quit() {
			if (translater != null) {
				translater.stop();
			}
			if (scaler != null) {
				scaler.stop();
			}
			if (rotater != null) {
				rotater.stop();
			}
			
			interrupt();
		}
	}
}
