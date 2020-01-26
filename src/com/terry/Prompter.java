package com.terry;

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.lang.reflect.Method;

import com.terry.Driver.DriverThread;
import com.terry.Scribe.ScribeException;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
	private static final int LOADING_WIDTH = 75;
	
	private static final String INTERCOM_PATH = "img/terry_150.png";
	private static final String MIC_PATH = "img/mic_75.png";
	private static final String LOADING_PATH = "img/loading_75.png";
	
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
		IntercomIcon mic = new IntercomIcon(Terry.class.getResource(Terry.RES_PATH + MIC_PATH).toString(), MIC_WIDTH, 0);
		IntercomIcon loading = new IntercomIcon(Terry.class.getResource(Terry.RES_PATH + LOADING_PATH).toString(), LOADING_WIDTH);
		loading.setVisible(false);
		
		intercomRoot.getChildren().addAll(glasses,mic,loading);
		
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
				/*
				try {
					switch (Scribe.state.get()) {
						case Scribe.STATE_IDLE:
							Scribe.start();
							
							break;
							
						case Scribe.STATE_RECORDING:
							Scribe.stop();
							
							break;
							
						case Scribe.STATE_TRANSCRIBING:
						case Scribe.STATE_DONE:
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
				*/
				
				//testing
				new DriverThread() {
					public void run() {
						Logger.log("quitting via mouse...");
						Driver.point(755, 899); //go to dock
						
						try {Thread.sleep(500);} catch (InterruptedException e) {}
						
						Driver.point(908, 860); //go to java
						
						Driver.clickRight(); //right-click menu
						
						try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for os to show options
						
						Driver.point(934, 771); //close option
						
						try {Thread.sleep(500);} catch (InterruptedException e) {} //dramatic effect
						
						Driver.clickLeft();
					}
				}.start();
			}
		});
		
		Scribe.state.addListener(new ChangeListener<Character>() {
			public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
				switch (newValue) {
					case Scribe.STATE_IDLE:						
						glasses.animate(1);
						
						mic.animate(0);
						
						loading.animate(-1,-1,0,-1,200);
						
						break;
						
					case Scribe.STATE_RECORDING:						
						mic.animate(1);
						
						glasses.animate(0);
						
						break;
						
					case Scribe.STATE_TRANSCRIBING:
						mic.animate(0);
						
						loading.setScaleX(1);
						loading.setScaleY(1);
						loading.setVisible(true);
						loading.animate(-1, -1, -1, 360, -2000);
						break;
						
					case Scribe.STATE_DONE:
						InstructionClassifier.parse(Scribe.getTranscription()); //search associated language mapping
						
						break;
						
					default:
						break;
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
