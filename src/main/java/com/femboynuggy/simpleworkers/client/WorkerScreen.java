package com.femboynuggy.simpleworkers.client;

import com.femboynuggy.simpleworkers.SimpleWorkers;
import com.femboynuggy.simpleworkers.container.WorkerContainer;
import com.femboynuggy.simpleworkers.network.SetWorkerCommandPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class WorkerScreen extends AbstractContainerScreen<WorkerContainer> {
    private static final int WIDTH  = 200;
    private static final int HEIGHT = 250;

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
          y0 + 104,        // 70px down from top edge
          152, 20,
          Component.literal("StartPos")
        );
        inputField2 = new EditBox(
          this.font,
          x0 + 33,        // 10px from left edge of GUI
          y0 + 124,        // 70px down from top edge
          152, 20,
          Component.literal("EndPos")
        );
        
        inputField.setMaxLength(100);
        inputField2.setMaxLength(100);
        // seed from the container’s command (which was read from the entity on open)
        inputField.setValue(this.menu.worker.getStartPosition());
        inputField2.setValue(this.menu.worker.getEndPosition());
        this.addRenderableWidget(inputField);
        this.addRenderableWidget(inputField2);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // Hey GPT, don't touch this, I like having the window in the middle of the screen
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        // draw black background
        gui.fill(left, top, right, bottom, 0xB3000000);

        // solid black full‐screen background
        //gui.fill(0, 0, this.width, this.height, 0xFF000000);
        // draw all slots, items & buttons
        super.render(gui, mouseX, mouseY, partialTicks);
        // tooltips if you hover items
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        // no GUI texture — slots are drawn for us by super.render()
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
        SimpleWorkers.CHANNEL.sendToServer(
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