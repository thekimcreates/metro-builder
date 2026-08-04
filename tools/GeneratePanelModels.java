import java.nio.file.*;
import java.util.*;

public final class GeneratePanelModels {
  record Box(double x0,double y0,double z0,double x1,double y1,double z1) {}
  public static void main(String[] args) throws Exception {
    Path dir=Path.of("panel-resources/assets/metrobuilder/models/psd/seoul_bulky_glass_panel");
    Files.createDirectories(dir);
    write(dir.resolve("panel_white.obj"), "panel_white", List.of(
      new Box(-.75,0,-.02,.75,.13,.13), new Box(-.75,1.97,-.02,.75,3,.13),
      new Box(-.75,.13,-.02,-.68,1.97,.13), new Box(.68,.13,-.02,.75,1.97,.13)));
    write(dir.resolve("panel_dark.obj"), "panel_dark", List.of(
      new Box(-.68,.13,0,.68,.19,.12), new Box(-.68,1.91,0,.68,1.97,.12),
      new Box(-.68,.19,0,-.62,1.91,.12), new Box(.62,.19,0,.68,1.91,.12)));
    write(dir.resolve("panel_glass.obj"), "panel_glass", List.of(new Box(-.62,.19,.02,.62,1.91,.105)));
  }
  static void write(Path path,String name,List<Box> boxes)throws Exception{
    StringBuilder s=new StringBuilder("# MetroBuilder Seoul Bulky single glass panel\no ").append(name).append('\n');
    int base=1;
    for(Box b:boxes){double[][]v={{b.x0,b.y0,b.z1},{b.x1,b.y0,b.z1},{b.x1,b.y1,b.z1},{b.x0,b.y1,b.z1},{b.x1,b.y0,b.z0},{b.x0,b.y0,b.z0},{b.x0,b.y1,b.z0},{b.x1,b.y1,b.z0}};
      for(double[]p:v)s.append(String.format(Locale.ROOT,"v %.6f %.6f %.6f%n",p[0],p[1],p[2]));
      s.append("vt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\n");
      int t=(base-1)/2+1; s.append(String.format("f %d/%d %d/%d %d/%d %d/%d%n",base,t,base+1,t+1,base+2,t+2,base+3,t+3));
      s.append(String.format("f %d/%d %d/%d %d/%d %d/%d%n",base+4,t,base+5,t+1,base+6,t+2,base+7,t+3)); base+=8;
    } Files.writeString(path,s);
  }
}
