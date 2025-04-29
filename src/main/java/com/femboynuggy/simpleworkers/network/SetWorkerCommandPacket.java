package com.femboynuggy.simpleworkers.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.femboynuggy.simpleworkers.entity.WorkerEntity;

import java.util.function.Supplier;

public class SetWorkerCommandPacket {
    private final int entityId;
    private final String startPosition;
    private final String endPosition;
    private final ItemStack targetStack;

    // Called on the client when you create & send it:
    public SetWorkerCommandPacket(int entityId, String startPosition, String endPosition, ItemStack targetStack) {
        this.entityId = entityId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.targetStack = targetStack.copy();
    }

    // Called on either side when reconstructing from the wire:
    public SetWorkerCommandPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.startPosition = buf.readUtf(100);
        this.endPosition = buf.readUtf(100);
        this.targetStack = buf.readItem();
    }

    // Called on either side when serializing to the wire:
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(startPosition, 100);
        buf.writeUtf(endPosition, 100);
        buf.writeItem(targetStack);
    }

    // Called on the server when the packet arrives:
    public static void handle(SetWorkerCommandPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;  // must be server‐side

            var level = sender.level();
            var e = level.getEntity(msg.entityId);
            if (!(e instanceof WorkerEntity worker)) return;

            // store command & target into the entity’s SynchedEntityData
            worker.setStartPosition(msg.startPosition);
            worker.setEndPosition(msg.endPosition);
            worker.setTargetItem(msg.targetStack);

            // persist immediately:
            worker.setPersistenceRequired();            // marks entity dirty
            level.getChunkAt(worker.blockPosition()).setUnsaved(true);

            // DEBUG to the player
            Minecraft.getInstance().player.sendSystemMessage(
              Component.literal("Saved worker → cmd=" + msg.startPosition + "  item=" + msg.targetStack.getDisplayName().getString()));
        });
        ctx.get().setPacketHandled(true);
    }
}