package cc.thonly.polydex2eiv;

import cc.thonly.polydex2eiv.network.CustomBytePayload;
import de.crafty.eiv.common.CommonEIVClient;
import de.crafty.eiv.common.recipe.ItemViewRecipes;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polymer.core.api.other.PolymerScreenHandlerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SuppressWarnings("unchecked")
public class Polydex2EIV implements ModInitializer {
    public static final String MOD_ID = "polydex2eiv";
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        ItemViewRecipes instance = ItemViewRecipes.INSTANCE;
        try {
            Class<ItemViewRecipes> clazz = ItemViewRecipes.class;
            Field field = clazz.getDeclaredField("fluidItemMap");
            field.setAccessible(true);
            Object object = field.get(instance);
            HashMap<Fluid, Item> fluidItemMap = (HashMap<Fluid, Item>) object;
            for (Map.Entry<Fluid, Item> entry : fluidItemMap.entrySet()) {
                Item item = entry.getValue();
                RegistrySyncUtils.setServerEntry(Registries.ITEM, item);
            }
            ScreenHandlerType<?> screenHandlerType = Registries.SCREEN_HANDLER.get(Identifier.of("eiv:recipe_view"));
            if (screenHandlerType != null) {
                RegistrySyncUtils.setServerEntry(Registries.SCREEN_HANDLER, screenHandlerType);
            }
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                PolymerScreenHandlerUtils.registerType(CommonEIVClient.RECIPE_VIEW_MENU);
            }
            PolymerResourcePackUtils.addModAssets("eiv");
        } catch (Exception e) {
            log.error("Can't make reflection for EIV");
        }

        CustomBytePayload.Receiver.registerHook("on_click_eiv_stack_input", (player, command, data) -> {
            String stringId = new String(data, StandardCharsets.UTF_8).intern();
            Identifier id = Identifier.tryParse(stringId);
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) {
                return;
            }
            ItemStack itemStack = item.getDefaultStack();
            PolydexEntry entry = (PolydexEntry) PolydexImpl.getEntry(itemStack);
            if (entry == null) {
                entry = PolydexImpl.ITEM_ENTRIES.nonEmptyById().get(id);
            }
            if (entry != null) {
                PolydexPageUtils.openUsagesListUi(player, entry, null);
            }
        });

        CustomBytePayload.Receiver.registerHook("on_click_eiv_stack_result", (player, command, data) -> {
            String stringId = new String(data, StandardCharsets.UTF_8).intern();
            Identifier id = Identifier.of(stringId);
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) {
                return;
            }
            ItemStack itemStack = item.getDefaultStack();
            PolydexEntry entry = (PolydexEntry) PolydexImpl.getEntry(itemStack);
            if (entry == null) {
                entry = PolydexImpl.ITEM_ENTRIES.nonEmptyById().get(id);
            }
            if (entry != null) {
                PolydexPageUtils.openRecipeListUi(player, entry, null);
            }
        });

        PayloadTypeRegistry.playC2S().register(CustomBytePayload.PACKET_ID, CustomBytePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CustomBytePayload.PACKET_ID, CustomBytePayload.Receiver::receiveServer);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static void setServer(MinecraftServer server) {
        Polydex2EIV.server = server;
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }
}