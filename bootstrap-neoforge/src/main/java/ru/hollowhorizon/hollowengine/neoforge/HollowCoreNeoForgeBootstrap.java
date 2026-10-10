package ru.hollowhorizon.hollowengine.neoforge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.RuntimePlatform;
import ru.hollowhorizon.hollowengine.neoforge.internal.NeoForgeFakePlayerFactory;
import ru.hollowhorizon.hollowengine.neoforge.internal.NeoForgeModList;
import ru.hollowhorizon.hollowengine.neoforge.internal.NeoForgeNetworkManager;
import ru.hollowhorizon.hollowengine.neoforge.internal.NeoForgeRegistryHolder;

@Mod("hollowengine")
public final class HollowCoreNeoForgeBootstrap {
    public HollowCoreNeoForgeBootstrap(IEventBus modBus) {
        // Normally undone by the first script mixin applied; a launch that applied none still gets it back.
        NeoForgeScriptMixinPlugin.restore();
        BootstrapRuntimeManager.bridge().setPlatform(RuntimePlatform.NEOFORGE);
        BootstrapRuntimeManager.bridge().setProduction(FMLEnvironment.isProduction());
        BootstrapRuntimeManager.bridge().setClient(FMLEnvironment.getDist().isClient());

        BootstrapRuntimeManager.bridge().initFakePlayers(new NeoForgeFakePlayerFactory());
        BootstrapRuntimeManager.bridge().initStackHelper(item -> {
            var remainder = item.getItem().getCraftingRemainder(item);
            return remainder == null ? ItemStack.EMPTY : remainder.create();
        });
        BootstrapRuntimeManager.bridge().initNetwork(new NeoForgeNetworkManager());
        BootstrapRuntimeManager.bridge().initModList(new NeoForgeModList());
        BootstrapRuntimeManager.bridge().initRegistryProvider((location, registry, model, generator, type) ->
                new NeoForgeRegistryHolder<>(modBus, location, registry, model, generator, type));

        BootstrapRuntimeManager.bridge().onCommonInitialize();
        modBus.addListener(NeoForgeNetworkManager::onRegisterPackets);
        NeoForgeEvents.init(modBus);
        if (FMLEnvironment.getDist().isClient()) {
            modBus.addListener(HollowCoreNeoForgeBootstrap::onClientInitialize);
            NeoForgeClientEvents.init(modBus);
        }
    }

    public static void onClientInitialize(FMLClientSetupEvent event) {
        BootstrapRuntimeManager.bridge().onClientInitialize();
    }
}
