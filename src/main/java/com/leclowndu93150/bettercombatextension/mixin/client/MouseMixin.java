package com.leclowndu93150.bettercombatextension.mixin.client;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.compatability.ShoulderSurfingCompat;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MouseHandler.class)
public abstract class MouseMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V", ordinal = 0), cancellable = true)
	private void bettercombatextension$updateMouse(CallbackInfo ci) {
		if (BetterCombatExtension.SERVER_CONFIG.enable_movement_locking_attacks.get() && ((MinecraftClient_BetterCombat) this.minecraft).isWeaponSwingInProgress() && !ShoulderSurfingCompat.isShoulderSurfingCameraDecoupled()) {
			ci.cancel();
		}
	}
}