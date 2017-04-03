package cameraMotionDetection;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import com.sun.javafx.scene.control.GlobalMenuAdapter;

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




	boolean testCamera = false;
	private int noise_movement;

	Mat imag=null;
	final static int HEIGHT = 480;
	final static int WIDTH = 640;

	final static String MAX_RECT_ALGO = "MAX_RECT_ALGO";
	final static String GLOBAL_RECT_ALGO = "GLOBAL_RECT_ALGO";

	final static float DISTANCE_CORRECTION = (float) (WIDTH*0.025);
	
	private int noise_mouvement;
	private String algo;
	private String ip;
	private String username;
	private String passeword;
	private boolean folow;
	private boolean record;
	private VideoWriter vNormal;
	private VideoWriter vGrey;
	private VideoWriter vBlackWhite;
	private VideoWriter vMotionDetection;


	public MotionDetection(String algo, int noise, String ip, String username,  String passeword, boolean folow, boolean record){
		this.algo = algo;
		this.noise_mouvement = noise;
		this.ip = ip;
		this.username = username;
		this.passeword = passeword;
		this.folow = folow;
		this.record = record;
	}

	public void motionDetection() {

		if(record){
			vNormal = new VideoWriter("video/videoNormal.mp4", VideoWriter.fourcc('H', '2', '6', '4'), 20, new Size(WIDTH, HEIGHT),true);
			vGrey = new VideoWriter("video/videoGrey.mp4", VideoWriter.fourcc('H', '2', '6', '4'), 20, new Size(WIDTH, HEIGHT), false);
			vBlackWhite = new VideoWriter("video/videoBlackWhite.mp4", VideoWriter.fourcc('H', '2', '6', '4'), 20, new Size(WIDTH, HEIGHT),false);
			vMotionDetection = new VideoWriter("video/videoMotionDetection.mp4", VideoWriter.fourcc('H', '2', '6', '4'), 20, new Size(WIDTH, HEIGHT), true);
		}
		

		JFrame jframe = new JFrame("Motion Detection");
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel vidpanel = new JLabel();
		jframe.setContentPane(vidpanel);
		jframe.setSize(WIDTH,HEIGHT);
		jframe.setVisible(true);

		Mat frame = new Mat();
		Mat outerBox = new Mat();
		Mat diff_frame = null;
		Mat tempon_frame = null;
		ArrayList<Rect> array = new ArrayList<Rect>();
		VideoCapture camera = new VideoCapture("http://"+username+":"+passeword+"@"+ip+"/video/mjpg.cgi");
		Size sz = new Size(WIDTH, HEIGHT);
		int i = 0;

		HttpQuery t =  new HttpQuery();
		t.start();

		Rect rectMax;
		int depX,depY;
		int azert =0;


		while (true && azert < 300) {
			azert+=1;
			rectMax = new Rect(0, 0, 0, 0 );

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

					if(algo ==  MAX_RECT_ALGO) {
						Imgproc.adaptiveThreshold(diff_frame, diff_frame, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);

					}
					else {
						Imgproc.threshold(diff_frame, diff_frame, 25, 255, Imgproc.THRESH_BINARY);

					}
					if(!t.getMove()){
						array = contourDetection(diff_frame, t);
						if (array.size() > 0) {

							Iterator<Rect> it2 = array.iterator();
							
							Rect firstRect = array.get(0);
							
							rectMax.x = firstRect.x; 
							rectMax.y = firstRect.y; 
							rectMax.width = firstRect.width; 
							rectMax.height = firstRect.height;
							while (it2.hasNext()) {
								Rect obj = it2.next();
								//Imgproc.rectangle(imag, obj.br(), obj.tl(),new Scalar(0, 255, 0), 1);

								rectMax = algoRect(algo, rectMax, obj);

							}
							Imgproc.rectangle(imag, rectMax.br(), rectMax.tl(),new Scalar(0, 0, 255), 3);

							if(folow){
								depX = (int) ((WIDTH/2-rectMax.x - rectMax.width/2)/DISTANCE_CORRECTION);
								depY = (int) ((HEIGHT/2-rectMax.y - rectMax.height/2)/DISTANCE_CORRECTION);
								System.out.println("deplacement "+depX+" "+ depY);
								t.setUrl("http://"+username+":"+passeword+"@"+ip+"/cgi/ptdc.cgi?command=set_relative_pos&posX="+-depX+"&posY="+depY);
								t.setDetectMotion(true);
							}



						}
					}
				}

				i = 1;

				ImageIcon image = new ImageIcon(Mat2bufferedImage(imag));
				
				if(record){
					vNormal.write(frame);
					vGrey.write(outerBox);
					vBlackWhite.write(diff_frame);
					vMotionDetection.write(imag);
				}
				
				
				vidpanel.setIcon(image);
				vidpanel.repaint();
				tempon_frame = outerBox.clone();

			}
			//System.out.println(azert);

		}
		
		if(record){
			vNormal.release();
			vGrey.release();
			vBlackWhite.release();
			vMotionDetection.release();
		}
		
		jframe.dispose();
		jframe.setVisible(false);

	}
	public BufferedImage Mat2bufferedImage(Mat image) {
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


	public ArrayList<Rect> contourDetection(Mat outmat, HttpQuery t) {
		Mat v = new Mat();
		Mat vv = outmat.clone();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

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
				if(r.area()>noise_movement) {
					rect_array.add(r);
				}

				//Imgproc.drawContours(imag, contours, maxAreaIdx, new Scalar(0,0, 255));
			}

		}



		return rect_array;

	}

	public Rect algoRect(String algo, Rect rect1,  Rect rect2){
		switch (algo){
		case MAX_RECT_ALGO:
			return rectangleMax(rect1, rect2);

		case GLOBAL_RECT_ALGO :
			return globalRect(rect1, rect2);

		}

		return null;
	}

	public Rect rectangleMax(Rect rect1, Rect rect2 ){
		if(rect1.area() < rect2.area() ){
			return rect2 ;
		}
		return  rect1;
	}

	public Rect globalRect(Rect globalRect, Rect rect) {
		if(globalRect.x + globalRect.width < rect.x + rect.width) {
			globalRect.width =  rect.x + rect.width - globalRect.x;
		}
		if(globalRect.y + globalRect.height < rect.y + rect.height) {
			globalRect.height =  rect.y + rect.height - globalRect.y;
		}

		if(globalRect.x > rect.x) {
			globalRect.x = rect.x;
		}
		if(globalRect.y > rect.y) {
			globalRect.y = rect.y;
		}
		

		return globalRect;
	}

	public Rect globalRect(){


		return null;
	}


}