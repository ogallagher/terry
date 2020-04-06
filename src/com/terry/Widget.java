package com.terry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_java;
import org.bytedeco.opencv.opencv_core.PCA;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.Params;
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
	
	private char type;				//interaction type
	private String label;			//associated text
	private Dimension bounds;		//size
	private Appearance appearance;	//associated icon
	
	private Rectangle zone;			//search result
	
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
		bounds = appearance.getDimensions();
	}
	
	public void setType(char type) {
		this.type = type;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void findInScreen(BufferedImage screen) throws WidgetException {
		zone = null;
		
		if (type == TYPE_LABEL || appearance == null) {
			findLabelInScreen(screen);
		}
		else {
			findAppearanceInScreen(screen);
		}
	}
	
	public void findAppearanceInScreen(BufferedImage screen) throws WidgetException {
		if (appearance != null) {	
			state.set(STATE_SEARCHING);
			
			//convert screen to grayscale matrix
			Mat colored = Appearance.bufferedImageToMat(screen);
			
			//convert to 8bit grayscale image
			int channels = screen.getColorModel().getNumComponents();
			Mat grayscale = new Mat(colored.size(), CvType.CV_8UC1);
			if (channels == 4) { //rgba
				Imgproc.cvtColor(colored, grayscale, Imgproc.COLOR_RGBA2GRAY);
			}
			else if (channels == 3) { //rgb
				Imgproc.cvtColor(colored, grayscale, Imgproc.COLOR_RGB2GRAY);
			}
			
			//create template match result
			Mat result = new Mat(grayscale.cols() - appearance.template.cols() + 1, 
								 grayscale.rows() - appearance.template.rows() + 1, 
								 CvType.CV_8UC1);
			
			Imgproc.matchTemplate(grayscale, appearance.template, result, Imgproc.TM_CCOEFF_NORMED);
			
			//analyze result
			org.opencv.core.Point location = Core.minMaxLoc(result).maxLoc;
			zone = new Rectangle((int) location.x, (int) location.y, bounds.width, bounds.height);
			state.set(STATE_FOUND);
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
			stream.writeInt(bounds.width);
			stream.writeInt(bounds.height);
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
			bounds = new Dimension(stream.readInt(),stream.readInt());
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
	 * Appearance stores a collection of normalized features (keypoint,descriptor pairs)
	 */
	private static class Appearance implements Serializable {
		private static final long serialVersionUID = -7252907290860821682L;
		
		private static Java2DFrameConverter toFrame; //BufferedImage to and from Frame 
		private static OpenCVFrameConverter.ToOrgOpenCvCoreMat toMat; //Frame to and from Mat
		
		private ArrayList<Feature> features;
		private Mat template = null;
		
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
			
			template = grayscale;
			
			/*
			//create keypoints
			MatOfKeyPoint keypoints = new MatOfKeyPoint();
			FastFeatureDetector analyzer = FastFeatureDetector.create();
			analyzer.detect(grayscale, keypoints);
			KeyPoint[] kps = keypoints.toArray();
			
			//create descriptors and features and bounds
			features = new ArrayList<>();
			int x,y;
			int w = original.width();
			int h = original.height();
			int bx=w,bX=0,by=h,bY=0;
			
			for (int i=0; i<kps.length; i++) {
				x = (int) kps[i].pt.x;
				y = (int) kps[i].pt.y;
				
				if (y > 0 && y < h-1 && x > 0 && x < w-1) {
					
					//create feature
					features.add(new Feature(kps[i], original.get(y, x)));
					
					//update bounds
					if (x < bx) {
						bx = x;
					}
					if (x > bX) {
						bX = x;
					}
					if (y < by) {
						by = y;
					}
					if (y > bY) {
						bY = y;
					}
				}
				//else, ignore edge keypoint
			}
			
			//define widget bounds
			bounds = new Dimension(bX-bx,bY-by);
			
			//normalize
			double cx = (bx+bX)/2.0;
			double cy = (by+bY)/2.0;
			Point2D.Double evector = normalize(cx,cy);
			*/
			
			Logger.log(this.toString());
			
			//TODO remove testing
			saveToFile(template);
		}
		
		/*
		 * To be robust against rotation and scaling, normalize features by:
		 * 	- defining in terms of center
		 * 	- rotating so the eigenvector's angle is 0
		 * 
		 * Taken from: https://github.com/bytedeco/javacv/blob/master/samples/PrincipalComponentAnalysis.java: line 91
		 */
		@SuppressWarnings("unused")
		private Point2D.Double normalize(double cx, double cy) {
			org.bytedeco.opencv.opencv_core.Mat pts = new org.bytedeco.opencv.opencv_core.Mat(features.size(), 2, CvType.CV_64F);
			
			DoubleIndexer ptsIndexer = pts.createIndexer();
			
			//put keypoints into matrix and define in terms of data center
			for (int f=0; f<features.size(); f++) {
				ptsIndexer.put(f, 0, features.get(f).keypoint.pt.x - cx);
				ptsIndexer.put(f, 1, features.get(f).keypoint.pt.y - cy);
			}
			
			PCA pca = new PCA();
			pca.apply(pts, 											//point data
					  new org.bytedeco.opencv.opencv_core.Mat(),	//no known mean eigenvalue 
					  PCA.DATA_AS_ROW,								//shape of data matrix
					  1);											//care only about max eigenvalue
			
			//get principal component vector
			DoubleIndexer evectorIndexer = pca.eigenvectors().createIndexer();
			Point2D.Double evector = new Point2D.Double(evectorIndexer.get(0, 0), evectorIndexer.get(0,1));
			
			pca.close();
			return evector;
		}
		
		public Dimension getDimensions() {
			return new Dimension(template.cols(), template.rows());
		}
		
		public String toString() {
			String string = "appearance: " 
							+ template.cols() 
							+ "x" 
							+ template.rows() 
							+ "x" 
							+ template.channels() 
							+ "x" 
							+ template.depth();
			
			return string;
		}
		
		public static void saveToFile(Mat template) {			
			Utilities.saveImage(matToBufferedImage(template), Terry.RES_PATH + "vision/", "appearance.png");
			Logger.log("appearance saved");
		}
		
		public static BufferedImage matToBufferedImage(Mat mat) {
			return toFrame.convert(toMat.convert(mat));
		}
		
		/*
		 * For some reason, matrices with an odd number of columns do not convert to opencv matrices correctly
		 */
		public static Mat bufferedImageToMat(BufferedImage img) {
			if (img.getWidth() % 2 != 0 ) {
				return toMat.convert(toFrame.convert(img.getSubimage(0, 0, img.getWidth()-1, img.getHeight())));
			}
			else {
				return toMat.convert(toFrame.convert(img));
			}
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
		
		public void draw(Graphics2D g) {
			int x = (int)keypoint.pt.x;
			int y = (int)keypoint.pt.y;
			g.drawLine(x, y, x, y);
		}
		
		public String toString() {
			String string = "feature: p=(" + keypoint.pt.x + "," + keypoint.pt.y + ") size=" + (int)keypoint.size + " descriptor=";
			
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
			visionDir = new File(Terry.class.getResource(Terry.RES_PATH).getPath(), "vision/");
			if (!visionDir.exists()) {
				try {
					visionDir.createNewFile();
				} 
				catch (IOException e) {
					throw new WidgetException("could not create res/vision/ directory");
				}
			}
			
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
