package com.leclowndu93150.bettercombatextension;

import com.leclowndu93150.bettercombatextension.config.ServerConfig;
import com.leclowndu93150.bettercombatextension.packet.AttackStaminaCostPacket;
import com.leclowndu93150.bettercombatextension.packet.AttackStaminaCostPacketReceiver;
import com.github.theredbrain.staminaattributes.entity.StaminaUsingEntity;
import com.leclowndu93150.bettercombatextension.packet.CancelAttackPacket;
import com.leclowndu93150.bettercombatextension.packet.CancelAttackPacketReceiver;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BetterCombatExtension.MOD_ID)
public class BetterCombatExtension{
	public static final String MOD_ID = "bettercombatextension";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ServerConfig SERVER_CONFIG = ConfigApiJava.registerAndLoadConfig(ServerConfig::new, RegisterType.BOTH);

	public static final boolean isShoulderSurfingLoaded = FabricLoader.getInstance().isModLoaded("shouldersurfing");

	public static final boolean isStaminaAttributesLoaded = FabricLoader.getInstance().isModLoaded("staminaattributes");

	public static Attribute ATTACK_STAMINA_COST;

	public static float getCurrentStamina(LivingEntity livingEntity) {
		float currentStamina = 0.0F;
		if (isStaminaAttributesLoaded) {
			currentStamina = ((StaminaUsingEntity) livingEntity).staminaattributes$getStamina();
		}
		return currentStamina;
	}

	public static void addStamina(LivingEntity livingEntity, float amount) {
		if (isStaminaAttributesLoaded) {
			((StaminaUsingEntity) livingEntity).staminaattributes$addStamina(amount);
		}
	}

	public BetterCombatExtension() {
		ServerPlayNetworking.registerGlobalReceiver(AttackStaminaCostPacket.TYPE, new AttackStaminaCostPacketReceiver());

		if(FMLLoader.getDist().isClient()) ClientPlayNetworking.registerGlobalReceiver(CancelAttackPacket.TYPE, new CancelAttackPacketReceiver());
	}

	public static ResourceLocation identifier(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static final TagKey<EntityType<?>> DISABLES_JUMP_RESTRICTION_WHEN_RIDDEN = TagKey.create(Registries.ENTITY_TYPE, identifier("disables_jump_restriction_when_ridden"));
	public static final TagKey<EntityType<?>> DISABLES_MOVEMENT_LOCKING_WHEN_RIDDEN = TagKey.create(Registries.ENTITY_TYPE, identifier("disables_movement_locking_when_ridden"));
	public static final TagKey<Item> DISABLES_JUMP_RESTRICTION_DURING_ATTACK = TagKey.create(Registries.ITEM, identifier("disables_jump_restriction_during_attack"));
	public static final TagKey<Item> DISABLES_MOVEMENT_LOCKING_DURING_ATTACK = TagKey.create(Registries.ITEM, identifier("disables_movement_locking_during_attack"));
	public static final TagKey<Item> IGNORES_ATTACK_MOVEMENT_PENALTY = TagKey.create(Registries.ITEM, identifier("ignores_attack_movement_penalty"));
	public static final TagKey<Item> EMPTY_HAND_WEAPONS = TagKey.create(Registries.ITEM, identifier("empty_hand_weapons"));
}