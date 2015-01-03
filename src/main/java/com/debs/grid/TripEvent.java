package com.debs.grid;

import java.util.Calendar;

public class TripEvent {
	public final Calendar startTime; 
	public final Calendar endTime; 
	
	public TripEvent(Calendar startTime, Calendar endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
