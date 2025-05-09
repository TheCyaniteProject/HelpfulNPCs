package com.femboynuggy.helpfulnpcs.item;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;
import com.femboynuggy.helpfulnpcs.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WorkerContractItem extends Item {
    public WorkerContractItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!world.isClientSide && player instanceof ServerPlayer) {
        ServerLevel sLevel = (ServerLevel) world;

        // 1) Compute a spawn Vec3 2 blocks in front of the player's eyes
        Vec3 eye   = player.getEyePosition(1.0F);
        Vec3 look  = player.getLookAngle();
        Vec3 spawnV = eye.add(look.scale(2.0));

        // 2) Create the entity
        WorkerEntity worker = ModEntities.WORKER.get().create(sLevel);
        if (worker == null) return InteractionResultHolder.fail(stack);

        // 3) Position & rotation
        worker.moveTo(spawnV.x, spawnV.y, spawnV.z,
                    player.getYRot(), player.getXRot());

        // 4) Call finalizeSpawn so that your random‐skin code in finalizeSpawn()
        //    actually runs and NBT fields get set
        worker.finalizeSpawn(
        sLevel,
        sLevel.getCurrentDifficultyAt(worker.blockPosition()),
        MobSpawnType.TRIGGERED,
        null,  // no prior SpawnGroupData
        null   // no NBTTag
        );

        // 5) Add it to the world
        sLevel.addFreshEntity(worker);

        worker.setOwnerUUID(player.getUUID());

        // 6) Consume the contract
        if (!player.isCreative()) stack.shrink(1);

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }
        return InteractionResultHolder.pass(stack);
    }
}