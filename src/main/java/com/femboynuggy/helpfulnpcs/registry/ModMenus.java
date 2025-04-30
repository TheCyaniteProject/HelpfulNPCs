package com.femboynuggy.helpfulnpcs.registry;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.container.WorkerContainer;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
  // ➊ Create a DeferredRegister for MenuType<?>
  public static final DeferredRegister<MenuType<?>> MENUS =
    DeferredRegister.create(ForgeRegistries.MENU_TYPES, HelpfulNPCs.MODID);

  // ➋ Register your menu type under the name "worker_menu"
   public static final RegistryObject<MenuType<WorkerContainer>> WORKER_MENU =
    MENUS.register("worker_menu",
      // THIS factory is run *on both* client *and* server
      () -> IForgeMenuType.create((windowId, inv, buf) -> {
        int entityId = buf.readInt();               // <-- read what we will write
        return new WorkerContainer(windowId, inv, entityId);
      })
    );
}