package samzrat.debs2015

class Grid(cellSize: Int, firstCellCentreLongitude: Double, firstCellCentreLatitude: Double, maxCell: Int) {
  
  
  
  def getCell(longitute: Double, latitude: Double): Option[Cell] = {
    val cell: Cell = Cell(((longitute-(firstCellCentreLongitude-(cellSize.toDouble/2)))/cellSize).toInt + 1, ((latitude-(firstCellCentreLatitude-(cellSize.toDouble/2)))/cellSize).toInt + 1)  
    if(cell.xCell<1 || cell.yCell<1 || cell.xCell>maxCell || cell.yCell>maxCell)
      None
    else
      Some(cell)      
  }
}