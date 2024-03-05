package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityMosquitoSpit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class ItemBloodSprayer extends Item {

    public static final Predicate<ItemStack> IS_BLOOD = (stack) -> {
        return stack.getItem() == AMItemRegistry.BLOOD_SAC.get();
    };

    public ItemBloodSprayer(Item.Properties properties) {
        super(properties);
    }

    public int getUseDuration(ItemStack stack) {
        return isUsable(stack) ? Integer.MAX_VALUE : 0;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack itemstack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        if(!isUsable(itemstack)){
            ItemStack ammo = findAmmo(playerIn);
            boolean flag = playerIn.isCreative();
            if(!ammo.isEmpty()){
                ammo.shrink(1);
                flag = true;
            }
            if(flag){
                itemstack.setDamageValue(0);
            }
        }
        return InteractionResultHolder.consume(itemstack);
    }

    public ItemStack findAmmo(Player entity) {
        if(entity.isCreative()){
            return ItemStack.EMPTY;
        }
        for(int i = 0; i < entity.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack1 = entity.getInventory().getItem(i);
            if (IS_BLOOD.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }
    
    public boolean isBarVisible(ItemStack itemStack) {
        return super.isBarVisible(itemStack) && isUsable(itemStack);
    }

    public void onUseTick(Level worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {
        if(isUsable(stack)) {
            if (count % 2 == 0) {
                boolean left = false;
                if (livingEntityIn.getUsedItemHand() == InteractionHand.OFF_HAND && livingEntityIn.getMainArm() == HumanoidArm.RIGHT || livingEntityIn.getUsedItemHand() == InteractionHand.MAIN_HAND && livingEntityIn.getMainArm() == HumanoidArm.LEFT) {
                    left = true;
                }
                EntityMosquitoSpit blood = new EntityMosquitoSpit(worldIn, livingEntityIn, !left);
                Vec3 vector3d = livingEntityIn.getViewVector(1.0F);
                RandomSource rand = worldIn.getRandom();
                livingEntityIn.gameEvent(GameEvent.ITEM_INTERACT_START);
                livingEntityIn.playSound(SoundEvents.LAVA_POP,1.0F, 1.2F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
                blood.shoot((double) vector3d.x(), (double) vector3d.y(), (double) vector3d.z(), 1F, 10);
                if (!worldIn.isClientSide) {
                    worldIn.addFreshEntity(blood);
                }
                stack.hurtAndBreak(1, livingEntityIn, (player) -> {
                    player.broadcastBreakEvent(livingEntityIn.getUsedItemHand());
                });
            }
        }else{
            if(livingEntityIn instanceof Player){
                ItemStack ammo = findAmmo((Player)livingEntityIn);
                boolean flag = ((Player) livingEntityIn).isCreative();
                if(!ammo.isEmpty()){
                    ammo.shrink(1);
                    flag = true;
                }
                if(flag){
                    ((Player) livingEntityIn).getCooldowns().addCooldown(this, 20);
                    stack.setDamageValue(0);
                }
                livingEntityIn.stopUsingItem();
            }
        }
    }
}
