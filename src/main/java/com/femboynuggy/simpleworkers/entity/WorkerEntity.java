package com.femboynuggy.simpleworkers.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.femboynuggy.simpleworkers.client.ClientSetup;
import com.femboynuggy.simpleworkers.entity.goal.CommandMoveToGoal;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class WorkerEntity extends PathfinderMob {
    // renamed from DATA_GHOST_ITEM → DATA_TARGET_ITEM
    private static final EntityDataAccessor<ItemStack> DATA_TARGET_ITEM =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> DATA_STARTPOS =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ENDPOS =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BODY =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OUTFIT =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EYES =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HAIR =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);

    // our 15-slot item handler: 0–8 main, 9–12 armor, 13 tool, 14 shield
    private final ItemStackHandler inventory = new ItemStackHandler(15) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
    
            // Only on the server do we actually change the mob’s equipment:
            if (level().isClientSide) return;
    
            ItemStack stack = this.getStackInSlot(slot);
            switch(slot) {
            case 9:  // armor FEET
                WorkerEntity.this.setItemSlot(EquipmentSlot.FEET, stack);
                break;
            case 10: // armor LEGS
                WorkerEntity.this.setItemSlot(EquipmentSlot.LEGS, stack);
                break;
            case 11: // armor CHEST
                WorkerEntity.this.setItemSlot(EquipmentSlot.CHEST, stack);
                break;
            case 12: // armor HEAD
                WorkerEntity.this.setItemSlot(EquipmentSlot.HEAD, stack);
                break;
            case 13: // tool in main hand
                WorkerEntity.this.setItemSlot(EquipmentSlot.MAINHAND, stack);
                break;
            case 14: // shield in off hand
                WorkerEntity.this.setItemSlot(EquipmentSlot.OFFHAND, stack);
                break;
            default:
                // slots 0–8 are your “backpack”, do nothing
            }
        }
    };
    private final LazyOptional<IItemHandler> invCap = LazyOptional.of(() -> inventory);

    public WorkerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.moveControl = new MoveControl(this);
        this.navigation  = new GroundPathNavigation(this, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TARGET_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_STARTPOS, "");
        this.entityData.define(DATA_ENDPOS, "");

        this.entityData.define(DATA_TEXTURE, 0);

        this.entityData.define(DATA_BODY,   0);
        this.entityData.define(DATA_OUTFIT, 0);
        this.entityData.define(DATA_EYES,   0);
        this.entityData.define(DATA_HAIR,   0);

        // DEBUG:
        System.out.println("[DEBUG] WorkerEntity.defineSynchedData() called on " +
           (this.level().isClientSide ? "CLIENT" : "SERVER"));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag ) {
        // pick random skins only on server side when first spawning:
        if (!this.level().isClientSide) {
            int bodies  = ClientSetup.BODIES.length;
            int outfits = ClientSetup.OUTFITS.length;
            int eyes    = ClientSetup.EYES.length;
            int hair    = ClientSetup.HAIR.length;

            this.entityData.set(DATA_BODY,   random.nextInt(bodies));
            this.entityData.set(DATA_OUTFIT, random.nextInt(outfits));
            this.entityData.set(DATA_EYES,   random.nextInt(eyes));
            this.entityData.set(DATA_HAIR,   random.nextInt(hair));
        }
        return super.finalizeSpawn(world, difficulty, reason, data, tag);
    }

    // getters for the four indices:
    public int  getBodyIndex()   { return this.entityData.get(DATA_BODY);   }
    public int  getOutfitIndex() { return this.entityData.get(DATA_OUTFIT); }
    public int  getEyesIndex()   { return this.entityData.get(DATA_EYES);   }
    public int  getHairIndex()   { return this.entityData.get(DATA_HAIR);   }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        //this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0)); // TODO Might re-implement this later, but for now it's just annoying
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new CommandMoveToGoal(this, 1.0D));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    public int getTextureIndex() {
        return this.entityData.get(DATA_TEXTURE);
    }

    // target-item getters/setters
    public void setTargetItem(ItemStack s) {
        entityData.set(DATA_TARGET_ITEM, s);
    }
    public ItemStack getTargetItem() {
        return entityData.get(DATA_TARGET_ITEM);
    }

    // startPosition getters/setters
    public void setStartPosition(String cmd) {
        entityData.set(DATA_STARTPOS, cmd);
    }
    public String getStartPosition() {
        return entityData.get(DATA_STARTPOS);
    }
    // EndPosition getters/setters
    public void setEndPosition(String cmd) {
        entityData.set(DATA_ENDPOS, cmd);
    }
    public String getEndPosition() {
        return entityData.get(DATA_ENDPOS);
    }

    // expose our inventory capability
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
        @Nonnull Capability<T> cap, @Nullable Direction side
    ) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return invCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invCap.invalidate();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // target-item & command
        tag.put("TargetItem", getTargetItem().save(new CompoundTag()));
        tag.putString("StartPos", getStartPosition());
        tag.putString("EndPos", getEndPosition());
        // full inventory
        tag.put("WorkerInv", inventory.serializeNBT());

        tag.putInt("BodyIndex",   getBodyIndex());
        tag.putInt("OutfitIndex", getOutfitIndex());
        tag.putInt("EyesIndex",   getEyesIndex());
        tag.putInt("HairIndex",   getHairIndex());

        System.out.println("[DEBUG] addAdditionalSaveData: cmd=" + getStartPosition());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // target-item & command
        setTargetItem(ItemStack.of(tag.getCompound("TargetItem")));
        setStartPosition(tag.getString("StartPos"));
        setEndPosition(tag.getString("EndPos"));
        // full inventory
        if (tag.contains("WorkerInv")) {
            inventory.deserializeNBT(tag.getCompound("WorkerInv"));
        }
        if (tag.contains("BodyIndex")) {
            this.entityData.set(DATA_BODY,   tag.getInt("BodyIndex"));
            this.entityData.set(DATA_OUTFIT, tag.getInt("OutfitIndex"));
            this.entityData.set(DATA_EYES,   tag.getInt("EyesIndex"));
            this.entityData.set(DATA_HAIR,   tag.getInt("HairIndex"));
        }

        System.out.println("[DEBUG] readAdditionalSaveData: cmd=" + getStartPosition());
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // first let the vanilla mob drop its equipment, XP, etc.
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        // 1) drop the “target” item if any
        ItemStack target = this.getTargetItem();
        if (!target.isEmpty()) {
            this.spawnAtLocation(target.copy());
        }

        // 2) drop everything in our 15-slot handler (or just slots 0–8 if you only
        //    want the “backpack” and leave armor/tools to vanilla)
        this.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
            // if you want ONLY slots 0–8 (backpack), loop i<9; here we drop all 15
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack.copy());
                }
            }
        });
    }
}