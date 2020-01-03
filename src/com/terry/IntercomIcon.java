package com.terry;

import javafx.animation.Animation;
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
	
	public void animate(double tx, double ty, double s, double r, int duration) {
		if (animator != null) {
			animator.quit();
		}
		animator = new AnimateThread(tx,ty,s,r,this,duration);
		animator.start();
	}
	
	public void animate(double s, double r) {
		animate(-1,-1,s,r,500);
	}
	
	public void animate(double s) {
		animate(-1,-1,s,-1,500);
	}
	
	public void stop() {
		if (animator != null) {
			animator.quit();
		}
	}
	
	private static class AnimateThread extends Thread {
		private TranslateTransition translater = null;
		private ScaleTransition scaler = null;
		private RotateTransition rotater = null;
		
		private BooleanProperty ivisible = null;
				
		public AnimateThread(double tx, double ty, double s, double r, IntercomIcon i, int duration) {
			if (tx != -1) {
				translater = new TranslateTransition(Duration.millis(Math.abs(duration)),i);
				translater.setByX(tx);
				translater.setByY(ty);
				
				if (duration < 0) {
					translater.setCycleCount(Animation.INDEFINITE);
				}
			}
			
			if (s != -1) {
				i.setVisible(true);
				
				scaler = new ScaleTransition(Duration.millis(Math.abs(duration)),i);
				scaler.setByX(s - i.getScaleX());
				scaler.setByY(s - i.getScaleY());
				
				if (s == 0) {
					ivisible = i.visibleProperty();
				}
				
				if (duration < 0) {
					scaler.setCycleCount(Animation.INDEFINITE);
				}
			}
			
			if (r != -1) {
				rotater = new RotateTransition(Duration.millis(Math.abs(duration)),i);
				rotater.setByAngle(r + i.getRotate());
				
				if (duration < 0) {
					rotater.setCycleCount(Animation.INDEFINITE);
				}
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
