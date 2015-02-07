package org.usfirst.frc.team5652.robot;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.DrawMode;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.ShapeMode;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Vision implements Runnable {

	int session;
	Image frame;
	CameraServer server;
	AtomicBoolean vision_busy;
	

	public Vision(AtomicBoolean is_vision_busy) {

		// Vision init
		frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);

		// the camera name (ex "cam0") can be found through the roborio web
		// interface
		session = NIVision.IMAQdxOpenCamera("cam0",
				NIVision.IMAQdxCameraControlMode.CameraControlModeController);
		NIVision.IMAQdxConfigureGrab(session);


		server = CameraServer.getInstance();
		server.setQuality(5);
		
		vision_busy = is_vision_busy;
	}

	@Override
	public void run() {
		long profile_vision = System.currentTimeMillis();
		NIVision.IMAQdxStartAcquisition(session);

		/**
		 * grab an image, draw the circle, and provide it for the camera server
		 * which will in turn send it to the dashboard.
		 */
		NIVision.Rect rect = new NIVision.Rect(10, 10, 100, 100);

		// Grab a frame from the webcam
		NIVision.IMAQdxGrab(session, frame, 1);

		// Draw a oval within a frame
		NIVision.imaqDrawShapeOnImage(frame, frame, rect, DrawMode.DRAW_VALUE,
				ShapeMode.SHAPE_OVAL, 0.0f);

		// Draw a rect within a frame
		NIVision.imaqDrawShapeOnImage(frame, frame, rect, DrawMode.DRAW_INVERT,
				ShapeMode.SHAPE_RECT, 0.0f);

		// Send it to the driver station
		CameraServer.getInstance().setImage(frame);

		// Profiling the time it takes to process the image.
		SmartDashboard.putNumber("profiler_vision", System.currentTimeMillis()
				- profile_vision);
		
		NIVision.IMAQdxStopAcquisition(session);

		vision_busy.set(false);

	}

}
