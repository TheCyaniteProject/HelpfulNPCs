package com.femboynuggy.helpfulnpcs.registry;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.item.WorkerContractItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, HelpfulNPCs.MODID);
  
    public static final RegistryObject<Item> WORKER_CONTRACT =
      ITEMS.register("contract", () -> new WorkerContractItem(new Item.Properties().stacksTo(16)));
  
}