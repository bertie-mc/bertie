package io.github.bertie_mc.spellrestrictions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;

public final class ProgressData {
    public static final Codec<ProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().fieldOf("spells").forGetter(data -> List.copyOf(data.state.spells())),
                    Codec.intRange(-1, 4).fieldOf("tier").forGetter(data -> data.state.tier()))
            .apply(instance, (spells, tier) -> new ProgressData(new UnlockState(new HashSet<>(spells), tier))));
    public final UnlockState state;

    public ProgressData() {
        this(new UnlockState(
                java.util.Set.of(), RestrictionConfig.STARTING_TIER.get().rank()));
    }

    public ProgressData(UnlockState state) {
        this.state = state;
    }
}
