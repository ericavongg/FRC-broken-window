package org.usfirst.frc.team5652.robot;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This is a demo program showing the use of the RobotDrive class. The
 * SampleRobot class is the base of a robot application that will automatically
 * call your Autonomous and OperatorControl methods at the right time as
 * controlled by the switches on the driver station or the field controls.
 *
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SampleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 *
 * WARNING: While it may look like a good choice to use for your code if you're
 * inexperienced, don't. Unless you know what you are doing, complex code will
 * be much more difficult under this system. Use IterativeRobot or Command-Based
 * instead if you're new.
 */
public class Robot extends SampleRobot {
	RobotDrive myRobot;
	Joystick stick;
	Victor motor_5, motor_6,motor_7;
	Solenoid pneumatic_solenoid;
	Button btn_lift_up, btn_lift_down, btn_pneu_close, btn_pneu_open, btn_soft_mode;
	AtomicBoolean soft_touch_mode = new AtomicBoolean(false);
	
	static private boolean IS_VISION_SIMPLE = true;
	
	double sensitivity = 0.30; // 30% sensitivity
	double lift_power_down = 0.45;
	double lift_power_up = 1.0;
	double lift_power_stop = 0.00;
	Integer loop_count = 0;

	long profiler_start = System.currentTimeMillis();
	long profiler_end = System.currentTimeMillis();
		
	Vision vision;
	Thread thread;    
	
	CameraServer camserver;

	public Robot() {
		// We have 2 motors per wheel
		myRobot = new RobotDrive(0, 1);

		myRobot.setExpiration(0.1);
		
		/* 
		 * Setup the victors objects
		 * http://content.vexrobotics.com/docs/217-2769-Victor888UserManual.pdf
		 * 
		 */
		motor_5 = new Victor(2);
		motor_6 = new Victor(3);
		motor_7 = new Victor(4);
		
		/*
		 * http://crosstheroadelectronics.com/control_system.html
		 * http://www.vexrobotics.com/217-4243.html
		 * http://khengineering.github.io/RoboRio/faq/pcm/
		 * http://content.vexrobotics.com/vexpro/pdf/217-4243-PCM-Users-Guide-20141230.pdf
		 */
		pneumatic_solenoid = new Solenoid(0); // This is the pneumatic object

		// Joystick init
		/*
		 * https://wpilib.screenstepslive.com/s/3120/m/7912/l/133053-joysticks
		 * 
		 */
		stick = new Joystick(0);

		btn_lift_up = new JoystickButton(stick, 1); // Lift up
		btn_lift_down = new JoystickButton(stick, 2); // Lift down

		btn_pneu_close = new JoystickButton(stick, 4); // pneumatic close
		btn_pneu_open = new JoystickButton(stick, 6); // pneumatic open
		
		btn_soft_mode = new JoystickButton(stick,  12); // Soft touch mode
		
		
		// Create vision object and thread
		/*
		 * http://khengineering.github.io/RoboRio/vision/cameratest/
		 * 
		 */
		if (IS_VISION_SIMPLE == false) {
			vision = new Vision();
			thread = new Thread(vision);
			thread.start();
		}
		else {
			// For practice, we don't need complicated.
			camserver = CameraServer.getInstance();
			camserver.setQuality(10);
			camserver.setSize(2);
		     //the camera name (ex "cam0") can be found through the roborio web interface
			camserver.startAutomaticCapture("cam0");
		}
		
	}

	/**
	 * Drive left & right motors for 2 seconds then stop
	 */
	public void autonomous() {
		myRobot.setSafetyEnabled(false);
		myRobot.drive(-0.5, 0.0); // drive forwards at 5%
		Timer.delay(1.5); // for 1.5 seconds
		myRobot.drive(-0.05, 0.0); // slows down to 5% to pick up box
		Timer.delay(0.5); // for .5 seconds

		forklift_up();
		Timer.delay(.3);
		forklift_stop();

		myRobot.drive(-0.5, 0.0); // drive forwards half speed
		Timer.delay(5.0); // for 5 seconds

		myRobot.drive(0.0, 0.0); // stop

		forklift_down();
		Timer.delay(.3);
		forklift_stop();

		myRobot.drive(0.0, 0.0); // stop robot
	}

	public void forklift_up() {
		
		motor_5.set(sensitivity * lift_power_up);
		motor_6.set(sensitivity * -1 * lift_power_up);
		motor_7.set(sensitivity * lift_power_up);
		
	}

	public void forklift_down() {
		motor_5.set(sensitivity * -1 * 	lift_power_down);
		motor_6.set(sensitivity * lift_power_down);
		motor_7.set(sensitivity * -1 * 	lift_power_down);
	}

	public void forklift_stop() {
		motor_5.set(lift_power_stop);
		motor_6.set(lift_power_stop);
		motor_7.set(lift_power_stop);
	}

	public void close_arm() {
		pneumatic_solenoid.set(true);
	}

	public void open_arm() {
		pneumatic_solenoid.set(false);
	}

	/**
	 * Runs the motors with arcade steering.
	 */
	public void operatorControl() {
		myRobot.setSafetyEnabled(true);
		while (isOperatorControl() && isEnabled()) {
			profiler_start = System.currentTimeMillis();
			
			// Hold the soft touch button to force sensitive controls.
			soft_touch_mode.set(btn_soft_mode.get());
			if (soft_touch_mode.get() == true){
				sensitivity = 0.3;
			}
			else {
				sensitivity = 1.0;
			}
			
			myRobot.arcadeDrive(sensitivity * stick.getY(), 
								sensitivity * -1 * stick.getX());
			

			// lifts fork lift up
			if (btn_lift_up.get() == true && btn_lift_down.get() == false) {
				forklift_up();
			}
			// brings fork lift down
			else if (btn_lift_down.get() == true && btn_lift_up.get() == false) {
				forklift_down();
			} else {
				forklift_stop();
			}

			if (btn_pneu_close.get() == true && btn_pneu_open.get() == false) {
				close_arm();
			} else if (btn_pneu_open.get() == true && btn_pneu_close.get() == false) {
				open_arm();
			}

			// 1/0.005 s = 5 ms
			// 200 * 0.005 = 1000 = 1 sec
			if ((loop_count++ % 30) == 0) {

				// Profiler code
				profiler_end = System.currentTimeMillis();
				SmartDashboard.putNumber("profiler_drive", profiler_end
						- profiler_start);
				SmartDashboard.putString("ERROR", "NONE");
				profiler_start = System.currentTimeMillis();
				
				if(soft_touch_mode.get() == true){
					SmartDashboard.putString("SOFT_TOUCH", "ENABLED");
				}
				else {
					SmartDashboard.putString("SOFT_TOUCH", "DISABLED");
				}
				
				// If we want to do image processing. 
				if (IS_VISION_SIMPLE == false){
					vision.set_vision_send_image();
				}

				profiler_end = System.currentTimeMillis();
				SmartDashboard.putNumber("profiler_vision_thread", profiler_end
						- profiler_start);
			}

			Timer.delay(0.005); // wait for a motor update time
		}

	}

	/**
	 * Runs during test mode
	 */
	public void test() {
	}
}
