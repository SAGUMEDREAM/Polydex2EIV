package cc.thonly.polydex2eiv;

import cc.thonly.polydex2eiv.network.CustomBytePayload;
import cc.thonly.polydex2eiv.network.CustomBytePayloadClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Polydex2EIVClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(CustomBytePayload.PACKET_ID, CustomBytePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(CustomBytePayload.PACKET_ID, CustomBytePayloadClient.Receiver::receiveClient);
    }
}
