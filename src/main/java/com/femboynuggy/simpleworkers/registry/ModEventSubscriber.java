package com.femboynuggy.simpleworkers.registry;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.femboynuggy.simpleworkers.SimpleWorkers;
import com.femboynuggy.simpleworkers.entity.WorkerEntity;

@Mod.EventBusSubscriber(modid=SimpleWorkers.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent e) {
        e.put(ModEntities.WORKER.get(), WorkerEntity.createAttributes().build());
    }
}