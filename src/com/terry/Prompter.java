package com.terry;

import java.awt.MouseInfo;
import java.awt.Point;

import com.terry.Scribe.ScribeException;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Prompter extends Application {
	@SuppressWarnings("unused")
	private static JFXPanel dummyPanel = new JFXPanel();	//This prevents "Java Toolkit Not Initialized Error". 
															//I don't really get it, but an extra line doesn't do much harm anyway.
	
	private static Stage intercom;
	private static final int INTERCOM_WIDTH = 200;
	private static final int GLASSES_WIDTH = 150;
	private static final int MIC_WIDTH = 75;
	
	private static final String INTERCOM_PATH = "img/terry_150.png";
	private static final String MIC_PATH = "img/mic_75.png";
	
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
		
		//load icons
		IntercomIcon glasses = new IntercomIcon(Terry.class.getResource(Terry.RES_PATH + INTERCOM_PATH).toString(), GLASSES_WIDTH);
		intercomRoot.getChildren().add(glasses);
		
		IntercomIcon mic = new IntercomIcon(Terry.class.getResource(Terry.RES_PATH + MIC_PATH).toString(), MIC_WIDTH);
		intercomRoot.getChildren().add(mic);
		
		Scene intercomScene = new Scene(intercomRoot);
		intercomScene.setFill(Color.TRANSPARENT);
		intercomScene.getStylesheets().add(Terry.class.getResource(Terry.RES_PATH + "style.css").toString());
		
		intercom.setScene(intercomScene);
		intercom.centerOnScreen();
		intercom.setMinWidth(INTERCOM_WIDTH);
		intercom.setMaxWidth(INTERCOM_WIDTH);
		intercom.setMinHeight(INTERCOM_WIDTH);
		intercom.setMaxHeight(INTERCOM_WIDTH);
		intercom.show();
		
		intercomScene.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				try {
					switch (Scribe.state) {
						case Scribe.STATE_IDLE:
							Scribe.start();
							break;
							
						case Scribe.STATE_RECORDING:
							Scribe.stop();
							break;
							
						case Scribe.STATE_TRANSCRIBING:
							Logger.log("waiting for scribe to finish transcription...");
							break;
							
						default:
							Logger.logError("scribe in unknown state");
							break;
					}
				} 
				catch (ScribeException e) {
					Logger.logError(e.getMessage());
				}
			}
		});
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
