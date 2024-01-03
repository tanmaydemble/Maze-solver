import java.awt.Color;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

class Rough extends World {

  @Override
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(500, 500);
    WorldImage wall1 = new RectangleImage(1, 30, OutlineMode.OUTLINE, Color.BLACK);
    scene.placeImageXY(wall1, 30, 15);
    WorldImage wall2 = new RectangleImage(30, 1, OutlineMode.OUTLINE, Color.BLACK);
    scene.placeImageXY(wall2, 15, 30);
    return scene;
  }
  
}

class Examples {
  
 Rough one = new Rough();
  
  void testScene(Tester t) {
    this.one.bigBang(1500, 900);
  }
  
}