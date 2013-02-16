package com.asim.learning;

import android.os.SystemClock;

public class Stopwatch {

	private long ticks;
	
	public Stopwatch(){
		ticks = 0;
	}
	
	public void start(){
		ticks = SystemClock.elapsedRealtime();
	}
	
	public long stop(){
		ticks = SystemClock.elapsedRealtime() - ticks;
		return ticks;
	}
	

}
