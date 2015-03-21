package samzrat.debs2015

class Grid(cellSize: Int, firstCellCentreLongitude: Double, firstCellCentreLatitude: Double, maxCell: Int) {
  
  val XdegreesPer500 = 0.005986
  val YdegreesPer500 = 0.004491556
  
  val XdegreesPerCell = if(cellSize==500) XdegreesPer500 else XdegreesPer500/2.0
  val YdegreesPerCell = if(cellSize==500) YdegreesPer500 else YdegreesPer500/2.0
  
  val origin_longitude = firstCellCentreLongitude - XdegreesPerCell/2.0
  val origin_latitude  = firstCellCentreLatitude + YdegreesPerCell/2.0
  
  def getCell(longitute: Double, latitude: Double): Option[Cell] = {
    val cell: Cell = Cell(((longitute-origin_longitude)/XdegreesPerCell).toInt + 1, ((origin_latitude-latitude)/YdegreesPerCell).toInt + 1)  
    if(cell.xCell<1 || cell.yCell<1 || cell.xCell>maxCell || cell.yCell>maxCell)
      None
    else
      Some(cell)      
  }
}