package com.terry;

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;

import com.terry.Scribe.ScribeException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

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
	
	private static Stage console;
	private static final int CONSOLE_WIDTH = 500;
	private static final int CONSOLE_HEIGHT = 800;
	
	private static ObservableList<String> consoleOut;
	private static ListView<String> consoleOutView;
		
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
		intercomScene.getStylesheets().add(Terry.class.getResource(Terry.RES_PATH + "style_intercom.css").toString());
		
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
			}
		});
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				primaryStage.close();
			}
		});
		
		//launch console window
		console = new Stage();
		console.initStyle(StageStyle.DECORATED);
		
		BorderPane consoleRoot = new BorderPane();
		
		consoleOut = FXCollections.observableArrayList();
		
		consoleOutView = new ListView<String>(consoleOut);
		consoleOutView.setEditable(false);
		consoleOutView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			public ListCell<String> call(ListView<String> param) {
				return new ListCell<String>() {
					@Override
					public void updateItem(String item, boolean empty) {
			            super.updateItem(item, empty);
			            setText(item);
			            getStyleClass().add("console_out_cell");			            
			        }
				};
			}
		});
		consoleOutView.setId("console_out");
		
		consoleRoot.setId("console_root");
		consoleRoot.setCenter(consoleOutView);
		
		Scene consoleScene = new Scene(consoleRoot);
		console.setScene(consoleScene);
		consoleScene.getStylesheets().add(Terry.class.getResource(Terry.RES_PATH + "style_console.css").toString());
		
		//console dimensions
		console.setWidth(CONSOLE_WIDTH);
		console.setMinWidth(CONSOLE_WIDTH);
		console.setHeight(CONSOLE_HEIGHT);
		console.setMinHeight(CONSOLE_HEIGHT);
		
		//console location
		Dimension screen = Driver.getScreen();
		console.setX(screen.width - CONSOLE_WIDTH);
		console.setY(screen.height - CONSOLE_HEIGHT);
		console.show();
		consoleRoot.requestFocus();
		
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
						InstructionParser.parse(Scribe.getTranscription()); //search associated language mapping
						
						break;
						
					default:
						break;
				}
			}
		});
		
		//tell logger that prompter is ready for logs
		Logger.emptyBacklog();
	}
	
	@Override
	public void stop() throws Exception {
		//destroy terry-specific resources
		intercom.close();
	}
	
	public void consoleLog(String entry) {
		int last = consoleOut.size();
		consoleOut.add(entry);
		consoleOutView.scrollTo(last);
	}
}
