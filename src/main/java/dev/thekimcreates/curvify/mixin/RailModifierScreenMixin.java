package dev.thekimcreates.curvify.mixin;

import dev.thekimcreates.curvify.client.PsdPropertiesScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.screen.MTRScreenBase;
import org.mtr.mod.screen.RailModifierScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RailModifierScreen.class)
public abstract class RailModifierScreenMixin extends MTRScreenBase {
    @Unique
    private String curvify$railId;

    @Unique
    private ButtonWidgetExtension curvify$propertiesButton;

    public RailModifierScreenMixin() {
        super();
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void curvify$afterConstruct(String railId, CallbackInfo callbackInfo) {
        curvify$railId = railId;
        curvify$propertiesButton = new ButtonWidgetExtension(
                0,
                0,
                100,
                20,
                TextHelper.translatable("curvify.psd_properties"),
                button -> MinecraftClient.getInstance().setScreen(
                        new PsdPropertiesScreen((Screen) (Object) this, curvify$railId)
                )
        );
    }

    @Inject(method = "init2", at = @At("TAIL"), remap = false)
    private void curvify$afterInit(CallbackInfo callbackInfo) {
        addChild(new ClickableWidget(curvify$propertiesButton));
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void curvify$beforeRender(
            GraphicsHolder graphicsHolder,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo callbackInfo
    ) {
        IDrawing.setPositionAndWidth(curvify$propertiesButton, width - 100, height - 20, 100);
    }
}
