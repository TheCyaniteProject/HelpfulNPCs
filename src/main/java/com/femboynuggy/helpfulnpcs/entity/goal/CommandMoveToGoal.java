package com.femboynuggy.helpfulnpcs.entity.goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandMoveToGoal extends Goal {
    private final WorkerEntity mob;
    private final double speed;

    private enum Stage { GO_TO_TARGET, WAIT_AFTER_OP }
    private Stage stage = Stage.GO_TO_TARGET;

    private final List<TargetEntry> entries = new ArrayList<>();
    private int currentIndex = 0;
    private int waitTicks    = 0;

    // ← NEW: remember what size we loaded at start()
    private int lastListSize = 0;

    public CommandMoveToGoal(WorkerEntity mob, double speed) {
        this.mob   = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // only run this goal if the worker is holding a book & quill
        if (mob.getMainHandItem().getItem() != Items.WRITABLE_BOOK) return false;
        if (mob.isInteracting()) return false;
        boolean ok = parseEntries();
        if (ok) lastListSize = entries.size();
        return ok;
    }

    @Override
    public boolean canContinueToUse() {
        // drop out immediately if they let go of the book & quill
        if (mob.getMainHandItem().getItem() != Items.WRITABLE_BOOK) return false;
        if (mob.isInteracting()) return false;
        // also re-parse and check for listData changes
        boolean ok = parseEntries();
        if (!ok || entries.size() != lastListSize) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        currentIndex = 0;
        waitTicks    = 0;
        stage        = Stage.GO_TO_TARGET;
        navigateToCurrent();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        entries.clear();
    }

    @Override
    public void tick() {
        if (mob.isInteracting()) {
            mob.getNavigation().stop();
            return;
        }
        if (entries.isEmpty()) return;
        TargetEntry te = entries.get(currentIndex);

        double dist2 = mob.position()
                        .distanceToSqr(te.pos.getX() + 0.5,
                                    te.pos.getY() + 0.5,
                                    te.pos.getZ() + 0.5);
        switch(stage) {
            case GO_TO_TARGET:
                if (dist2 <= 5.0) {
                    mob.getNavigation().stop();

                    if (!mob.level().isClientSide) {
                        mob.swing(InteractionHand.MAIN_HAND, true); // This isn't working for some reason
                        //System.out.println(">> SERVER: swung arm and broadcasted entity event");
                    }
                    
                    if ("extract".equalsIgnoreCase(te.mode)) {
                        extractFromContainer(te);
                    } else {
                        insertToContainer(te);
                    }

                    stage     = Stage.WAIT_AFTER_OP;
                    waitTicks = 0;
                }
                break;

            case WAIT_AFTER_OP:
                if (++waitTicks >= 20) {
                    currentIndex = (currentIndex + 1) % entries.size();
                    navigateToCurrent();
                    stage = Stage.GO_TO_TARGET;
                }
                break;
        }
    }

    private void navigateToCurrent() {
        TargetEntry te = entries.get(currentIndex);
        mob.getNavigation()
           .moveTo(te.pos.getX() + 0.5,
                   te.pos.getY() + 0.5,
                   te.pos.getZ() + 0.5,
                   speed);
    }

    private boolean parseEntries() {
        entries.clear();
        CompoundTag data = mob.getCompoundData();
        if (data == null) return false;

        ListTag listTag = data.getList("listData", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag eTag = listTag.getCompound(i);
            try {
                // position
                String[] p = eTag.getString("position").split(",");
                if (p.length != 3) throw new IllegalArgumentException();
                int x = Integer.parseInt(p[0].trim());
                int y = Integer.parseInt(p[1].trim());
                int z = Integer.parseInt(p[2].trim());
                BlockPos pos = new BlockPos(x, y, z);

                // mode
                String mode = eTag.getString("mode");
                if (!mode.equalsIgnoreCase("insert") &&
                    !mode.equalsIgnoreCase("extract")) {
                    mode = "insert";
                }

                // direction
                Direction dir = Direction.byName(
                                  eTag.getString("direction").toLowerCase()
                                );
                if (dir == null) dir = Direction.UP;

                // stack
                ItemStack stack = ItemStack.of(eTag.getCompound("stack"));
                //if (stack.isEmpty()) throw new IllegalArgumentException();

                entries.add(new TargetEntry(pos, stack, mode, dir));
            } catch (Exception ex) {
                // skip malformed entry
            }
        }
        return !entries.isEmpty();
    }

    // pull items out of the block into the worker
    private void extractFromContainer(TargetEntry te) {
        Level level = mob.level();
        BlockEntity be = level.getBlockEntity(te.pos);
        if (be == null) return;

        IItemHandler blockInv = be.getCapability(ForgeCapabilities.ITEM_HANDLER, te.dir)
                                .resolve().orElse(null);
        if (blockInv == null) return;

        IItemHandler workerInv = mob.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                                    .resolve().orElse(null);
        if (workerInv == null) return;

        boolean wildcard = te.stack.isEmpty();
        for (int i = 0; i < blockInv.getSlots(); i++) {
            ItemStack in = blockInv.getStackInSlot(i);
            if (in.isEmpty()) continue;
            if (!wildcard && in.getItem() != te.stack.getItem()) continue;

            // take the entire slot
            ItemStack extracted = blockInv.extractItem(i, in.getCount(), false);
            if (extracted.isEmpty()) continue;

            // now insert into hotbar only (slots 0–8)
            int remain = extracted.getCount();
            for (int s = 0; s < 9 && remain > 0; s++) {
                ItemStack toInsert = new ItemStack(extracted.getItem(), remain);
                ItemStack leftover = workerInv.insertItem(s, toInsert, false);
                remain = leftover.getCount();
            }
            // if some items couldn't fit, put them back in the container
            if (remain > 0) {
                blockInv.insertItem(i, new ItemStack(extracted.getItem(), remain), false);
            }
        }
    }


    // push items from the worker into the block
    private void insertToContainer(TargetEntry te) {
        Level level = mob.level();
        BlockEntity be = level.getBlockEntity(te.pos);
        if (be == null) return;

        IItemHandler blockInv = be.getCapability(ForgeCapabilities.ITEM_HANDLER, te.dir)
                                .resolve().orElse(null);
        if (blockInv == null) return;

        IItemHandler workerInv = mob.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                                    .resolve().orElse(null);
        if (workerInv == null) return;

        boolean wildcard = te.stack.isEmpty();
        // only iterate the first 9 slots of the worker
        for (int i = 0; i < 9; i++) {
            ItemStack in = workerInv.getStackInSlot(i);
            if (in.isEmpty()) continue;
            if (!wildcard && in.getItem() != te.stack.getItem()) continue;

            // remove the entire stack from slot i
            ItemStack toMove = workerInv.extractItem(i, in.getCount(), false);
            if (toMove.isEmpty()) continue;

            // try to insert into the container
            ItemStack leftover = ItemHandlerHelper.insertItem(blockInv, toMove, false);

            // put any leftover back into the same worker slot
            if (!leftover.isEmpty()) {
                workerInv.insertItem(i, leftover, false);
            }
        }
    }

    // small holder for each configured target
    private static class TargetEntry {
        final BlockPos   pos;
        final ItemStack  stack;
        final String     mode;
        final Direction  dir;

        TargetEntry(BlockPos pos, ItemStack stack, String mode, Direction dir) {
            this.pos   = pos;
            this.stack = stack;
            this.mode  = mode;
            this.dir   = dir;
        }
    }
}