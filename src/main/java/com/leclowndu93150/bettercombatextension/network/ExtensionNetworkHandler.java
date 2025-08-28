package com.leclowndu93150.bettercombatextension.network;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.packet.AttackStaminaCostPacket;
import com.leclowndu93150.bettercombatextension.packet.CancelAttackPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ExtensionNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterCombatExtension.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(CancelAttackPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CancelAttackPacket::new)
                .encoder(CancelAttackPacket::toBytes)
                .consumerMainThread(CancelAttackPacket::handle)
                .add();

        INSTANCE.messageBuilder(AttackStaminaCostPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AttackStaminaCostPacket::new)
                .encoder(AttackStaminaCostPacket::toBytes)
                .consumerMainThread(AttackStaminaCostPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}