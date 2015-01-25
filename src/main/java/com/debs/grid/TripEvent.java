package com.debs.grid;

import java.util.Date;

import com.debs.grid.LL2XY.Cell;

public class TripEvent {
   public final Date startTime;
   public final Date endTime;

   public final Cell startCell;
   public final Cell endCell;
   public final Double distance;

   public TripEvent(Date startTime, Date endTime, Cell startCell, Cell endCell, Double distance) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.startCell = startCell;
      this.endCell = endCell;
      this.distance = distance;
   }
}
