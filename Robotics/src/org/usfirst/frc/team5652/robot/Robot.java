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
	private DigitalInput upperLimitSwitch = new LimitSwitch (0);
	private DigitalInput lowerLimitSwitch = new LimitSwitch (1);
	
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
	private Solenoid pneumatic_valve0;
	private Solenoid pneumatic_valve1;
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
	
	// TWEENING variables
	private boolean disable_tween = false;
	private long tween_last_seen = System.currentTimeMillis();
	
	static private enum LIFT_STATES  {STOP, UP, DOWN};
	private LIFT_STATES last_lift_state = LIFT_STATES.STOP;
	
	
	// LOWER THIS VALUE TO MODIFY THE TWEEN SPEED
	private double period = 200; // 200 ms * 10 = 2 seconds
	
	// This is the tween hash table.
	// sin(x) is computationally expensive so 
	// made a pseudo waveform using a hash table.
	// Each entry is a power value for the lift motors.
	// MODIFY/TWEAK THESE VALUES!!
	private static double[] TWEEN_SLOW_THEN_FAST = { 
			0.20, // 0 * n ms 
			0.20, 
			0.25,
			0.35,
			0.45,
			0.55, 
			0.65,
			0.75,
			0.90,  
			1.00  // 10 * n ms
			};
	
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
		pneumatic_valve0 = new Solenoid(PCM_CAM_ID, 0); // This is the pneumatic object
		pneumatic_valve1 = new Solenoid(PCM_CAM_ID, 1); // This is the second pneumatic valve? object?
		
		pneumatic_valve0.set(true); //true close
		pneumatic_valve1.set(true); //true close - false means open 
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
		
		// Open arm 
		open_arm(); 
		
		// Disable arms 
		disable_pneumaticvalves();
		
		// TODO: PLEASE EDIT THE AUTONOMOUS CODE!!
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
	
	/*
	 * Tween logic
	 * 
	 * Check if the lift states has changed.
	 * So if the operator released the trigger, 
	 * 		The state is reset to STOP
	 * If the operators holds it down and the states don't change
	 * 		Then we check if 200 ms (may change) has elapsed 
	 * 		since the last time we checked the button state. 
	 * 
	 * If 200 ms has elapsed, we then go and increment the index
	 * while ensuring the index doesn't go over 9 (the current 
	 * array size is of 10).
	 * 
	 * If none of these conditions are met, we return an index of 0. 
	 * index is returned to the caller at the end of the method.
	 * 
	 */
	private int fork_lift_tween(LIFT_STATES state_to_watch) {
		int index = 0;
		// If tween is disabled, just set it to Max value.
		if (disable_tween == true){
			return TWEEN_SLOW_THEN_FAST.length - 1;
		}
		
		if (last_lift_state == state_to_watch) {
			// tween_last_seen should be ok as a shared variable.
			// last_lift_state should change and avoid entry here.
			if (System.currentTimeMillis() - tween_last_seen > period){
				index++;
				if (index >= TWEEN_SLOW_THEN_FAST.length) {
					index = TWEEN_SLOW_THEN_FAST.length - 1;
				}
				tween_last_seen = System.currentTimeMillis();
			}
		} 
		
		return index;
	}

	/*
	 * Code to lift the fork up
	 * 
	 * First it checks if the limit switch is triggered,
	 * 		If so, it won't go up anymore to protect the hardware
	 * else
	 * 		It will move up depending on the sensitivity mode and
	 * 		the amount of tween'd power
	 * At the end, it checks if it needs to stop the forklift
	 * because it hit a limit switch.
	 * TODO? Make it an interrupt, not a poll
	 */
	public void forklift_up() {
		// Tween logic
		int pwr_index;
		double current_power;
		pwr_index = fork_lift_tween(LIFT_STATES.UP);
		
		current_power = TWEEN_SLOW_THEN_FAST[pwr_index];		
		
		if (!upperLimitSwitch.get()) {
			motor_5.set(sensitivity * lift_power_up * current_power);
			motor_6.set(sensitivity * -1 * lift_power_up * current_power);
			motor_7.set(sensitivity * lift_power_up * current_power);
			motor_8.set(sensitivity * -1 * lift_power_up * current_power);
		}
		
		last_lift_state = LIFT_STATES.UP;
		if (lowerLimitSwitch.get()) {
			forklift_stop();
		}
	}

	/*
	 * Code to lift the fork down
	 * 
	 * First it checks if the limit switch is triggered,
	 * 		If so, it won't go down anymore to protect the hardware
	 * else
	 * 		It will move up depending on the sensitivity mode and
	 * 		the amount of tween'd power
	 * 
	 * At the end, it checks if it needs to stop the forklift
	 * because it hit a limit switch.
	 * 
	 * TODO? Make it an interrupt, not a poll
	 */
	public void forklift_down() {
		// Tween logic
		int pwr_index;
		double current_power;
		pwr_index = fork_lift_tween(LIFT_STATES.DOWN);
		
		current_power = TWEEN_SLOW_THEN_FAST[pwr_index];
		
		if (!lowerLimitSwitch.get()) {
			motor_5.set( -1 * lift_power_down * current_power);
			motor_6.set( lift_power_down);
			motor_7.set( -1 * lift_power_down * current_power);
			motor_8.set( lift_power_down);
		}
		
		last_lift_state = LIFT_STATES.DOWN;
		// Probably need this just in case
		if (lowerLimitSwitch.get()) {
			forklift_stop();
		}
	}

	public void forklift_stop() {
		motor_5.set(lift_power_stop);
		motor_6.set(lift_power_stop);
		motor_7.set(lift_power_stop);
		motor_8.set(lift_power_stop);
		last_lift_state = LIFT_STATES.STOP;
	}
/* 
 * Let x be pneumatic valve 0 and let y pneumatic valve 1 be y 
 * If x is open, y needs to be close 
 * If x is closed, y needs to be open 
 * open is false and true is closed (for now - remember to verify later) 
 */
	public void close_arm() {
		pneumatic_valve0.set(true); 
		pneumatic_valve1.set(false);
	}

	public void open_arm() {
		pneumatic_valve0.set(false);
		pneumatic_valve1.set(true);
	}
	
//	Disable valve - have valves close 
	public void disable_pneumaticvalves(){
		pneumatic_valve0.set(true);
		pneumatic_valve1.set(true);
	}
	//pneumatic flush - flush storage cylinders - useful after game 
	public void pneumatic_flush(){
		pneumatic_valve0.set(false);
		pneumatic_valve1.set(false);
	}
	
	private void forklift_logic() {
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
	}
	
	private void soft_touch_logic(){
		// Hold the soft touch button to force sensitive controls.
		soft_touch_mode.set(btn_soft_mode.get());
		if (soft_touch_mode.get() == true) {
			sensitivity = MIN_SENSITIVITY;
		} else {
			sensitivity = MAX_SENSITIVITY;
		}
	}
	
	private void soft_touch_diagnostics() {
		if(soft_touch_mode.get() == true){
			SmartDashboard.putString("SOFT_TOUCH", "ENABLED");
		}
		else {
			SmartDashboard.putString("SOFT_TOUCH", "DISABLED");
		}
		sensitivity = SmartDashboard.getNumber("SENSITIVITY");
	}
	
	private void compressor_diagnostics() {
		// Compressor diagnostics
		// http://wpilib.screenstepslive.com/s/4485/m/13503/l/216217?data-resolve-url=true&data-manual-id=13503
		SmartDashboard.putNumber("Compressor AMPS", pneumatic_compressor.getCompressorCurrent());
		SmartDashboard.putBoolean("CLOSED LOOP?", pneumatic_compressor.getClosedLoopControl());
		SmartDashboard.putBoolean("Compressor Current Fault", pneumatic_compressor.getCompressorCurrentTooHighFault());
		SmartDashboard.putBoolean("Compressor missing", pneumatic_compressor.getCompressorNotConnectedFault());
		SmartDashboard.putBoolean("Compressor Shorted", pneumatic_compressor.getCompressorShortedFault());
		SmartDashboard.putBoolean("Pressure switch too low", pneumatic_compressor.getPressureSwitchValue());
		
		SmartDashboard.putBoolean("Solenoid voltage fault", pneumatic_valve0.getPCMSolenoidVoltageFault());
		SmartDashboard.putNumber("Solenoid bit faults", pneumatic_valve0.getPCMSolenoidBlackList());
		SmartDashboard.putNumber("Solenoid bit status", pneumatic_valve0.getAll());
		
		SmartDashboard.putNumber("Stick POV", stick.getPOV());
		SmartDashboard.putNumber("Stick throttle", stick.getThrottle());
	}
	
	private void interval_logic() {
		// 1/0.005 s = 5 ms
		// 200 * 0.005 = 1000 = 1 sec
		if ((loop_count++ % 100) == 0) {

			// Profiler code, don't edit
			profiler_end = System.currentTimeMillis();
			SmartDashboard.putNumber("profiler_drive_ms", profiler_end
					- profiler_start);
			SmartDashboard.putString("ERROR", "NONE");
			profiler_start = System.currentTimeMillis();
			
			// ADD LOGIC HERE FOR DIAGNOSTICS
			soft_touch_diagnostics();
			compressor_diagnostics();
			
			auto_drive_power = SmartDashboard.getNumber("AUTO_DRIVE_POWER");


			// If we want to do image processing. 
			if (IS_VISION_SIMPLE == false){
				vision.set_vision_send_image();
			}

			// DON't EDIT, PROFILER CODE
			profiler_end = System.currentTimeMillis();
			SmartDashboard.putNumber("profiler_loop_ms", profiler_end
					- profiler_start);
		}
	}

	/**
	 * Runs the motors with arcade steering.
	 */
	public void operatorControl() {
		myRobot.setSafetyEnabled(true);
		while (isOperatorControl() && isEnabled()) {
			profiler_start = System.currentTimeMillis();
			
			// I lowered the sensitivity of the Y axis 
			// so the robot doesn't turn so fast anymore.
			myRobot.arcadeDrive(sensitivity * stick.getY(), 
								-1 * 0.5 * stick.getX());
			
			// This needs to run all the time
			soft_touch_logic();
			forklift_logic();
			
			// The following runs occasionally.
			interval_logic();

			
			Timer.delay(0.005); // wait for a motor update time
		}

	}

	/**
	 * Runs during test mode
	 */
	public void test() {
	}
}
