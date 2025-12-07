package com.fifthbit.spearmod.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class DiamondSpearItem extends Item {
    private static final int MAX_CHARGE_TIME = 100; // 5 seconds for max charge
    private static final float BASE_DAMAGE = 4.0F; // Quick jab damage (less than sword)
    private static final float MAX_DAMAGE = 30.0F; // Massive damage like mace when fully charged
    private static final float KNOCKBACK = 2.0F; // Extra knockback
    private static final double LUNGE_DISTANCE = 3.0;
    private static final int LUNGE_COOLDOWN = 70; // 3.5 seconds cooldown

    public DiamondSpearItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Check if on cooldown
        if (isOnCooldown(stack)) {
            return ActionResult.FAIL;
        }

        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            int chargeTime = this.getMaxUseTime(stack, user) - remainingUseTicks;

            // Show charging particles every 4 ticks on server side
            if (chargeTime % 4 == 0) {
                Vec3d pos = player.getEyePos();
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        pos.x, pos.y - 0.3, pos.z,
                        1, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return false;
        }

        int chargeTime = this.getMaxUseTime(stack, user) - remainingUseTicks;

        // Quick jab (less than 0.5 seconds / 10 ticks)
        if (chargeTime < 10) {
            performJab(world, player, stack);
        } else {
            // Charged lunge attack
            performLunge(world, player, stack, chargeTime);

            // Set cooldown after lunge
            setCooldown(stack, LUNGE_COOLDOWN);
        }
        return true;
    }

    private void performJab(World world, PlayerEntity player, ItemStack stack) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d lookVec = player.getRotationVector();
            Vec3d startPos = player.getEyePos();
            Vec3d endPos = startPos.add(lookVec.multiply(2.5)); // Sword-like reach

            Box hitBox = new Box(startPos, endPos).expand(0.8);

            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, hitBox,
                    entity -> entity != player && player.canSee(entity));

            for (LivingEntity target : entities) {
                // Quick jab - less damage than sword
                target.damage(serverWorld, serverWorld.getDamageSources().playerAttack(player), BASE_DAMAGE);

                // Extra knockback
                Vec3d knockbackVec = lookVec.multiply(KNOCKBACK);
                target.setVelocity(target.getVelocity().add(
                        knockbackVec.x, 0.3, knockbackVec.z));
                target.velocityModified = true;

                // Play attack sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS,
                        1.0F, 1.0F);

                // Small particle effect
                for (int i = 0; i < 5; i++) {
                    serverWorld.spawnParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                            1, (Math.random() - 0.5) * 0.3, Math.random() * 0.3, (Math.random() - 0.5) * 0.3, 0.0);
                }

                stack.damage(1, player, player.getActiveHand());
                break; // Only hit first entity for jab
            }
        }
    }

    private void performLunge(World world, PlayerEntity player, ItemStack stack, int chargeTime) {
        float chargeRatio = Math.min((float) chargeTime / MAX_CHARGE_TIME, 1.0F);
        float damage = BASE_DAMAGE + (MAX_DAMAGE - BASE_DAMAGE) * chargeRatio;

        Vec3d lookVec = player.getRotationVector();
        double lungeDistance = 1.5 + (LUNGE_DISTANCE - 1.5) * chargeRatio;

        Vec3d motion = lookVec.multiply(lungeDistance * 0.5);
        player.setVelocity(motion.x, motion.y * 0.3 + 0.2, motion.z);
        player.velocityModified = true;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS,
                1.0F, 1.0F);

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d startPos = player.getEyePos();
            Vec3d endPos = startPos.add(lookVec.multiply(lungeDistance + 1.5));

            Box hitBox = new Box(startPos, endPos).expand(1.0);

            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, hitBox,
                    entity -> entity != player && player.canSee(entity));

            for (LivingEntity target : entities) {
                target.damage(serverWorld, serverWorld.getDamageSources().playerAttack(player), damage);

                Vec3d knockbackVec = lookVec.multiply(KNOCKBACK);
                target.setVelocity(target.getVelocity().add(
                        knockbackVec.x, 0.3, knockbackVec.z));
                target.velocityModified = true;

                for (int i = 0; i < 10; i++) {
                    serverWorld.spawnParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                            1, (Math.random() - 0.5) * 0.5, Math.random() * 0.5, (Math.random() - 0.5) * 0.5, 0.0);
                }

                stack.damage(1, player, player.getActiveHand());
            }
        }
    }

    // Helper methods for cooldown management
    private boolean isOnCooldown(ItemStack stack) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();

        if (!compound.contains("CooldownEnd")) {
            return false;
        }

        long cooldownEnd = compound.getLong("CooldownEnd").orElse(0L);
        return System.currentTimeMillis() < cooldownEnd;
    }

    private void setCooldown(ItemStack stack, int ticks) {
        NbtComponent currentNbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = currentNbt.copyNbt();

        // Convert ticks to milliseconds (20 ticks = 1 second = 1000ms)
        long cooldownTime = ticks * 50; // 50ms per tick
        nbt.putLong("CooldownEnd", System.currentTimeMillis() + cooldownTime);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}