package cameraMotionDetection;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import cameraMotionDetection.HttpQuery;

import org.opencv.imgproc.Imgproc;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.CvType;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;


public class MotionDetection {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	final static int pmax=314;
	final static int pmin=4;
	final static int tmax=136;
	final static int tmin=49;
	
	static boolean testCamera = false;
	
	static Mat imag=null;
	final static int HEIGHT = 480;
	final static int WIDTH = 640;

	public static void main(String[] args) {
		JFrame jframe = new JFrame("HUMAN MOTION DETECTOR FPS");
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel vidpanel = new JLabel();
		jframe.setContentPane(vidpanel);
		jframe.setSize(640, 480);
		jframe.setVisible(true);

		Mat frame = new Mat();
		Mat outerBox = new Mat();
		Mat diff_frame = null;
		Mat tempon_frame = null;
		ArrayList<Rect> array = new ArrayList<Rect>();
		VideoCapture camera = new VideoCapture("http://admin:azerty@192.168.43.203/video/mjpg.cgi");
		Size sz = new Size(640, 480);
		int i = 0;
		
		HttpQuery t =  new HttpQuery();
		t.start();

		Rect rectMax = new Rect(0, 0, 0, 0);
		int depX,depY;
		

		while (true) {
			rectMax = new Rect(0, 0, 0, 0);
			
			if (camera.read(frame)) {
				Imgproc.resize(frame, frame, sz);
				imag = frame.clone();
				outerBox = new Mat(frame.size(), CvType.CV_8UC1);
				Imgproc.cvtColor(frame, outerBox, Imgproc.COLOR_BGR2GRAY);
				Imgproc.GaussianBlur(outerBox, outerBox, new Size(3, 3), 0);

				if (i == 0) {
					jframe.setSize(frame.width(), frame.height());
					diff_frame = new Mat(outerBox.size(), CvType.CV_8UC1);
					tempon_frame = new Mat(outerBox.size(), CvType.CV_8UC1);
					diff_frame = outerBox.clone();
				}

				if (i == 1) {
					Core.subtract(outerBox, tempon_frame, diff_frame);
					Imgproc.adaptiveThreshold(diff_frame, diff_frame, 255,
							Imgproc.ADAPTIVE_THRESH_MEAN_C,
							Imgproc.THRESH_BINARY_INV, 5, 2);
					array = detection_contours(diff_frame);
					if (array.size() > 0) {

						Iterator<Rect> it2 = array.iterator();
						while (it2.hasNext()) {
							Rect obj = it2.next();
							Imgproc.rectangle(imag, obj.br(), obj.tl(),new Scalar(0, 255, 0), 1);

							if(rectMax.area() < obj.area() ){
								rectMax = obj;
							}
						}

						if(rectMax.area() > 1000 ){
							
							depX = (int) ((WIDTH/2-rectMax.x)/16);
							depY = (int) ((HEIGHT/2-rectMax.y)/16);
							System.out.println("deplacement "+depX+" "+ depY);
							t.setUrl("http://admin:azerty@192.168.43.203/cgi/ptdc.cgi?command=set_relative_pos&posX="+-depX+"&posY="+depY);
							t.setDetectMotion(true);
							System.out.println(t.getDetectMotion());

							//httpController.controlCamera("http://admin:azerty@192.168.43.203/cgi/ptdc.cgi?command=set_relative_pos&posX="+-10+"&posY="+0);
						}
//						if (testCamera == false) {
//							testCamera = !testCamera;
//							System.out.println("RENTRE ICI");;
//							httpController.controlCamera("http://admin:azerty@192.168.43.203/cgi/ptdc.cgi?command=set_relative_pos&posX="+10+"&posY="+0);
//						}
						
						
					}
				}

				i = 1;

				ImageIcon image = new ImageIcon(Mat2bufferedImage(imag));
				vidpanel.setIcon(image);
				vidpanel.repaint();
				tempon_frame = outerBox.clone();

			}
		}
	}
	public static BufferedImage Mat2bufferedImage(Mat image) {
		MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".jpg", image, bytemat);
		byte[] bytes = bytemat.toArray();
		InputStream in = new ByteArrayInputStream(bytes);
		BufferedImage img = null;
		try {
			img = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
	}


	public static ArrayList<Rect> detection_contours(Mat outmat) {
		Mat v = new Mat();
		Mat vv = outmat.clone();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST,
				Imgproc.CHAIN_APPROX_SIMPLE);

		double maxArea = 100;
		int maxAreaIdx = -1;
		Rect r = null;
		ArrayList<Rect> rect_array = new ArrayList<Rect>();

		for (int idx = 0; idx < contours.size(); idx++) { 
			Mat contour = contours.get(idx); 
			double contourarea = Imgproc.contourArea(contour); 
			if (contourarea > maxArea) {
				// maxArea = contourarea;
				maxAreaIdx = idx;
				r = Imgproc.boundingRect(contours.get(maxAreaIdx));
				rect_array.add(r);
				Imgproc.drawContours(imag, contours, maxAreaIdx, new Scalar(0,0, 255));
			}

		}

		v.release();

		return rect_array;

	}
	//		if(rectMax.area() < obj.area() ){
	//		rectMax = obj;
	//	}
	//}

	//if(rectMax.area() > 50){
	//
	//	depX =(int) (WIDTH/2-rectMax.x);
	//	depY = (int) (HEIGHT/2-rectMax.y);
	//	System.out.println(depX+" "+ depY);
	//	//httpController.controlCamera("http://admin:azerty@192.168.43.43/cgi/ptdc.cgi?command=set_relative_pos&posX="+depX+"&posY="+0);
	//
	//}


}