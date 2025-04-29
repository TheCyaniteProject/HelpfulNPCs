package com.femboynuggy.simpleworkers.registry;

import com.femboynuggy.simpleworkers.SimpleWorkers;
import com.femboynuggy.simpleworkers.item.WorkerContractItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, SimpleWorkers.MODID);
  
    public static final RegistryObject<Item> WORKER_CONTRACT =
      ITEMS.register("worker_contract", () -> new WorkerContractItem(new Item.Properties().stacksTo(64)));
  
    // call ITEMS.register(bus) in your mod constructor…
  }