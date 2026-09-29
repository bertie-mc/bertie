package io.github.bertie_mc.fletching;

import static org.junit.jupiter.api.Assertions.*;

import com.fletchery.mod.arrow.ArrowComponentResolver;
import com.fletchery.mod.config.ModConfig;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.Test;

class WheatGravityTest {
    @Test
    void gravityIsAnAdditionAndTooltipUsesPercent() {
        ModConfig config = ModConfig.get();
        float savedGravity = config.wheatGravity, savedSpeed = config.wheatSpeed;
        try {
            config.wheatGravity = .05F;
            config.wheatSpeed = .8F;
            CompoundTag tag = new CompoundTag();
            tag.putString("feather", "minecraft:wheat");
            var properties = ArrowComponentResolver.resolve(tag);
            assertEquals(1.05F, properties.gravityMultiplier, .000001F);
            assertEquals(.8F, properties.speedMultiplier, .000001F);
            ItemStack stack = ArrowRecipe.craft(
                    new ItemStack(Items.WHEAT),
                    new ItemStack(Items.STICK),
                    new ItemStack(Items.FLINT),
                    ItemStack.EMPTY,
                    new PotionTank());
            ArrayList<Component> lines = new ArrayList<>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
            var text = (TranslatableContents) lines.getFirst().getContents();
            assertEquals("fletchery_expanded.tooltip.feather.wheat", text.getKey());
            assertArrayEquals(new Object[] {"20", "5"}, text.getArgs());
            config.wheatGravity = 0;
            assertEquals(1F, ArrowComponentResolver.resolve(tag).gravityMultiplier);
            config.wheatGravity = .25F;
            assertEquals(1.25F, ArrowComponentResolver.resolve(tag).gravityMultiplier);
            tag.putString("feather", "minecraft:feather");
            assertEquals(1F, ArrowComponentResolver.resolve(tag).gravityMultiplier);
        } finally {
            config.wheatGravity = savedGravity;
            config.wheatSpeed = savedSpeed;
        }
    }
}
