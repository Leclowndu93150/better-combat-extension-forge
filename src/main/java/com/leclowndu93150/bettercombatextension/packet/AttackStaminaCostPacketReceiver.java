package com.leclowndu93150.bettercombatextension.packet;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class AttackStaminaCostPacketReceiver implements ServerPlayNetworking.PlayPacketHandler<AttackStaminaCostPacket> {

	@Override
	public void receive(AttackStaminaCostPacket packet, ServerPlayer player, PacketSender responseSender) {

		float staminaCost = packet.staminaCost;

		if (BetterCombatExtension.isStaminaAttributesLoaded) {
			if (BetterCombatExtension.getCurrentStamina(player) <= 0 && !player.isCreative()) {
				ServerPlayNetworking.send(player, new CancelAttackPacket(player.getId()));
			} else {
				BetterCombatExtension.addStamina(player, -staminaCost);
			}
		}
	}
}
