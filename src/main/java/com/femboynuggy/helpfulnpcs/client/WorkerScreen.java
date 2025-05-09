package com.femboynuggy.helpfulnpcs.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.container.WorkerContainer;
import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;
import com.femboynuggy.helpfulnpcs.network.SetWorkerCommandPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WorkerScreen extends AbstractContainerScreen<WorkerContainer> {
    private static final ResourceLocation GUI_TEXTURE =
        new ResourceLocation(HelpfulNPCs.MODID, "textures/gui/worker_gui.png");
    
    private static final ResourceLocation SECOND_GUI_TEXTURE =
        new ResourceLocation(HelpfulNPCs.MODID, "textures/gui/secondary_worker_gui.png");
    private static final int WIDTH  = 200;
    private static final int HEIGHT = 255;

    // private EditBox inputField;
    // private EditBox inputField2;

    // New constants for second window
    private static final int SECOND_WINDOW_WIDTH = 180; // Increased width to fit item slot and buttons
    private static final int SECOND_WINDOW_HEIGHT = HEIGHT;

    // Scrollable button list variables
    private int scrollOffset = 0;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 2;
    private static final int VISIBLE_BUTTONS = (SECOND_WINDOW_HEIGHT - 20) / (BUTTON_HEIGHT * 2 + BUTTON_SPACING + 5); // Fit full height with padding

    private boolean lastHoldingBook = false;
    private List<ListEntry> listEntries = new ArrayList<>();
    private List<EntryData> listData = new ArrayList<>();

    private class ListEntry {
        int index;
        int x;
        int y;
        EntryData data;
        ItemButton itemSelectButton;
        Button modeButton;
        Button directionButton;
        Button closeButton;
        EditBox inputField;

        private static final List<String> DIRECTIONS = Arrays.asList(
            "up", "down", "north", "south", "east", "west"
        );
        public static String nextDirection(String current) {
            int idx = DIRECTIONS.indexOf(current);
            if (idx < 0) {
                // not found: default to first
                return DIRECTIONS.get(0);
            }
            // cycle to the next index (wraps around automatically)
            int nextIdx = (idx + 1) % DIRECTIONS.size();
            return DIRECTIONS.get(nextIdx);
        }

        public EntryData getData() {
            return this.data;
        }

        public void savePosition() {
            this.data.position = inputField.getValue();
        }

        public ListEntry(int index, int x, int y, WorkerContainer menu, EntryData data, WorkerScreen screen) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.data = data;


            itemSelectButton = ItemButton.builder(Component.literal(""), btn -> {
                //itemSelectButton.setStack(new ItemStack(Items.WRITABLE_BOOK, 1));
                ItemStack cursor = menu.getCarried();
                itemSelectButton.setStack(cursor.copy());
                // Minecraft.getInstance().player.sendSystemMessage(itemSelectButton.stack.getHoverName());
                data.stack = itemSelectButton.stack;
            }).bounds(x, y, 20, BUTTON_HEIGHT).build();
            itemSelectButton.stack = data.stack;

            modeButton = Button.builder(Component.literal(data.mode.toUpperCase()), btn -> {
                data.mode = (data.mode == "insert")? "extract" : "insert";
                modeButton.setMessage(Component.literal(data.mode.toUpperCase()));
            }).bounds(x + 24, y, 50, BUTTON_HEIGHT).build();

            directionButton = Button.builder(Component.literal(data.direction.toUpperCase()), btn -> {
                data.direction = nextDirection(data.direction);

                directionButton.setMessage(Component.literal(data.direction.toUpperCase()));
            }).bounds(x + 100, y, 50, BUTTON_HEIGHT).build();

            closeButton = Button.builder(Component.literal("X"), btn -> {
                screen.listData.remove(data);
                screen.generateFields();
            }).bounds(x + 134, y, 20, BUTTON_HEIGHT).build();

            inputField = new EditBox(WorkerScreen.this.font, x, y + BUTTON_HEIGHT + 5, SECOND_WINDOW_WIDTH - 30, BUTTON_HEIGHT, Component.literal("Input " + (index + 1)));
            inputField.setMaxLength(100);
            inputField.setBordered(true);
            inputField.setValue(data.position);
        }

        public void addWidgets() {
            if (itemSelectButton != null) WorkerScreen.this.addRenderableWidget(itemSelectButton);
            if (modeButton != null) WorkerScreen.this.addRenderableWidget(modeButton);
            if (directionButton != null) WorkerScreen.this.addRenderableWidget(directionButton);
            if (closeButton != null) WorkerScreen.this.addRenderableWidget(closeButton);
            if (inputField != null) WorkerScreen.this.addRenderableWidget(inputField);
            // Add item slot widget here if needed (handled by container)
        }

        public void removeWidgets() {
            if (itemSelectButton != null) WorkerScreen.this.removeWidget(itemSelectButton);
            if (modeButton != null) WorkerScreen.this.removeWidget(modeButton);
            if (directionButton != null) WorkerScreen.this.removeWidget(directionButton);
            if (closeButton != null) WorkerScreen.this.removeWidget(closeButton);
            if (inputField != null) WorkerScreen.this.removeWidget(inputField);
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
            itemSelectButton.setX(x);
            itemSelectButton.setY(y);
            modeButton.setX(x + 25);
            modeButton.setY(y);
            directionButton.setX(x + 75);
            directionButton.setY(y);
            closeButton.setX(x + 130);
            closeButton.setY(y);
            inputField.setX(x);
            inputField.setY(y + BUTTON_HEIGHT + 5);
        }

        public void setVisible(boolean visible) {
            itemSelectButton.visible = visible;
            modeButton.visible = visible;
            directionButton.visible = visible;
            closeButton.visible = visible;
            inputField.visible = visible;
        }
    }

    private class EntryData {
        public ItemStack stack;
        public String mode = "insert";
        public String direction = "up";
        public String position = "x,y,z";

        public EntryData(ItemStack stack, String mode, String direction, String position) {
            this.stack = stack;
            this.mode = mode;
            this.direction = direction;
            this.position = position;
        }
    }

    int x0 = this.leftPos;
    int y0 = this.topPos;

    public WorkerScreen(WorkerContainer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        if (isHoldingBook()) {
            this.imageWidth  = WIDTH + SECOND_WINDOW_WIDTH; // Increase width to fit second window
        } else {
            this.imageWidth = WIDTH;
        }
        this.imageHeight = HEIGHT;
        lastHoldingBook = isHoldingBook();
        menu.worker.setInteracting(true);
    }

    private Button addField;

    @Override
    protected void init() {
        super.init();
        x0 = this.leftPos;
        y0 = this.topPos;

        addField = Button.builder(Component.literal("+"), btn -> {
            ItemStack stack = new ItemStack(Items.AIR, 1);
            EntryData data = new EntryData(stack, "insert", "up", "x,y,z");
            listData.add(data);
            generateFields();
        }).bounds(x0 + WIDTH - (BUTTON_HEIGHT + 5), y0 + 5, BUTTON_HEIGHT, BUTTON_HEIGHT).build();
        

        loadCombinedTag(this.menu.worker.getCompoundData());
        generateFields();
    }

    public void generateFields() {
        // Remove old list entries widgets
        for (ListEntry entry : listEntries) {
            entry.removeWidgets();
        }
        listEntries.clear();

        if (isHoldingBook()) {
            this.addRenderableWidget(addField);
            int baseX = x0 + WIDTH + 10;
            int baseY = y0 + 10;

            for (int i = 0; i < listData.size(); i++) {
                int entryY = baseY + i * ((BUTTON_HEIGHT * 2) + BUTTON_SPACING + 5);
                ListEntry entry = new ListEntry(i, baseX, entryY, menu, listData.get(i), this);
                entry.addWidgets();
                listEntries.add(entry);
            }
        }
        else {
            this.removeWidget(addField);
        }
        updateButtonPositions();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        boolean currentlyHoldingBook = isHoldingBook();
        if (currentlyHoldingBook != lastHoldingBook) {
            lastHoldingBook = currentlyHoldingBook;
            if (currentlyHoldingBook) {
                this.imageWidth = WIDTH + SECOND_WINDOW_WIDTH;
            } else {
                this.imageWidth = WIDTH;

                // Remove list entries widgets when hiding second window
                for (ListEntry entry : listEntries) {
                    entry.removeWidgets();
                }
                listEntries.clear();
            }
            // Reinitialize widgets to update UI
            this.init();
        }
    }

    private boolean isHoldingBook() {
        return this.menu.worker.getMainHandItem().is(net.minecraft.world.item.Items.WRITABLE_BOOK);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // NO-OP: don't draw title or "Inventory"
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        int left = (this.width - (isHoldingBook() ? WIDTH + SECOND_WINDOW_WIDTH : WIDTH)) / 2;
        int top = (this.height - HEIGHT) / 2;
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        // Draw first window background
        gui.blit(GUI_TEXTURE,
            left, top,
            0,
            0, 0,
            WIDTH, HEIGHT,
            WIDTH, HEIGHT
        );
        // Draw second window background to the right only if holding book & quill
        if (isHoldingBook()) {
            gui.blit(SECOND_GUI_TEXTURE,
                left + WIDTH, top,
                0,
                0, 0,
                SECOND_WINDOW_WIDTH, SECOND_WINDOW_HEIGHT,
                SECOND_WINDOW_WIDTH, SECOND_WINDOW_HEIGHT
            );
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        super.render(gui, mouseX, mouseY, partialTicks);

        // Draw scroll bar if second window visible
        if (isHoldingBook()) {
            int left = (this.width - (WIDTH + SECOND_WINDOW_WIDTH)) / 2;
            int top = (this.height - HEIGHT) / 2;

            int scrollBarX = left + WIDTH + SECOND_WINDOW_WIDTH - 10;
            int scrollBarY = top + 10;
            int scrollBarHeight = SECOND_WINDOW_HEIGHT - 20;

            int maxScroll = (BUTTON_HEIGHT * 2 + BUTTON_SPACING + 5) * (listData.size() + 1) - scrollBarHeight;
            if (maxScroll < 1) maxScroll = 1; // prevent division by zero

            // Clamp scrollOffset to maxScroll to prevent overscroll
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            if (scrollOffset < 0) scrollOffset = 0;

            int thumbHeight = Math.max(scrollBarHeight * scrollBarHeight / ((BUTTON_HEIGHT * 2 + BUTTON_SPACING + 5) * ((listData.size() + 1) >= 5? (listData.size() + 1) : 5)), 10);
            int thumbY = scrollBarY + (int)((float)scrollOffset / maxScroll * (scrollBarHeight - thumbHeight));

            // Draw scroll bar background (gray)
            gui.fill(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, 0xFF555555);
            // Draw scroll bar thumb (lighter gray)
            gui.fill(scrollBarX, thumbY, scrollBarX + 6, thumbY + thumbHeight, 0xFFAAAAAA);
        }
        else {
            this.removeWidget(addField);
        }

        this.renderTooltip(gui, mouseX, mouseY);
    }

    private void updateButtonPositions() {
        int x0 = this.leftPos;
        int baseX = x0 + WIDTH + 10;
        int baseY = this.topPos + 10;

        for (int i = 0; i < listEntries.size(); i++) {
            ListEntry entry = listEntries.get(i);
            int entryY = baseY + i * ((BUTTON_HEIGHT * 2) + BUTTON_SPACING + 5) - scrollOffset;
            entry.setPosition(baseX, entryY);
            boolean visible = entryY + BUTTON_HEIGHT * 2 > baseY && entryY < baseY + VISIBLE_BUTTONS * ((BUTTON_HEIGHT * 2) + BUTTON_SPACING + 5);
            entry.setVisible(visible);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = (BUTTON_HEIGHT * 2 + BUTTON_SPACING + 5) * listData.size() - (SECOND_WINDOW_HEIGHT - 20);
        scrollOffset -= delta * (BUTTON_HEIGHT * 2 + BUTTON_SPACING + 5);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        updateButtonPositions();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {

        for (ListEntry entry : listEntries) {
            entry.savePosition();
        }

        HelpfulNPCs.CHANNEL.sendToServer(
            new SetWorkerCommandPacket(
                menu.worker.getId(),
                getCombinedTag()
            )
        );

        menu.worker.setInteracting(false);

        super.onClose();
    }

    public CompoundTag getCombinedTag() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (EntryData entry : this.listData) {
            CompoundTag eTag = new CompoundTag();
            // write the ItemStack under “stack”
            eTag.put("stack", entry.stack.save(new CompoundTag()));
            // write the three strings
            eTag.putString("mode",      entry.mode);
            eTag.putString("direction", entry.direction);
            eTag.putString("position",  entry.position);
            listTag.add(eTag);
        }
        tag.put("listData", listTag);
        return tag;
    }

    public void loadCombinedTag(CompoundTag tag) {
        this.listData.clear();
    
        ListTag listTag = tag.getList("listData", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag eTag = listTag.getCompound(i);
            // read back the ItemStack
            ItemStack stack = ItemStack.of(eTag.getCompound("stack"));
            // read back the strings
            String mode      = eTag.getString("mode");
            String direction = eTag.getString("direction");
            String position  = eTag.getString("position");
            // re-create your entry
            this.listData.add(new EntryData(stack, mode, direction, position));
        }
    }
}