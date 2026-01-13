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
    private static final float MAX_DAMAGE = 160F; // Massive damage like mace when fully charged
    private static final float KNOCKBACK = 2.0F; // Extra knockback
    private static final double LUNGE_DISTANCE = 3.0;
    private static final int LUNGE_COOLDOWN = 50; // 3.5 seconds cooldown

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

    private boolean isOnCooldown(ItemStack stack) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();

        if (!compound.contains("CooldownEnd")) {
            return false;
        }

        // Handle Optional<Long> return type
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

    // Get charge progress for animations (0.0 to 1.0)
    public static float getChargeProgress(ItemStack stack, LivingEntity user) {
        if (user == null || !user.isUsingItem() || user.getActiveItem() != stack) {
            return 0.0f;
        }

        int useTicks = stack.getMaxUseTime(user) - user.getItemUseTimeLeft();
        return Math.min(useTicks / 20.0f, 1.0f);
    }

    // Get animation stage (0 = pulling back, 1 = thrust forward)
    public static float getAnimationStage(ItemStack stack, LivingEntity user) {
        if (user == null || !user.isUsingItem() || user.getActiveItem() != stack) {
            return 0.0f;
        }

        int useTicks = stack.getMaxUseTime(user) - user.getItemUseTimeLeft();

        // Pull back phase: 0-10 ticks (0.5 seconds)
        if (useTicks <= 10) {
            return 0.3f; // Pulling back
        }
        // Thrust forward phase: 10+ ticks
        else {
            return 0.7f; // Thrusting forward
        }
    }

    // Lunge tracking helpers
    private void markLunging(ItemStack stack, float damage) {
        NbtComponent currentNbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = currentNbt.copyNbt();
        nbt.putBoolean("Lunging", true);
        nbt.putFloat("LungeDamage", damage);
        nbt.putLong("LungeStartTime", System.currentTimeMillis());
        nbt.putString("HitEntities", ""); // Reset hit list
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private boolean isLunging(ItemStack stack) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();
        return compound.contains("Lunging") && compound.getBoolean("Lunging").orElse(false);
    }

    private float getLungeDamage(ItemStack stack) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();
        return compound.contains("LungeDamage") ? compound.getFloat("LungeDamage").orElse(BASE_DAMAGE) : BASE_DAMAGE;
    }

    private boolean hasLungedFor(ItemStack stack, int ticks) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();
        if (!compound.contains("LungeStartTime")) return true;

        long startTime = compound.getLong("LungeStartTime").orElse(0L);
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= (ticks * 50); // 50ms per tick
    }

    private void clearLungeState(ItemStack stack) {
        NbtComponent currentNbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = currentNbt.copyNbt();
        nbt.remove("Lunging");
        nbt.remove("LungeDamage");
        nbt.remove("LungeStartTime");
        nbt.remove("HitEntities");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private boolean hasHitEntity(ItemStack stack, java.util.UUID entityId) {
        NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound compound = nbt.copyNbt();
        if (!compound.contains("HitEntities")) return false;

        String hitList = compound.getString("HitEntities").orElse("");
        return hitList.contains(entityId.toString());
    }

    private void addHitEntity(ItemStack stack, java.util.UUID entityId) {
        NbtComponent currentNbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = currentNbt.copyNbt();

        String hitList = nbt.contains("HitEntities") ? nbt.getString("HitEntities").orElse("") : "";
        hitList += entityId.toString() + ";";
        nbt.putString("HitEntities", hitList);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}