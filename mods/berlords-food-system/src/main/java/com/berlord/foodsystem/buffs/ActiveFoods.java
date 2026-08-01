package com.berlord.foodsystem.buffs;

import com.berlord.foodsystem.stomach.Stomach;
import com.berlord.foodsystem.stomach.StomachData;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Buffs granted by foods currently active in the player's stomach. Both logical sides. */
public final class ActiveFoods {

    public static List<BuffsConfig.FoodBuff> activeBuffs(Player player) {
        List<BuffsConfig.FoodBuff> out = new ArrayList<>(4);
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(player);
        int unlocked = Stomach.unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            if (data.slots[i].isActive()) {
                BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
                if (buff != null) out.add(buff);
            }
        }
        // a synthetic food buff from category synergy, merged by BuffEngine like any other
        BuffsConfig.FoodBuff synergy = SynergyEngine.computeSynergy(player);
        if (synergy != null) out.add(synergy);
        return out;
    }

    // The scalar/boolean queries below scan the stomach slots directly instead of going
    // through activeBuffs() — they run on movement/combat/particle paths and would otherwise
    // allocate a fresh ArrayList on every call. activeBuffs() above is kept for BuffEngine,
    // which genuinely needs the materialized list once per tick.

    /** is this effect currently granted by any active configured food? */
    public static boolean grantsEffect(Player p, net.minecraft.resources.ResourceLocation effectId) {
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(p);
        int unlocked = Stomach.unlockedSlots(p);
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isActive()) continue;
            BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
            if (buff == null) continue;
            for (var e : buff.effects) {
                if (e.effectId().equals(effectId)) return true;
            }
        }
        return false;
    }

    public static boolean hasEndermanCalm(Player p) {
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(p);
        int unlocked = Stomach.unlockedSlots(p);
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isActive()) continue;
            BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
            if (buff != null && buff.abilities.endermanCalm) return true;
        }
        return false;
    }

    public static boolean hasClimbing(Player p) {
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(p);
        int unlocked = Stomach.unlockedSlots(p);
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isActive()) continue;
            BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
            if (buff != null && buff.abilities.climbing) return true;
        }
        return false;
    }

    public static double xpMultiplier(Player p) {
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(p);
        int unlocked = Stomach.unlockedSlots(p);
        double m = 1.0;
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isActive()) continue;
            BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
            if (buff != null) m = Math.max(m, buff.abilities.xpBoost);
        }
        return m;
    }

    public static double durabilitySaverChance(Player p) {
        var foods = BuffsConfig.get().foods;
        StomachData data = Stomach.get(p);
        int unlocked = Stomach.unlockedSlots(p);
        double c = 0;
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isActive()) continue;
            BuffsConfig.FoodBuff buff = foods.get(data.slots[i].stack.getItem());
            if (buff != null) c = Math.max(c, buff.abilities.durabilitySaver);
        }
        return Math.min(c, 1.0);
    }

    private ActiveFoods() {}
}
