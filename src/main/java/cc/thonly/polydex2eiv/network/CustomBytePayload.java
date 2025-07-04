package cc.thonly.polydex2eiv.network;

import cc.thonly.polydex2eiv.Polydex2EIV;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public record CustomBytePayload(
        String command,     // 指令
        int offset,         // 当前片的偏移
        byte[] data         // 当前片的数据
) implements CustomPayload {
    public static final Identifier ID = Polydex2EIV.id("byte_payload");
    public static final Id<CustomBytePayload> PACKET_ID = new Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, CustomBytePayload> CODEC = PacketCodec.of(
            CustomBytePayload::write,
            CustomBytePayload::read
    );

    public static CustomBytePayload read(RegistryByteBuf buf) {
        String command = buf.readString();       // 指令
        int offset = buf.readVarInt();           // 当前偏移
        int length = buf.readVarInt();           // 当前片长度
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new CustomBytePayload(command, offset, data);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeString(command);
        buf.writeVarInt(offset);
        buf.writeVarInt(data.length);
        buf.writeBytes(data);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

    public static class Sender {
        public static final int MAX_CHUNK_SIZE = 1024;

        public static void sendLargePayload(ServerPlayerEntity player, String command, byte[] fullData) {
            MinecraftServer server = player.getServer();
            if (server == null) return;

            int totalLength = fullData.length;
            for (int offset = 0; offset < totalLength; offset += MAX_CHUNK_SIZE) {
                int end = Math.min(offset + MAX_CHUNK_SIZE, totalLength);
                byte[] chunk = Arrays.copyOfRange(fullData, offset, end);

                CustomBytePayload payload = new CustomBytePayload(command, offset, chunk);
                player.networkHandler.sendPacket(new CustomPayloadC2SPacket(payload));
            }
        }
    }

    public static class Receiver {
        private static final Map<UUID, Map<String, ByteArrayOutputStream>> playerCommandBuffer = new HashMap<>();
        private static final Map<String, PayloadHook> HOOKS = new HashMap<>();

        public static synchronized <T extends CustomPayload> void receiveServer(T rawPayload, ServerPlayNetworking.Context context) {
            if (!(rawPayload instanceof CustomBytePayload payload)) return;

            try {
                ServerPlayerEntity player = context.player();
                UUID playerId = player.getUuid();

                synchronized (MinecraftServer.class) {
                    context.server().execute(() -> {
                        playerCommandBuffer.putIfAbsent(playerId, new HashMap<>());
                        Map<String, ByteArrayOutputStream> commandBuffer = playerCommandBuffer.get(playerId);
                        ByteArrayOutputStream buffer = commandBuffer.computeIfAbsent(payload.command(), k -> new ByteArrayOutputStream());

                        try {
                            buffer.write(payload.data());
                        } catch (IOException e) {
                            log.error("Can't receive packet", e);
                            return;
                        }

                        if (payload.data().length < Sender.MAX_CHUNK_SIZE) {
                            byte[] fullData = buffer.toByteArray();
                            commandBuffer.remove(payload.command());

                            PayloadHook hook = HOOKS.get(payload.command());
                            if (hook != null) {
                                hook.handle(player, payload.command(), fullData);
                            } else {
                                player.sendMessage(Text.literal("Unknown payload command: " + payload.command()), false);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Can't receive packet", e);
            }
        }

        public static void register(String command, PayloadHook hook) {
            HOOKS.put(command, hook);
        }

        @FunctionalInterface
        public interface PayloadHook {
            void handle(ServerPlayerEntity player, String command, byte[] data);
        }
    }
}
