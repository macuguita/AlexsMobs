package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.citadel.Citadel;
import com.github.alexthe666.citadel.server.entity.CitadelEntityData;
import com.github.alexthe666.citadel.server.message.PropertiesMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class RockyChestplateUtil {

    private static final String ROCKY_ROLL_TICKS = "RockyRollTicksAlexsMobs";
    private static final String ROCKY_X = "RockyRollXAlexsMobs";
    private static final String ROCKY_Y = "RockyRollYAlexsMobs";
    private static final String ROCKY_Z = "RockyRollZAlexsMobs";
    private static final int MAX_ROLL_TICKS = 30;

    public static void rollFor(LivingEntity roller, int ticks) {
        CompoundTag lassoedTag = CitadelEntityData.getOrCreateCitadelTag(roller);
        lassoedTag.putInt(ROCKY_ROLL_TICKS, ticks);
        CitadelEntityData.setCitadelTag(roller, lassoedTag);
        if (!roller.level.isClientSide) {
            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", lassoedTag, roller.getId()));
        }
    }

    public static int getRollingTicksLeft(LivingEntity entity) {
        CompoundTag lassoedTag = CitadelEntityData.getOrCreateCitadelTag(entity);
        if (lassoedTag.contains(ROCKY_ROLL_TICKS)) {
            return lassoedTag.getInt(ROCKY_ROLL_TICKS);
        }
        return 0;
    }


    public static boolean isWearing(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == AMItemRegistry.ROCKY_CHESTPLATE;
    }

    public static boolean isRockyRolling(LivingEntity entity) {
        return isWearing(entity) && getRollingTicksLeft(entity) > 0;
    }

    public static void tickRockyRolling(LivingEntity roller) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(roller);
        boolean update = false;
        int rollCounter = getRollingTicksLeft(roller);
        if(rollCounter == 0){
            if(roller.isSprinting() && (!(roller instanceof Player) || !((Player) roller).getAbilities().flying)){
                update = true;
                rollFor(roller, MAX_ROLL_TICKS);
            }
            if(roller instanceof Player &&  ((Player)roller).getForcedPose() == Pose.SWIMMING){
                ((Player)roller).setForcedPose(null);
            }
        }else{
            if(roller instanceof Player){
                ((Player)roller).setForcedPose(Pose.SWIMMING);
            }
            if(!roller.level.isClientSide){
                for (Entity entity : roller.level.getEntitiesOfClass(LivingEntity.class, roller.getBoundingBox().inflate(1.0F))) {
                    if (!roller.isAlliedTo(entity) && !entity.isAlliedTo(roller) && entity != roller) {
                        entity.hurt(DamageSource.mobAttack(roller), 2.0F + roller.getRandom().nextFloat() * 1.0F);
                    }
                }
            }
            roller.refreshDimensions();
            Vec3 vec3 = roller.isOnGround() ? roller.getDeltaMovement() : roller.getDeltaMovement().multiply(0.9D, 1D, 0.9D);
            float f = roller.getYRot() * ((float) Math.PI / 180F);
            float f1 = 0.15F;
            Vec3 rollDelta = new Vec3(vec3.x + (double) (-Mth.sin(f) * f1), 0.0D, vec3.z + (double) (Mth.cos(f) * f1));
            roller.setDeltaMovement(rollDelta.add(0.0D, rollCounter >= MAX_ROLL_TICKS ? 0.27D : vec3.y, 0.0D));
            if(rollCounter > 1 || !roller.isSprinting()){
                rollFor(roller, rollCounter - 1);
            }
            if(roller instanceof Player && ((Player) roller).getAbilities().flying){
                rollCounter = 0;
                rollFor(roller, 0);
            }
            if(rollCounter == 0){
                update = true;
            }
        }
        if (!roller.level.isClientSide && update) {
            CitadelEntityData.setCitadelTag(roller, tag);
            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", tag, roller.getId()));
        }
    }
}