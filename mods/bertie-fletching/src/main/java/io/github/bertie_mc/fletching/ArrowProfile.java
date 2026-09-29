package io.github.bertie_mc.fletching;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record ArrowProfile(
        PartCatalog.Part feather, PartCatalog.Part shaft, PartCatalog.Part tip, PartCatalog.Part extra) {
    public static boolean redesigned(CompoundTag tag) {
        return tag.getInt("bertieVersion") >= 2;
    }

    public static ArrowProfile read(CompoundTag tag) {
        return new ArrowProfile(
                part(tag, 0, "feather", "feather"),
                part(tag, 1, "shaft", "stick"),
                part(tag, 2, "tip", "flint"),
                PartCatalog.find(3, tag.getString("effect")));
    }

    private static PartCatalog.Part part(CompoundTag tag, int slot, String field, String fallback) {
        var part = PartCatalog.find(slot, tag.getString(field));
        return part == null ? PartCatalog.key(slot, fallback) : part;
    }

    public boolean feather(String key) {
        return feather.key().equals(key);
    }

    public boolean shaft(String key) {
        return shaft.key().equals(key);
    }

    public boolean tip(String key) {
        return tip.key().equals(key);
    }

    public boolean extra(String key) {
        return extra != null && extra.key().equals(key);
    }

    public static boolean night(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000);
        return time >= 13000 && time < 23000;
    }

    public double speed(Level level) {
        return switch (feather.key()) {
            case "wheat" -> .8;
            case "emu" -> 1.2;
            case "raven" -> night(level) ? 1.6 : 1;
            case "roadrunner" -> night(level) ? 1 : 1.6;
            case "phantom", "resplendent" -> 1.5;
            case "stymphalian", "amphithere" -> 1.3;
            case "sun", "resonant" -> 2;
            case "neutronium" -> 11;
            default -> 1;
        };
    }

    public double gravity() {
        if (shaft("breeze") || feather("sun") || feather("neutronium") || hitscan()) return 0;
        return feather("wheat") ? 1.2 : feather("emu") ? .8 : 1;
    }

    public boolean phase() {
        return shaft("infinity") || shaft("ender");
    }

    public boolean homing() {
        return shaft("dielectric") || shaft("ender");
    }

    public boolean hitscan() {
        return feather("resonant");
    }

    public boolean water() {
        return tip("prismarine") || tip("abyssal") || extra("sea");
    }

    public int ricochets() {
        return shaft("ender") ? 3 : shaft("end") ? 1 : 0;
    }

    public float bonus(LivingEntity target, double peak) {
        float bonus = tip.damage();
        if (tip("quartz") && target.level().dimension().equals(Level.NETHER)) bonus += 2;
        if (tip("abyssal") && target.isInWaterOrRain()) bonus += 4;
        if ((tip("onyx") || tip("moonstone")) && night(target.level())) bonus += 3;
        if (tip("heavy")) bonus += (float) Math.max(0, peak - target.getY());
        return bonus;
    }

    public boolean execute() {
        return tip("divine") || tip("eldritch");
    }

    public boolean loot() {
        return execute() || extra("magnet");
    }

    public double experience() {
        return (shaft("experience") ? 2 : 1) * (extra("brilliance") ? 1.5 : 1);
    }
}
