package com.leclowndu93150.bettercombatextension.packet;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

public class CancelAttackPacket implements FabricPacket {
	public static final PacketType<CancelAttackPacket> TYPE = PacketType.create(
			BetterCombatExtension.identifier("cancel_attack"),
			CancelAttackPacket::new
	);

	public final int entityId;

	public CancelAttackPacket(int entityId) {
		this.entityId = entityId;
	}

	public CancelAttackPacket(FriendlyByteBuf buf) {
		this(buf.readInt());
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.entityId);
	}
}
