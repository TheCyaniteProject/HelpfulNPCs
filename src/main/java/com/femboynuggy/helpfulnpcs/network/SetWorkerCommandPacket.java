package com.femboynuggy.helpfulnpcs.network;

import java.util.function.Supplier;

import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class SetWorkerCommandPacket {
    private final int entityId;
    private final CompoundTag data;

    // Called on the client when you create & send it:
    public SetWorkerCommandPacket(int entityId, CompoundTag data) {
        this.entityId = entityId;
        this.data = data;
    }

    // Called on either side when reconstructing from the wire:
    public SetWorkerCommandPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.data = buf.readNbt();
    }

    // Called on either side when serializing to the wire:
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeNbt(data);
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
            worker.setCompoundData(msg.data);

            // persist immediately:
            worker.setPersistenceRequired();            // marks entity dirty
            level.getChunkAt(worker.blockPosition()).setUnsaved(true);

            // DEBUG to the player
            // Minecraft.getInstance().player.sendSystemMessage(Component.literal("Saved worker → cmd=" + msg.startPosition + "  item=" + msg.targetStack.getDisplayName().getString()));
        });
        ctx.get().setPacketHandled(true);
    }
}