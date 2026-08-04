package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.client.network.ClientPrecisionState;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import java.util.List;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_259;
import net.minecraft.class_265;
import net.minecraft.class_310;

public final class PSDCollisionShapes {
    private PSDCollisionShapes() {}

    public static void append(class_238 entityBox, class_243 movement, List<class_265> collisions) {
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null || ClientPrecisionState.dimensionId()
                .filter(client.field_1687.method_27983().method_29177()::equals).isEmpty()) return;
        class_238 swept = entityBox.method_18804(movement).method_1014(0.01);
        for (ClientPSDObject psd : ClientPrecisionState.psds()) {
            PrecisionTransform t = psd.transform();
            if (t.squaredDistanceTo(swept.method_1005()) > 4096.0) continue;
            if (SingleGlassPanelRenderer.PACK_ID.equals(psd.packId().toString())) {
                appendSegmented(collisions, swept, t, -0.75, 0.75, 0, 3, -0.125, 0.125);
                continue;
            }
            double open = Math.max(0, Math.min(1, MtrTrainDoorLink.findDoorValue(client, psd).orElse(psd.doorValue())));
            appendSegmented(collisions, swept, t, -2.5, -1, 0, 2.1, 0, .125);
            appendSegmented(collisions, swept, t, 1, 2.5, 0, 2.1, 0, .125);
            appendSegmented(collisions, swept, t, -2.5, 2.5, 2.1, 3, -.18, .18);
            appendSegmented(collisions, swept, t, -1-open, -open, 0, 2.1, -.125, 0);
            appendSegmented(collisions, swept, t, open, 1+open, 0, 2.1, -.125, 0);
        }
    }

    private static void appendSegmented(List<class_265> out, class_238 swept, PrecisionTransform t,
            double x0, double x1, double y0, double y1, double z0, double z1) {
        for (double x=x0; x<x1; x+=.25) {
            class_238 box=transformBox(t,x,Math.min(x1,x+.25),y0,y1,z0,z1);
            if (box.method_994(swept)) out.add(class_259.method_1078(box));
        }
    }

    private static class_238 transformBox(PrecisionTransform t,double x0,double x1,double y0,double y1,double z0,double z1){
        double ax=Double.POSITIVE_INFINITY,ay=ax,az=ax,bx=Double.NEGATIVE_INFINITY,by=bx,bz=bx;
        for(double x:new double[]{x0,x1})for(double y:new double[]{y0,y1})for(double z:new double[]{z0,z1}){
            class_243 p=point(t,x,y,z); ax=Math.min(ax,p.field_1352);ay=Math.min(ay,p.field_1351);az=Math.min(az,p.field_1350);
            bx=Math.max(bx,p.field_1352);by=Math.max(by,p.field_1351);bz=Math.max(bz,p.field_1350);
        } return new class_238(ax,ay,az,bx,by,bz);
    }

    private static class_243 point(PrecisionTransform t,double x,double y,double z){
        x*=t.scaleX();y*=t.scaleY();z*=t.scaleZ(); double a=Math.toRadians(t.roll()),n=x*Math.cos(a)-y*Math.sin(a);
        y=x*Math.sin(a)+y*Math.cos(a);x=n;a=Math.toRadians(t.pitch());n=y*Math.cos(a)-z*Math.sin(a);
        z=y*Math.sin(a)+z*Math.cos(a);y=n;a=Math.toRadians(-t.yaw());n=x*Math.cos(a)+z*Math.sin(a);
        z=-x*Math.sin(a)+z*Math.cos(a);x=n;return new class_243(t.x()+x,t.y()+y,t.z()+z);
    }
}
