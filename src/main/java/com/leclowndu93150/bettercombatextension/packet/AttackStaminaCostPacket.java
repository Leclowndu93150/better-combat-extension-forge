package com.leclowndu93150.bettercombatextension.packet;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.network.ExtensionNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AttackStaminaCostPacket {
	public final float staminaCost;

	public AttackStaminaCostPacket(float staminaCost) {
		this.staminaCost = staminaCost;
	}

	public AttackStaminaCostPacket(FriendlyByteBuf buf) {
		this(buf.readFloat());
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeFloat(this.staminaCost);
	}

	public static void handle(AttackStaminaCostPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			float staminaCost = packet.staminaCost;

			BetterCombatExtension.LOGGER.info("=== Attack Stamina Debug ===");
			BetterCombatExtension.LOGGER.info("Player: {}", player.getName().getString());
			BetterCombatExtension.LOGGER.info("Is Creative: {}", player.isCreative());
			BetterCombatExtension.LOGGER.info("Stamina Cost from Packet: {}", staminaCost);
			BetterCombatExtension.LOGGER.info("Is Stamina Attributes Loaded: {}", BetterCombatExtension.isStaminaAttributesLoaded);

			if (BetterCombatExtension.isStaminaAttributesLoaded) {
				float currentStamina = BetterCombatExtension.getCurrentStamina(player);
				BetterCombatExtension.LOGGER.info("Current Stamina: {}", currentStamina);

				boolean debugMode = true;

				if (debugMode) {
					BetterCombatExtension.LOGGER.info("DEBUG MODE: Allowing attack regardless of stamina");
					if (staminaCost > 0) {
						BetterCombatExtension.addStamina(player, -staminaCost);
					}
				} else {
					if (currentStamina <= 0 && !player.isCreative()) {
						BetterCombatExtension.LOGGER.warn("CANCELING ATTACK: Insufficient stamina!");
						ExtensionNetworkHandler.sendToPlayer(new CancelAttackPacket(player.getId()), player);
					} else {
						BetterCombatExtension.LOGGER.info("Attack allowed, deducting {} stamina", staminaCost);
						BetterCombatExtension.addStamina(player, -staminaCost);
					}
				}
			} else {
				BetterCombatExtension.LOGGER.info("Stamina Attributes not loaded, allowing attack");
			}

			BetterCombatExtension.LOGGER.info("=========================");
		});
		context.setPacketHandled(true);
	}
}
