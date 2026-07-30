package dev.metrobuilder.client;

import dev.metrobuilder.client.screen.PlatformBuilderScreen;
import dev.metrobuilder.client.screen.PrecisionPropertiesScreen;
import dev.metrobuilder.item.MetroBuilderItems;
import dev.metrobuilder.network.MetroBuilderNetworking;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class MetroBuilderClient implements ClientModInitializer {
    private static KeyBinding rotateRight, rotateLeft, moveLeft, moveRight, moveForward, moveBack, moveUp, moveDown, cycleType, cancel, properties;
    @Override public void onInitializeClient() {
        rotateRight=reg("rotate_right",GLFW.GLFW_KEY_RIGHT_BRACKET); rotateLeft=reg("rotate_left",GLFW.GLFW_KEY_LEFT_BRACKET);
        moveLeft=reg("move_left",GLFW.GLFW_KEY_LEFT); moveRight=reg("move_right",GLFW.GLFW_KEY_RIGHT);
        moveForward=reg("move_forward",GLFW.GLFW_KEY_UP); moveBack=reg("move_back",GLFW.GLFW_KEY_DOWN);
        moveUp=reg("move_up",GLFW.GLFW_KEY_PAGE_UP); moveDown=reg("move_down",GLFW.GLFW_KEY_PAGE_DOWN);
        cycleType=reg("cycle_psd_type",GLFW.GLFW_KEY_G); cancel=reg("cancel_psd",GLFW.GLFW_KEY_X); properties=reg("psd_properties",GLFW.GLFW_KEY_P);
        ClientPlayNetworking.registerGlobalReceiver(MetroBuilderNetworking.OPEN_PLATFORM_BUILDER,(client,handler,buf,sender)->{int n=buf.readVarInt();List<String> rows=new ArrayList<>();for(int i=0;i<n;i++)rows.add(buf.readString(128));client.execute(()->client.setScreen(new PlatformBuilderScreen(rows)));});
        ClientPlayNetworking.registerGlobalReceiver(MetroBuilderNetworking.SHOW_PSD_PROPERTIES,(client,handler,buf,sender)->{double x=buf.readDouble(),y=buf.readDouble(),z=buf.readDouble();float yaw=buf.readFloat();String id=buf.readString(128);client.execute(()->client.setScreen(new PrecisionPropertiesScreen(x,y,z,yaw,id)));});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.player==null)return;
            while(rotateRight.wasPressed()) action(client,"rotate",shift(client)?0.25:1.0); while(rotateLeft.wasPressed()) action(client,"rotate",shift(client)?-0.25:-1.0);
            double step=shift(client)?0.001:0.01;
            while(moveLeft.wasPressed())action(client,"x",-step); while(moveRight.wasPressed())action(client,"x",step); while(moveForward.wasPressed())action(client,"z",step); while(moveBack.wasPressed())action(client,"z",-step);
            while(moveUp.wasPressed())action(client,"y",step); while(moveDown.wasPressed())action(client,"y",-step); while(cycleType.wasPressed())action(client,"type",0); while(cancel.wasPressed())action(client,"cancel",0);
            while(properties.wasPressed()) if(holding(client)) ClientPlayNetworking.send(MetroBuilderNetworking.OPEN_PSD_PROPERTIES,new PacketByteBuf(Unpooled.buffer()));
        });
        HudRenderCallback.EVENT.register((c,t)->{MinecraftClient mc=MinecraftClient.getInstance();if(mc.player==null||mc.options.hudHidden)return;if(mc.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER))c.drawTextWithShadow(mc.textRenderer,"Precision PSD | Right-click place/select | [ ] rotate | Arrows move | G type | P properties | X cancel",8,8,0xFFFFFF);});
    }
    private static KeyBinding reg(String n,int k){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.metrobuilder."+n, InputUtil.Type.KEYSYM,k,"key.category.metrobuilder"));}
    private static boolean holding(MinecraftClient c){return c.player!=null&&c.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER);}
    private static boolean shift(MinecraftClient c){return InputUtil.isKeyPressed(c.getWindow().getHandle(),GLFW.GLFW_KEY_LEFT_SHIFT)||InputUtil.isKeyPressed(c.getWindow().getHandle(),GLFW.GLFW_KEY_RIGHT_SHIFT);}
    private static void action(MinecraftClient c,String a,double v){if(!holding(c))return;PacketByteBuf b=new PacketByteBuf(Unpooled.buffer());b.writeString(a);b.writeDouble(v);ClientPlayNetworking.send(MetroBuilderNetworking.PRECISION_PSD_ACTION,b);}
}
