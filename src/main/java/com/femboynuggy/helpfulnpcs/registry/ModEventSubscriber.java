package com.femboynuggy.helpfulnpcs.registry;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=HelpfulNPCs.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent e) {
        e.put(ModEntities.WORKER.get(), WorkerEntity.createAttributes().build());
    }
}