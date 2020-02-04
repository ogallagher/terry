package com.terry;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.terry.Scribe.ScribeException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

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
	
	public static void init() throws WidgetException {
		TextFinderThread.init();
		Logger.log("widget init success");
	}
	
	public Widget(String expr) {
		super(TYPE_WIDGET, expr);
		
		type = TYPE_BUTTON;
		label = null;
		bounds = null;
		appearance = null;
	}
	
	/*
	 * Given a screenshot of the widget, create a visual fingerprint by finding the widget's bounds within the image, creating a
	 * collection of features (keypoint-descriptor pairs), and normalizing them.
	 */
	public void setAppearance(BufferedImage img) {
		
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
	
	public Rectangle2D findInScreen(BufferedImage screen) throws WidgetException {
		if (type == TYPE_LABEL || appearance == null) {
			return findLabelInScreen(screen);
		}
		else {
			return findAppearanceInScreen(screen);
		}
	}
	
	public Rectangle2D findAppearanceInScreen(BufferedImage screen) throws WidgetException {
		if (appearance != null) {
			Rectangle2D bounds = null;
			
			return bounds;
		}
		else {
			throw new WidgetException("widget " + id + " has no appearance and cannot be found");
		}
	}
	
	public Rectangle2D findLabelInScreen(BufferedImage screen) throws WidgetException {
		if (label != null) {
			Rectangle2D bounds = null;
			
			TextFinderThread textFinder = new TextFinderThread(screen,label);
			textFinder.state.addListener(new ChangeListener<Character>() {
				public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
					switch (newValue.charValue()) {
						case TextFinderThread.STATE_PARSING:
							break;
							
						case TextFinderThread.STATE_SEARCHING:
							break;
							
						case TextFinderThread.STATE_FINDING:
							break;
							
						case TextFinderThread.STATE_DONE:
							break;
							
						case TextFinderThread.STATE_IDLE:
							break;
					}
				}
			});
			textFinder.run();
			
			return bounds;
		}
		else {
			throw new WidgetException("widget " + id + " has no label and cannot be found");
		}
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
	
	private static class TextFinderThread extends Thread {
		private static File visionDir;
		private static final String VISION_PATH = Terry.RES_PATH + "vision/";
		
		private static File gcloudCredentialsFile;
		private static final String GCLOUD_CREDENTIALS_PATH = "google_vision_credentials.json";
		
		private static ImageAnnotatorClient visionClient;
		private static Feature imageFeature;
		
		private BufferedImage image = null;
		private String text = null;
		
		public static final char STATE_IDLE = 0;
		public static final char STATE_PARSING = 1;
		public static final char STATE_SEARCHING = 2;
		public static final char STATE_FINDING = 3;
		public static final char STATE_DONE = 4;
		public CharProperty state;
		
		public static void init() throws WidgetException {
			visionDir = new File(Terry.class.getResource(VISION_PATH).getPath());
			gcloudCredentialsFile = new File(visionDir,GCLOUD_CREDENTIALS_PATH);
			
			CredentialsProvider credentials;
			try {
				credentials = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(gcloudCredentialsFile)));
				ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder().setCredentialsProvider(credentials).build();
				visionClient = ImageAnnotatorClient.create(settings);
				
				imageFeature = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
			} 
			catch (FileNotFoundException e) {
				throw new WidgetException("google text finder failed to load cloud credentials");
			}
			catch (IOException e) {
				throw new WidgetException("google text finder failed to create vision client: " + e.getMessage());
			}
		}
		
		public TextFinderThread(BufferedImage image, String text) {
			this.image = image;
			this.text = text;
			state = new CharProperty(STATE_IDLE);
		}
		
		public void run() {
			if (image != null) {
				state.set(STATE_PARSING);
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				try {
					ImageIO.write(image, "png", byteStream);
					byteStream.flush();
					byte[] bytes = byteStream.toByteArray();
					byteStream.close();
					
					ByteString byteString = ByteString.copyFrom(bytes);
					Image gimage = Image.newBuilder().setContent(byteString).build();
					
					ArrayList<AnnotateImageRequest> requests = new ArrayList<>();
					AnnotateImageRequest request = AnnotateImageRequest
													.newBuilder()
													.addFeatures(imageFeature)
													.setImage(gimage).build();
					requests.add(request);
					
					state.set(STATE_SEARCHING);
					AnnotateImageResponse response = visionClient.batchAnnotateImages(requests).getResponsesList().get(0);
					if (response.hasError()) {
						Logger.logError("text finder error: " + response.getError().getMessage());
					}
					else {
						List<EntityAnnotation> annotations = response.getTextAnnotationsList();
						
						state.set(STATE_FINDING);
						for (EntityAnnotation annotation : annotations) {
							Logger.log("found text " + annotation.getDescription() + " at " + annotation.getBoundingPoly());
						}
						state.set(STATE_DONE);
					}
				} 
				catch (IOException e) {
					Logger.logError("text finder failed to parse screenshot");
				}
			}
			else {
				Logger.logError("text finder was not given an image in which to search");
			}
			state.set(STATE_IDLE);
		}
	}
	
	public static class WidgetException extends Exception {
		private static final long serialVersionUID = -7383323102955403795L;
		private String message;

		public WidgetException(String message) {
			this.message = message;
		}
		
		@Override
		public String getMessage() {
			return message;
		}
	}
}
