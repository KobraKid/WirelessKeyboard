package peripherals;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import utilities.ImagePanel;
import utilities.Utils;

public class Mouse implements WindowListener {

	ImagePanel image;
	// JButton cameraButton;
	private Robot mouseMover;
	private VideoCapture capture;
	private final int width = 640, height = 480;
	private final int xSpeed = -35, ySpeed = 12;
	private ScheduledExecutorService timer;
	private boolean cameraActive;
	private CascadeClassifier faceCascade, eyeCascade;
	private int absoluteFaceSize;
	private int absoluteEyeSize;
	private int cameraID;
	// Default 33 (milliseconds)
	private final int interval = 33;
	// Default 0.2F (20% of screen)
	private final float facePortion = 0.2F;
	private final float eyePortion = 0.12F;
	private int eyeMask = 0;
	private boolean hasClicked = false;

	public Mouse() {
		this.init();

		try {
			mouseMover = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		// Simulate a mouse click
		// mouseMover.mouseMove(1000, 500);
		// mouseMover.mousePress(InputEvent.BUTTON1_MASK);
		// mouseMover.mouseRelease(InputEvent.BUTTON1_MASK);

		JFrame frame = new JFrame("Rat");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(this);
		frame.setLayout(new FlowLayout());

		image = new ImagePanel(null);
		image.setPreferredSize(new Dimension(640, 480));
		frame.add(image);

		// cameraButton = new JButton();
		// cameraButton.setPreferredSize(new Dimension(100, 20));
		// cameraButton.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// startCamera();
		// }
		// });
		// frame.add(cameraButton);

		GraphicsConfiguration gc = frame.getGraphicsConfiguration();
		Rectangle bounds = gc.getBounds();
		Dimension size = frame.getPreferredSize();
		frame.setLocation((int) ((bounds.width) - (size.getWidth())), (int) ((bounds.height) - (size.getHeight())));

		frame.setUndecorated(true);
		frame.pack();
		frame.setVisible(true);

		startCamera();
	}

	protected void init() {
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");
		this.eyeCascade = new CascadeClassifier();
		this.eyeCascade.load("resources/haarcascades/haarcascade_eye.xml");
		this.absoluteFaceSize = 0;
		this.cameraID = 0;
	}

	protected void startCamera() {
		if (!this.cameraActive) {

			// start the video capture
			this.capture.open(cameraID);

			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every /interval/ milliseconds (30 frames/second)
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						// effectively grab and process a single frame
						Mat frame = grabFrame();

						// convert and show the frame
						BufferedImage imageToShow = Utils.mat2Image(frame);
						updateImageView(imageToShow);
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, interval, TimeUnit.MILLISECONDS);

				// update the button content
				// this.cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;

			// update again the button content
			// this.cameraButton.setText("Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	private Mat grabFrame() {
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					// face detection
					this.detectAndDisplay(frame);
				}
			} catch (Exception e) {
				// log the (full) error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		return frame;
	}

	private void detectAndDisplay(Mat frame) {
		MatOfRect faces = new MatOfRect();
		MatOfRect eyes = new MatOfRect();
		Mat grayFrame = new Mat();

		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(grayFrame, grayFrame);

		// FACE
		if (this.absoluteFaceSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * facePortion) > 0) {
				this.absoluteFaceSize = Math.round(height * facePortion);
			}
		}

		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

		// EYES
		if (this.absoluteEyeSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * eyePortion) > 0) {
				this.absoluteEyeSize = Math.round(height * eyePortion);
			}
		}

		this.eyeCascade.detectMultiScale(grayFrame, eyes, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteEyeSize, this.absoluteEyeSize),
				new Size(this.absoluteEyeSize * 3, this.absoluteEyeSize * 3));

		// DRAWING
		Rect[] facesArray = faces.toArray();
		Rect[] eyesArray = eyes.toArray();

		if (eyesArray.length == 0 || facesArray.length == 0) {
			return;
		}

		Rect largestFace = getLargestFace(facesArray);
		Point faceCenter = new Point(largestFace.x + (largestFace.width / 2), largestFace.y + (largestFace.height / 2));
		// System.out.println("Face: " + faceCenter.x);

		Imgproc.rectangle(frame, largestFace.tl(), largestFace.br(), new Scalar(0, 255, 0, 255), 3);
		moveMouseBy(faceCenter.x - (width / 2), faceCenter.y - (height / 2));

		for (int i = 0; i < eyesArray.length; i++) {
			Imgproc.rectangle(frame, eyesArray[i].tl(), eyesArray[i].br(), new Scalar(0, 0, 255, 255), 3);
		}

		if (eyesArray.length == 1) {
			eyeMask++;
			if (!hasClicked && eyeMask > 8) {
				// Determine which eye is hidden
				Point eyeCenter = new Point(eyesArray[0].x + (eyesArray[0].width / 2),
						eyesArray[0].y + (eyesArray[0].height / 2));
				// System.out.println("Eye: " + eyeCenter.x);
				clickMouse(eyeCenter.x < faceCenter.x);
			}
		} else {
			eyeMask = 0;
			hasClicked = false;
		}
	}

	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(interval, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
	}

	private Rect getLargestFace(Rect[] faces) {
		Rect largest = faces[0];
		for (int i = 1; i < faces.length; i++) {
			if (faces[i].size().area() > largest.size().area())
				largest = faces[i];
		}
		return largest;
	}

	private void updateImageView(BufferedImage image) {
		this.image.setImage(image);
		this.image.repaint();
	}

	private void moveMouseBy(int x, int y) {
		Point p = MouseInfo.getPointerInfo().getLocation();
		mouseMover.mouseMove(p.x + (x / xSpeed), p.y + (y / ySpeed));
	}

	private void clickMouse(boolean left) {
		if (left) {
			mouseMover.mousePress(InputEvent.BUTTON1_MASK);
			mouseMover.mouseRelease(InputEvent.BUTTON1_MASK);
		} else {
			mouseMover.mousePress(InputEvent.BUTTON2_MASK);
			mouseMover.mouseRelease(InputEvent.BUTTON2_MASK);
		}
		hasClicked = true;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		stopAcquisition();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

}
