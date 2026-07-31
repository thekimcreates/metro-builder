package dev.metrobuilder.display;

import dev.metrobuilder.entity.MetroBuilderEntities;
import dev.metrobuilder.entity.PrecisionPSDEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrecisionPSDManager {
    public static final String DOOR_ID = "door";
    public static final String GLASS_ID = "glass";
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private PrecisionPSDManager() {}

    private static Session get(ServerPlayerEntity player){return SESSIONS.computeIfAbsent(player.getUuid(),u->new Session());}

    public static boolean selectLookedAt(ServerPlayerEntity player){
        Session s=get(player); if(s.entity!=null&&!s.entity.isRemoved()) return false;
        Entity hit=findLookedAt(player,6);
        if(!(hit instanceof PrecisionPSDEntity psd)||psd.isPreview()) return false;
        s.entity=psd; s.type=psd.getPsdType(); psd.setPreview(true);
        player.sendMessage(Text.literal("Precision PSD selected | edit, then right-click to save"),true); return true;
    }

    public static void startOrConfirm(ServerPlayerEntity player,Vec3d position){
        Session s=get(player);
        if(s.entity!=null&&!s.entity.isRemoved()) { s.entity.setPreview(false); s.entity=null; player.sendMessage(Text.literal("Precision PSD saved"),true); return; }
        PrecisionPSDEntity e=new PrecisionPSDEntity(MetroBuilderEntities.PRECISION_PSD,player.getServerWorld());
        e.setPosition(position); e.setYaw(s.yaw); e.setPsdType(s.type); e.setPreview(true);
        if(player.getServerWorld().spawnEntity(e)){s.entity=e;player.sendMessage(Text.literal("Precision PSD preview | right-click to place"),true);}
    }

    public static void rotate(ServerPlayerEntity p,float d){Session s=get(p);if(valid(s)){s.yaw=norm(s.entity.getYaw()+d);s.entity.setYaw(s.yaw);}}
    public static void nudge(ServerPlayerEntity p,double lx,double y,double lz){Session s=get(p);if(!valid(s))return;double r=Math.toRadians(s.entity.getYaw());double dx=lx*Math.cos(r)-lz*Math.sin(r),dz=lx*Math.sin(r)+lz*Math.cos(r);s.entity.setPosition(s.entity.getX()+dx,s.entity.getY()+y,s.entity.getZ()+dz);}
    public static void toggleType(ServerPlayerEntity p){Session s=get(p);s.type=PrecisionPSDEntity.TYPE_GLASS.equals(s.type)?PrecisionPSDEntity.TYPE_DOOR:PrecisionPSDEntity.TYPE_GLASS;if(valid(s))s.entity.setPsdType(s.type);p.sendMessage(Text.literal("PSD type: "+s.type),true);}
    public static void cancel(ServerPlayerEntity p){Session s=get(p);if(valid(s)){if(s.entity.isPreview())s.entity.discard();}s.entity=null;p.sendMessage(Text.literal("PSD edit cancelled"),true);}
    public static PropertiesSnapshot getProperties(ServerPlayerEntity p){Session s=get(p);return valid(s)?new PropertiesSnapshot(s.entity.getX(),s.entity.getY(),s.entity.getZ(),s.entity.getYaw(),s.entity.getPsdType()):null;}
    public static void applyProperties(ServerPlayerEntity p,double x,double y,double z,float yaw,String type){Session s=get(p);if(!valid(s))return;s.entity.setPosition(x,y,z);s.entity.setYaw(norm(yaw));s.entity.setPsdType(type);s.type=s.entity.getPsdType();s.yaw=s.entity.getYaw();}
    public static void remove(ServerPlayerEntity p){cancel(p);SESSIONS.remove(p.getUuid());}
    public record PropertiesSnapshot(double x,double y,double z,float yaw,String blockId){}

    private static boolean valid(Session s){return s.entity!=null&&!s.entity.isRemoved();}
    private static float norm(float y){y%=360;return y<0?y+360:y;}
    private static Entity findLookedAt(ServerPlayerEntity p,double distance){Vec3d start=p.getCameraPosVec(1),end=start.add(p.getRotationVec(1).multiply(distance));Box search=p.getBoundingBox().stretch(p.getRotationVec(1).multiply(distance)).expand(2);Entity closest=null;double best=distance*distance;for(Entity e:p.getServerWorld().getOtherEntities(p,search,x->x instanceof PrecisionPSDEntity)){Optional<Vec3d> hit=e.getBoundingBox().raycast(start,end);if(hit.isPresent()){double d=start.squaredDistanceTo(hit.get());if(d<best){best=d;closest=e;}}}return closest;}
    private static final class Session{private String type=PrecisionPSDEntity.TYPE_DOOR;private float yaw;private PrecisionPSDEntity entity;}
}
