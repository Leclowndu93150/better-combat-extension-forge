package com.leclowndu93150.bettercombatextension.packet;

import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelAttackPacket {
	public final int entityId;

	public CancelAttackPacket(int entityId) {
		this.entityId = entityId;
	}

	public CancelAttackPacket(FriendlyByteBuf buf) {
		this(buf.readInt());
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.entityId);
	}

	public static void handle(CancelAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			LocalPlayer player = Minecraft.getInstance().player;
			int entityId = packet.entityId;

			if (player != null && player.level().getEntity(entityId) != null) {
				Player player2 = (Player) player.level().getEntity(entityId);
				if (player2 != null && player == player2) {
					((MinecraftClient_BetterCombat) Minecraft.getInstance()).cancelUpswing();
				}
			}
		});
		context.setPacketHandled(true);
	}
}
