package com.terry;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.Params;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.imgproc.Imgproc;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

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
		Appearance.init();
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
		appearance = new Appearance(img);
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
	
	/*
	 * Appearance stores a collection of features (keypoint,descriptor pairs)
	 */
	private static class Appearance {
		private static Java2DFrameConverter toFrame; //BufferedImage to and from Frame 
		private static OpenCVFrameConverter.ToOrgOpenCvCoreMat toMat; //Frame to and from Mat
		
		private ArrayList<Feature> features;
		
		public static void init() {
			Loader.load(opencv_java.class);
			toFrame = new Java2DFrameConverter();
			toMat = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();
			Logger.log("widget appearance init success");
		}
		
		public Appearance(BufferedImage src) {
			int channels = src.getColorModel().getNumComponents();
			
			//convert to javacv image class
			Mat original = toMat.convert(toFrame.convert(src));
			
			//convert to 8bit grayscale image
			Mat grayscale = new Mat(original.size(), CvType.CV_8UC1);
			if (channels == 4) { //rgba
				Imgproc.cvtColor(original, grayscale, Imgproc.COLOR_RGBA2GRAY);
			}
			else if (channels == 3) { //rgb
				Imgproc.cvtColor(original, grayscale, Imgproc.COLOR_RGB2GRAY);
			}
			
			//create keypoints
			MatOfKeyPoint keypoints = new MatOfKeyPoint();
			FastFeatureDetector analyzer = FastFeatureDetector.create();
			analyzer.detect(grayscale, keypoints);
			KeyPoint[] kps = keypoints.toArray();
			
			//create descriptors and features
			features = new ArrayList<>();
			double[][] descriptors = new double[kps.length][6]; //r,g,b gradients in x,y directions 
			int x,y;
			double[][] pixels = new double[4][3]; //3x3 pixel frame around keypoint, excluding corners and middle
			int w = original.width();
			int h = original.height();
			for (int i=0; i<kps.length; i++) {
				x = (int) kps[i].pt.x;
				y = (int) kps[i].pt.y;
				
				if (y > 0 && y < h && x > 0 && x < w) {
					pixels[0] = original.get(y-1, x);
					pixels[1] = original.get(y, x+1);
					pixels[2] = original.get(y+1, x);
					pixels[3] = original.get(y, x-1);
					
					descriptors[i][0] = pixels[1][0]-pixels[3][0]; //dx.r
					descriptors[i][1] = pixels[1][1]-pixels[3][1]; //dx.g
					descriptors[i][2] = pixels[1][2]-pixels[3][2]; //dx.b
					
					descriptors[i][3] = pixels[2][0]-pixels[0][0]; //dy.r
					descriptors[i][4] = pixels[2][1]-pixels[0][1]; //dy.g
					descriptors[i][5] = pixels[2][2]-pixels[0][2]; //dy.b
					
					//create feature
					features.add(new Feature(kps[i], descriptors[i]));
				}
				//else, ignore edge keypoint
			}
			
			Logger.log(this.toString());
		}
		
		public String toString() {
			String string = "appearance:";
			
			for (Feature f : features) {
				string += "\n\t" + f;
			}
			
			return string;
		}
		
		public static BufferedImage MatToBufferedImage(Mat mat) {
			return toFrame.convert(toMat.convert(mat));
		}
	}
	
	private static class Feature {
		private KeyPoint keypoint;
		private double[] descriptor;
		
		public Feature(KeyPoint kp, double[] desc) {
			keypoint = kp;
			descriptor = desc;
		}
		
		public String toString() {
			String string = "feature: p=(" + keypoint.pt.x + "," + keypoint.pt.x + ") size=" + (int)keypoint.size + " descriptor=";
			
			for (double d : descriptor) {
				string += " " + d;
			}
			return string;
		}
	}
	
	private static class TextFinderThread extends Thread {
		private static File visionDir;
		private static final String VISION_PATH = Terry.RES_PATH + "vision/";
		
		private static File gcloudCredentialsFile;
		private static final String GCLOUD_CREDENTIALS_PATH = "google_vision_credentials.json";
		
		private static ImageAnnotatorClient visionClient;
		private static com.google.cloud.vision.v1.Feature imageFeature;
		
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
				
				imageFeature = com.google.cloud.vision.v1.Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
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
