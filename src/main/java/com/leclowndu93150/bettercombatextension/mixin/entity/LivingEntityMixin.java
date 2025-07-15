package com.leclowndu93150.bettercombatextension.mixin.entity;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.entity.DuckLivingEntityMixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DuckLivingEntityMixin {
	@Shadow
	public abstract double getAttributeValue(Attribute attribute);

	public LivingEntityMixin(EntityType<?> type, Level world) {
		super(type, world);
	}

	@Inject(method = "createLivingAttributes", at = @At("RETURN"))
	private static void bettercombatextension$createLivingAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.getReturnValue()
				.add(BetterCombatExtension.ATTACK_STAMINA_COST)
		;
	}

	@Override
	public float bettercombatextension$getAttackStaminaCost() {
		return (float) this.getAttributeValue(BetterCombatExtension.ATTACK_STAMINA_COST);
	}

}
