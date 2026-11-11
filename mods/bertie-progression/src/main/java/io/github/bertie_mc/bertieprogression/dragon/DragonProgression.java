package io.github.bertie_mc.bertieprogression.dragon;

import net.minecraft.resources.ResourceLocation;

public final class DragonProgression {
    public static final ResourceLocation IMBRIFER = ResourceLocation.parse("pastel:deeper_down");

    public static int generatedAge(int age, ResourceLocation dimension) {
        return IMBRIFER.equals(dimension) ? age : Math.min(age, 99);
    }

    private DragonProgression() {}
}
