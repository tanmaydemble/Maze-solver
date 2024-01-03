import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to represent a cell in a maze
class Cell {
  int x;
  int y;
  ArrayList<Cell> neighbor = new ArrayList<Cell>();

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // to find which tree does this cell belong to
  public int findRepresentatives(HashMap<Integer, Integer> map, int cols) {
    int index = this.y * cols + this.x;
    int representativeIndex = map.get(index);
    if (representativeIndex == index) {
      return index;
    } else {
      return new Cell(representativeIndex % cols,
          representativeIndex / cols).findRepresentatives(map, cols);
    }
  }

  // to find the index of a cell in the cell list
  public int findIndex(int cols) {
    return this.x + this.y * cols;
  }

  // to find whether the given cell is to the right of this cell
  public boolean checkRightRelation(Cell cell2) {
    return cell2.x == (this.x + 1)
        && cell2.y == this.y;
  }

  // EFFECT: to place the required wall on the scene
  // if the boolean right is true then we draw the right wall else the bottom one
  public void placeWallHelper(WorldScene scene, int cellSize, boolean right) {
    WorldImage verticalWall = new RectangleImage(1, cellSize, OutlineMode.OUTLINE, Color.BLACK);
    WorldImage horizontalWall = new RectangleImage(cellSize, 1, OutlineMode.OUTLINE, Color.BLACK);

    if (right) {
      scene.placeImageXY(verticalWall, (this.x + 1) * cellSize, this.y * cellSize + cellSize / 2);
    } else {
      scene.placeImageXY(horizontalWall, this.x * cellSize + cellSize / 2,
          (this.y + 1) * cellSize);
    }
  }

  // EFFECT: to add the given cell as the neighbor of this cell
  public void addNeighbor(Cell c) {
    this.neighbor.add(c);

  }

  // EFFECT: to draw the all the cells visited by dfs or bfs
  public void drawVisited(WorldScene scene, int cellSize) {
    WorldImage square = new RectangleImage(cellSize / 2, cellSize / 2, OutlineMode.SOLID,
        new Color(135, 206, 250));    
    scene.placeImageXY(square, this.x * cellSize + cellSize / 2,
        this.y * cellSize + cellSize / 2);
  }

  // EFFECT: to draw the correct path from the start to the end
  public void drawPath(WorldScene scene, int cellSize) {
    WorldImage square = new RectangleImage(cellSize / 2, cellSize / 2, OutlineMode.SOLID,
        new Color(0, 0, 128));   
    scene.placeImageXY(square, this.x * cellSize + cellSize / 2,
        this.y * cellSize + cellSize / 2);

  }

  // to find the x coordinate of the cell that we would reach
  // provided we clicked the given button
  public int findNextX(String key) {    
    if (key.equals("right")) {
      return this.x + 1;
    } else if (key.equals("left")) {
      return this.x - 1;
    } else {
      return this.x;
    }
  }

  //to find the y coordinate of the cell that we would reach
  // provided we clicked the given button
  public int findNextY(String key) {
    if (key.equals("down")) {
      return this.y + 1;
    } else if (key.equals("up")) {
      return this.y - 1;
    } else {
      return this.y;
    }
  }

  // returns the current cell if the key press is not valid
  // returns the next cell corresponding to the key press if the key press is valid
  // given that it has the coordinates of where it will be on the next key press
  // also adds this cell to the visited list if we have not visited it
  public Cell validKey(int nextX, int nextY, ArrayList<Cell> visited) {
    for (Cell c : this.neighbor) {
      if (c.x == nextX && c.y == nextY) {
        if (!visited.contains(c)) {
          visited.add(c);
        }
        return c;
      }
    }
    return this;
  }

  // EFFECT: when in manual mode, this method draws the current position of the user in 
  // the maze
  public void drawCell(WorldScene scene, int cellSize) {
    WorldImage square = new RectangleImage(cellSize / 2, cellSize / 2, OutlineMode.SOLID,
        new Color(128, 0, 128));
    scene.placeImageXY(square, this.x * cellSize + cellSize / 2,
        this.y * cellSize + cellSize / 2);
  }
}

// to represent edges that connect cells
class Edge implements Comparator<Edge> {
  Cell cell1;
  Cell cell2;
  int weight;
  boolean connected;

  Edge(Cell cell1, Cell cell2, int weight, boolean connected) {
    this.cell1 = cell1;
    this.cell2 = cell2;
    this.weight = weight;
    this.connected = connected;
  }

  // just to give an edge to the comparator
  Edge(){

  }

  // to compare the weight of two edges and find the minimum
  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }

  // to find if the representatives of the cells in this edge are the same
  public boolean sameRepresentatives(HashMap<Integer, Integer> map, int cols) {
    return this.cell1.findRepresentatives(map, cols) == this.cell2.findRepresentatives(map, cols);
  }

  // EFFECT: to change the status of an edge to connected
  public void connectCells() {
    this.connected = true;
  }

  // EFFECT: to make cell1's representative equal to cell2's representative
  public void changeRepresentative(HashMap<Integer, Integer> map, int cols) {
    map.put(this.cell1.findRepresentatives(map, cols), this.cell2.findRepresentatives(map, cols));
  }

  // EFFECT: to place the right wall of a cell on the scene if the cell and its
  // right neighbor are not connected. If a cell and its bottom neighbor
  // are not connected then we place the bottom wall
  public void placeWall(WorldScene scene, int cellSize) {
    if (!this.connected) {
      if (this.cell1.checkRightRelation(this.cell2)) {
        this.cell1.placeWallHelper(scene, cellSize, true);
      } else {
        this.cell1.placeWallHelper(scene, cellSize, false);
      }
    }
  }

  // EFFECT: Makes cell1 the neighbor of cell 2 and vice versa
  public void addNeighbor() {
    this.cell1.addNeighbor(this.cell2);
    this.cell2.addNeighbor(this.cell1);
  }
}


class Maze extends World {
  int rows;
  int cols;
  int cellSize;
  int counter; // to move through the edges in the worklist
  int currPos; // to backtrack from the end of the maze to the start
  int keyCounter; // to count the score in manual mode
  // the score will be equal to the number of keys pressed
  boolean bfs; // to signal onTick when to implement bfs
  boolean dfs; // to signal onTick when to implement dfs
  boolean manual; // to toggle manual mode
  boolean render; // to toggle drawing and solving of the maze 
  boolean animatePath; // to toggle animation of the part once the solution
  // has been found by bfs or dfs
  boolean showVisited; // to toggle viewing of the visited cells
  boolean winMessage; // to indicate the user that the maze has been solved 
  Cell currentCell; // to indicate where the user is in the maze
  ArrayList<Cell> cellList = new ArrayList<Cell>(); //to represent the grid of cells  
  ArrayList<Edge> workList = new ArrayList<Edge>(); // all the edges in the grid
  // we run kruskal's on this
  ArrayList<Edge> edgesInTree = new ArrayList<Edge>(); // records all the edges in the tree
  HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(); // the hashmap for kruskal's
  HashMap<Integer, Integer> cameFromEdge = new HashMap<Integer, Integer>();
  // helps backtrack the path from the end cell to the beginning
  Queue<Cell> workQueue = new LinkedList<Cell>(); // the queue used by bfs
  Stack<Cell> workStack = new Stack<Cell>(); // the stack used by dfs
  ArrayList<Cell> visited = new ArrayList<Cell>(); // the list of cells visited by bfs or dfs or 
  // in the manual mode
  ArrayList<Cell> path = new ArrayList<Cell>(); // the list of cells containing the path between
  // the beginning and end of the maze
  Random rand;

  Maze(int rows, int cols, Random rand) {
    this.rows = rows;
    this.cols = cols;
    this.rand = rand;
    this.bfs = false;
    this.dfs = false;
    this.render = true;
    this.animatePath = false;
    this.showVisited = true;
    this.manual = false;
    this.winMessage = false;
    this.keyCounter = 0;
    this.cellSize = 30;
    this.counter = 0;

    this.initializeCellList();
    this.initializeHashMap();
    this.initializeWorkList("noBias");
    this.path.add(this.cellList.get(this.cellList.size() - 1));
    this.currPos = this.cellList.size() - 1;
    this.currentCell = this.cellList.get(0);
  }

  // to add key related functionality to the maze
  public void onKeyReleased(String key) {
    if (!this.manual && key.equals("b")) {
      // to implement bfs 
      this.workQueue.add(this.cellList.get(0));
      this.bfs = true;
    } else if (!this.manual && key.equals("d")) {
      // to implement dfs
      this.workStack.push(this.cellList.get(0));
      this.dfs = true;
    } else if (key.equals("n")) {
      // new random maze
      this.rand = new Random();
      this.bfs = false;
      this.dfs = false;
      this.render = true;
      this.animatePath = false;
      this.counter = 0;
      this.keyCounter = 0;
      this.winMessage = false;
      this.edgesInTree = new ArrayList<Edge>();
      this.cameFromEdge = new HashMap<Integer, Integer>();
      this.workQueue = new LinkedList<Cell>();
      this.workList = new ArrayList<Edge>();
      this.workStack = new Stack<Cell>();
      this.visited = new ArrayList<Cell>();
      this.path = new ArrayList<Cell>();
      this.initializeHashMap();
      this.initializeWorkList("noBias");
      this.currentCell = this.cellList.get(0);
      this.path.add(this.cellList.get(this.cellList.size() - 1));
      this.currPos = this.cellList.size() - 1;
      for (Cell c : this.cellList) {
        c.neighbor = new ArrayList<Cell>();
      } 
    } else if (key.equals("r")) {
      // refresh the maze
      // recreate the same maze without closing the program
      this.bfs = false;
      this.dfs = false;
      this.render = true;
      this.animatePath = false;
      this.winMessage = false;
      this.keyCounter = 0;
      this.currentCell = this.cellList.get(0);
      this.cameFromEdge = new HashMap<Integer, Integer>();
      this.workQueue = new LinkedList<Cell>();
      this.workStack = new Stack<Cell>();
      this.visited = new ArrayList<Cell>();
      this.path = new ArrayList<Cell>();
      this.path.add(this.cellList.get(this.cellList.size() - 1));
      this.currPos = this.cellList.size() - 1;
    } else if (key.equals("s")) {
      // to toggle the viewing of visited cells
      if (this.showVisited) {
        this.showVisited = false;
      } else {
        this.showVisited = true;
      }
    } else if (key.equals("m")) {
      // to toggle the manual mode
      if (this.manual) {
        this.manual = false;
        this.visited = new ArrayList<Cell>();
        this.currentCell = this.cellList.get(0);
      } else {
        this.manual = true;
        this.visited = new ArrayList<Cell>(Arrays.asList(this.cellList.get(0)));
      }
    } else if (this.manual && (key.equals("up")
        || key.equals("down") || key.equals("left") || key.equals("right"))) {
      // to move once manual mode is toggled
      int nextX = this.currentCell.findNextX(key);
      int nextY = this.currentCell.findNextY(key);
      this.keyCounter++;
      this.currentCell = this.currentCell.validKey(nextX, nextY, this.visited);         
      if (currentCell.equals(this.cellList.get(this.cellList.size() - 1))) {          
        this.winMessage = true;        
      }
    } else if (key.equals("h") || key.equals("v")) {  
      // to create a new random maze with vertical or horizontal bias
      this.rand = new Random();
      this.bfs = false;
      this.dfs = false;
      this.render = true;
      this.animatePath = false;
      this.counter = 0;
      this.keyCounter = 0;
      this.winMessage = false;
      this.edgesInTree = new ArrayList<Edge>();
      this.cameFromEdge = new HashMap<Integer, Integer>();
      this.workQueue = new LinkedList<Cell>();
      this.workList = new ArrayList<Edge>();
      this.workStack = new Stack<Cell>();
      this.visited = new ArrayList<Cell>();
      this.path = new ArrayList<Cell>();
      this.initializeHashMap();
      this.initializeWorkList(key);
      this.currentCell = this.cellList.get(0);
      this.path.add(this.cellList.get(this.cellList.size() - 1));
      this.currPos = this.cellList.size() - 1;
      for (Cell c : this.cellList) {
        c.neighbor = new ArrayList<Cell>();
      }
    }
  }

  // to update the world on every tick
  public void onTick() {
    if (this.render && this.edgesInTree.size() < this.rows * this.cols - 1) {
      Edge currentEdge = this.workList.get(counter);
      if (!currentEdge.sameRepresentatives(this.map, this.cols)) {
        currentEdge.connectCells();    
        this.edgesInTree.add(currentEdge);
        currentEdge.addNeighbor();
        currentEdge.changeRepresentative(this.map, this.cols); 
      }
      counter++;
    }
    if (this.render && this.edgesInTree.size() == this.rows * this.cols - 1) {
      this.render = false;
    }
    if (this.bfs  && !this.render) {
      this.bfs();
    }
    if (this.dfs  && !this.render) {
      this.dfs();
    }
    if (this.animatePath) {
      this.animatePath(this.currPos);
    }
  }

  // EFFECT: to animate the solution of the dfs or bfs search on every tick
  void animatePath(int currPos) {
    int prevPos = this.cameFromEdge.get(currPos);
    if (prevPos == 0) {
      this.path.add(this.cellList.get(0));
      this.animatePath = false;
    } else {
      this.path.add(this.cellList.get(prevPos));
      this.currPos = prevPos;
    }
  }

  // EFFECT: to perform a single step of the breadth first search
  void bfs() {
    if (this.bfs) {
      if (this.workQueue.size() != 0) {
        Cell next = this.workQueue.remove();
        if (!this.visited.contains(next)) {   
          if (next.equals(this.cellList.get(this.cellList.size() - 1))) {
            this.visited.add(next);
            this.animatePath = true;
            this.bfs = false;
          } else {
            this.visited.add(next);
            for (Cell c : next.neighbor) {
              if (!this.visited.contains(c)) {
                this.workQueue.add(c);
                this.cameFromEdge.put(this.cellList.indexOf(c), this.cellList.indexOf(next));
              }
            }
          }              
        }      
      }
    }
  }

  // EFFECT: to perform a single step of the depth first search
  void dfs() {
    if (this.dfs) {
      if (this.workStack.size() != 0) {
        Cell next = this.workStack.pop();
        if (!this.visited.contains(next)) {   
          if (next.equals(this.cellList.get(this.cellList.size() - 1))) {
            this.visited.add(next);
            this.animatePath = true;
            this.dfs = false;
          } else {
            this.visited.add(next);
            for (Cell c : next.neighbor) {
              if (!this.visited.contains(c)) {
                this.workStack.add(c);
                this.cameFromEdge.put(this.cellList.indexOf(c), this.cellList.indexOf(next));
              }
            }
          }              
        }      
      }
    }
  }

  // EFFECT: to create a list of all the edges in the maze and sort them by weights
  void initializeWorkList(String key) {
    // to add the horizontal edges  
    if (key.equals("h")) {
      for (int i = 0, j = 1; j < this.rows * this.cols; i++, j++) {
        if (j % this.cols != 0) {
          Edge newEdge = new Edge(this.cellList.get(i), this.cellList.get(j),
              this.rand.nextInt(50), false);
          this.workList.add(newEdge);
        }
      }
    } else {
      for (int i = 0, j = 1; j < this.rows * this.cols; i++, j++) {
        if (j % this.cols != 0) {
          Edge newEdge = new Edge(this.cellList.get(i), this.cellList.get(j),
              this.rand.nextInt(100), false);
          this.workList.add(newEdge);
        }
      }
    }

    // to add the vertical edges
    if (key.equals("v")) {
      for (int i = 0, j = i + this.cols; j < this.rows * this.cols; i++, j++) {
        Edge newEdge = new Edge(this.cellList.get(i), this.cellList.get(j),
            this.rand.nextInt(50), false);
        this.workList.add(newEdge);
      }
      Collections.sort(this.workList, new Edge());
    } else {
      for (int i = 0, j = i + this.cols; j < this.rows * this.cols; i++, j++) {
        Edge newEdge = new Edge(this.cellList.get(i), this.cellList.get(j),
            this.rand.nextInt(100), false);
        this.workList.add(newEdge);
      }
      Collections.sort(this.workList, new Edge());
    }
  }

  // EFFECT: to add the index of cells in the cellList as keys and set
  // their respective values to themselves
  void initializeHashMap() {
    for (int i = 0; i < this.rows * this.cols; i++) {
      this.map.put(i, i);
    }  
  }

  // EFFECT: to add cells to the cell list that should contain all the cells in the maze
  void initializeCellList() {
    for (int i = 0; i < this.rows * this.cols; i++) {
      this.cellList.add(new Cell(i % this.cols, i / this.cols));
    }  
  }

  // to count the number of steps taken by breadth or depth first search
  int stepCounter() {
    int size = this.visited.size();
    if (size == 0) {
      return 0;
    } else {
      return size - 1;
    }
  }


  // to render the maze with all its passages
  public WorldScene makeScene() {
    int width = this.cols * this.cellSize;
    int length = this.rows * this.cellSize;
    WorldScene scene = new WorldScene(width, length);
    int steps = this.stepCounter();
    int wrongMoves = steps - this.path.size() + 1;
    // to draw the maze
    for (int i = 0; i < this.workList.size(); i++) {
      Edge currentEdge = this.workList.get(i);
      currentEdge.placeWall(scene, this.cellSize);
    }

    // to highlight the first cell 
    scene.placeImageXY(new RectangleImage(cellSize / 2, cellSize / 2, OutlineMode.SOLID,
        new Color(135, 206, 250)), this.cellSize / 2, this.cellSize / 2);

    // to toggle the visited cells
    if (this.showVisited) {
      for (int i = 0; i < this.visited.size(); i++) {
        this.visited.get(i).drawVisited(scene, this.cellSize);
      }
    }

    // to draw the solution path
    for (int i = 0; i < this.path.size(); i++) {
      this.path.get(i).drawPath(scene, this.cellSize);
    }

    // to draw which cell we are at when we are in manual mode
    // to draw the number of steps during manual mode
    if (this.manual) {      
      this.currentCell.drawCell(scene, this.cellSize);
      scene.placeImageXY(new TextImage("Steps: " + this.keyCounter, 15, FontStyle.BOLD,
          Color.BLACK), width / 2, length + 15);
    }

    // to indicate that the user has won the game
    if (this.winMessage) {
      scene.placeImageXY(new TextImage("You won!", 20, FontStyle.BOLD,
          new Color(255, 211, 0)), width / 2, length + 50);
      scene.placeImageXY(new TextImage("Press 'n' to play a new random maze", 15, FontStyle.BOLD,
          Color.BLACK), width / 2, length + 70);
      scene.placeImageXY(new TextImage("Press 'r' to play the same maze again", 15, FontStyle.BOLD,
          Color.BLACK), width / 2, length + 90);
    }

    // to display the steps and wrong moves statistics when we are not in manual mode
    if (!this.manual) {
      scene.placeImageXY(new TextImage("Steps: " + steps, 15, FontStyle.BOLD,
          Color.BLACK), width / 2, length + 15);

      scene.placeImageXY(new TextImage("Wrong moves: " + wrongMoves, 15, FontStyle.BOLD,
          Color.BLACK), width / 2 + 20, length + 33);
    }

    return scene;
  }



}

class ExamplesMaze {

  Maze maze1;
  Maze maze2;
  HashMap<Integer, Integer> repMap;
  Cell cell0;
  Cell cell1;
  Cell cell2;
  Cell cell3;
  Edge edge0;
  Edge edge1;
  Edge edge2;
  WorldScene scene;
  WorldScene testScene; 


  void initData() {
    this.maze1 = new Maze(2, 3, new Random(5));
    this.maze2 = new Maze(25, 25, new Random(1));
    this.repMap = new HashMap<Integer, Integer>();
    this.repMap.put(0, 0);
    this.repMap.put(1, 1);
    this.repMap.put(2, 2);
    this.repMap.put(3, 3);
    this.cell0 = new Cell(0, 0);
    this.cell1 = new Cell(1, 0);
    this.cell2 = new Cell(0, 1);
    this.cell3 = new Cell(1, 1);
    this.edge0 = new Edge(this.cell0, this.cell1, 45, false);
    this.edge1 = new Edge(this.cell1, this.cell2, 23, false);
    this.edge2 = new Edge(this.cell0, this.cell2, 12, false);
    this.scene = new WorldScene(this.maze1.cols * this.maze1.cellSize,
        this.maze1.rows * this.maze1.cellSize);
    this.testScene = new WorldScene(90, 60);

  }

  // to test the method initializeCellList() in the class Maze
  void testInitializeCellList(Tester t) {
    initData();
    t.checkExpect(this.maze1.cellList, new ArrayList<Cell>(Arrays.asList(new Cell(0, 0),
        new Cell(1, 0), new Cell(2, 0), new Cell(0, 1), new Cell(1, 1),
        new Cell(2, 1)))); 
  }

  //to test the method initializeHashMap() in the class Maze
  void testInitializeHashMap(Tester t) {
    initData();
    HashMap<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(0, 0);
    testMap.put(1, 1);
    testMap.put(2, 2);
    testMap.put(3, 3);
    testMap.put(4, 4);
    testMap.put(5, 5);
    t.checkExpect(this.maze1.map, testMap);
  }

  //to test the method initializeWorkList() in the class Maze
  void testInitializeWorkList(Tester t) {
    initData();
    Cell cell0 = this.maze1.cellList.get(0);
    Cell cell1 = this.maze1.cellList.get(1);
    Cell cell2 = this.maze1.cellList.get(2);
    Cell cell3 = this.maze1.cellList.get(3);
    Cell cell4 = this.maze1.cellList.get(4);
    Cell cell5 = this.maze1.cellList.get(5);

    ArrayList<Edge> testList = new ArrayList<Edge>(Arrays.asList(                
        new Edge(cell1, cell4, 5, false), new Edge(cell0, cell3, 6, false),
        new Edge(cell4, cell5, 24, false), new Edge(cell2, cell5, 54, false),
        new Edge(cell3, cell4, 74, false), new Edge(cell0, cell1, 87, false),
        new Edge(cell1, cell2, 92, false)));

    t.checkExpect(this.maze1.workList, testList);
  }

  // to test the method findRepresentatives(HashMap<Integer, Integer>, int) in the class Cell
  void testFindRepresentatives(Tester t) {
    initData();
    t.checkExpect(this.cell0.findRepresentatives(repMap, 2), 0);
    t.checkExpect(this.cell1.findRepresentatives(repMap, 2), 1);

    // modifying the hash map to test cases where the representative
    // of some cells is not itself
    this.repMap.put(0, 1);
    this.repMap.put(1, 2);
    this.repMap.put(3, 2);

    t.checkExpect(this.cell0.findRepresentatives(repMap, 2), 2);
    t.checkExpect(this.cell1.findRepresentatives(repMap, 2), 2);
    t.checkExpect(this.cell2.findRepresentatives(repMap, 2), 2);
    t.checkExpect(this.cell3.findRepresentatives(repMap, 2), 2);
  }

  // to test the method sameRepresentatives(HashMap<Integer, Integer>, int) in the class Cell
  void testSameRepresentatives(Tester t) {
    initData();
    t.checkExpect(this.edge0.sameRepresentatives(repMap, 2), false);
    // to modify cell0's representative to cell1's representative
    this.repMap.put(0, 1);
    t.checkExpect(this.edge0.sameRepresentatives(repMap, 2), true);
  }

  // to test the method connectCells() in the class Edge
  void testConnectCells(Tester t) {
    initData();
    t.checkExpect(this.edge0.connected, false);
    this.edge0.connectCells();
    t.checkExpect(this.edge0.connected, true);
  }

  // to test the method findIndex() in the class Cell
  void testFindIndex(Tester t) {
    initData();
    t.checkExpect(this.cell0.findIndex(2), 0);
    t.checkExpect(this.cell2.findIndex(2), 2);
  }

  // to test the method changeRepresentative(HashMap<Integer, Integer>, int) in the class Edge
  void testChangeRepresentative(Tester t) {
    initData();
    t.checkExpect(this.repMap.get(0), 0);
    this.edge0.changeRepresentative(repMap, 2);
    t.checkExpect(this.repMap.get(0), 1);

    this.repMap.put(3, 2);
    t.checkExpect(this.cell0.findRepresentatives(this.repMap, 2), 1);
    t.checkExpect(this.cell1.findRepresentatives(this.repMap, 2), 1);
    t.checkExpect(this.cell2.findRepresentatives(this.repMap, 2), 2);
    t.checkExpect(this.cell3.findRepresentatives(this.repMap, 2), 2);
    this.edge1.changeRepresentative(repMap, 2);
    t.checkExpect(this.cell1.findRepresentatives(this.repMap, 2), 2);
    t.checkExpect(this.cell0.findRepresentatives(this.repMap, 2), 2);
    t.checkExpect(this.cell2.findRepresentatives(this.repMap, 2), 2);
    t.checkExpect(this.cell3.findRepresentatives(this.repMap, 2), 2);
  }

  // to test the method checkRightRelation(Cell) in the class Cell
  void testCheckRightRelation(Tester t) {
    initData();
    t.checkExpect(this.cell0.checkRightRelation(cell2), false);
    t.checkExpect(this.cell0.checkRightRelation(cell1), true);
  }

  // to test the method placeWallHelper(WorldScene, int, boolean) in the class Cell
  void testPlaceWallHelper(Tester t) {
    initData();
    WorldImage verticalWall = new RectangleImage(1, 30, OutlineMode.OUTLINE, Color.BLACK);
    WorldImage horizontalWall = new RectangleImage(30, 1, OutlineMode.OUTLINE, Color.BLACK);
    this.cell0.placeWallHelper(this.scene, this.maze1.cellSize, true);
    testScene.placeImageXY(verticalWall, 30, 15);
    t.checkExpect(this.scene, this.testScene);

    initData();
    this.cell1.placeWallHelper(this.scene, this.maze1.cellSize, false);
    testScene.placeImageXY(horizontalWall, 45, 30);
    t.checkExpect(this.scene, this.testScene);
  }

  // to test the method placeWall(WorldScene, int) in the class Edge
  void testPlaceWall(Tester t) {
    initData();
    WorldImage verticalWall = new RectangleImage(1, 30, OutlineMode.OUTLINE, Color.BLACK);
    WorldImage horizontalWall = new RectangleImage(30, 1, OutlineMode.OUTLINE, Color.BLACK);
    this.edge0.placeWall(this.scene, this.maze1.cellSize);
    testScene.placeImageXY(verticalWall, 30, 15);
    t.checkExpect(this.scene, this.testScene);

    initData();
    this.edge2.placeWall(scene, 30);
    testScene.placeImageXY(horizontalWall, 15, 30);
    t.checkExpect(this.scene, this.testScene);

    initData();
    this.edge0.connected = true;
    this.edge0.placeWall(scene, 30);
    t.checkExpect(this.scene, this.testScene);
  }

  // to test the method addNeighbor(Cell) in the class Cell
  void testAddNeighbor(Tester t) {
    initData();
    t.checkExpect(this.cell0.neighbor, new ArrayList<Cell>());
    this.cell0.addNeighbor(this.cell1);
    t.checkExpect(this.cell0.neighbor, new ArrayList<Cell>(Arrays.asList(this.cell1)));
    this.cell0.addNeighbor(cell2);
    t.checkExpect(this.cell0.neighbor, new ArrayList<Cell>(Arrays.asList(this.cell1, this.cell2)));
  }

  // to test the method drawVisited(scene, int) in the class Cell
  void testDrawVisited(Tester t) {
    initData();
    WorldImage square = new RectangleImage(this.maze1.cellSize / 2, this.maze1.cellSize / 2,
        OutlineMode.SOLID,
        new Color(135, 206, 250));    
    scene.placeImageXY(square, this.cell0.x * this.maze1.cellSize + this.maze1.cellSize / 2,
        this.cell0.y * this.maze1.cellSize + this.maze1.cellSize / 2);
    this.cell0.drawVisited(this.testScene, this.maze1.cellSize);
    t.checkExpect(this.scene,this.testScene);

    initData();
    WorldImage square2 = new RectangleImage(this.maze1.cellSize / 2, this.maze1.cellSize / 2,
        OutlineMode.SOLID,
        new Color(135, 206, 250));    
    scene.placeImageXY(square2, this.cell1.x * this.maze1.cellSize + this.maze1.cellSize / 2,
        this.cell1.y * this.maze1.cellSize + this.maze1.cellSize / 2);
    this.cell1.drawVisited(this.testScene, this.maze1.cellSize);
    t.checkExpect(this.scene,this.testScene);
  }

  // to test the method drawPath(scene, int) in hte class Cell
  void testDrawPath(Tester t) {
    initData();
    WorldImage square = new RectangleImage(this.maze1.cellSize / 2, this.maze1.cellSize / 2,
        OutlineMode.SOLID,
        new Color(0, 0, 128));   
    scene.placeImageXY(square, this.cell0.x * this.maze1.cellSize + this.maze1.cellSize / 2,
        this.cell0.y * this.maze1.cellSize + this.maze1.cellSize / 2);
    this.cell0.drawPath(this.testScene, this.maze1.cellSize);
    t.checkExpect(this.scene,this.testScene);

    initData();
    WorldImage square2 = new RectangleImage(this.maze1.cellSize / 2, this.maze1.cellSize / 2,
        
        OutlineMode.SOLID,
        new Color(0, 0, 128));   
    scene.placeImageXY(square2, this.cell1.x * this.maze1.cellSize + this.maze1.cellSize / 2,
        this.cell1.y * this.maze1.cellSize + this.maze1.cellSize / 2);
    this.cell1.drawPath(this.testScene, this.maze1.cellSize);
    t.checkExpect(this.scene,this.testScene);
  }

  // to test the method findNextX(String) in the class cell
  void testFindNextX(Tester t) {
    initData();
    t.checkExpect(this.cell0.findNextX("right"), 1);
    t.checkExpect(this.cell1.findNextX("left"), 0);
    t.checkExpect(this.cell1.findNextX("right"), 2);
    t.checkExpect(this.cell1.findNextX("up"), 1);
  }

  //to test the method findNextX(String) in the class cell
  void testFindNextY(Tester t) {
    initData();
    t.checkExpect(this.cell0.findNextY("right"), 0);
    t.checkExpect(this.cell0.findNextY("down"), 1);
    t.checkExpect(this.cell0.findNextY("up"), -1);
  }
  
  // to test the method validKey(int, int, ArrayList<Cell>) in the class Cell
  void testValidKey(Tester t) {
    initData();
    this.cell0.addNeighbor(cell1);
    ArrayList<Cell> visited = new ArrayList<Cell>(Arrays.asList(this.cell0));
    t.checkExpect(this.cell0.validKey(1, 0, visited),
        this.cell1);
    t.checkExpect(visited, new ArrayList<Cell>(Arrays.asList(this.cell0, this.cell1)));
    t.checkExpect(this.cell0.validKey(-1, 0, new ArrayList<Cell>(Arrays.asList(this.cell0))),
        this.cell0);
  }
  
  // to test the method drawCell(scene, int) in the class Cell
  void testDrawCell(Tester t) {
    initData();
    WorldImage square = new RectangleImage(this.maze1.cellSize / 2, this.maze1.cellSize / 2,
        OutlineMode.SOLID, new Color(128, 0, 128));
    scene.placeImageXY(square, this.cell0.x * this.maze1.cellSize + this.maze1.cellSize / 2,
        this.cell0.y * this.maze1.cellSize + this.maze1.cellSize / 2);
    this.cell0.drawCell(testScene, this.maze1.cellSize);
    t.checkExpect(this.testScene, scene);
  }
  
  // to test the method compare(edge, edge) in the class Edge
  void testCompare(Tester t) {
    initData();
    t.checkExpect(this.edge0.compare(edge1, edge0), -22);
    t.checkExpect(this.edge0.compare(edge1, edge2), 11);
  }
  
  // to test the method stepCounter() in the class Edge
  void testStepCounter(Tester t) {
    initData();
    t.checkExpect(this.maze1.stepCounter() , 0);
    this.maze1.visited.add(cell0);
    this.maze1.visited.add(cell1);
    t.checkExpect(this.maze1.stepCounter() , 1);
  }

  // to call big bang in the class Maze
  void testBigBang(Tester t) {
    initData();
    this.maze2.bigBang(1500, 1000, 0.00001);
  }

  // to test the method addOutEdgesToCell() in the class Maze
  void testAddOUtEdgesToCell(Tester t) {
    initData();
    this.edge0.addNeighbor();
    t.checkExpect(this.cell0.neighbor.get(0).x, 1);
    t.checkExpect(this.cell0.neighbor.get(0).y, 0);
  }
}





