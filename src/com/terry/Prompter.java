package com.terry;

import java.awt.MouseInfo;
import java.awt.Point;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Prompter extends Application {
	@SuppressWarnings("unused")
	private static JFXPanel dummyPanel = new JFXPanel();	//This prevents "Java Toolkit Not Initialized Error". 
															//I don't really get it, but an extra line doesn't do much harm anyway.
	
	private static final String RES_PATH = "res/";
	
	private static Stage intercom;
	private static final int INTERCOM_WIDTH = 75;
	private static final int ICON_WIDTH = 75;
	
	private static TestThread testThread;
	
	/*
	 * (non-Javadoc)
	 * @see javafx.application.Application#init()
	 * 
	 * This cannot be a static method because it's inherited from Application, but effectively I'll treat it
	 * as if it were static.
	 */
	public void init(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		//launch intercom window
		intercom = primaryStage;
		intercom.initStyle(StageStyle.TRANSPARENT);
		
		StackPane intercomRoot = new StackPane();
		intercomRoot.setId("intercom_root");
		
		//load icon
		ImageView icon = new ImageView(Terry.class.getResource(RES_PATH + "terry_150.png").toString());
		icon.setPreserveRatio(true);
		icon.setFitWidth(ICON_WIDTH);
		intercomRoot.getChildren().add(icon);
		
		Scene intercomScene = new Scene(intercomRoot);
		intercomScene.setFill(Color.TRANSPARENT);
		intercomScene.getStylesheets().add(Terry.class.getResource(RES_PATH + "style.css").toString());
		
		intercom.setScene(intercomScene);
		intercom.centerOnScreen();
		intercom.setMaxWidth(INTERCOM_WIDTH);
		intercom.setMinHeight(INTERCOM_WIDTH);
		intercom.setMaxHeight(INTERCOM_WIDTH);
		intercom.show();
		
		testThread = new TestThread(primaryStage);
		testThread.setDaemon(false);
		testThread.start();
	}
	
	@Override
	public void stop() throws Exception {
		//destroy terry-specific resources
		testThread.quit();
		
		super.stop();
	}
	
	private static class TestThread extends Thread {
		Stage primaryStage;
		
		TestThread(Stage primaryStage) {
			this.primaryStage = primaryStage;
		}
		
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					Point cursor = MouseInfo.getPointerInfo().getLocation();
					
					if (cursor != null) {
						primaryStage.setX(cursor.getX() - 0.5*INTERCOM_WIDTH);
						primaryStage.setY(cursor.getY() - 0.5*INTERCOM_WIDTH);						
					}
					
					sleep(50);
				}
				catch (InterruptedException e) {
					//quit
				}
			}
		}
		
		/*
		 * quick way to allow other threads to interrupt this one without throwing
		 * an access exception.
		 */
		public void quit() {
			interrupt();
		}
	}
}
