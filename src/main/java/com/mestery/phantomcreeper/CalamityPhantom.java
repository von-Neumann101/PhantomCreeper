package com.mestery.phantomcreeper;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CalamityPhantom extends PhantomCreeper {
    public enum Phase { SWOOP, AIRBORNE, SUICIDE }
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(CalamityPhantom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BOOSTING = SynchedEntityData.defineId(CalamityPhantom.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(CalamityPhantom.class, EntityDataSerializers.BOOLEAN);
    private final List<WrappedGoal> phantomGoals;
    private boolean riderInitialized;
    private int boostTicks;
    private double boostHeight;
    private int sonicChargeTicks;
    private int sonicCooldown = 60;
    private int dragonCooldown = 45;
    private int skullCooldown = 20;
    private int nextDragon;
    private int nextSkull;

    public CalamityPhantom(EntityType<? extends CalamityPhantom> type, Level level) {
        super(type, level);
        phantomGoals = List.copyOf(goalSelector.getAvailableGoals());
        xpReward = 30;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 0);
        builder.define(BOOSTING, false);
        builder.define(CHARGING, false);
    }

    public Phase getPhase() { return Phase.values()[entityData.get(PHASE)]; }
    public boolean isRocketBoosting() { return entityData.get(BOOSTING); }
    public boolean isChargingSonicBoom() { return entityData.get(CHARGING); }

    @Override
    protected boolean usesExplosiveAttack() { return getPhase() == Phase.SUICIDE; }

    @Override
    protected float getExplosionPower() { return 6; } // Vanilla charged Creeper: 3 * 2.

    @Override
    public LivingEntity getControllingPassenger() {
        // A real passenger must never lend its ground AI to this flying vehicle.
        return null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, 0.9, 0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    private void changePhase(Phase phase) {
        if (phase == getPhase()) return;
        LivingEntity target = getTarget();
        // Stop the actual vanilla goals; their stop() can clear the current target.
        if (phase == Phase.AIRBORNE) {
            for (WrappedGoal goal : phantomGoals) goalSelector.removeGoal(goal.getGoal());
        } else if (getPhase() == Phase.AIRBORNE) {
            for (WrappedGoal goal : phantomGoals) goalSelector.addGoal(goal.getPriority(), goal.getGoal());
            if (boostTicks > 0) setDeltaMovement(Vec3.ZERO);
            boostTicks = 0;
            sonicChargeTicks = 0;
        }
        entityData.set(PHASE, phase.ordinal());
        entityData.set(BOOSTING, boostTicks > 0);
        entityData.set(CHARGING, false);
        if (canPursue(target)) setTarget(target);
    }

    private void updatePhase() {
        float fraction = getHealth() / getMaxHealth();
        if (fraction < 0.25F || getFuseTicks() >= 0) changePhase(Phase.SUICIDE);
        else if (fraction < 0.6F && getPhase() == Phase.SWOOP) {
            changePhase(Phase.AIRBORNE);
            boostTicks = 24;
            // Keep the original boost's maximum climb (0.8 * 24); only reach it faster.
            boostHeight = Math.min(getY() + 0.8 * 24,
                    Math.max(getY() + 8, canPursue(getTarget()) ? getTarget().getY() + 12 : getY() + 12));
            entityData.set(BOOSTING, true);
            playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2, 0.8F);
        }
    }

    private void initializeRider() {
        if (riderInitialized) return;
        riderInitialized = true;
        if (!getPassengers().isEmpty()) return;
        TridentZombie rider = PhantomCreeperMod.TRIDENT_ZOMBIE.get().create(level());
        if (rider != null) {
            rider.setCommander(getUUID());
            rider.moveTo(getX(), getY() + 0.9, getZ(), getYRot(), 0);
            if (rider.startRiding(this, true) && level().addFreshEntity(rider)) positionRider(rider);
            else rider.discard();
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide && isAlive()) {
            updatePhase();
            initializeRider();
        }
        super.tick();
        if (level() instanceof ServerLevel server && isAlive() && !isNoAi() && boostTicks > 0) {
            Vec3 tail = headPosition(0, -1.1, 0.95);
            server.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 8, 0.09, 0.09, 0.09, 0.035);
            server.sendParticles(ParticleTypes.SMOKE, tail.x, tail.y, tail.z, 4, 0.12, 0.06, 0.12, 0.025);
            server.sendParticles(ParticleTypes.FIREWORK, tail.x, tail.y, tail.z, 8, 0.1, 0.1, 0.1, 0.1);
            if (--boostTicks == 0) entityData.set(BOOSTING, false);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (!level().isClientSide && isAlive() && !isNoAi() && getPhase() == Phase.AIRBORNE) {
            LivingEntity target = getTarget();
            Vec3 movement = Vec3.ZERO;
            boolean hasTarget = canPursue(target);
            if (boostTicks > 0 || hasTarget) {
                double clearance = getBbHeight();
                for (Entity passenger : getPassengers()) {
                    clearance = Math.max(clearance, getPassengerRidingPosition(passenger).y - getY()
                            - passenger.getVehicleAttachmentPoint(this).y + passenger.getBbHeight());
                }
                double height = Math.min(level().getMaxBuildHeight() - clearance - 1,
                        boostTicks > 0 ? boostHeight : target.getY() + 12);
                var ceiling = level().clip(new ClipContext(getBoundingBox().getCenter(),
                        new Vec3(getX(), height + clearance, getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (ceiling.getType() != HitResult.Type.MISS) height = Math.min(height, ceiling.getLocation().y - clearance - 0.2);
                double angle = tickCount * 0.025;
                Vec3 destination = boostTicks > 0 ? new Vec3(getX(), height, getZ())
                        : new Vec3(target.getX() + Math.cos(angle) * 8, height, target.getZ() + Math.sin(angle) * 8);
                Vec3 offset = destination.subtract(position());
                movement = offset.normalize().scale(Math.min(boostTicks > 0 ? 1.6 : 0.25, offset.length()));
                if (hasTarget) {
                    Vec3 aim = target.position().subtract(position());
                    setYRot((float) (Mth.atan2(aim.z, aim.x) * Mth.RAD_TO_DEG) - 90);
                    yBodyRot = getYRot();
                }
                setXRot(boostTicks > 0 ? 25 : 0);
            }
            setDeltaMovement(movement);
            input = Vec3.ZERO;
        }
        super.travel(input);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getPhase() != Phase.AIRBORNE && super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == this || attacker instanceof TridentZombie rider && rider.isCommander(this)) return false;
        // Stage changes run at the next tick boundary, outside goal iteration (e.g. Thorns retaliation).
        return super.hurt(source, amount);
    }

    private Vec3 headPosition(double side, double forward, double height) {
        return position().add(new Vec3(side, height, forward).yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getPhase() != Phase.AIRBORNE) return;
        LivingEntity target = getTarget();
        if (!canPursue(target)) {
            sonicChargeTicks = 0;
            entityData.set(CHARGING, false);
            return;
        }
        if (sonicCooldown > 0) sonicCooldown--;
        if (dragonCooldown > 0) dragonCooldown--;
        if (skullCooldown > 0) skullCooldown--;
        boolean sonicRange = position().subtract(target.position()).horizontalDistanceSqr() <= 225
                && Math.abs(target.getY() - getY()) <= 20;
        if (sonicChargeTicks > 0) {
            if (--sonicChargeTicks == 0) {
                entityData.set(CHARGING, false);
                sonicCooldown = 120;
                if (sonicRange) fireSonicBoom(target);
            }
        } else if (sonicCooldown == 0 && sonicRange) {
            sonicChargeTicks = 34;
            entityData.set(CHARGING, true);
            playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3, 1);
        }
        if (distanceToSqr(target) > 1024 || !hasLineOfSight(target)) return;
        if (dragonCooldown == 0) {
            Vec3 muzzle = headPosition((nextDragon++ % 2 == 0 ? -1 : 1) * 0.6, 1.3, 0.62);
            // Vanilla dragon fireballs create a damage cloud without an explosion or block destruction.
            DragonFireball shot = new DragonFireball(level(), this, target.getBoundingBox().getCenter().subtract(muzzle).normalize());
            shot.setPos(muzzle);
            level().addFreshEntity(shot);
            playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.5F, 1);
            dragonCooldown = 80;
        }
        if (skullCooldown == 0) {
            int head = nextSkull++ % 6;
            // Match the two wing joints driven by vanilla PhantomModel's flap phase.
            double flap = Mth.cos((getUniqueFlapTickOffset() + tickCount) * FLAP_DEGREES_PER_TICK * Mth.DEG_TO_RAD)
                    * 16 * Mth.DEG_TO_RAD;
            double tip = 0.125 + (head % 3) * 0.3125;
            double side = 0.25 + 0.5 * Math.cos(flap) + tip * Math.cos(2 * flap) + 0.12 * Math.sin(2 * flap);
            double height = 0.92 - 0.5 * Math.sin(flap) - tip * Math.sin(2 * flap) + 0.12 * (Math.cos(2 * flap) - 1);
            Vec3 muzzle = headPosition((head < 3 ? -1 : 1) * side, 0.5, height);
            WitherSkull shot = new WitherSkull(level(), this, target.getBoundingBox().getCenter().subtract(muzzle).normalize());
            shot.setPos(muzzle);
            level().addFreshEntity(shot);
            playSound(SoundEvents.WITHER_SHOOT, 1, 1);
            skullCooldown = 25;
        }
    }

    private void fireSonicBoom(LivingEntity target) {
        Vec3 start = headPosition(0, 0.9, 0.45);
        Vec3 offset = target.getEyePosition().subtract(start);
        Vec3 direction = offset.normalize();
        for (int i = 1; i <= Mth.floor(offset.length()) + 3; i++) {
            Vec3 particle = start.add(direction.scale(i));
            ((ServerLevel) level()).sendParticles(ParticleTypes.SONIC_BOOM, particle.x, particle.y, particle.z, 1, 0, 0, 0, 0);
        }
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 3, 1);
        if (target.hurt(damageSources().sonicBoom(this), 10)) target.push(direction.x * 1.5, direction.y * 0.5, direction.z * 1.5);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CombatPhase", getPhase().ordinal());
        tag.putBoolean("RiderInitialized", riderInitialized);
        tag.putInt("RocketTicks", boostTicks);
        tag.putDouble("RocketHeight", boostHeight);
        tag.putInt("SonicCharge", sonicChargeTicks);
        tag.putInt("SonicCooldown", sonicCooldown);
        tag.putInt("DragonCooldown", dragonCooldown);
        tag.putInt("SkullCooldown", skullCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        changePhase(Phase.values()[Mth.clamp(tag.getInt("CombatPhase"), 0, 2)]);
        riderInitialized = tag.getBoolean("RiderInitialized");
        boostTicks = getPhase() == Phase.AIRBORNE ? Mth.clamp(tag.getInt("RocketTicks"), 0, 24) : 0;
        boostHeight = tag.contains("RocketHeight") ? tag.getDouble("RocketHeight") : getY() + 12;
        if (!Double.isFinite(boostHeight)) boostHeight = getY() + 12;
        // Old saves may contain an unreachable high target; retain the old remaining climb budget.
        if (boostTicks > 0) boostHeight = Math.min(boostHeight, getY() + 0.8 * boostTicks);
        sonicChargeTicks = getPhase() == Phase.AIRBORNE ? Mth.clamp(tag.getInt("SonicCharge"), 0, 34) : 0;
        entityData.set(BOOSTING, boostTicks > 0);
        entityData.set(CHARGING, sonicChargeTicks > 0);
        sonicCooldown = Mth.clamp(tag.getInt("SonicCooldown"), 20, 120);
        dragonCooldown = Mth.clamp(tag.getInt("DragonCooldown"), 20, 80);
        skullCooldown = Mth.clamp(tag.getInt("SkullCooldown"), 10, 25);
    }
}
