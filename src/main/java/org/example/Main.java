package org.example;

import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);

    // Open webcam
    VideoCapture capture = new VideoCapture(0); // Use index 0 for default camera

    if (!capture.isOpened()) {
      System.out.println("Error: Cannot open video capture.");
      return;
    }

    // Set up JFrame to display video
    JFrame frame = new JFrame("Multiple Yellow Rectangle Detection");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JLabel label = new JLabel();
    frame.getContentPane().add(label, BorderLayout.CENTER);
    frame.setSize(640, 480);
    frame.setVisible(true);

    Mat matFrame = new Mat();
    Mat hsvFrame = new Mat();
    Mat mask = new Mat();
    Mat blurred = new Mat();
    Mat edged = new Mat();

    // Adjust HSV range for yellow (tune based on your environment)
    Scalar lowerYellow = new Scalar(20, 100, 100);  // Lower bound of yellow in HSV
    Scalar upperYellow = new Scalar(30, 255, 255);  // Upper bound of yellow in HSV

    if (!capture.read(matFrame)) {
      capture.release();
      frame.dispose();
      throw new RuntimeException("Cannot open webcam");
    }
    while (capture.read(matFrame)) {
      // Convert the frame to HSV color space
      Imgproc.cvtColor(matFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);

      // Create a mask for the yellow color
      Core.inRange(hsvFrame, lowerYellow, upperYellow, mask);

      // Apply Gaussian blur to reduce noise
      Imgproc.GaussianBlur(mask, blurred, new Size(5, 5), 0);

      // Apply Canny edge detection
      Imgproc.Canny(blurred, edged, 50, 150);

      // Find contours in the edge-detected image
      List<MatOfPoint> contours = new ArrayList<>();
      Mat hierarchy = new Mat();
      Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

      // Loop over the contours to find rectangles
      for (MatOfPoint contour : contours) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(contour2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(contour2f, approx, 0.02 * perimeter, true);

        // If the contour has 4 vertices, it's likely a rectangle
        if (approx.total() == 4) {
          // Get bounding rectangle
          Rect rect = Imgproc.boundingRect(new MatOfPoint(approx.toArray()));

          // Filter out small rectangles to ignore noise
          if (rect.width > 30 && rect.height > 30) {
            // Draw the rectangle on the original frame
            Imgproc.rectangle(matFrame, new Point(rect.x, rect.y),
                              new Point(rect.x + rect.width, rect.y + rect.height),
                              new Scalar(0, 255, 0), 2);

            // Label the detected rectangle
            Imgproc.putText(matFrame, "Yellow Rectangle", new Point(rect.x, rect.y - 10),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 0), 2);
            System.out.println("COBOR GHEARA");
            return;
          }
        }
      }

      // Convert Mat to BufferedImage for display
      BufferedImage img = matToBufferedImage(matFrame);
      label.setIcon(new ImageIcon(img));

      // Add a slight delay to mimic real-time video
      try {
        Thread.sleep(33);  // ~30 FPS
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // Exit condition: close the window
      if (!frame.isVisible()) {
        break;
      }
    }

    // Release resources
    capture.release();
    frame.dispose();
  }

  // Utility function to convert Mat to BufferedImage for displaying in JFrame
  public static BufferedImage matToBufferedImage(Mat mat) {
    int type = (mat.channels() == 1) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
    BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
    byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    mat.get(0, 0, data); // Copy data from Mat to BufferedImage
    return image;
  }
}
