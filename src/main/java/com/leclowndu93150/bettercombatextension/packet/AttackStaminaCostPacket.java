package com.leclowndu93150.bettercombatextension.packet;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

public class AttackStaminaCostPacket implements FabricPacket {
	public static final PacketType<AttackStaminaCostPacket> TYPE = PacketType.create(
			BetterCombatExtension.identifier("attack_stamina_cost"),
			AttackStaminaCostPacket::new
	);

	public final float staminaCost;

	public AttackStaminaCostPacket(float staminaCost) {
		this.staminaCost = staminaCost;
	}

	public AttackStaminaCostPacket(FriendlyByteBuf buf) {
		this(buf.readFloat());
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(this.staminaCost);
	}
}
