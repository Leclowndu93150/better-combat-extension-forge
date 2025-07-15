package com.leclowndu93150.bettercombatextension.mixin.bettercombat;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = PlayerAttackHelper.class, remap = false)
public class PlayerAttackHelperMixin {

	@Inject(method = "isDualWielding", at = @At("RETURN"), cancellable = true)
	private static void bettercombatextension$isDualWielding(Player player, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(cir.getReturnValue() && !(!player.getMainHandItem().is(BetterCombatExtension.EMPTY_HAND_WEAPONS) && player.getOffhandItem().is(BetterCombatExtension.EMPTY_HAND_WEAPONS)));
		cir.cancel();
	}

	/**
	 * @author TheRedBrain
	 * @reason account for alternative two-handing condition
	 */
	@Overwrite
	public static boolean isTwoHandedWielding(Player player) {
		WeaponAttributes mainAttributes = WeaponRegistry.getAttributes(player.getMainHandItem());
		return (mainAttributes != null && mainAttributes.isTwoHanded()) || (player.getOffhandItem().isEmpty() && BetterCombatExtension.SERVER_CONFIG.empty_offhand_equals_two_handing_mainhand.get());
	}
}
