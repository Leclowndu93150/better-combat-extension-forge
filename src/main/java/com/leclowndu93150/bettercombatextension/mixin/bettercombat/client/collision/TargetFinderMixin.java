package com.leclowndu93150.bettercombatextension.mixin.bettercombat.client.collision;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.config.ServerConfig;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.AttackRangeExtensions;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.client.collision.WeaponHitBoxes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = TargetFinder.class, remap = false)
public abstract class TargetFinderMixin {

	@Shadow
	private static double applyAttackRangeModifiers(Player player, double attackRange) {
		throw new AssertionError();
	}

	/**
	 *
	 * @author TheRedBrain
	 * @reason integrate "restricted_attack_pitch" option
	 */
	@Overwrite
	public static TargetFinder.TargetResult findAttackTargetResult(Player player, Entity cursorTarget, WeaponAttributes.Attack attack, double attackRange) {
		Vec3 origin = TargetFinder.getInitialTracingPoint(player);
		List<Entity> entities = TargetFinder.getInitialTargets(player, cursorTarget, attackRange);
		if (!AttackRangeExtensions.sources().isEmpty()) {
			attackRange = applyAttackRangeModifiers(player, attackRange);
		}

		boolean isSpinAttack = attack.angle() > 180.0;
		Vec3 size = WeaponHitBoxes.createHitbox(attack.hitbox(), attackRange, isSpinAttack);
		ServerConfig serverConfig = BetterCombatExtension.SERVER_CONFIG;
		float attackPitch = serverConfig.restrict_attack_pitch.get() ? Mth.clamp(player.getXRot(), -serverConfig.attack_pitch_range.get(), serverConfig.attack_pitch_range.get()) : player.getXRot();
		OrientedBoundingBox obb = new OrientedBoundingBox(origin, size, attackPitch, player.getYRot());
		if (!isSpinAttack) {
			obb = obb.offsetAlongAxisZ(size.z / 2.0);
		}

		obb.updateVertex();
		TargetFinder.CollisionFilter collisionFilter = new TargetFinder.CollisionFilter(obb);
		entities = collisionFilter.filter(entities);
		TargetFinder.RadialFilter radialFilter = new TargetFinder.RadialFilter(origin, obb.axisZ, attackRange, attack.angle());
		entities = radialFilter.filter(entities);
		return new TargetFinder.TargetResult(entities, obb);
	}

}
