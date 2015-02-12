package com.example

import java.text.NumberFormat
import java.util.Locale
import java.text.ParsePosition
import java.util.Date
import java.text.ParseException
import java.text.SimpleDateFormat

object DataExtractor {

  val RADIANS: Double = 57.2957795
  val SIMPLE_DATE_FORMAT: SimpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
    
  def extractTripEventData(line: String): TripEvent = {
    implicit val formatter = NumberFormat.getInstance(Locale.ENGLISH)
    
        val tripData = line.split(",")  

        val sLong1 = parseDouble(tripData(6)).get
        val sLat1 = parseDouble(tripData(7)).get
        val sLong2 = parseDouble(tripData(8)).get
        val sLat2 = parseDouble(tripData(9)).get
        
        //log.info(sLong1.toString + sLat1.toString + sLong2.toString + sLat2.toString)
        
        val xy1 = computeLL2XY(sLat1, sLong1)
        val xy2 = computeLL2XY(sLat2, sLong2)
        
        val distance = (getEuclideanDistance(xy1, xy2)) / 1000
        val cell1 = getCellID(xy1)
        val cell2 = getCellID(xy2)
            
        val start = tripData(2)
        val end = tripData(3)
        
        TripEvent(extractDateTime(start), extractDateTime(end), cell1, cell2, distance);
  
    
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
  
  // returns distance in meters
  def getEuclideanDistance(xy1: XY, xy2: XY): Double = Math.sqrt(((xy2.xx - xy1.xx) * (xy2.xx - xy1.xx))
		  													  + ((xy2.yy - xy1.yy) * (xy2.yy - xy1.yy)))
        
  def computeLL2XY(srcLat: Double, srcLong: Double): XY = {
      val oLat: Double = 41.474937
      val oLong: Double  = -74.913585
      val angle: Double  = 0.0
      var xx = (srcLong - oLong) * metersDegLong(oLat)
      var yy = (srcLat - oLat) * metersDegLat(oLat)

      val r = Math.sqrt(xx * xx + yy * yy)

      val ct = xx / r
      val st = yy / r

      xx = r * ((ct * Math.cos(angle)) + (st * Math.sin(angle)))
      yy = r * ((st * Math.cos(angle)) - (ct * Math.sin(angle)))

      return new XY(xx, yy)
  }
  
  def getCellID(xy: XY): Cell = new Cell(Math.round(xy.xx / 500), Math.round(xy.yy / 500))
   
  
  def metersDegLong(x: Double): Double = {
      val d2r = degToRadians(x);
      return ((111415.13 * Math.cos(d2r)) - (94.55 * Math.cos(3.0 * d2r)) + (0.12 * Math
               .cos(5.0 * d2r)));
  }
  
  def metersDegLat(x: Double): Double = {
      val d2r = degToRadians(x);
      return (111132.09 - (566.05 * Math.cos(2.0 * d2r)) + (1.20 * Math.cos(4.0 * d2r)) - (0.002 * Math
               .cos(6.0 * d2r)));
  }
  
  def degToRadians(x: Double): Double = x / RADIANS
  
  def parseDouble(s: String)(implicit nf: NumberFormat) = {
    val pp = new ParsePosition(0)
    val d = nf.parse(s, pp)
    if (pp.getErrorIndex == -1) Some(d.doubleValue) else None
  }
}