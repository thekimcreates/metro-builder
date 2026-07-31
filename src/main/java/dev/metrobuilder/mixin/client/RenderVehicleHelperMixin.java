package dev.metrobuilder.mixin.client;

import dev.metrobuilder.entity.PrecisionPSDEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets="org.mtr.mod.render.RenderVehicleHelper",remap=false)
public abstract class RenderVehicleHelperMixin {
    @Inject(method="canOpenDoors",at=@At("HEAD"),require=0)
    private static void metrobuilder$capture(@Coerce Object doorway,@Coerce Object transform,double doorValue, CallbackInfoReturnable<Boolean> cir){
        try {
            MinecraftClient client=MinecraftClient.getInstance(); if(client.world==null)return;
            double minX=call(doorway,"getMinXMapped"),maxX=call(doorway,"getMaxXMapped");
            double minY=call(doorway,"getMinYMapped"),maxY=call(doorway,"getMaxYMapped");
            double minZ=call(doorway,"getMinZMapped"),maxZ=call(doorway,"getMaxZMapped");
            Field pf=transform.getClass().getField("position"), yf=transform.getClass().getField("yaw");
            Object pos=pf.get(transform); double px=field(pos,"x"),py=field(pos,"y"),pz=field(pos,"z"),yaw=yf.getDouble(transform);
            double lx=(minX+maxX)/2,lz=(minZ+maxZ)/2,ly=(minY+maxY)/2;
            double wx=px+lx*Math.cos(yaw)+lz*Math.sin(yaw), wz=pz-lx*Math.sin(yaw)+lz*Math.cos(yaw), wy=py+ly;
            for(Entity e:client.world.getOtherEntities(null,new net.minecraft.util.math.Box(wx-3,wy-3,wz-3,wx+3,wy+3,wz+3),x->x instanceof PrecisionPSDEntity)){
                PrecisionPSDEntity psd=(PrecisionPSDEntity)e; if(PrecisionPSDEntity.TYPE_DOOR.equals(psd.getPsdType()))psd.setDoorValue((float)doorValue);
            }
        }catch(Throwable ignored){}
    }
    private static double call(Object o,String n)throws Exception{Method m=o.getClass().getMethod(n);return ((Number)m.invoke(o)).doubleValue();}
    private static double field(Object o,String n)throws Exception{return ((Number)o.getClass().getField(n).get(o)).doubleValue();}
}
