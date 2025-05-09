package com.femboynuggy.helpfulnpcs.entity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.femboynuggy.helpfulnpcs.client.ClientSetup;
import com.femboynuggy.helpfulnpcs.entity.goal.CommandMoveToGoal;
import com.femboynuggy.helpfulnpcs.registry.ModEntities;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.world.entity.TamableAnimal;

public class WorkerEntity extends PathfinderMob implements RangedAttackMob {
    // renamed from DATA_GHOST_ITEM → DATA_TARGET_ITEM
    private static final EntityDataAccessor<CompoundTag> DATA_COMPOUND =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UNIQUE_ID =
        SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

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

    private long lastDamageTime = Long.MIN_VALUE;

    // our 30-slot item handler: 0–8 main, 9–12 armor, 13 tool, 14 shield, 15-35 misc slots
    private final ItemStackHandler inventory = new ItemStackHandler(35) {
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

    public WorkerEntity(EntityType<? extends WorkerEntity> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.moveControl = new MoveControl(this);
        this.navigation  = new GroundPathNavigation(this, world);
    }

    public WorkerEntity(PlayMessages.SpawnEntity msg, Level world) {
    // call your “principal” constructor, passing in your registered EntityType
        this(ModEntities.WORKER.get(), world);
    }

    /** Called by setCustomClientFactory on the client side. */
    public WorkerEntity(FriendlyByteBuf buf, Level world) {
        this(ModEntities.WORKER.get(), world);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private boolean interacting = false;

    // called when you open the GUI in your onInteract or NetworkHooks.openGui callback:
    public void setInteracting(boolean b) {
        System.out.println("CLIENT: "+b);
        this.interacting = b;
    }
    public boolean isInteracting() {
        return this.interacting;
    }

    @Override
    public void handleEntityEvent(byte id) {
        System.out.println("CLIENT: WorkerEntity got entity‐event id="+id);
        super.handleEntityEvent(id);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COMPOUND, new CompoundTag());

        this.entityData.define(OWNER_UNIQUE_ID, Optional.empty());

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
        super.registerGoals();

        this.goalSelector.addGoal(0, new NaturalRegenGoal(this, 20*10, 20));
        // MELEE first:
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5D, true));
        // then follow master if we have a weapon:
        this.goalSelector.addGoal(3, new FollowMasterGoal(this, 1.5D, 5.0F));
        // then bow (if bow in hand):
        this.goalSelector.addGoal(1, new WorkerBowAttackGoal(this, 1.2D, 20, 15.0F));
        // normal idle/look goals:
        this.goalSelector.addGoal(4, new PanicGoal(this, 1.25));
        //this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0)); // TODO Might re-implement this later, but for now it's just annoying
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // your move‐to‐listData goal:
        this.goalSelector.addGoal(9, new CommandMoveToGoal(this, 1.0D));

        // DEFENSIVE target goals:
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(1, new DefendMasterHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DefendMasterHurtTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,       20.0D)
            .add(Attributes.MOVEMENT_SPEED,   0.25D)
            .add(Attributes.ATTACK_DAMAGE,    1.0D)
            .add(Attributes.FOLLOW_RANGE,     16.0D);
    }

    public boolean isAlliedWorker(LivingEntity other) {
        if (!(other instanceof WorkerEntity)) return false;
        UUID myOwner = this.getOwnerUUID();
        UUID theirOwner = ((WorkerEntity)other).getOwnerUUID();
        return myOwner != null && myOwner.equals(theirOwner);
    }

    public int getTextureIndex() {
        return this.entityData.get(DATA_TEXTURE);
    }

    // target-item getters/setters
    public void setCompoundData(CompoundTag data) {
        entityData.set(DATA_COMPOUND, data);
    }
    public CompoundTag getCompoundData() {
        return entityData.get(DATA_COMPOUND);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UNIQUE_ID, Optional.ofNullable(uuid));
    }
    
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UNIQUE_ID).orElse(null);
    }

    @Nullable
    public Player getMaster() {
        UUID id = getOwnerUUID();
        return id == null ? null : this.level().getPlayerByUUID(id);
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
        tag.put("listData", getCompoundData());
        if (getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", getOwnerUUID());
        }
        // full inventory
        tag.put("WorkerInv", inventory.serializeNBT());

        tag.putInt("BodyIndex",   getBodyIndex());
        tag.putInt("OutfitIndex", getOutfitIndex());
        tag.putInt("EyesIndex",   getEyesIndex());
        tag.putInt("HairIndex",   getHairIndex());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // target-item & command
        setCompoundData(tag.getCompound("listData"));

        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }

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
    }

    protected boolean isHoldingWeapon() {
        ItemStack main = this.getMainHandItem();
        return main.getItem() instanceof SwordItem
            || main.getItem() instanceof AxeItem
            || main.getItem() instanceof BowItem
            || main.getItem() instanceof TridentItem; // etc.
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        this.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack.copy());
                }
            }
        });
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Never allow the worker to target its owner
        if (target instanceof Player p && Objects.equals(p.getUUID(), getOwnerUUID())) {
            super.setTarget(null);
        } else {
            super.setTarget(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (!this.level().isClientSide) {
            // If the owner is the one damaging us, clear any target immediately
            Entity attacker = source.getEntity();
            if (attacker instanceof Player p && Objects.equals(p.getUUID(), getOwnerUUID())) {
                // This will also prevent any default HurtByTargetGoal from snapping on
                this.setTarget(null);
                this.setLastHurtByMob(null);
            }
            this.lastDamageTime = this.level().getGameTime();
        }
        return hit;
    }


    public class FollowMasterGoal extends Goal {
        private final WorkerEntity mob;
        private final double speed;
        private final float stopDistanceSq;
    
        public FollowMasterGoal(WorkerEntity mob, double speed, float stopDistance) {
            this.mob = mob;
            this.speed = speed;
            this.stopDistanceSq = stopDistance * stopDistance;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
    
        @Override
        public boolean canUse() {
            Player master = mob.getMaster();
            return master != null
                && mob.isHoldingWeapon()
                && mob.distanceToSqr(master) > stopDistanceSq;
        }
    
        @Override
        public boolean canContinueToUse() {
            Player master = mob.getMaster();
            return master != null
                && mob.isHoldingWeapon()
                && mob.distanceToSqr(master) > stopDistanceSq
                && mob.getNavigation().isInProgress();
        }
    
        @Override
        public void start() {
            mob.getNavigation().moveTo(mob.getMaster(), speed);
        }
    
        @Override
        public void stop() {
         mob.getNavigation().stop();
        }
    
        @Override
        public void tick() {
            mob.getNavigation().moveTo(mob.getMaster(), speed);
        }
    }


    // 2) If holding a bow, use the vanilla RangedBowAttackGoal but only when you really hold a bow
    public class WorkerBowAttackGoal extends RangedBowAttackGoal<WorkerEntity> {
        private final WorkerEntity mob;

        public WorkerBowAttackGoal(WorkerEntity entity, double moveSpeedAmp, int attackInterval, float maxAttackDistance) {
            super(entity, moveSpeedAmp, attackInterval, maxAttackDistance);
            this.mob = entity;
        }

        @Override
        public boolean canUse() {
            Player master = mob.getMaster();
            if (master == null) return false;
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker == null) return false;
            if (mob.isAlliedWorker(attacker)) return false;
            return super.canUse() && this.mob.isHoldingWeapon()
                && this.mob.getMainHandItem().getItem() instanceof BowItem;
        }

        @Override
        public boolean canContinueToUse() {
            Player master = mob.getMaster();
            if (master == null) return false;
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker == null) return false;
            if (mob.isAlliedWorker(attacker)) return false;
            return super.canContinueToUse() && this.mob.isHoldingWeapon()
                && this.mob.getMainHandItem().getItem() instanceof BowItem;
        }
    }


    // DEFEND MASTER’S ATTACKER
    public static class DefendMasterHurtByTargetGoal extends TargetGoal {
        private final WorkerEntity worker;

        public DefendMasterHurtByTargetGoal(WorkerEntity worker) {
            // mustSee = true, mustReach = true
            super(worker, true, true);
            this.worker = worker;
        }

        @Override
        public boolean canUse() {
            // only if we have a weapon
            if (!worker.isHoldingWeapon()) return false;
            Player master = worker.getMaster();
            if (master == null) return false;
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker == null) return false;
            if (worker.isAlliedWorker(attacker)) return false;
            // only if there *is* an attacker and we can legally attack it
            return attacker != null
                && this.canAttack(attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            super.start();
            // pull the attacker back out of the master, set it as our target
            Player master = worker.getMaster();
            if (master != null && master.getLastHurtByMob() != null) {
                worker.setTarget(master.getLastHurtByMob());
            }
        }

        // optional: let the superclass handle continuation logic
        @Override
        public boolean canContinueToUse() {
            LivingEntity tgt = worker.getTarget();
            return super.canContinueToUse() 
                && worker.isHoldingWeapon() && (tgt == null || !worker.isAlliedWorker(tgt));
        }
    }


    // DEFEND MASTER’S TARGET
    public static class DefendMasterHurtTargetGoal extends TargetGoal {
        private final WorkerEntity worker;

        public DefendMasterHurtTargetGoal(WorkerEntity worker) {
            super(worker, true, true);
            this.worker = worker;
        }

        @Override
        public boolean canUse() {
            if (!worker.isHoldingWeapon()) return false;
            if (!worker.isHoldingWeapon()) return false;
            Player master = worker.getMaster();
            if (master == null) return false;
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker == null) return false;
            if (worker.isAlliedWorker(attacker)) return false;
            return attacker != null
                && this.canAttack(attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            super.start();
            Player master = worker.getMaster();
            if (master != null && master.getLastHurtMob() != null) {
                worker.setTarget(master.getLastHurtMob());
            }
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity tgt = worker.getTarget();
            return super.canContinueToUse() 
                && worker.isHoldingWeapon() && (tgt == null || !worker.isAlliedWorker(tgt));
        }
    }

    public class NaturalRegenGoal extends Goal {
        private final WorkerEntity mob;
        private final int REST_DELAY;      // ticks to wait after last hit
        private final int HEAL_INTERVAL;   // ticks between each +1 HP
        private int healTicker = 0;

        /**
         * @param mob           the worker
         * @param restDelay     how many ticks after last hit before regen starts (e.g. 200 = 10s)
         * @param healInterval  how many ticks between each point of healing (e.g. 20 = 1s)
         */
        public NaturalRegenGoal(WorkerEntity mob, int restDelay, int healInterval) {
            this.mob           = mob;
            this.REST_DELAY    = restDelay;
            this.HEAL_INTERVAL = healInterval;
            // no MOVE or LOOK flags - let them stand still
        }

        @Override
        public boolean canUse() {
            // must be hurt, not already full
            if (mob.getHealth() >= mob.getMaxHealth()) return false;
            // must have waited long enough since last damage
            long age = mob.level().getGameTime() - mob.lastDamageTime;
            return age >= REST_DELAY;
        }

        @Override
        public boolean canContinueToUse() {
            // keep going until fully healed or we get hit again
            if (mob.getHealth() >= mob.getMaxHealth()) return false;
            long age = mob.level().getGameTime() - mob.lastDamageTime;
            return age >= REST_DELAY;
        }

        @Override
        public void start() {
            healTicker = 0;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            // every HEAL_INTERVAL ticks, heal 1 HP
            if (++healTicker >= HEAL_INTERVAL) {
                healTicker = 0;
                mob.heal(1.0F);
                // spawn a little heart particle
                if (mob.level() instanceof ServerLevel) {
                    ((ServerLevel)mob.level()).sendParticles(
                        ParticleTypes.HEART,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.2,
                        mob.getZ(),
                        1,  // count
                        0.2, 0.2, 0.2, 0.0  // dx,dy,dz, speed
                    );
                }
            }
        }
    }


    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, getMainHandItem(), pullProgress);

        // position the arrow at your eye height
        Vec3 eyePos = this.position().add(0, this.getEyeHeight() - 0.1, 0);
        arrow.setPos(eyePos.x, eyePos.y, eyePos.z);

        // aim for the target’s mid‐height (you can use getEyeHeight() if you like)
        Vec3 targPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        double dx = targPos.x - eyePos.x;
        double dy = targPos.y - eyePos.y;
        double dz = targPos.z - eyePos.z;

        double flat = Math.sqrt(dx*dx + dz*dz);

        // add the flat*0.2 arc adjustment
        dy += flat * 0.1;

        // velocity: pullProgress ∈ [0,1], full‐draw is ~1.0*3.0F in vanilla
        float velocity = 1.0F + pullProgress * 2.0F; // or use pullProgress*3.0F
        float inaccuracy = 1.0F;

        arrow.shoot(dx, dy, dz, velocity, inaccuracy);

        this.level().addFreshEntity(arrow);
    }
}