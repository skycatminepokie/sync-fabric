package dev.kir.sync.compat.iris;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class IrisCustomRenderLayer {
    public static RenderLayer initVoxelRenderLayer() {
        return IrisRenderLayer.getVoxels();
    }

    public static RenderLayer getEntityTranslucentPartiallyTextured(Identifier textureId, float cutoutY, boolean affectsOutline) {
        return IrisRenderLayer.getEntityTranslucentPartiallyTextured(textureId, cutoutY, affectsOutline);
    }
}
