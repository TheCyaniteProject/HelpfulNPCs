package com.femboynuggy.simpleworkers;

import java.util.Optional;

import com.femboynuggy.simpleworkers.network.SetWorkerCommandPacket;
import com.femboynuggy.simpleworkers.registry.ModEntities;
import com.femboynuggy.simpleworkers.registry.ModMenus;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(SimpleWorkers.MODID)
public class SimpleWorkers {
    public static final String MODID = "simpleworkers";
    private static final String PROTOCOL = "1";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(MODID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    public SimpleWorkers() {
    
        // 1. Grab the mod‐event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 2. Register your DeferredRegister instances
        ModEntities.ENTITIES.register(modEventBus);
        ModEntities.ITEMS.register(modEventBus);

        ModMenus.MENUS.register(modEventBus);

        // 3. (Any other registration e.g. client setup)
        //modEventBus.addListener(ClientSetup::onClientSetup);

        int idx = 0;
        CHANNEL.registerMessage(
            idx++,
            SetWorkerCommandPacket.class,
            SetWorkerCommandPacket::toBytes,
            SetWorkerCommandPacket::new,
            SetWorkerCommandPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}