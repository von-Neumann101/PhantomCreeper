package com.mestery.phantomcreeper;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TridentZombie extends Zombie implements RangedAttackMob {
    private UUID commander;

    public TridentZombie(EntityType<? extends TridentZombie> type, Level level) {
        super(type, level);
        setBaby(true);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    public void setCommander(UUID commander) { this.commander = commander; }
    public boolean isCommander(Entity entity) { return entity != null && entity.getUUID().equals(commander); }

    @Override
    public boolean canControlVehicle() { return false; }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new MountedAttackGoal());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() == this || isCommander(source.getEntity())) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (level().isClientSide || !getMainHandItem().is(Items.TRIDENT)) return;
        ThrownTrident trident = new ThrownTrident(level(), this, new ItemStack(Items.TRIDENT));
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getY(0.333) - trident.getY();
        trident.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 2);
        level().addFreshEntity(trident);
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.DROWNED_SHOOT, 1, 1);
    }

    private class MountedAttackGoal extends Goal {
        private int cooldown = 20;

        MountedAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return getVehicle() instanceof CalamityPhantom; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { getNavigation().stop(); }
        @Override public void stop() { stopUsingItem(); setAggressive(false); }

        @Override
        public void tick() {
            if (!(getVehicle() instanceof CalamityPhantom mount)) return;
            LivingEntity target = mount.getTarget();
            boolean active = mount.isAlive() && mount.getPhase() != CalamityPhantom.Phase.SUICIDE && mount.canPursue(target);
            setAggressive(active);
            if (cooldown > 0) cooldown--;
            if (!active || !hasLineOfSight(target) || distanceToSqr(target) > 1024) {
                stopUsingItem();
                return;
            }
            getLookControl().setLookAt(target, 60, 60);
            if (distanceToSqr(target) <= 6.25) {
                stopUsingItem();
                if (cooldown == 0) {
                    swing(InteractionHand.MAIN_HAND);
                    doHurtTarget(target);
                    cooldown = 20;
                }
            } else if (getMainHandItem().is(Items.TRIDENT)) {
                if (cooldown <= 15 && !isUsingItem()) startUsingItem(InteractionHand.MAIN_HAND);
                if (cooldown == 0) {
                    performRangedAttack(target, 1);
                    stopUsingItem();
                    cooldown = 50;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (commander != null) tag.putUUID("Commander", commander);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        commander = tag.hasUUID("Commander") ? tag.getUUID("Commander") : null;
    }
}
