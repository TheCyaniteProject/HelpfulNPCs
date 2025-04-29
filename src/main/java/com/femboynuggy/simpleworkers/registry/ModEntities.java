package com.femboynuggy.simpleworkers.registry;

import com.femboynuggy.simpleworkers.SimpleWorkers;
import com.femboynuggy.simpleworkers.entity.WorkerEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
  public static final DeferredRegister<EntityType<?>> ENTITIES =
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SimpleWorkers.MODID);
  public static final DeferredRegister<Item> ITEMS =
    DeferredRegister.create(ForgeRegistries.ITEMS, SimpleWorkers.MODID);

  public static final RegistryObject<EntityType<WorkerEntity>> WORKER =
    ENTITIES.register("worker", () ->
      EntityType.Builder
        .of(WorkerEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.8F)
        .build(new ResourceLocation(SimpleWorkers.MODID, "worker").toString())
    );

//   public static final RegistryObject<Item> WORKER_SPAWN_EGG =
//     ITEMS.register("worker_spawn_egg", () ->
//       new SpawnEggItem(WORKER, 0x996600, 0xCCCCCC,
//         new Item.Properties().tab(CreativeModeTab.TAB_MISC))
//     );
}