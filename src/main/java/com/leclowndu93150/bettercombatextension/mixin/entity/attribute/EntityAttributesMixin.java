package com.leclowndu93150.bettercombatextension.mixin.entity.attribute;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Attributes.class)
public class EntityAttributesMixin {
	@Shadow
	private static Attribute register(String id, Attribute attribute) {
		throw new AssertionError();
	}

	static {
		BetterCombatExtension.ATTACK_STAMINA_COST = register(BetterCombatExtension.MOD_ID + ":generic.attack_stamina_cost", new RangedAttribute("attribute.name.generic.attack_stamina_cost", 1.0, 0.0, 1024.0).setSyncable(true));
	}
}
