class Square{
  public int xCoor;
  public int yCoor;
  public String pieceName;
  //Object class that represents squares on the chess board
  public Square(int x, int y, String name){
    xCoor = x;
    yCoor = y;
    pieceName = name;
  }

  public Square(int x, int y){
    xCoor = x;
    yCoor = y;
    pieceName = "";
  }

  //Getting/Returns x coordinate
  public int getXC(){
    return xCoor;
  }
  //Getting/Returns y coordinate
  public int getYC(){
    return yCoor;
  }
  //Getting/Returns piece Name
  public String getName(){
      return pieceName;
  }
}
