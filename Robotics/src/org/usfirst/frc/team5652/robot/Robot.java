
package org.usfirst.frc.team5652.robot;


import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.ButtonType;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This is a demo program showing the use of the RobotDrive class.
 * The SampleRobot class is the base of a robot application that will automatically call your
 * Autonomous and OperatorControl methods at the right time as controlled by the switches on
 * the driver station or the field controls.
 *
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SampleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 *
 * WARNING: While it may look like a good choice to use for your code if you're inexperienced,
 * don't. Unless you know what you are doing, complex code will be much more difficult under
 * this system. Use IterativeRobot or Command-Based instead if you're new.
 */
public class Robot extends SampleRobot {
    RobotDrive myRobot;
    Joystick stick;
    Victor lift_system,lift_system2;
    Solenoid pneumatic_solenoid;
    Button b1, b2, b3, b4, b6;
    
     double lift_power_down = 0.35;
     double lift_power_up = 1;
     Integer loop_count = 0;

    public Robot() {
    	// We have 2 motors per wheel 
        myRobot = new RobotDrive(0,1);
        myRobot.setExpiration(0.1);
        stick = new Joystick(0);
        lift_system = new Victor(2);
        lift_system2 = new Victor(3);
        pneumatic_solenoid = new Solenoid(0);
        
        b1 = new JoystickButton(stick, 1);
        b2 = new JoystickButton(stick, 2);
       
        b4 = new JoystickButton(stick, 4);   // pneumatic 
        b6 = new JoystickButton(stick, 6);
        
    }
   

    /**
     * Drive left & right motors for 2 seconds then stop
     */
    public void autonomous() {
        myRobot.setSafetyEnabled(false);
        myRobot.drive(-0.5, 0.0);	// drive forwards at 5%
        Timer.delay(1.5);			// for 1.5 seconds
        myRobot.drive(-0.05, 0.0);  // slows down to 5% to pick up box
        Timer.delay(0.5);			// for .5 seconds 
        
        forklift_up();
        Timer.delay(.3);
        forklift_stop();
        
        myRobot.drive(-0.5, 0.0);	// drive forwards half speed
        Timer.delay(5.0);			// for 5 seconds
        
        myRobot.drive(0.0, 0.0);    // stop
        
        forklift_down();
        Timer.delay(.3);
        forklift_stop();
        
        myRobot.drive(0.0, 0.0);	// stop robot
    }
    public void forklift_up() {
    	lift_system.set (lift_power_up);
    	lift_system2.set(-1*lift_power_up);
    }
    public void forklift_down() {
    	lift_system.set(-1*lift_power_down);
    	lift_system2.set(lift_power_down);
    }
    public void forklift_stop() {
    	lift_system.set(0);
    	lift_system2.set(0);
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
            myRobot.arcadeDrive(stick); // drive with arcade style (use right stick)
            //lift_system.set(0.5);
            
            // lifts fork lift up
            if (b1.get() == true && b2.get() == false) {
            	forklift_up();
            }
            // brings fork lift down
            else if (b2.get() == true && b1.get() == false) {
            	forklift_up();
            }else
            {
            	forklift_stop();
            }
      
            if (b4.get() == true && b6.get() == false) {
            	close_arm();
            }
            else if (b6.get() == true && b4.get() == false) {
            	open_arm();
            }
            else {
            	SmartDashboard.putString("ERROR", "STOP TOUCHING THE BUTTONS");
            	
            }
      
           
             Timer.delay(0.005);		// wait for a motor update time
        }
           
    }

    /**
     * Runs during test mode
     */
    public void test() {
    }
}