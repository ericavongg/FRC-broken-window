package org.usfirst.frc.team5652.robot;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.ColorMode;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.Range;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Vision implements Runnable {

	private int session;
	private Image frame;
	private CameraServer server;
	private AtomicBoolean vision_busy = new AtomicBoolean(false);
	private AtomicBoolean vision_send_image = new AtomicBoolean(false);
	private AtomicBoolean vision_enable = new AtomicBoolean(false);

	public boolean get_vision_enable() {
		return vision_enable.get();
	}

	public void set_vision_enable(boolean enable) {
		vision_enable.set(enable);
	}

	public boolean is_vision_busy() {
		return vision_busy.get();
	}

	public boolean set_vision_send_image() {
		return vision_send_image.getAndSet(true);
	}

	public Vision() {

		// Vision init
		frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);

		// the camera name (ex "cam0") can be found through the roborio web
		// interface
		session = NIVision.IMAQdxOpenCamera("cam0",
				NIVision.IMAQdxCameraControlMode.CameraControlModeController);
		NIVision.IMAQdxConfigureGrab(session);

		server = CameraServer.getInstance();
		// 2 = kSize160x120
		// Not sure why CameraServer sets this private and not public.
		server.setSize(2);
		server.setQuality(5);
		NIVision.IMAQdxStartAcquisition(session);
		vision_enable.set(true);

	}

	@Override
	public void run() {
		while (vision_enable.get() == true) {
			long profile_vision = System.currentTimeMillis();
			if (vision_busy.get() == false && 
					vision_send_image.get() == true) {
				vision_busy.set(true);

				/**
				 * grab an image, draw the circle, and provide it for the camera
				 * server which will in turn send it to the dashboard.
				 */
//				NIVision.Rect rect = new NIVision.Rect(10, 10, 100, 100);

				// Grab a frame from the webcam
				NIVision.IMAQdxGrab(session, frame, 1);

//				// Draw a oval within a frame
//				NIVision.imaqDrawShapeOnImage(frame, frame, rect,
//						DrawMode.DRAW_VALUE, ShapeMode.SHAPE_OVAL, 0.0f);
//
//				// Draw a rect within a frame
//				NIVision.imaqDrawShapeOnImage(frame, frame, rect,
//						DrawMode.DRAW_INVERT, ShapeMode.SHAPE_RECT, 0.0f);

				Image filteredFrame = null;
				// Filter RED & gray. 
				NIVision.imaqColorThreshold(filteredFrame, frame, 0, 
						ColorMode.HSL, 
						new Range(100, 255), 
						new Range(30, 50), 
						new Range(30, 50));

				// Send it to the driver station
				// Actually not necessary for autonomous.
				CameraServer.getInstance().setImage(frame);

				// Profiling the time it takes to process the image.
				SmartDashboard.putNumber("profiler_vision",
						System.currentTimeMillis() - profile_vision);

				// Clean up.
				vision_busy.set(false);
				vision_send_image.set(false);

			}
			
		}
		NIVision.IMAQdxStopAcquisition(session);
	}

}
