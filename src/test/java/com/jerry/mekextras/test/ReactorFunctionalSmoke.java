package com.jerry.mekextras.test;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import igentuman.mbtool.util.MultiblocksProvider;
import giselle.jei_mekanism_multiblocks.client.jei.category.NaquadahLayout;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = MekanismExtras.MOD_ID)
public class ReactorFunctionalSmoke {
    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        MekanismExtras.LOGGER.info("REACTOR PASS: {}", label);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mekextras.reactorSmoke")) return;
        try {
            var level = event.getServer().overworld();
            var template = MultiblocksProvider.getStructures().stream().filter(s -> s.getName().equals("mbtool.structure.naquadah_reactor")).findFirst().orElseThrow();
            require(template.getBlocks().size() == 729, "MultiBuilder loads the complete 9x9x9 template");
            int count = 0;
            var origin = new BlockPos(32, 130, 0);
            for (var entry : template.getBlocks().entrySet()) {
                var local = entry.getKey();
                boolean guideHasBlock = NaquadahLayout.block(local.getX(), local.getY(), local.getZ()) != '.';
                if (guideHasBlock != !entry.getValue().isAir()) throw new AssertionError("JMM and MultiBuilder disagree at " + local);
                if (!entry.getValue().isAir()) count++;
                level.setBlockAndUpdate(origin.offset(local), entry.getValue());
            }
            require(count == 330, "330 construction blocks, with empty interior");
            require(true, "JMM layer guide matches all 729 MultiBuilder positions");
            for (int tick = 0; tick < 100; tick++) {
                ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(level.getGameTime() + 1);
                for (var local : template.getBlocks().keySet()) {
                    var pos = origin.offset(local);
                    if (level.getBlockEntity(pos) instanceof TileEntityMekanism tile) TileEntityMekanism.tickServer(level, pos, level.getBlockState(pos), tile);
                }
            }
            var controller = (TileEntityNaquadahReactorController) level.getBlockEntity(origin.offset(4,8,4));
            require(controller.getMultiblock().isFormed(), "template forms a real Naquadah reactor");
            MekanismExtras.LOGGER.info("REACTOR FUNCTIONAL COMPLETE");
        } catch (Throwable error) { MekanismExtras.LOGGER.error("REACTOR FUNCTIONAL FAILED", error); }
        finally { event.getServer().halt(false); }
    }
}
