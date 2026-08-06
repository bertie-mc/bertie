package io.github.bertie_mc.pack.gametest;

import io.github.bertie_mc.carving.ArmorKind;
import io.github.bertie_mc.carving.Carving;
import io.github.bertie_mc.carving.CarvingMaterial;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertiepacktests")
@PrefixGameTestTemplate(false)
public final class PackGameTests {
    private PackGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void packArmorOverrideConnectsCarvingAndImmersiveArmors(GameTestHelper helper) {
        var expected =
                BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersive_armors", "wooden_helmet"));
        var carved = Carving.resultStack(CarvingMaterial.WOOD, true, ArmorKind.HELMET.ordinal(), 0, 0);
        if (carved.getItem() != expected) {
            helper.fail("Pack armor override did not map a carved wood helmet to Immersive Armors");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void paxiLoadsDeclaredDatapacks(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        if (!structures.containsKey(ResourceLocation.fromNamespaceAndPath("joshie", "sunken_spires/spires"))) {
            helper.fail("Paxi did not load the declared Sunken Spires datapack");
            return;
        }
        helper.succeed();
    }
}
