package cc.thonly.polydex2eiv.network;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class CustomBytePayloadClient {
    public static class Sender {
        public static final int MAX_CHUNK_SIZE = 1024;

        public static void sendLargePayload(String command, byte[] fullData) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() == null) {
                log.warn("Cannot send payload: client not connected.");
                return;
            }

            int totalLength = fullData.length;
            for (int offset = 0; offset < totalLength; offset += MAX_CHUNK_SIZE) {
                int end = Math.min(offset + MAX_CHUNK_SIZE, totalLength);
                byte[] chunk = new byte[end - offset];
                System.arraycopy(fullData, offset, chunk, 0, end - offset);

                CustomBytePayload payload = new CustomBytePayload(command, offset, chunk);
                ClientPlayNetworking.send(payload);
            }
        }
    }

    public static class Receiver {
        private static final Map<String, PayloadHook> HOOKS = new HashMap<>();
        private static final Map<UUID, Map<String, ByteArrayOutputStream>> playerCommandBuffer = new HashMap<>();

        public static synchronized <T extends CustomPayload> void receiveClient(T rawPayload, ClientPlayNetworking.Context context) {
            if (!(rawPayload instanceof CustomBytePayload payload)) return;

            try {
                UUID clientId = MinecraftClient.getInstance().getSession().getUuidOrNull();
                MinecraftClient client = MinecraftClient.getInstance();
                if (clientId == null) {
                    log.error("Client UUID is null. Skipping payload handle.");
                    return;
                }
                client.execute(() -> {
                    playerCommandBuffer.putIfAbsent(clientId, new HashMap<>());
                    Map<String, ByteArrayOutputStream> commandBuffer = playerCommandBuffer.get(clientId);
                    ByteArrayOutputStream buffer = commandBuffer.computeIfAbsent(payload.command(), k -> new ByteArrayOutputStream());

                    try {
                        buffer.write(payload.data());
                    } catch (IOException e) {
                        log.error("Can't receive packet", e);
                        return;
                    }

                    if (payload.data().length < CustomBytePayload.Sender.MAX_CHUNK_SIZE) {
                        byte[] fullData = buffer.toByteArray();
                        commandBuffer.remove(payload.command());

                        PayloadHook hook = HOOKS.get(payload.command());
                        if (hook != null) {
                            hook.handle(client.player, payload.command(), fullData);
                        } else {
                            log.error("Unknown payload command: {}", payload.command());
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Exception while receiving packet", e);
            }
        }

        public static void register(String command, PayloadHook hook) {
            HOOKS.put(command, hook);
        }

        @FunctionalInterface
        public interface PayloadHook {
            void handle(PlayerEntity player, String command, byte[] data);
        }
    }
}
