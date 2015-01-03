package com.debs.grid;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class Window {
	private Calendar startTime; 
	private Calendar endTime; 
	
	private List<TripEvent> currentEvents = new ArrayList<TripEvent>();
	
	public void addNewTripEvent(TripEvent newTripEvent) {
		endTime = newTripEvent.endTime;
		
		startTime = ((Calendar)(endTime.clone()));
		startTime.add(Calendar.HOUR_OF_DAY, -30);
		
		for (TripEvent tripEvent : currentEvents) {
			if(tripEvent.endTime.compareTo(this.startTime)<0) {
				currentEvents.remove(tripEvent);
			}
		}
		currentEvents.add(newTripEvent);
	}
	
}
