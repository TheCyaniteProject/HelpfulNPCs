package com.femboynuggy.helpfulnpcs.client;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.container.WorkerContainer;
import com.femboynuggy.helpfulnpcs.network.SetWorkerCommandPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class WorkerScreen extends AbstractContainerScreen<WorkerContainer> {
    private static final ResourceLocation GUI_TEXTURE =
        new ResourceLocation(HelpfulNPCs.MODID, "textures/gui/worker_gui.png");
    private static final int WIDTH  = 200;
    private static final int HEIGHT = 255;

    private EditBox inputField;
    private EditBox inputField2;

    public WorkerScreen(WorkerContainer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // leftPos and topPos are set by super.init():
        int x0 = this.leftPos;
        int y0 = this.topPos;

        // reposition every slot in the container
        // (slots were added in WorkerContainer with x/y relative to GUI)
        // for (var slot : this.menu.slots) {
        //     slot.x += x0;
        //     slot.y += y0;
        // }

        // --- Text entry box ---
        inputField = new EditBox(
          this.font,
          x0 + 33,        // 10px from left edge of GUI
          y0 + 110,        // 70px down from top edge
          152, 20,
          Component.literal("StartPos")
        );
        inputField2 = new EditBox(
          this.font,
          x0 + 33,        // 10px from left edge of GUI
          y0 + 130,        // 70px down from top edge
          152, 20,
          Component.literal("EndPos")
        );
        
        inputField.setMaxLength(100);
        inputField2.setMaxLength(100);
        inputField.setBordered(false);
        inputField2.setBordered(false);
        // seed from the container’s command (which was read from the entity on open)
        inputField.setValue(this.menu.worker.getStartPosition());
        inputField2.setValue(this.menu.worker.getEndPosition());
        this.addRenderableWidget(inputField);
        this.addRenderableWidget(inputField2);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        super.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        // draw it at the top-left of our GUI window
        gui.blit(GUI_TEXTURE,
            left, top,
            0,
            0,0,
            WIDTH, HEIGHT,  // width, height to draw
            WIDTH, HEIGHT  // width, height to draw
        );
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // no-op: don't draw any labels
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // 1) send your debug message
        Minecraft.getInstance()
                 .player
                 .sendSystemMessage(Component.literal("Saved."));

        // 2) gather your data
        ItemStack target = this.menu.getSlot(0).getItem();
        String startPos = inputField.getValue();
        String endPos   = inputField2.getValue();

        // 3) send your packet
        HelpfulNPCs.CHANNEL.sendToServer(
            new SetWorkerCommandPacket(
                menu.worker.getId(),
                startPos,
                endPos,
                target
            )
        );

        // 4) now actually close the screen
        super.onClose();
    }
}