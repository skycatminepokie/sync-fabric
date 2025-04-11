package dev.kir.sync.compat.iris;

import dev.kir.sync.api.shell.ShellState;
import dev.kir.sync.client.texture.GeneratedTextureManager;
import dev.kir.sync.client.texture.TextureGenerators;
import dev.kir.sync.entity.ShellEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.util.Identifier;

public class IrisShellEntityRenderer {
    public static VertexConsumer getVertexConsumerForPartiallyTexturedEntity(ShellEntity shellEntity, float progress, RenderLayer baseLayer, VertexConsumerProvider vertexConsumers, VertexConsumer baseConsumer) {

        if (!(progress >= ShellState.PROGRESS_PRINTING && progress < ShellState.PROGRESS_DONE)) {
            return baseConsumer;
        }

        Identifier[] textures = GeneratedTextureManager.getTextures(TextureGenerators.PlayerEntityPartiallyTexturedTextureGenerator);
        if (textures.length == 0) {
            return baseConsumer;
        }

        float printingProgress = (progress - ShellState.PROGRESS_PRINTING) / (ShellState.PROGRESS_PAINTING);
        RenderLayer printingMaskLayer = IrisRenderLayer.getPrintingMask(textures[(int)(textures.length * printingProgress)]);

        // TODO
        // Fix dev.kir.sync.compat.iris.IrisRenderLayer::getPrintingMask,
        // and then fix combining baseConsumer with printingMaskVertexConsumer
        VertexConsumer printingMaskVertexConsumer = vertexConsumers.getBuffer(printingMaskLayer);
        if (printingMaskVertexConsumer == baseConsumer) {
            return(vertexConsumers.getBuffer(baseLayer));
        }

        return(VertexConsumers.union(printingMaskVertexConsumer, baseConsumer));
    }
}
