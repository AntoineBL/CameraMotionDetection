package test;

import cameraMotionDetection.MotionDetection;

public class main {

	public static void main(String[] args) {


		MotionDetection c = new MotionDetection("GLOBAL_RECT_ALGO", 1000, "192.168.43.203", "admin", "azerty", true, false);
		c.motionDetection();



	}

}
