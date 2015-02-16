package org.usfirst.frc.team5652.robot;

import edu.wpi.first.wpilibj.DigitalInput;

public class LimitSwitch extends DigitalInput {
	
	private long debounce_prev;
	private long debounce_timeout;
	private boolean cur_value = false;

	public LimitSwitch(int channel) {
		super(channel);
		debounce_timeout = 100; // 100 ms
		debounce_prev = System.currentTimeMillis();
	}
	
	public boolean get(){
		if (System.currentTimeMillis() - debounce_prev > debounce_timeout) {
			cur_value = super.get();
		}
		return cur_value;
	}

}
