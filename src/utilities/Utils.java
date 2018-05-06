package utilities;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JPanel;

import org.opencv.core.Mat;

import javafx.application.Platform;

public class Utils {

	public static BufferedImage mat2Image(Mat frame) {
		try {
			return matToBufferedImage(frame);
		} catch (Exception e) {
			System.err.println("Couldn't convert image");
			e.printStackTrace();
			return null;
		}
	}

	private static BufferedImage matToBufferedImage(Mat original) {
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}

		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		return image;
	}

	public static <T> void onFXThread(final JPanel panel, final BufferedImage image) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				panel.imageUpdate(image, 0, 0, 0, 640, 480);
			}
		});
	}

}
