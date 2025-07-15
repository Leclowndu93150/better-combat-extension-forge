package com.leclowndu93150.bettercombatextension.mixin.client.network;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.bettercombat.DuckWeaponAttributesAttackMixin;
import com.leclowndu93150.bettercombatextension.client.DuckMinecraftClientMixin;
import com.mojang.authlib.GameProfile;
import net.bettercombat.BetterCombat;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.config.ServerConfig;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.utils.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(LocalPlayer.class)
@SuppressWarnings("UnreachableCode")
public abstract class ClientPlayerEntityMixin_BetterCombatReplacementMixin extends AbstractClientPlayer {

	@Shadow
	@Final
	protected Minecraft minecraft;

	@Shadow
	public abstract boolean isUsingItem();

	@Shadow
	public abstract float getViewXRot(float tickDelta);

	@Shadow
	public abstract void displayClientMessage(Component message, boolean overlay);

	public ClientPlayerEntityMixin_BetterCombatReplacementMixin(ClientLevel world, GameProfile profile) {
		super(world, profile);
	}

	@Inject(
			method = "aiStep",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
					shift = At.Shift.AFTER
			)
	)
	public void bettercombatextension$tickMovement(CallbackInfo ci) {
		boolean isWeaponSwingInProgress = ((MinecraftClient_BetterCombat) this.minecraft).isWeaponSwingInProgress();
		ServerConfig config = BetterCombat.config;
		com.leclowndu93150.bettercombatextension.config.ServerConfig betterCombatExtensionServerConfig = BetterCombatExtension.SERVER_CONFIG;
		double multiplier = Math.min(
				Math.max(
						(double) config.movement_speed_while_attacking,
						betterCombatExtensionServerConfig.minimum_global_attack_movement_speed_multiplier.get()
				),
				betterCombatExtensionServerConfig.maximum_global_attack_movement_speed_multiplier.get()
		);
		ItemStack activeItemStack = this.getItemInHand(((DuckMinecraftClientMixin) this.minecraft).bettercombatextension$getCurrentAttackHand());
		boolean isMovementPenaltyIgnored = activeItemStack.is(BetterCombatExtension.IGNORES_ATTACK_MOVEMENT_PENALTY) && isWeaponSwingInProgress;
		LocalPlayer clientPlayer = (LocalPlayer) (Object) this;
		MinecraftClient_BetterCombat client = (MinecraftClient_BetterCombat) Minecraft.getInstance();

		// attack specific movement modifier
		double attack_specific_modifier = 1.0;
		AttackHand attackHand = PlayerAttackHelper.getCurrentAttack(clientPlayer, client.getComboCount());
		if (attackHand != null) {
			attack_specific_modifier = Math.min(
					Math.max(
							((DuckWeaponAttributesAttackMixin) (Object) attackHand.attack()).bettercombatextension$getMovementSpeedMultiplier(),
							betterCombatExtensionServerConfig.minimum_attack_specific_movement_speed_multiplier.get()
					),
					betterCombatExtensionServerConfig.maximum_attack_specific_movement_speed_multiplier.get()
			);
		}

		if ((attack_specific_modifier != 1.0 || multiplier != 1.0) && !isMovementPenaltyIgnored) {
			if (!clientPlayer.isPassenger() || config.movement_speed_effected_while_mounting) {
				float swingProgress = client.getSwingProgress();

				if ((double) swingProgress < 1/*0.98*/) {
					if (config.movement_speed_applied_smoothly) {
						double p2 = 0.0;
						if ((double) swingProgress <= 0.5) {
							p2 = MathHelper.easeOutCubic((double) (swingProgress * 2.0F));
						} else {
							p2 = MathHelper.easeOutCubic(1.0 - ((double) swingProgress - 0.5) * 2.0);
						}

						if (multiplier != 1.0) {
							multiplier = (double) ((float) (1.0 - (1.0 - multiplier) * p2));
						}
						if (attack_specific_modifier != 1.0) {
							attack_specific_modifier = (double) ((float) (1.0 - (1.0 - attack_specific_modifier) * p2));
						}
					}

					Input var10000 = clientPlayer.input;
					var10000.forwardImpulse = (float) ((double) var10000.forwardImpulse * multiplier * attack_specific_modifier);
					var10000 = clientPlayer.input;
					var10000.leftImpulse = (float) ((double) var10000.leftImpulse * multiplier * attack_specific_modifier);
				}
			}
		}
		boolean isMovementLockingDisabled = activeItemStack.is(BetterCombatExtension.DISABLES_MOVEMENT_LOCKING_DURING_ATTACK);
		if (betterCombatExtensionServerConfig.enable_movement_locking_attacks.get() && !isMovementLockingDisabled && isWeaponSwingInProgress) {
			boolean isVehicleDisablingMovementLocking = clientPlayer.getVehicle() != null && clientPlayer.getVehicle().getType().is(BetterCombatExtension.DISABLES_MOVEMENT_LOCKING_WHEN_RIDDEN);
			if (!clientPlayer.isPassenger() || !isVehicleDisablingMovementLocking) {
				Input var10000 = clientPlayer.input;
				var10000.forwardImpulse = 0.0F;
				var10000 = clientPlayer.input;
				var10000.leftImpulse = 0.0F;
			}
		}
		boolean isJumpRestrictionDisabled = activeItemStack.is(BetterCombatExtension.DISABLES_JUMP_RESTRICTION_DURING_ATTACK);
		if (betterCombatExtensionServerConfig.enable_jump_restriction_during_attacks.get() && !isJumpRestrictionDisabled && isWeaponSwingInProgress) {
			boolean isVehicleDisablingJumpRestriction = clientPlayer.getVehicle() != null && clientPlayer.getVehicle().getType().is(BetterCombatExtension.DISABLES_JUMP_RESTRICTION_WHEN_RIDDEN);
			if (!clientPlayer.isPassenger() || !isVehicleDisablingJumpRestriction) {
				Input var10000 = clientPlayer.input;
				var10000.jumping = false;
			}
		}
	}
}
