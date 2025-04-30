package com.femboynuggy.helpfulnpcs.entity.goal;

import java.util.EnumSet;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandler;

public class CommandMoveToGoal extends Goal {
  private final WorkerEntity mob;
  private final double speed;

  private enum Stage { GO_TO_START, WAIT_PULL, GO_TO_END, WAIT_DUMP }
  private Stage stage = Stage.GO_TO_START;

  private BlockPos startPos, endPos;
  private int waitTicks = 0;

  public CommandMoveToGoal(WorkerEntity mob, double speed) {
    this.mob   = mob;
    this.speed = speed;
    this.setFlags(EnumSet.of(Flag.MOVE));
  }

  /** Try to re-parse the two GUI‐set position strings. */
  private boolean parsePositions() {
    try {
      String s1 = mob.getStartPosition();
      String s2 = mob.getEndPosition();
      if (s1 == null || s1.isBlank() || s2 == null || s2.isBlank()) return false;
      String[] p1 = s1.split(","), p2 = s2.split(",");
      if (p1.length != 3 || p2.length != 3) return false;
      int sx = Integer.parseInt(p1[0].trim());
      int sy = Integer.parseInt(p1[1].trim());
      int sz = Integer.parseInt(p1[2].trim());

      int ex = Integer.parseInt(p2[0].trim());
      int ey = Integer.parseInt(p2[1].trim());
      int ez = Integer.parseInt(p2[2].trim());
      startPos = new BlockPos(
        sx < 0? sx - 1 : sx,
        sy < 0? sy - 1 : sy,
        sz < 0? sz - 1 : sz
      );
      endPos = new BlockPos(
        ex < 0? ex - 1 : ex,
        ey < 0? ey - 1 : ey,
        ez < 0? ez - 1 : ez
      );
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean canUse() {
    return parsePositions();
  }

  @Override
  public boolean canContinueToUse() {
    // continue looping as long as the positions still parse
    return parsePositions();
  }

  @Override
  public void start() {
    stage = Stage.GO_TO_START;
    waitTicks = 0;
    // issue our first journey
    Vec3 c = Vec3.atCenterOf(startPos);
    mob.getNavigation().moveTo(c.x, c.y, c.z, speed);
  }

  @Override
  public void tick() {
    // distance squared test for “within 3 blocks”
    Vec3 pos = mob.position();
    double dist2;

    switch(stage) {
      case GO_TO_START:
        dist2 = pos.distanceToSqr(Vec3.atCenterOf(startPos));
        if (dist2 <= 9.0) {
          // arrived at start!  pull items
          tryTransferFromContainer(startPos);
          stage = Stage.WAIT_PULL;
          waitTicks = 0;
        }
        break;

      case WAIT_PULL:
        if (++waitTicks >= 20) {
          // after 1s, head to endPos
          Vec3 c2 = Vec3.atCenterOf(endPos);
          mob.getNavigation().moveTo(c2.x, c2.y, c2.z, speed);
          stage = Stage.GO_TO_END;
        }
        break;

      case GO_TO_END:
        dist2 = pos.distanceToSqr(Vec3.atCenterOf(endPos));
        if (dist2 <= 9.0) {
          // arrived at end!  dump items
          tryDumpToContainer(endPos);
          stage = Stage.WAIT_DUMP;
          waitTicks = 0;
        }
        break;

      case WAIT_DUMP:
        if (++waitTicks >= 20) {
          // after 1s, re‐parse and loop back
          if (!parsePositions()) {
            mob.getNavigation().stop();
            return;
          }
          Vec3 c3 = Vec3.atCenterOf(startPos);
          mob.getNavigation().moveTo(c3.x, c3.y, c3.z, speed);
          stage = Stage.GO_TO_START;
        }
        break;
    }
  }

  @Override
  public void stop() {
    mob.getNavigation().stop();
  }

  // identical to before
  private void tryTransferFromContainer(BlockPos pos) {
    Level level = mob.level();
    BlockEntity be = level.getBlockEntity(pos);
    if (be == null) return;
    LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
    if (!cap.isPresent()) return;
    IItemHandler blockInv  = cap.orElseThrow(() -> new IllegalStateException("Worker has no item handler"));
    IItemHandler workerInv = mob.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(() -> new IllegalStateException("Worker has no item handler"));
    ItemStack target = mob.getTargetItem().copy();
    if (target.isEmpty()) return;
    target.setCount(1);
    for (int i = 0; i < blockInv.getSlots(); i++) {
      ItemStack in = blockInv.getStackInSlot(i);
      if (in.isEmpty() || in.getItem() != target.getItem()) continue;
      ItemStack extracted = blockInv.extractItem(i, in.getCount(), false);
      if (extracted.isEmpty()) continue;
      int remain = extracted.getCount();
      for (int s = 0; s < 9 && remain > 0; s++) {
        ItemStack toInsert = new ItemStack(extracted.getItem(), remain);
        ItemStack leftover  = workerInv.insertItem(s, toInsert, false);
        remain = leftover.getCount();
      }
      if (remain > 0) {
        blockInv.insertItem(i, new ItemStack(extracted.getItem(), remain), false);
      }
    }
  }

  // identical to before
  private void tryDumpToContainer(BlockPos pos) {
    Level level = mob.level();
    BlockEntity be = level.getBlockEntity(pos);
    if (be == null) return;
    LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
    if (!cap.isPresent()) return;
    IItemHandler blockInv  = cap.orElseThrow(() -> new IllegalStateException("Worker has no item handler"));
    IItemHandler workerInv = mob.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(() -> new IllegalStateException("Worker has no item handler"));
    for (int s = 0; s < 9; s++) {
      ItemStack in = workerInv.getStackInSlot(s);
      if (in.isEmpty()) continue;
      ItemStack leftover = ItemHandlerHelper.insertItem(blockInv, in.copy(), false);
      int dumped = in.getCount() - leftover.getCount();
      if (dumped > 0) {
        workerInv.extractItem(s, dumped, false);
      }
    }
  }
}