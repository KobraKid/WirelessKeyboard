package utilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ImagePanel extends JPanel {
	
	BufferedImage image;
	public ImagePanel(BufferedImage image) {
	      this.image = image;
	      this.setBackground(new Color(0, true));
	}
	
	public void setImage(BufferedImage image) {
		this.image = image;
	}

	@Override
	public void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    if (image != null) {
	    	AffineTransform at = new AffineTransform();
	    	at.concatenate(AffineTransform.getScaleInstance(-1, 1));
	        at.concatenate(AffineTransform.getTranslateInstance(-image.getWidth(), 0));
	        Graphics2D g2d = (Graphics2D) g;
	        g2d.transform(at);
	    	g2d.drawImage(image, 0, 0, null);
	    }
	}
	
}
