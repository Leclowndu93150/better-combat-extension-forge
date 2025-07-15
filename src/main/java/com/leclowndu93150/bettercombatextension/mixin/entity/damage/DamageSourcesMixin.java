package com.leclowndu93150.bettercombatextension.mixin.entity.damage;

import com.leclowndu93150.bettercombatextension.bettercombat.DuckWeaponAttributesAttackMixin;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(DamageSources.class)
public abstract class DamageSourcesMixin {

	@Inject(method = "playerAttack", at = @At("RETURN"), cancellable = true)
	public void bettercombatextension$playerAttack(Player attacker, CallbackInfoReturnable<DamageSource> cir) {
		AttackHand attackHand = ((EntityPlayer_BetterCombat) attacker).getCurrentAttack();
		if (attackHand != null) {
			String damageTypeString = ((DuckWeaponAttributesAttackMixin) (Object) attackHand.attack()).bettercombatextension$getDamageType();
			if (damageTypeString != null && !damageTypeString.isEmpty() && ResourceLocation.isValidResourceLocation(damageTypeString)) {
				ResourceLocation damageTypeId = ResourceLocation.tryParse(damageTypeString);
				if (damageTypeId != null) {
					ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);
					Registry<DamageType> registry = attacker.damageSources().damageTypes;
					Optional<Holder.Reference<DamageType>> optional = registry.getHolder(key);
					optional.ifPresent(damageTypeReference -> cir.setReturnValue(new DamageSource(damageTypeReference, attacker)));
				}
			}
		}
	}
}
