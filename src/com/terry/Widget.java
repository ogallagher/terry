package com.terry;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.Vertex;
import com.google.protobuf.ByteString;
import com.terry.Lesson.Definition;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Widget extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = 7635399172801274223L;
	
	public static final char TYPE_BUTTON = 'b';
	public static final char TYPE_TEXTBOX = 't';
	public static final char TYPE_LABEL = 'l';
	public static final char TYPE_GRAPHIC = 'g';
	public static final char TYPE_RADIO = 'r';
	public static final char TYPE_CHECK = 'c';
	
	private char type;
	private String label;
	private Point bounds;
	private Appearance appearance;
	
	private Rectangle zone;
	
	public static final char STATE_IDLE = 0;
	public static final char STATE_SEARCHING = 1;
	public static final char STATE_FOUND = 2;
	public static final char STATE_NOT_FOUND = 3;
	
	public CharProperty state;
	
	
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
		zone = null;
		state = new CharProperty(STATE_IDLE);
	}
	
	public String getName() {
		return pattern.toString();
	}
	
	public Rectangle getZone() {
		return zone;
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
	
	public void findInScreen(BufferedImage screen) throws WidgetException {
		if (type == TYPE_LABEL || appearance == null) {
			findLabelInScreen(screen);
		}
		else {
			findAppearanceInScreen(screen);
		}
	}
	
	public void findAppearanceInScreen(BufferedImage screen) throws WidgetException {
		if (appearance != null) {
			Rectangle bounds = null;
		}
		else {
			throw new WidgetException("widget " + id + " has no appearance and cannot be found");
		}
	}
	
	public void findLabelInScreen(BufferedImage screen) throws WidgetException {
		if (label != null) {			
			TextFinderThread textFinder = new TextFinderThread(screen,label);
			
			textFinder.state.addListener(new ChangeListener<Character>() {
				public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
					switch (newValue.charValue()) {
						case TextFinderThread.STATE_PARSING:
							state.set(STATE_SEARCHING);
							Logger.log("packing screen capture into api message");
							break;
							
						case TextFinderThread.STATE_SEARCHING:
							Logger.log("searching screen");
							break;
							
						case TextFinderThread.STATE_FINDING:
							Logger.log("analyzing text search results");
							break;
							
						case TextFinderThread.STATE_DONE:
							ArrayList<Rectangle> zones = textFinder.candidates;
							Logger.log("finished text search; found " + zones.size() + " candidate zones");
							
							int zn = zones.size();
							if (zn != 0) {
								zone = zones.get(zn-1); //return last zone
								state.set(STATE_FOUND);
							}
							else {
								zone = null;
								state.set(STATE_NOT_FOUND);
							}
							break;
							
						case TextFinderThread.STATE_IDLE:
							state.set(STATE_IDLE);
							break;
					}
				}
			});
			textFinder.run();			
		}
		else {
			throw new WidgetException("widget " + id + " has no label and cannot be found");
		}
	}
	
	/*
	 * type   id   pattern   widget_type   label   bounds   appearance
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		System.out.println("serializing widget " + id);
		
		stream.writeObject(super.toString());
		stream.writeChar(type);
		stream.writeObject(label);
		
		boolean nullBounds = (bounds == null);
		stream.writeBoolean(nullBounds);
		if (!nullBounds) {
			stream.writeInt(bounds.x);
			stream.writeInt(bounds.y);
		}
		
		boolean nullAppearance = (appearance == null);
		stream.writeBoolean(nullAppearance);
		if (!nullBounds) {
			stream.writeObject(appearance);
		}
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		fromString((String) stream.readObject());
		type = stream.readChar();
		label = (String) stream.readObject();
		
		boolean nullBounds = stream.readBoolean();
		if (!nullBounds) {
			bounds = new Point(stream.readInt(),stream.readInt());
		}
		else {
			bounds = null;
		}
		
		boolean nullAppearance = stream.readBoolean();
		if (!nullAppearance) {
			appearance = (Appearance) stream.readObject();
		}
		else {
			appearance = null;
		}
		
		Logger.log("deserialized widget " + id + ": " + pattern);
	}
	
	/*
	 * Appearance stores a collection of features (keypoint,descriptor pairs)
	 */
	private static class Appearance implements Serializable {
		private static final long serialVersionUID = -7252907290860821682L;
		
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
			Mat original = bufferedImageToMat(src);
			
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
		
		@SuppressWarnings("unused")
		public static BufferedImage matToBufferedImage(Mat mat) {
			return toFrame.convert(toMat.convert(mat));
		}
		
		public static Mat bufferedImageToMat(BufferedImage img) {
			return toMat.convert(toFrame.convert(img));
		}
	}
	
	private static class Feature implements Serializable {
		private static final long serialVersionUID = 9106278574480066191L;
		
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
	
	private static class TextFinderThread extends Thread implements Serializable {
		private static final long serialVersionUID = -7574636617579442887L;
		
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
		
		ArrayList<Rectangle> candidates;
		
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
			candidates = new ArrayList<>();
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
						float bestScore = 0;
						int bestDist = text.length();
						int maxDist = (int) Math.ceil(text.length() * 0.4);
						for (EntityAnnotation annotation : annotations) {
							String text = annotation.getDescription().trim().toLowerCase();
							BoundingPoly poly = annotation.getBoundingPoly();
							float score = annotation.getScore();
							
							Logger.log("checking text " + text);
							
							//compare to query label text and previous best score
							int dist = Utilities.editDistance(this.text, text, maxDist);
							if (dist != -1 && dist <= bestDist && score >= bestScore) {
								Logger.log(text + " has edit dist " + dist + " and score " + score);
								
								//convert to rectangle
								List<Vertex> vertices = poly.getVerticesList();
								Path2D.Double path = new Path2D.Double();
								
								Vertex vertex = vertices.get(0);
								path.moveTo(vertex.getX(), vertex.getY());
								for (int i=1; i<vertices.size(); i++) {
									vertex = vertices.get(i);
									path.lineTo(vertex.getX(), vertex.getY());
								}
								
								//append to candidates
								candidates.add(path.getBounds());
								
								//update best score and dist
								bestDist = dist;
								bestScore = score;
							}
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
	
	public static class WidgetException extends Exception implements Serializable {
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
