package com.femboynuggy.helpfulnpcs.registry;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
  public static final DeferredRegister<EntityType<?>> ENTITIES =
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HelpfulNPCs.MODID);

  public static final RegistryObject<EntityType<WorkerEntity>> WORKER =
    ENTITIES.register("worker", () ->
      EntityType.Builder
        .of(WorkerEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.8F)
        .build(new ResourceLocation(HelpfulNPCs.MODID, "worker").toString())
    );
}