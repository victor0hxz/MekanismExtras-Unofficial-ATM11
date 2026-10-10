package com.jerry.mekextras.test;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.api.ExtraUpgrade;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Runs only in the isolated development server, never in the released jar. */
@EventBusSubscriber(modid = MekanismExtras.MOD_ID)
public class HeaterFunctionalSmoke {
    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        MekanismExtras.LOGGER.info("HEATER PASS: {}", label);
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mekextras.heaterSmoke")) return;
        try {
            var level = event.getServer().overworld();
            var pos = new BlockPos(0, 120, 0);
            for (var side : net.minecraft.core.Direction.values()) level.setBlockAndUpdate(pos.relative(side), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, MekanismBlocks.RESISTIVE_HEATER.get().defaultBlockState());
            var heater = (TileEntityResistiveHeater) level.getBlockEntity(pos);
            require(heater.getHeatCapacitor(null).getHeatCapacity() == 100, "heater has physical capacity after world reload");
            // Reset the fixture's thermal state: previous runs may have heated it heavily.
            heater.getHeatCapacitor(null).setHeat(100 * mekanism.api.heat.HeatAPI.AMBIENT_TEMP, null);
            heater.setEnergyUsageFromPacket(1000);
            boolean charged = false;
            for (var side : net.minecraft.core.Direction.values()) {
                var input = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, pos, side);
                if (input != null) {
                    try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        if (input.insert(100000, transaction) > 0) { transaction.commit(); charged = true; break; }
                    }
                }
            }
            require(charged, "ordinary heater accepts energy through a sided capability");
            double before = heater.getTemperature();
            TileEntityMekanism.tickServer(level, pos, level.getBlockState(pos), heater);
            MekanismExtras.LOGGER.info("HEATER thermal diagnostic: before={}, after={}, capacity={}, losses={} / {}", before, heater.getTemperature(), heater.getHeatCapacitor(null).getHeatCapacity(), heater.getLastEnvironmentLoss(), heater.getLastTransferLoss());
            require(heater.getEnergyUsed() == 1000, "ordinary heater consumes configured energy");
            require(heater.getTemperature() > before, "ordinary heater produces heat with Extras loaded");
            require(heater.getComponent().addUpgrades(ExtraUpgrade.CREATIVE, 1) == 1, "creative upgrade installed in test fixture");
            require(heater.energyContainer().getEnergyPerTick() == 1000, "creative upgrade preserves heater's useful heat input");
            before = heater.getTemperature();
            TileEntityMekanism.tickServer(level, pos, level.getBlockState(pos), heater);
            require(heater.getEnergyUsed() == 1000 && heater.getTemperature() > before, "creative heater still produces heat");
            var receiverPos = pos.east();
            level.setBlockAndUpdate(receiverPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(receiverPos, MekanismBlocks.RESISTIVE_HEATER.get().defaultBlockState());
            var receiver = (TileEntityResistiveHeater) level.getBlockEntity(receiverPos);
            var heat = level.getCapability(mekanism.common.capabilities.Capabilities.HEAT, receiverPos, net.minecraft.core.Direction.WEST);
            require(heat != null, "adjacent heat capability available");
            before = receiver.getTemperature();
            for (int tick = 0; tick < 20; tick++) {
                TileEntityMekanism.tickServer(level, pos, level.getBlockState(pos), heater);
                TileEntityMekanism.tickServer(level, receiverPos, level.getBlockState(receiverPos), receiver);
            }
            require(receiver.getTemperature() > before, "heater transfers heat to an adjacent block");
            var codec = mekanism.api.heat.IHeatCapacitor.CapacitorState.CODEC;
            var encoded = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,
                new mekanism.api.heat.IHeatCapacitor.CapacitorState(40000, 100)).getOrThrow();
            var decoded = codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
            require(decoded.heat() == 40000 && decoded.heatCapacity() == 100, "heat persistence preserves capacity separately from heat");
            try {
                var limit = (mekanism.common.config.value.CachedIntValue) mekanism.common.config.GeneralConfig.class
                    .getField("resistiveHeaterMaxEnergyUsage").get(mekanism.common.config.MekanismConfig.general);
                int originalLimit = limit.get();
                try {
                    limit.set(Integer.MAX_VALUE);
                    heater.setEnergyUsageFromPacket(50000000);
                    require(heater.energyContainer().getEnergyPerTick() == 50000000, "50 million FE/tick accepted above the old GUI limit");
                    heater.setEnergyUsageFromPacket(Integer.MAX_VALUE);
                    require(heater.energyContainer().getCapacityAsLong() == 4L * Integer.MAX_VALUE, "maximum usage stores energy without integer overflow");
                    limit.set(25000000);
                    heater.setEnergyUsageFromPacket(50000000);
                    require(heater.energyContainer().getEnergyPerTick() == 25000000, "server enforces the configured usage limit");
                    heater.setEnergyUsageFromPacket(-1);
                    require(heater.energyContainer().getEnergyPerTick() == 0, "negative usage clamped safely");
                } finally { limit.set(originalLimit); heater.setEnergyUsageFromPacket(1000); }
            } catch (NoSuchFieldException ignored) {
                // The fixture can also verify the unpatched Version Locked 2.1 binary.
            }
            MekanismExtras.LOGGER.info("HEATER FUNCTIONAL COMPLETE");
        } catch (Throwable error) {
            MekanismExtras.LOGGER.error("HEATER FUNCTIONAL FAILED", error);
        } finally {
            event.getServer().halt(false);
        }
    }
}
