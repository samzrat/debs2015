package com.example

import java.text.NumberFormat
import java.util.Locale
import java.text.ParsePosition
import java.util.Date
import java.text.ParseException
import java.text.SimpleDateFormat

object DataExtractor {

  val grid500 = new Grid(500, 41.474937, -74.913585, 300)
  val grid250 = new Grid(250, 41.474937, -74.913585, 600)
  
  val SIMPLE_DATE_FORMAT: SimpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
    
  def extractTripEventData(line: String): Option[TripEvent] = {
    implicit val formatter = NumberFormat.getInstance(Locale.ENGLISH)
    
        val tripData = line.split(",")  

        val startTime = tripData(2)
        val endTime   = tripData(3)
        
        val sLong1 = parseDouble(tripData(6)) 
        val sLat1 = parseDouble(tripData(7))
        val sLong2 = parseDouble(tripData(8))
        val sLat2 = parseDouble(tripData(9))
        
        if(sLong1==None || sLat1==None || sLong2==None || sLat2==None)
          return None
        
        val grid500Cells =  (grid500.getCell(sLong1.get, sLat1.get), grid500.getCell(sLong2.get, sLat2.get)) match {
          case (startCell, endCell) if(startCell==None || endCell==None) => None
          case (startCell, endCell) => Some(TripCells(startCell.get, endCell.get))   
        }
        
        val grid250Cells =  (grid250.getCell(sLong1.get, sLat1.get), grid250.getCell(sLong2.get, sLat2.get)) match {
          case (startCell, endCell) if(startCell==None || endCell==None) => None
          case (startCell, endCell) => Some(TripCells(startCell.get, endCell.get))   
        }
            
                
        (grid500Cells, grid250Cells) match {
          case (x, y) if(x==None || y==None) => None
          case (x, y) => Some(TripEvent(extractDateTime(startTime), extractDateTime(endTime), x.get, y.get))
        }
  }
  
  def extractDateTime(dateTimeStr: String): Date = {
      try {
         return SIMPLE_DATE_FORMAT.parse(dateTimeStr);
      }  catch {
         case ex: ParseException =>{
            println("Invalid date - " + dateTimeStr);
            ex.printStackTrace();
            return null;
         }
      }
   }
  
 
  def parseDouble(s: String)(implicit nf: NumberFormat) = {
    val pp = new ParsePosition(0)
    val d = nf.parse(s, pp)
    if (pp.getErrorIndex == -1) Some(d.doubleValue) else None
  }
}