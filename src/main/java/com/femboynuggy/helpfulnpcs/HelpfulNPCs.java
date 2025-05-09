package com.femboynuggy.helpfulnpcs;

import java.util.Optional;

import com.femboynuggy.helpfulnpcs.network.SetWorkerCommandPacket;
import com.femboynuggy.helpfulnpcs.registry.ModEntities;
import com.femboynuggy.helpfulnpcs.registry.ModItems;
import com.femboynuggy.helpfulnpcs.registry.ModMenus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(HelpfulNPCs.MODID)
public class HelpfulNPCs {
    public static final String MODID = "helpfulnpcs";
    private static final String PROTOCOL = "1";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(MODID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    public HelpfulNPCs() {
    
        // 1. Grab the mod‐event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 2. Register your DeferredRegister instances
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(this::addCreative);

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


    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.WORKER_CONTRACT);
        }
    }
}