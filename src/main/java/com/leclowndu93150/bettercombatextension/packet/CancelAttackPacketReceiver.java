package com.leclowndu93150.bettercombatextension.packet;

import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class CancelAttackPacketReceiver implements ClientPlayNetworking.PlayPacketHandler<CancelAttackPacket> {

	@Override
	public void receive(CancelAttackPacket packet, LocalPlayer player, PacketSender responseSender) {

		int entityId = packet.entityId;

		if (player != null && player.level().getEntity(entityId) != null) {
			Player player2 = (Player) player.level().getEntity(entityId);
			if (player2 != null && player == player2) {
				((MinecraftClient_BetterCombat) Minecraft.getInstance()).cancelUpswing();
			}
		}
	}
}
