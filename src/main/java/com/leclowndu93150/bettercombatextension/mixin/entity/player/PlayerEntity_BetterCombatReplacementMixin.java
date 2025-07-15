package com.leclowndu93150.bettercombatextension.mixin.entity.player;

import com.leclowndu93150.bettercombatextension.bettercombat.DuckWeaponAttributesAttackMixin;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.bettercombat.BetterCombat;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.mixin.PlayerEntityAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin({Player.class})
public abstract class PlayerEntity_BetterCombatReplacementMixin implements PlayerAttackProperties, EntityPlayer_BetterCombat {
	private int comboCount = 0;
	private Multimap<Attribute, AttributeModifier> dualWieldingAttributeMap;
	private Multimap<Attribute, AttributeModifier> currentAttackAttributeMap;
	private static UUID dualWieldingSpeedModifierId = UUID.fromString("6b364332-0dc4-11ed-861d-0242ac120002");
	private static UUID currentAttackSpeedModifierId = UUID.fromString("2e7e0032-3cd7-4e62-b14a-6c770e6c65ca");
	private AttackHand lastAttack;

	public PlayerEntity_BetterCombatReplacementMixin() {
	}

	public int getComboCount() {
		return this.comboCount;
	}

	public void setComboCount(int comboCount) {
		this.comboCount = comboCount;
	}

	@Inject(
			method = {"tick"},
			at = {@At("TAIL")}
	)
	public void post_Tick(CallbackInfo ci) {
		Object instance = this;
		if (((Player)instance).level().isClientSide()) {
			((PlayerAttackAnimatable)this).updateAnimationsOnTick();
		}

		this.updateDualWieldingSpeedBoost();
		this.updateAttackSpecificSpeedBoost();
	}

	@ModifyVariable(
			method = {"attack"},
			at = @At("STORE"),
			ordinal = 3
	)
	private boolean disableSweeping(boolean value) {
		if (BetterCombat.config.allow_vanilla_sweeping) {
			return value;
		} else {
			Player player = (Player) (Object) this;
			AttackHand currentHand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount);
			return currentHand != null ? false : value;
		}
	}

	@Inject(
			method = {"getItemBySlot"},
			at = {@At("HEAD")},
			cancellable = true
	)
	public void getEquippedStack_Pre(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
		boolean mainHandHasTwoHanded = false;
		ItemStack mainHandStack = ((PlayerEntityAccessor)this).getInventory().getSelected();
		WeaponAttributes mainHandAttributes = WeaponRegistry.getAttributes(mainHandStack);
		if (mainHandAttributes != null && mainHandAttributes.isTwoHanded()) {
			mainHandHasTwoHanded = true;
		}

		boolean offHandHasTwoHanded = false;
		ItemStack offHandStack = (ItemStack)((PlayerEntityAccessor)this).getInventory().offhand.get(0);
		WeaponAttributes offHandAttributes = WeaponRegistry.getAttributes(offHandStack);
		if (offHandAttributes != null && offHandAttributes.isTwoHanded()) {
			offHandHasTwoHanded = true;
		}

		if (slot == EquipmentSlot.OFFHAND && (mainHandHasTwoHanded || offHandHasTwoHanded)) {
			cir.setReturnValue(ItemStack.EMPTY);
			cir.cancel();
		}
	}

	@Unique
	private void updateDualWieldingSpeedBoost() {
		Player player = (Player) (Object) this;
		boolean newState = PlayerAttackHelper.isDualWielding(player);
		boolean currentState = this.dualWieldingAttributeMap != null;
		if (newState != currentState) {
			if (newState) {
				this.dualWieldingAttributeMap = HashMultimap.create();
				double multiplier = (double)(BetterCombat.config.dual_wielding_attack_speed_multiplier - 1.0F);
				this.dualWieldingAttributeMap.put(Attributes.ATTACK_SPEED, new AttributeModifier(dualWieldingSpeedModifierId, "Dual wielding attack speed boost", multiplier, AttributeModifier.Operation.MULTIPLY_BASE));
				player.getAttributes().addTransientAttributeModifiers(this.dualWieldingAttributeMap);
			} else if (this.dualWieldingAttributeMap != null) {
				player.getAttributes().removeAttributeModifiers(this.dualWieldingAttributeMap);
				this.dualWieldingAttributeMap = null;
			}
		}

	}

	@Unique
	private void updateAttackSpecificSpeedBoost() {
		Player player = (Player) (Object) this;
		WeaponAttributes.Attack newAttack = null;
		AttackHand attackHand = PlayerAttackHelper.getCurrentAttack(player, this.getComboCount());
		if (attackHand != null) {
			newAttack = attackHand.attack();
		}
		WeaponAttributes.Attack currentAttack = null;
		if (this.lastAttack != null) {
			currentAttack = this.lastAttack.attack();
		}

		boolean bl = false;
		if (newAttack != null && newAttack != currentAttack) {
			float newAttackSpeedModifier = ((DuckWeaponAttributesAttackMixin) (Object) newAttack).bettercombatextension$getAttackSpeedMultiplier();
			if (newAttackSpeedModifier != 1.0F) {
				this.currentAttackAttributeMap = HashMultimap.create();
				double multiplier = (double)(newAttackSpeedModifier - 1.0F);
				this.currentAttackAttributeMap.put(Attributes.ATTACK_SPEED, new AttributeModifier(currentAttackSpeedModifierId, "Current attack speed boost", multiplier, AttributeModifier.Operation.MULTIPLY_BASE));
				player.getAttributes().addTransientAttributeModifiers(this.currentAttackAttributeMap);
			} else if (this.currentAttackAttributeMap != null) {
				bl = true;
			}
		} else if (newAttack == null && this.currentAttackAttributeMap != null) {
			bl = true;
		}

		if (bl) {
			player.getAttributes().removeAttributeModifiers(this.currentAttackAttributeMap);
			this.currentAttackAttributeMap = null;
		}
	}

	@ModifyArg(
			method = {"attack"},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
			),
			index = 0
	)
	public InteractionHand getHand(InteractionHand hand) {
		Player player = (Player) (Object) this;
		AttackHand currentHand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount);
		if (currentHand != null) {
			return currentHand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		} else {
			return InteractionHand.MAIN_HAND;
		}
	}

	@Redirect(
			method = {"attack"},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
			)
	)
	public ItemStack getMainHandStack_Redirect(Player instance) {
		if (this.comboCount < 0) {
			return instance.getMainHandItem();
		} else {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount);
			if (hand == null) {
				boolean isOffHand = PlayerAttackHelper.shouldAttackWithOffHand(instance, this.comboCount);
				return isOffHand ? ItemStack.EMPTY : instance.getMainHandItem();
			} else {
				this.lastAttack = hand;
				return hand.itemStack();
			}
		}
	}

	@Redirect(
			method = {"attack"},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"
			)
	)
	public void setStackInHand_Redirect(Player instance, InteractionHand handArg, ItemStack itemStack) {
		if (this.comboCount < 0) {
			instance.setItemInHand(handArg, itemStack);
		}

		AttackHand hand = this.lastAttack;
		if (hand == null) {
			hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount);
		}

		if (hand == null) {
			instance.setItemInHand(handArg, itemStack);
		} else {
			InteractionHand redirectedHand = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			instance.setItemInHand(redirectedHand, itemStack);
		}
	}

	public @Nullable AttackHand getCurrentAttack() {
		if (this.comboCount < 0) {
			return null;
		} else {
			Player player = (Player) (Object) this;
			return PlayerAttackHelper.getCurrentAttack(player, this.comboCount);
		}
	}
}
