package com.femboynuggy.helpfulnpcs.registry;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
  public static final DeferredRegister<EntityType<?>> ENTITIES =
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HelpfulNPCs.MODID);

  public static final RegistryObject<EntityType<WorkerEntity>> WORKER =
    ENTITIES.register("worker", () ->
      EntityType.Builder
        .<WorkerEntity>of(WorkerEntity::new, MobCategory.MISC)
        .sized(0.6f, 1.8f)
        .setTrackingRange(80)
        .setUpdateInterval(1)
        .setCustomClientFactory(WorkerEntity::new)
        .build("worker")
    );
}