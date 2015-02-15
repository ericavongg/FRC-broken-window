package org.usfirst.frc.team5652.robot;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
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
	// Robot drive settings (tank drive)
	private RobotDrive myRobot;
	
	// Joystick code
	private Joystick stick;
	// Joystick buttons
	private Button btn_lift_up, btn_lift_down, btn_pneu_close, btn_pneu_open, btn_soft_mode;
	private AtomicBoolean soft_touch_mode = new AtomicBoolean(false);
	
	// Limit switches for fork lift.
	private DigitalInput upperLimitSwitch = new DigitalInput (0);
	private DigitalInput lowerLimitSwitch = new DigitalInput (1);
	
	// Autonomous drive power. 
	// 1.0 is FULL SPEED. 
	// Change this if you need to power
	private double auto_drive_power = 0.5; 
	
	static private double MAX_SENSITIVITY = 1.0; // DO NOT EDIT
	
	// Modify this value to change sensitivity of the controls. 
	private double MIN_SENSITIVITY = 0.75; 
	

	private double sensitivity = MIN_SENSITIVITY;
	
	// Change to false to use image processing code.
	static private boolean IS_VISION_SIMPLE = true;
	
	// Power distribution module
	private PowerDistributionPanel pdp;
	
	// Motors
	private Victor motor_5, motor_6,motor_7, motor_8;
	
	// Pneumatics
	static private int PCM_CAM_ID = 2;
	private Solenoid pneumatic_solenoid;
	private Compressor pneumatic_compressor;
	
	// Lift controls p
	private double lift_power_down = 0.45;
	private double lift_power_up = 1.0;
	private double lift_power_stop = 0.00;
	private Integer loop_count = 0;

	private long profiler_start;
	private long profiler_end;

	// Camera variables
	private Vision vision;
	private Thread thread;
	private CameraServer camserver;

	
	public Robot() {
		// We have 2 motors per wheel
		myRobot = new RobotDrive(0, 1);
		
		// Is 100 ms too little for a timeout?
		// Should it be 1 second?
		myRobot.setExpiration(0.1);
		
		// PDP setup
		pdp = new PowerDistributionPanel();
		/* 
		 * Setup the victors objects
		 * http://content.vexrobotics.com/docs/217-2769-Victor888UserManual.pdf
		 * 
		 */
		motor_5 = new Victor(2);
		motor_6 = new Victor(3);
		motor_7 = new Victor(4);
		motor_8 = new Victor(5);
		
		/*
		 * http://crosstheroadelectronics.com/control_system.html
		 * http://www.vexrobotics.com/217-4243.html
		 * http://khengineering.github.io/RoboRio/faq/pcm/
		 * http://content.vexrobotics.com/vexpro/pdf/217-4243-PCM-Users-Guide-20141230.pdf
		 */
		pneumatic_solenoid = new Solenoid(PCM_CAM_ID, 0); // This is the pneumatic object
		pneumatic_compressor = new Compressor(PCM_CAM_ID);
		pneumatic_compressor.setClosedLoopControl(true);

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
			SmartDashboard.putString("CAMERA", "MANUAL MODE");
		}
		else {
			// For practice, we don't need complicated.
			camserver = CameraServer.getInstance();
			camserver.setQuality(10);
			camserver.setSize(2);
		     //the camera name (ex "cam0") can be found through the roborio web interface
			camserver.startAutomaticCapture("cam0");
			SmartDashboard.putString("CAMERA", "AUTOMATIC MODE");
		}
		
		// SmartDashboard defaults
		SmartDashboard.putNumber("AUTO_DRIVE_POWER", auto_drive_power);
		SmartDashboard.putNumber("AUTO_DRIVE_POWER", sensitivity);
	}

	/*
	 * Stop the robot drive system
	 */
	private void drive_stop(){
		myRobot.drive(0, 0.0); 
	}
	
	/*
	 * For autonomous
	 * Depends the auto_drive_power 
	 */
	private void drive_forward(double seconds){
		myRobot.drive(-1 * auto_drive_power, 0.0); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_forward(double power, double seconds){
		myRobot.drive(-1 * power, 0.0); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Depends the auto_drive_power 
	 */
	private void drive_backwards(double seconds){
		myRobot.drive(auto_drive_power, 0.0); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_backwards(double power, double seconds){
		myRobot.drive(power, 0.0); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_rotate_left(double power, double seconds){
		myRobot.drive(power, 1); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_rotate_left(double seconds){
		myRobot.drive(auto_drive_power, 1); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_rotate_right(double seconds){
		myRobot.drive(auto_drive_power, -1); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/*
	 * For autonomous
	 * Set your own power
	 */
	private void drive_rotate_right(double power, double seconds){
		myRobot.drive(power, -1); 
		Timer.delay(seconds);
		drive_stop();
	}
	
	/**
	 * Drive left & right motors for 2 seconds then stop
	 */
	public void autonomous() {
		myRobot.setSafetyEnabled(false);
		
		drive_forward(0.5, 1);
		drive_rotate_right(0.4, 0.5);
		drive_backwards(0.5,1);
//		
//		forklift_up();
//		Timer.delay(0.5);
//		forklift_stop();
//
//		drive_rotate_right(0.75, 4);
//		
//		drive_forward(0.5, 5);
//
//		forklift_down();
//		Timer.delay(.3);
//		forklift_stop();
//		
//		drive_backwards(0.5,3);
//		
//		drive_rotate_left(0.75, 4);
//		
//		drive_forward(0.5, 3);

		myRobot.drive(0.0, 0.0); // stop robot
	}

	public void forklift_up() {
		//TODO Add tweening
		if (!upperLimitSwitch.get()) {
			motor_5.set(sensitivity * lift_power_up);
			motor_6.set(sensitivity * -1 * lift_power_up);
			motor_7.set(sensitivity * lift_power_up);
			motor_8.set(sensitivity * -1 * lift_power_up);
		}
	}

	public void forklift_down() {
		// TODO Add limit switch check to prevent hitting the sprockets.
		if (!lowerLimitSwitch.get()) {
			motor_5.set( -1 * 	lift_power_down);
			motor_6.set( lift_power_down);
			motor_7.set( -1 * 	lift_power_down);
			motor_8.set( lift_power_down);
		}
	}

	public void forklift_stop() {
		motor_5.set(lift_power_stop);
		motor_6.set(lift_power_stop);
		motor_7.set(lift_power_stop);
		motor_8.set(lift_power_stop);
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
				sensitivity = MIN_SENSITIVITY;
			}
			else {
				sensitivity = MAX_SENSITIVITY;
			}
			
			myRobot.arcadeDrive(sensitivity * stick.getY(), 
								-1 * 0.5 * stick.getX());
			

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
				
				// Compressor diagnostics
				// http://wpilib.screenstepslive.com/s/4485/m/13503/l/216217?data-resolve-url=true&data-manual-id=13503
				SmartDashboard.putNumber("Compressor AMPS", pneumatic_compressor.getCompressorCurrent());
				SmartDashboard.putBoolean("CLOSED LOOP?", pneumatic_compressor.getClosedLoopControl());
				SmartDashboard.putBoolean("Compressor Current Fault", pneumatic_compressor.getCompressorCurrentTooHighFault());
				SmartDashboard.putBoolean("Compressor missing", pneumatic_compressor.getCompressorNotConnectedFault());
				SmartDashboard.putBoolean("Compressor Shorted", pneumatic_compressor.getCompressorShortedFault());
				SmartDashboard.putBoolean("Pressure switch too low", pneumatic_compressor.getPressureSwitchValue());
				
				SmartDashboard.putBoolean("Solenoid voltage fault", pneumatic_solenoid.getPCMSolenoidVoltageFault());
				SmartDashboard.putNumber("Solenoid bit faults", pneumatic_solenoid.getPCMSolenoidBlackList());
				SmartDashboard.putNumber("Solenoid bit status", pneumatic_solenoid.getAll());
				
				SmartDashboard.putNumber("Stick POV", stick.getPOV());
				SmartDashboard.putNumber("Stick throttle", stick.getThrottle());
				
				auto_drive_power = SmartDashboard.getNumber("AUTO_DRIVE_POWER");
				sensitivity = SmartDashboard.getNumber("SENSITIVITY");

				// If we want to do image processing. 
				if (IS_VISION_SIMPLE == false){
					vision.set_vision_send_image();
				}

				profiler_end = System.currentTimeMillis();
				SmartDashboard.putNumber("profiler_vision_thread_ms", profiler_end
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
