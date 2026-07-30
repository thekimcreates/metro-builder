package dev.metrobuilder.client.screen;

import dev.metrobuilder.display.PrecisionPSDManager;
import dev.metrobuilder.network.MetroBuilderNetworking;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public final class PrecisionPropertiesScreen extends Screen {
    private final double initialX, initialY, initialZ;
    private final float initialYaw;
    private String blockId;
    private TextFieldWidget xField, yField, zField, yawField;

    public PrecisionPropertiesScreen(double x, double y, double z, float yaw, String blockId) {
        super(Text.literal("Precision Properties"));
        this.initialX=x; this.initialY=y; this.initialZ=z; this.initialYaw=yaw; this.blockId=blockId;
    }

    @Override protected void init() {
        int cx=width/2, top=height/2-105;
        xField=field(cx-90,top+35,"X",initialX);
        yField=field(cx-90,top+62,"Y",initialY);
        zField=field(cx-90,top+89,"Z",initialZ);
        yawField=field(cx-90,top+116,"Yaw",initialYaw);
        addDrawableChild(ButtonWidget.builder(typeText(), b -> { blockId = blockId.equals(PrecisionPSDManager.DOOR_ID) ? PrecisionPSDManager.GLASS_ID : PrecisionPSDManager.DOOR_ID; b.setMessage(typeText()); }).dimensions(cx-90,top+8,180,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> apply()).dimensions(cx-90,top+151,86,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close()).dimensions(cx+4,top+151,86,20).build());
    }

    private TextFieldWidget field(int x,int y,String label,double value){
        TextFieldWidget f=new TextFieldWidget(textRenderer,x,y,180,20,Text.literal(label));
        f.setText(String.format(java.util.Locale.ROOT,"%.3f",value)); addDrawableChild(f); return f;
    }
    private Text typeText(){ return Text.literal(blockId.equals(PrecisionPSDManager.DOOR_ID)?"Type: Tianjin BMT PSD Door":"Type: MTR PSD Glass + Top"); }
    private void apply(){
        try {
            PacketByteBuf buf=new PacketByteBuf(Unpooled.buffer());
            buf.writeDouble(Double.parseDouble(xField.getText())); buf.writeDouble(Double.parseDouble(yField.getText())); buf.writeDouble(Double.parseDouble(zField.getText()));
            buf.writeFloat(Float.parseFloat(yawField.getText())); buf.writeString(blockId);
            ClientPlayNetworking.send(MetroBuilderNetworking.APPLY_PSD_PROPERTIES,buf); close();
        } catch(NumberFormatException ignored) {}
    }
@Override
public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context, mouseX, mouseY, delta);

    int cx = width / 2;
    int top = height / 2 - 105;

    super.render(context, mouseX, mouseY, delta);
}
    @Override public boolean shouldPause(){ return false; }
}
