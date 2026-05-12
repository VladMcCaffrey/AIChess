class Move{
  Square start;
  Square landing;
  //Move class returns starting and landing positions of pieces
  public Move(Square x, Square y){
    start = x;
    landing = y;
  }

  public Move(){
    
  }
  //Getting/Returns start posistion of piece
  public Square getStart(){
    return start;
  }
  //Getting/Returns landing posistion of piece
  public Square getLanding(){
    return landing;
  }
}
