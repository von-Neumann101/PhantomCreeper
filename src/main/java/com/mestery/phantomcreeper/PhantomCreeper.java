package com.mestery.phantomcreeper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class PhantomCreeper extends Phantom {
    public static final int FUSE_DURATION = 30;
    private static final int DAMAGE_ADVANCE = 5;
    private static final EntityDataAccessor<Integer> FUSE_TICKS =
            SynchedEntityData.defineId(PhantomCreeper.class, EntityDataSerializers.INT);
    private int previousFuseTicks = -1;

    public PhantomCreeper(EntityType<? extends PhantomCreeper> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE_TICKS, -1);
    }

    public int getFuseTicks() {
        return entityData.get(FUSE_TICKS);
    }

    public float getFuseProgress(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, Math.max(0, previousFuseTicks), Math.max(0, getFuseTicks())) / FUSE_DURATION, 0, 1);
    }

    private boolean canPrimeAt(LivingEntity target) {
        return target != null && target.isAlive() && canAttack(target)
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                && distanceToSqr(target.getX(), target.getY(0.5), target.getZ()) <= 9
                && hasLineOfSight(target);
    }

    private void prime() {
        if (!level().isClientSide && getFuseTicks() < 0 && isAlive()) {
            entityData.set(FUSE_TICKS, 0);
            playSound(SoundEvents.CREEPER_PRIMED, 1, 0.5F);
            gameEvent(GameEvent.PRIME_FUSE);
        }
    }

    @Override
    public void tick() {
        previousFuseTicks = getFuseTicks();
        if (isAlive()) {
            if (!level().isClientSide) {
                if (getFuseTicks() >= 0) {
                    int elapsed = getFuseTicks() + 1;
                    entityData.set(FUSE_TICKS, elapsed);
                    if (elapsed >= FUSE_DURATION) {
                        dead = true;
                        level().explode(this, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.MOB);
                        triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
                        discard();
                        return;
                    }
                }
            }
        }
        super.tick();
        // Let vanilla goal validation (including cat avoidance) run before initial ignition.
        if (!level().isClientSide && isAlive() && getFuseTicks() < 0 && canPrimeAt(getTarget())) {
            prime();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Vanilla Phantom calls this on contact; this mob attacks with its fuse.
        if (target instanceof LivingEntity living && canPrimeAt(living)) {
            prime();
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float healthBefore = getHealth() + getAbsorptionAmount();
        boolean accepted = super.hurt(source, amount);
        // ponytail: sun and other ongoing burns share ON_FIRE; track ignition source if they need different fuse behavior.
        if (!level().isClientSide && accepted && isAlive() && getFuseTicks() >= 0
                && !source.is(DamageTypes.ON_FIRE)
                && getHealth() + getAbsorptionAmount() < healthBefore) {
            // Keep at least one tick so lethal hits and simultaneous events remain well-defined.
            entityData.set(FUSE_TICKS, Math.min(FUSE_DURATION - 1, getFuseTicks() + DAMAGE_ADVANCE));
        }
        return accepted;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuseTicks", getFuseTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int fuse = tag.contains("FuseTicks", Tag.TAG_ANY_NUMERIC) ? tag.getInt("FuseTicks") : -1;
        entityData.set(FUSE_TICKS, Mth.clamp(fuse, -1, FUSE_DURATION - 1));
        previousFuseTicks = getFuseTicks();
    }
}
