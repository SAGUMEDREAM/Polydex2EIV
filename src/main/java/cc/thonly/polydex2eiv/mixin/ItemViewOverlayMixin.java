package cc.thonly.polydex2eiv.mixin;

import cc.thonly.polydex2eiv.Polydex2EIV;
import cc.thonly.polydex2eiv.network.CustomBytePayloadClient;
import cc.thonly.polydex2eiv.util.ClientPolymerItemUtils;
import de.crafty.eiv.common.overlay.ItemViewOverlay;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Pseudo
@Mixin(
        value = ItemViewOverlay.class
)
public class ItemViewOverlayMixin {
    @ModifyVariable(
            method = "openRecipeView",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    public ItemStack modifyStack(ItemStack stack) {
        MinecraftServer server = Polydex2EIV.getServer();
        if (server != null) {
            DynamicRegistryManager.Immutable registryManager = server.getRegistryManager();
            return PolymerItemUtils.getRealItemStack(stack, registryManager);
        }
        return stack;
    }

    @Inject(method = "openRecipeView", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getInstance()Lnet/minecraft/client/MinecraftClient;"),
            cancellable = true)
    private static void invokeItemStack(ItemStack stack, ItemViewOverlay.ItemViewOpenType openType, CallbackInfo ci) {
        if (ClientPolymerItemUtils.isPolymerItem(stack)) {
            Optional<String> polymerStackIdOptional = ClientPolymerItemUtils.getPolymerStackId(stack);
            if (polymerStackIdOptional.isPresent()) {
                String itemId = polymerStackIdOptional.get();
                String packetId = openType == ItemViewOverlay.ItemViewOpenType.INPUT ? "on_click_eiv_stack_input" : "on_click_eiv_stack_result";
                CustomBytePayloadClient.Sender.sendLargePayload(packetId, itemId.getBytes(StandardCharsets.UTF_8));
                ci.cancel();
            }
        }
    }

    @Inject(method = "openRecipeView",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/crafty/eiv/common/overlay/ItemViewOverlay$ItemViewOpenType$RecipeProvider;retrieveRecipes(Lnet/minecraft/item/ItemStack;)Ljava/util/List;"
            ),
            cancellable = true
    )
    public void beforeFound(ItemStack stack, ItemViewOverlay.ItemViewOpenType openType, CallbackInfo ci) {
        if (PolymerItemUtils.isPolymerServerItem(stack)) {
            Item item = stack.getItem();
            Identifier id = Registries.ITEM.getId(item);
            String stringId = id.toString();
            String packetId = openType == ItemViewOverlay.ItemViewOpenType.INPUT ? "on_click_eiv_stack_input" : "on_click_eiv_stack_result";
            CustomBytePayloadClient.Sender.sendLargePayload(packetId, stringId.getBytes(StandardCharsets.UTF_8));
            ci.cancel();
        }
    }

    @Inject(
            method = "openRecipeView",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
            ),
            cancellable = true
    )
    public void beforeSetScreen(ItemStack stack, ItemViewOverlay.ItemViewOpenType openType, CallbackInfo ci) {
        if (PolymerItemUtils.isPolymerServerItem(stack)) {
            Item item = stack.getItem();
            Identifier id = Registries.ITEM.getId(item);
            String stringId = id.toString();
            String packetId = openType == ItemViewOverlay.ItemViewOpenType.INPUT ? "on_click_eiv_stack_input" : "on_click_eiv_stack_result";
            CustomBytePayloadClient.Sender.sendLargePayload(packetId, stringId.getBytes(StandardCharsets.UTF_8));
            ci.cancel();
        }
    }

}
