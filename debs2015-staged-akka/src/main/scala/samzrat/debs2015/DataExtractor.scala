package samzrat.debs2015

import java.text.NumberFormat
import java.util.Locale
import java.text.ParsePosition
import java.util.Date
import java.text.ParseException
import java.text.SimpleDateFormat

object DataExtractor {

  val grid500 = new Grid(500, -74.913585, 41.474937, 300)
  val grid250 = new Grid(250, -74.913585, 41.474937, 600)
  
  val SIMPLE_DATE_FORMAT: SimpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
    
  def extractTripEventData(line: String): Option[TripEvent] = {
    implicit val formatter = NumberFormat.getInstance(Locale.ENGLISH)
    
        val tripData = line.split(",")  
       
        (parseDouble(tripData(6)), parseDouble(tripData(7)), parseDouble(tripData(8)),parseDouble(tripData(9))) match {
          case (a, b, c, d) if(a==None || b==None || c==None || d==None) => return None
          case (Some(sLong1), Some(sLat1), Some(sLong2), Some(sLat2)) => {
            
            (tripData(0), getGrid500Cells(sLong1, sLat1, sLong2, sLat2), getGrid250Cells(sLong1, sLat1, sLong2, sLat2), extractDateTime(tripData(2)), extractDateTime(tripData(3)), parseDouble(tripData(11)), parseDouble(tripData(14))) match {
              case (a, b, c, d, e, f, g) if(b==None || c==None || d==None || e==None || f==None || g==None) => return None
              case (medallion, Some(grid500Cells), Some(grid250Cells), Some(startTime), Some(endTime), Some(fareAmount), Some(tipAmount)) => return Some(TripEvent(medallion, startTime, endTime, grid500Cells, grid250Cells, fareAmount, tipAmount))
            }
          }
        }
  }
  
  def getGrid500Cells(sLong1: Double, sLat1: Double, sLong2: Double, sLat2: Double): Option[TripCells] = {
    (grid500.getCell(sLong1, sLat1), grid500.getCell(sLong2, sLat2)) match {
      case (None, Some(_)) | (Some(_), None) | (None, None)=> return None
      case (startCell, endCell) => Some(TripCells(startCell.get, endCell.get))   
    }
  }
  
  def getGrid250Cells(sLong1: Double, sLat1: Double, sLong2: Double, sLat2: Double): Option[TripCells] = {
    (grid250.getCell(sLong1, sLat1), grid250.getCell(sLong2, sLat2)) match {
      case (None, Some(_)) | (Some(_), None) | (None, None)=> return None
      case (startCell, endCell) => Some(TripCells(startCell.get, endCell.get))   
    }
  }
  
  
  def extractDateTime(dateTimeStr: String): Option[Date] = {
      try {
         Some(SIMPLE_DATE_FORMAT.parse(dateTimeStr))
      }  catch {
         case ex: ParseException =>{
            println("Invalid date - " + dateTimeStr);
            ex.printStackTrace();
            None
         }
      }
   }
  
 
  def parseDouble(s: String)(implicit nf: NumberFormat) = {
    val pp = new ParsePosition(0)
    val d = nf.parse(s, pp)
    if (pp.getErrorIndex == -1) Some(d.doubleValue) else None
  }
}