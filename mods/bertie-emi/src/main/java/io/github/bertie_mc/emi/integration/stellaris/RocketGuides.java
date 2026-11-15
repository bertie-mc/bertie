package io.github.bertie_mc.emi.integration.stellaris;

import com.st0x0ef.stellaris.Stellaris;
import com.st0x0ef.stellaris.common.data_components.RocketComponent;
import com.st0x0ef.stellaris.common.items.VehicleUpgradeItem;
import com.st0x0ef.stellaris.common.registry.DataComponentsRegistry;
import com.st0x0ef.stellaris.common.registry.TagRegistry;
import com.st0x0ef.stellaris.common.vehicle_upgrade.FuelType;
import com.st0x0ef.stellaris.common.vehicle_upgrade.ModelUpgrade;
import com.st0x0ef.stellaris.common.vehicle_upgrade.MotorUpgrade;
import com.st0x0ef.stellaris.common.vehicle_upgrade.SkinUpgrade;
import com.st0x0ef.stellaris.common.vehicle_upgrade.TankUpgrade;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

final class RocketGuides {
    private RocketGuides() {}

    static void register(EmiRegistry registry) {
        EmiRecipeCategory category =
                Categories.machine(registry, "stellaris_rocket_operation", "stellaris:rocket", "Stellaris: Rockets");
        registry.addWorkstation(category, stack("rocket_launch_pad"));
        registry.addWorkstation(category, stack("rocket_station"));
        add(
                registry,
                category,
                "launch_pad",
                RocketGuideEmiRecipe.Kind.PAD,
                stack("rocket_launch_pad").setAmount(9),
                null,
                "Build a 3x3 Launch Pad",
                "Use the Rocket on the centre",
                "Place nine Rocket Launch Pad blocks at one height. The centre activates automatically. Use the Rocket there and leave room above for the vehicle.",
                EmiStack.EMPTY,
                EmiStack.EMPTY,
                false);
        add(
                registry,
                category,
                "inventory",
                RocketGuideEmiRecipe.Kind.INVENTORY,
                EmiStack.EMPTY,
                null,
                "Open the Rocket inventory",
                "Fuel left; optional upgrades right",
                "Crouch-right-click the placed Rocket with an empty hand. It already has a Tiny model, regular Fuel motor and 3,000 fuel capacity. Fuel and container slots are on the left, cargo at the top right, and four optional upgrade slots below it. The Upgrade Station is for spacesuits.",
                EmiStack.EMPTY,
                EmiStack.EMPTY,
                false);
        upgrades(registry, category);
        fuels(registry, category);
        add(
                registry,
                category,
                "launch",
                RocketGuideEmiRecipe.Kind.LAUNCH,
                EmiStack.EMPTY,
                null,
                "Launch and choose a destination",
                "10-second countdown; then ascend",
                "Board with a normal right-click, then press Jump. Above Y=" + Stellaris.CONFIG.rocketTpHeight
                        + " the destination screen opens. Select a body and a direct or registered landing point. Reach depends on current fuel and distance from the departure body; a larger model adds seats rather than a planet tier. Carry fuel for the return trip.",
                EmiStack.EMPTY,
                EmiStack.EMPTY,
                false);
        add(
                registry,
                category,
                "antenna",
                RocketGuideEmiRecipe.Kind.ANTENNA,
                stack("antenna"),
                null,
                "Register a named landing pad",
                "Use Antenna on the active centre",
                "Use an Antenna on the activated Launch Pad centre. It replaces an allowed block directly beneath the centre; open the Antenna menu to register and name the pad and set access. A named pad is optional for an initial direct landing.",
                EmiStack.EMPTY,
                EmiStack.EMPTY,
                false);
    }

    private static void upgrades(EmiRegistry registry, EmiRecipeCategory category) {
        List<EmiStack> skins = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof VehicleUpgradeItem upgradeItem)) continue;
            var upgrade = upgradeItem.getUpgrade();
            EmiStack module = EmiStack.of(item);
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString().replace(':', '/');
            ItemStack result = stack("rocket").getItemStack().copy();
            RocketComponent base = result.get(DataComponentsRegistry.ROCKET_COMPONENT.get());
            RocketLayouts.UpgradeSlot slot;
            String title;
            String summary;
            String detail;
            RocketComponent changed;
            if (upgrade instanceof ModelUpgrade model) {
                slot = RocketLayouts.UpgradeSlot.MODEL;
                title = "Model: " + Categories.capitalize(model.getModel().getSerializedName());
                int seats = model.getModel().getMaxPlayerNumber();
                summary = seats + (seats == 1 ? " passenger" : " passengers");
                detail = "Insert this item in the rightmost Model slot of the placed Rocket. It changes the model to "
                        + model.getModel().getSerializedName() + " with " + seats
                        + " passenger seats. Models do not set fuel type, range or travel speed. Removing the model restores Tiny (one seat).";
                changed = new RocketComponent(
                        base.skin(),
                        model.getModel(),
                        base.fuelType(),
                        base.fuel(),
                        base.fuelTexture(),
                        base.tankCapacity());
            } else if (upgrade instanceof TankUpgrade tank) {
                slot = RocketLayouts.UpgradeSlot.TANK;
                title = "Increase Rocket fuel capacity";
                summary = base.tankCapacity() + " -> " + tank.getTankCapacity() + " fuel";
                detail = "Put this upgrade in the Tank slot (second from the left). The installed tank holds "
                        + tank.getTankCapacity()
                        + " fuel. Empty the tank before removing the upgrade. It stays in the slot and is not consumed.";
                changed = new RocketComponent(
                        base.skin(),
                        base.model(),
                        base.fuelType(),
                        base.fuel(),
                        base.fuelTexture(),
                        tank.getTankCapacity());
            } else if (upgrade instanceof MotorUpgrade motor && motor.getFuelType() != FuelType.Type.DIESEL) {
                slot = RocketLayouts.UpgradeSlot.MOTOR;
                title = "Motor: " + Categories.capitalize(motor.getFuelType().getSerializedName());
                summary = motor.getFuelType() == FuelType.Type.RADIOACTIVE
                        ? "Uranium / Neptunium / Plutonium"
                        : "Accepts " + motor.getFuelType().getSerializedName();
                detail = "Empty all fuel before changing motor type. Insert this upgrade in the Motor slot (leftmost). "
                        + (motor.getFuelType() == FuelType.Type.RADIOACTIVE
                                ? "The Radioactive Motor accepts Uranium, Neptunium or Plutonium Ingots, one type per tank."
                                : "The Hydrogen Motor accepts Hydrogen Buckets.")
                        + " The upgrade remains installed; motor removal also requires an empty tank. Without a motor upgrade the Rocket uses regular Fuel.";
                var type =
                        motor.getFuelType() == FuelType.Type.RADIOACTIVE ? FuelType.Type.URANIUM : motor.getFuelType();
                changed = new RocketComponent(
                        base.skin(),
                        base.model(),
                        type.getSerializedName(),
                        0,
                        type.getFuelTexture(),
                        base.tankCapacity());
            } else if (upgrade instanceof SkinUpgrade) {
                skins.add(module);
                continue;
            } else {
                continue;
            }
            result.set(DataComponentsRegistry.ROCKET_COMPONENT.get(), changed);
            registry.addWorkstation(category, module);
            add(
                    registry,
                    category,
                    "upgrade/" + itemId,
                    RocketGuideEmiRecipe.Kind.UPGRADE,
                    module,
                    slot,
                    title,
                    summary,
                    detail,
                    EmiStack.of(result),
                    EmiStack.EMPTY,
                    false);
        }
        if (!skins.isEmpty()) {
            add(
                    registry,
                    category,
                    "skins",
                    RocketGuideEmiRecipe.Kind.UPGRADE,
                    EmiIngredient.of(skins),
                    RocketLayouts.UpgradeSlot.SKIN,
                    "Change the Rocket skin",
                    "Skin slot: third from the left",
                    "Put a Rocket Skin in the Skin slot. It changes appearance and remains in the slot. Removing it restores the standard skin.",
                    EmiStack.EMPTY,
                    EmiStack.EMPTY,
                    false);
        }
    }

    private static void fuels(EmiRegistry registry, EmiRecipeCategory category) {
        fuel(registry, category, "fuel", EmiIngredient.of(TagRegistry.FUEL), FuelType.Type.FUEL, EmiStack.EMPTY, true);
        fuel(
                registry,
                category,
                "hydrogen",
                stack("hydrogen_bucket"),
                FuelType.Type.HYDROGEN,
                stack("hydrogen_motor"),
                true);
        fuel(
                registry,
                category,
                "uranium",
                stack("uranium_ingot"),
                FuelType.Type.URANIUM,
                stack("radioactive_motor"),
                false);
        fuel(
                registry,
                category,
                "neptunium",
                stack("neptunium_ingot"),
                FuelType.Type.NEPTUNIUM,
                stack("radioactive_motor"),
                false);
        fuel(
                registry,
                category,
                "plutonium",
                stack("plutonium_ingot"),
                FuelType.Type.PLUTONIUM,
                stack("radioactive_motor"),
                false);
    }

    private static void fuel(
            EmiRegistry registry,
            EmiRecipeCategory category,
            String key,
            EmiIngredient input,
            FuelType.Type type,
            EmiStack motor,
            boolean returnsBucket) {
        ItemStack result = stack("rocket").getItemStack().copy();
        RocketComponent base = result.get(DataComponentsRegistry.ROCKET_COMPONENT.get());
        result.set(
                DataComponentsRegistry.ROCKET_COMPONENT.get(),
                new RocketComponent(
                        base.skin(),
                        base.model(),
                        type.getSerializedName(),
                        1000,
                        type.getFuelTexture(),
                        base.tankCapacity()));
        String range = String.format(Locale.ROOT, "%,.0f", FuelType.getMegametersTraveled(1000, type));
        add(
                registry,
                category,
                "fuel/" + key,
                RocketGuideEmiRecipe.Kind.FUEL,
                input,
                RocketLayouts.UpgradeSlot.MOTOR,
                "Fuel: " + Categories.capitalize(key),
                "+1,000 fuel; " + range + " Mm range",
                (motor.isEmpty()
                                ? "Use the default motor (leave the Motor slot empty). "
                                : "Install the matching motor first. ")
                        + "Put the fuel item in the top-left fuel slot. Each adds 1,000 fuel, limited by tank capacity. "
                        + "Fuel types cannot be mixed: empty the tank before switching. Distance is measured between the planets' configured distances from Earth. Diesel cannot power a Rocket.",
                EmiStack.of(result),
                motor,
                returnsBucket);
    }

    private static void add(
            EmiRegistry registry,
            EmiRecipeCategory category,
            String id,
            RocketGuideEmiRecipe.Kind kind,
            EmiIngredient subject,
            RocketLayouts.UpgradeSlot target,
            String title,
            String summary,
            String detail,
            EmiStack result,
            EmiStack motor,
            boolean returnsBucket) {
        registry.addRecipe(new RocketGuideEmiRecipe(
                category,
                id,
                kind,
                stack("rocket"),
                subject,
                target,
                title,
                summary,
                detail,
                result,
                motor,
                returnsBucket));
    }

    static EmiStack stack(String path) {
        return Categories.stack("stellaris:" + path);
    }
}
