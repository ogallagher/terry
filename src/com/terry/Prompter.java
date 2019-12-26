package com.terry;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Prompter extends Application {
	@SuppressWarnings("unused")
	private static JFXPanel dummyPanel = new JFXPanel();	//This prevents "Java Toolkit Not Initialized Error". 
															//I don't really get it, but an extra line doesn't do much harm anyway.
	
	private static final String RES_PATH = "res/";
	
	private static Stage intercom;
	
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
		
		//load icon
		ImageView icon = new ImageView(Terry.class.getResource(RES_PATH + "terry.png").toString());
		intercomRoot.getChildren().add(icon);
		
		Scene intercomScene = new Scene(intercomRoot);
		intercom.setScene(intercomScene);
		intercom.centerOnScreen();
		intercom.show();
	}
	
	@Override
	public void stop() throws Exception {
		//destroy terry-specific resources
		
		super.stop();
	}
}
