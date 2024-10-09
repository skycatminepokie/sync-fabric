package dev.kir.sync.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(value = WorldRenderer.class, priority = 1010)
abstract class WorldRendererMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    /**
     * This method forces renderer to render the player when they aren't a camera entity.
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getFocusedEntity()Lnet/minecraft/entity/Entity;", ordinal = 3), require = 1)
    private Entity getFocusedEntity(Camera camera) {
        ClientPlayerEntity player = this.client.player;
        if (player != null && player != this.client.getCameraEntity() && !player.isSpectator()) {
            return player;
        }
        return camera.getFocusedEntity();
    }
}