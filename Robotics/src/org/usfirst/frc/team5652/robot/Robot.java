
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
    Button b1, b2, b3;
    

    public Robot() {
    	// We have 2 motors per wheel 
        myRobot = new RobotDrive(0, 1);
        myRobot.setExpiration(0.1);
        stick = new Joystick(0);
        lift_system = new Victor(2);
        lift_system2 = new Victor(3);
        pneumatic_solenoid = new Solenoid(6);
        
        b1 = new JoystickButton(stick, 1);
        b2 = new JoystickButton(stick, 2);
        b3 = new JoystickButton(stick, 3);
        /*
        b1.whenReleased(new Command() {
			
			@Override
			protected boolean isFinished() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			protected void interrupted() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void initialize() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void execute() {
				// TODO Auto-generated method stub
				pneumatic_solenoid.set(true);
				
			}
			
			@Override
			protected void end() {
				// TODO Auto-generated method stub
				
			}
		});
        
        b2.whenReleased(new Command() {
			
			@Override
			protected boolean isFinished() {
				// TODO Auto-generated method stub
				pneumatic_solenoid.set(false);
				return false;
			}
			
			@Override
			protected void interrupted() {
				// TODO Auto-generated method stub
				pneumatic_solenoid.set(false);
			}
			
			@Override
			protected void initialize() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void execute() {
				// TODO Auto-generated method stub
				pneumatic_solenoid.set(false);
				
			}
			
			@Override
			protected void end() {
				// TODO Auto-generated method stub
				pneumatic_solenoid.set(false);
			}
		});
        
        b3.whenReleased(new Command() {
			
			@Override
			protected boolean isFinished() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			protected void interrupted() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void initialize() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void execute() {
				// TODO Auto-generated method stub
				lift_system.set(0);
				lift_system2.set(0);
			}
			
			@Override
			protected void end() {
				// TODO Auto-generated method stub
				lift_system.set(0);
				lift_system2.set(0);
			}
		});

        b3.whenPressed(new Command() {
			
			@Override
			protected boolean isFinished() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			protected void interrupted() {
				// TODO Auto-generated method stub
			}
			
			@Override
			protected void initialize() {
				// TODO Auto-generated method stub
				lift_system.set(0.5);
				lift_system2.set(-0.5);
			}
			
			@Override
			protected void execute() {
				// TODO Auto-generated method stub		
				
			}
			
			@Override
			protected void end() {
				// TODO Auto-generated method stub
			}
		});
        */

    }

    /**
     * Drive left & right motors for 2 seconds then stop
     */
    public void autonomous() {
        myRobot.setSafetyEnabled(false);
        myRobot.drive(-0.5, 0.0);	// drive forwards half speed
        Timer.delay(2.0);		//    for 2 seconds
        myRobot.drive(0.0, 0.0);	// stop robot
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
            	lift_system.set(0.5);
            	lift_system2.set(-0.5);
            }
            // brings fork lift down
            else if (b2. get() == true && b1.get() == false) {
            	lift_system. set(-0.5);
            	lift_system2. set(0.5);
            }else
            {
            	lift_system. set(0);
            	lift_system2. set(0);
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