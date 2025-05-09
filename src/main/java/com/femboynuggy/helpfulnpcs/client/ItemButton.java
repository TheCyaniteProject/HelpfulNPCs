package com.femboynuggy.helpfulnpcs.client;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class ItemButton extends AbstractButton { // This is just the vanilla Button class but with a custom renderWidget() function
    public static final int SMALL_WIDTH = 120;
    public static final int DEFAULT_WIDTH = 150;
    public static final int DEFAULT_HEIGHT = 20;
    protected static final ItemButton.CreateNarration DEFAULT_NARRATION = (p_253298_) -> {
        return p_253298_.get();
    };
    protected final ItemButton.OnPress onPress;
    protected final ItemButton.CreateNarration createNarration;

    public ItemStack stack;

    public static ItemButton.Builder builder(Component p_254439_, ItemButton.OnPress p_254567_) {
        return new ItemButton.Builder(p_254439_, p_254567_);
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    protected ItemButton(int p_259075_, int p_259271_, int p_260232_, int p_260028_, Component p_259351_, ItemButton.OnPress p_260152_, ItemButton.CreateNarration p_259552_) {
        super(p_259075_, p_259271_, p_260232_, p_260028_, p_259351_);
        this.onPress = p_260152_;
        this.createNarration = p_259552_;
    }

    protected ItemButton(Builder builder) {
        this(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.createNarration);
        setTooltip(builder.tooltip); // Forge: Make use of the Builder tooltip
    }

    public void onPress() {
        this.onPress.onPress(this);
    }

    protected MutableComponent createNarrationMessage() {
        return this.createNarration.createNarrationMessage(() -> {
            return super.createNarrationMessage();
        });
    }

    public void updateWidgetNarration(NarrationElementOutput p_259196_) {
        this.defaultButtonNarrationText(p_259196_);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // First draw the normal button background
        super.renderWidget(gui, mouseX, mouseY, partialTick);

        // Now draw the item (if any)
        if (stack != null && !stack.isEmpty()) {
            // renderItem puts the item icon
            gui.renderItem(stack, this.getX() + 2, this.getY() + 2);
            // renderItemDecorations draws the stack count, damage bar, etc
            //gui.renderItemDecorations(font, stack, this.getX() + 2, this.getY() + 2);
        }
    }

    
    public static class Builder {
        private final Component message;
        private final ItemButton.OnPress onPress;
        @Nullable
        private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private ItemButton.CreateNarration createNarration = ItemButton.DEFAULT_NARRATION;

        public Builder(Component p_254097_, ItemButton.OnPress p_253761_) {
            this.message = p_254097_;
            this.onPress = p_253761_;
        }

        public ItemButton.Builder pos(int p_254538_, int p_254216_) {
            this.x = p_254538_;
            this.y = p_254216_;
            return this;
        }

        public ItemButton.Builder width(int p_254259_) {
            this.width = p_254259_;
            return this;
        }

        public ItemButton.Builder size(int p_253727_, int p_254457_) {
            this.width = p_253727_;
            this.height = p_254457_;
            return this;
        }

        public ItemButton.Builder bounds(int p_254166_, int p_253872_, int p_254522_, int p_253985_) {
            return this.pos(p_254166_, p_253872_).size(p_254522_, p_253985_);
        }

        public ItemButton.Builder tooltip(@Nullable Tooltip p_259609_) {
            this.tooltip = p_259609_;
            return this;
        }

        public ItemButton.Builder createNarration(ItemButton.CreateNarration p_253638_) {
            this.createNarration = p_253638_;
            return this;
        }

        public ItemButton build() {
            return build(ItemButton::new);
        }

        public ItemButton build(java.util.function.Function<Builder, ItemButton> builder) {
            return builder.apply(this);
        }
    }


    public interface CreateNarration {
        MutableComponent createNarrationMessage(Supplier<MutableComponent> p_253695_);
    }

    public interface OnPress {
        void onPress(ItemButton p_93751_);
    }
}