package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RefinedFortuneEmi {
    static void reorder() {
        // EMI 1.1's insertion API duplicates existing entries; its removal predicates run later.
        // Move the existing stacks together before EMI applies user index customizations.
        List<EmiStack> stacks = EmiStackList.stacks;
        List<EmiStack> upgrades = new ArrayList<>();
        for (var stack : stacks) {
            String id = stack.getId().toString();
            if (id.matches("refinedstorage:fortune_[4-8]_upgrade")
                    && upgrades.stream().noneMatch(old -> old.getId().equals(stack.getId()))) upgrades.add(stack);
        }
        if (upgrades.isEmpty()) return;
        upgrades.sort(Comparator.comparing(stack -> stack.getId().getPath()));
        int anchor = -1;
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).getId().toString().equals("refinedstorage:fortune_3_upgrade")) {
                anchor = i;
                break;
            }
        }
        if (anchor < 0) return;
        var pivot = stacks.get(anchor);
        stacks.removeIf(stack -> stack.getId().toString().matches("refinedstorage:fortune_[4-8]_upgrade"));
        stacks.addAll(stacks.indexOf(pivot) + 1, upgrades);
    }

    private RefinedFortuneEmi() {}
}
