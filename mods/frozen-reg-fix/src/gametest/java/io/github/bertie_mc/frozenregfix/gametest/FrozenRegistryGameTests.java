package io.github.bertie_mc.frozenregfix.gametest;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frozenregfix")
@PrefixGameTestTemplate(false)
public final class FrozenRegistryGameTests {
    private static final List<String> MATERIALS = List.of(
            "bone", "wither", "warrior", "heavy", "robe", "slime", "divine", "prismarine", "wooden", "steampunk");

    private FrozenRegistryGameTests() {}

    @GameTest(template = "empty")
    public static void registersLateImmersiveArmorMaterials(GameTestHelper helper) {
        forceLazyItemsClass();
        for (String path : MATERIALS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersive_armors", path);
            if (!BuiltInRegistries.ARMOR_MATERIAL.containsKey(id)) {
                throw new AssertionError("Immersive Armors material is not registered: " + id);
            }
        }
        helper.succeed();
    }

    private static void forceLazyItemsClass() {
        try {
            Class.forName("immersive_armors.Items", true, FrozenRegistryGameTests.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Immersive Armors Items class is missing", exception);
        }
    }
}
