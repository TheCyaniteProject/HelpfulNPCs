package com.femboynuggy.helpfulnpcs.container;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;
import com.femboynuggy.helpfulnpcs.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class WorkerContainer extends AbstractContainerMenu {
    public final WorkerEntity worker;
    private final IItemHandler inv; // the 29-slot inventory

    public WorkerContainer(int windowId, Inventory playerInv, int entityId) {
        super(ModMenus.WORKER_MENU.get(), windowId);

        // 1) Lookup the WorkerEntity on both client & server
        this.worker = (WorkerEntity) playerInv.player.level().getEntity(entityId);

        // 2) Grab its ItemStackHandler capability (29 slots)
        this.inv = worker.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new IllegalStateException("Missing Worker inventory"));

        // 4) Worker’s 9 main inventory slots (indices 0–8 in the handler)
        int x0 = 10;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(inv, i, 20 + i*18, 82));
        }

        // 5) Armor slots (handler indices 9-12, FEET→HEAD)
        EquipmentSlot[] armor = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < 4; i++) {
            final int idx = 12 - i;
            final EquipmentSlot slotType = armor[i];
            this.addSlot(new SlotItemHandler(inv, idx, x0 + 18, (i*18) + 10) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ArmorItem ai
                        && ai.getEquipmentSlot() == slotType;
                }
            });
        }

        // 6) Tool slot (handler index 13)
        this.addSlot(new SlotItemHandler(inv, 13, x0, 64) {
            @Override public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        // 7) Shield slot (handler index 14)
        this.addSlot(new SlotItemHandler(inv, 14, x0, 46) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ShieldItem;
            }
        });

        // 8) Add 15 item slots for second window scrollable list (indices 15–29 in the handler)
        // int baseX = 210; // Adjusted to align with second window (WIDTH + padding)
        // int baseY = 10;
        // int slotSize = 18;
        // for (int i = 0; i < 15; i++) {
        //     int slotY = baseY + i * (slotSize * 2 + 7); // match WorkerScreen layout spacing
        //     this.addSlot(new SlotItemHandler(inv, 15 + i, baseX, slotY));
        // }

        // TODO (Optionally) add player inventory slots so you can click real items around:
        layoutPlayerInventorySlots(playerInv, /*left=*/20, /*top=*/172);
        this.slotsChanged(playerInv);
    }

    @Override
    public boolean stillValid(Player player) {
        // Only stay open if the player is near the worker
        return worker.isAlive() && player.distanceToSqr(worker) < 64;
    }

    /** Draw the 3×9 player inventory + 1×9 hotbar below your GUI. */
    private void layoutPlayerInventorySlots(Inventory inv, int left, int top) {
        // main 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row*9 + 9,
                                     left + col*18,
                                     top + row*18));
            }
        }
        // hotbar 1×9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col,
                                 left + col*18,
                                 top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // TODO Implement shift‐click behavior
        return ItemStack.EMPTY;
    }

    // This constructor is only needed if you open with a FriendlyByteBuf:
    public static WorkerContainer fromNetwork(int windowId, Inventory inv, FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new WorkerContainer(windowId, inv, entityId);
    }
}