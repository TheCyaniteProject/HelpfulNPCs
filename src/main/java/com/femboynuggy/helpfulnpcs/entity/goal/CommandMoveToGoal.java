package com.femboynuggy.helpfulnpcs.entity.goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandMoveToGoal extends Goal {
    private final WorkerEntity mob;
    private final double speed;
    private Path path;  
    private int recalcCooldown = 0;
    private double stoppingDistance = 3.0;

    enum Stage { GO_TO_TARGET, WAIT_AFTER_OP }
    private Stage stage = Stage.GO_TO_TARGET;
    private final List<TargetEntry> entries = new ArrayList<>();
    private int currentIndex = 0;
    private int waitTicks    = 0;
    private int lastListSize = 0;

    public CommandMoveToGoal(WorkerEntity mob, double speed) {
        this.mob   = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
        if (!(mob.getNavigation() instanceof GroundPathNavigation))
            throw new IllegalArgumentException("Must have GroundPathNavigation");
    }

    @Override
    public boolean canUse() {
        if (mob.getMainHandItem().getItem() != Items.WRITABLE_BOOK) return false;
        if (mob.isInteracting()) return false;
        if (!parseEntries()) return false;
        lastListSize = entries.size();
        currentIndex = 0;
        stage = Stage.GO_TO_TARGET;
        waitTicks = 0;
        // force an immediate path calculation
        recalcCooldown = 0;
        computePathToCurrent();
        return path != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getMainHandItem().getItem() != Items.WRITABLE_BOOK) return false;
        if (mob.isInteracting()) return false;
        if (!parseEntries() || entries.size() != lastListSize) return false;
        return true;
    }

    @Override
    public void start() {
        // nothing more to do; we've already computed path in canUse()
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        entries.clear();
    }

    @Override
    public void tick() {
        if (entries.isEmpty()) return;
        TargetEntry te = entries.get(currentIndex);

        // look at the block
        mob.getLookControl().setLookAt(
            te.pos.getX()+0.5, te.pos.getY()+0.5, te.pos.getZ()+0.5,
            30, 30
        );

        // if we've lost our path or it’s done, or we’ve been on it too long, recompute
        if (mob.getNavigation().isDone() 
            || mob.getNavigation().isStuck() 
            || --recalcCooldown <= 0) {
            computePathToCurrent();
        }

        // now the nav will automatically follow 'path' around obstacles
        // check arrival
        double dist2 = mob.position().distanceToSqr(
            te.pos.getX()+0.5, te.pos.getY()+0.5, te.pos.getZ()+0.5
        );

        if (stage == Stage.GO_TO_TARGET && dist2 <= stoppingDistance) {
            mob.getNavigation().stop();

            mob.swing(InteractionHand.MAIN_HAND);

            if ("extract".equalsIgnoreCase(te.mode)) {
                extractFromContainer(te);
            } else {
                insertToContainer(te);
            }

            stage = Stage.WAIT_AFTER_OP;
            waitTicks = 0;
            return;
        }

        if (stage == Stage.WAIT_AFTER_OP && ++waitTicks >= 20) {
            currentIndex = (currentIndex + 1) % entries.size();
            stage = Stage.GO_TO_TARGET;
            // force a fresh path for the new target
            computePathToCurrent();
            waitTicks = 0;
        }
    }

    /** Builds a fresh A* path to entries.get(currentIndex) and starts following it. */
    private void computePathToCurrent() {
        TargetEntry te = entries.get(currentIndex);
        // use a small “fudge” parameter so we can step up blocks if needed
        this.path = mob.getNavigation().createPath(te.pos, 0);
        if (path != null) {
            mob.getNavigation().moveTo(path, speed);
            recalcCooldown = 40 + mob.getRandom().nextInt(40);
        }
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