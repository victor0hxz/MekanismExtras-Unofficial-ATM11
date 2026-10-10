package com.jerry.mekextras.test;

import com.jerry.mekextras.MekanismExtras;
import giselle.jei_mekanism_multiblocks.client.jei.JeiPlugin;
import giselle.jei_mekanism_multiblocks.client.jei.category.NaquadahReactorCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = MekanismExtras.MOD_ID, value = Dist.CLIENT)
public class GuideClientSmoke {
    private static boolean connected;
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("mekextras.guideClientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (!connected && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            connected = true;
            ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString("127.0.0.1:25565"),
                new ServerData("Guide test", "127.0.0.1:25565", ServerData.Type.LAN), false, null);
        }
        if (mc.level == null || mc.player == null) return;
        var plugin = JeiPlugin.instance();
        if (plugin == null || plugin.getRuntime() == null) return;
        ticks++;
        try {
            if (ticks == 60) {
                mc.getWindow().setWindowed(1280, 960);
                var category = plugin.getCategories().stream().filter(c -> c instanceof NaquadahReactorCategory).map(c -> (NaquadahReactorCategory)c).findFirst().orElseThrow();
                var recipe = plugin.getRuntime().getRecipeManager().createRecipeLookup(category.getRecipeType()).get().findFirst().orElseThrow();
                recipe.initialize();
                if (recipe.getCosts().stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum() != 330) throw new AssertionError("Wrong cost");
                plugin.getRuntime().getRecipesGui().showRecipes(category, java.util.List.of(recipe), java.util.List.of());
                if (igentuman.mbtool.util.MultiblocksProvider.getStructures().stream().noneMatch(s -> s.getName().equals("mbtool.structure.naquadah_reactor"))) throw new AssertionError("Missing client template");
                MekanismExtras.LOGGER.info("GUIDE CLIENT PASS: JEI recipe and MultiBuilder template registered");
            }
            if (ticks == 100) {
                Screenshot.grab(mc.gameDirectory, "naquadah-jmm-port3.png", mc.getMainRenderTarget(), 1,
                    message -> MekanismExtras.LOGGER.info("GUIDE CAPTURE: {}", message.getString()));
            }
            if (ticks == 140) { MekanismExtras.LOGGER.info("GUIDE CLIENT COMPLETE"); mc.stop(); }
        } catch (Throwable error) { MekanismExtras.LOGGER.error("GUIDE CLIENT FAILED", error); mc.stop(); }
    }
}
