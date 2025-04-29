package com.femboynuggy.simpleworkers.client;

import com.femboynuggy.simpleworkers.SimpleWorkers;
import com.femboynuggy.simpleworkers.container.WorkerContainer;
import com.femboynuggy.simpleworkers.entity.WorkerEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = SimpleWorkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForgeEvents {
    @SubscribeEvent
    public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getTarget() instanceof WorkerEntity worker)) return;
    
        ServerPlayer player = (ServerPlayer)event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
  
        // This is the magic overload: we give it our MenuType and
        // a lambda that writes *exactly* the data our factory expects.
        NetworkHooks.openScreen(
            player,
            new SimpleMenuProvider(
                (windowId, inv, _player) -> {
                return new WorkerContainer(windowId, inv, event.getTarget().getId());
                },
                Component.literal("Worker")
            ),
            (FriendlyByteBuf buf) -> buf.writeInt(worker.getId())
        );
    }
}