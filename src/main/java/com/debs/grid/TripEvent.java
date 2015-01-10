package com.debs.grid;

import java.util.Calendar;

import com.debs.grid.LL2XY.Cell;

public class TripEvent {
   public final Calendar startTime;
   public final Calendar endTime;

   public final Cell startCell;
   public final Cell endCell;
   public final Double distance;

   public TripEvent(Calendar startTime, Calendar endTime, Cell startCell, Cell endCell,
            Double distance) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.startCell = startCell;
      this.endCell = endCell;
      this.distance = distance;
   }
}
