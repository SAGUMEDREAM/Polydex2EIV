package cc.thonly.polydex2eiv.mixin;

import cc.thonly.polydex2eiv.Polydex2EIV;
import cc.thonly.polydex2eiv.network.CustomBytePayloadClient;
import de.crafty.eiv.common.api.recipe.IEivViewRecipe;
import de.crafty.eiv.common.overlay.ItemViewOverlay;
import de.crafty.eiv.common.recipe.inventory.RecipeViewMenu;
import de.crafty.eiv.common.recipe.inventory.RecipeViewScreen;
import de.crafty.eiv.common.recipe.inventory.SlotContent;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Mixin(value = ItemViewOverlay.class, remap = false, priority = 1)
@Pseudo
public class ItemViewOverlayMixin {
    @ModifyVariable(
            method = "openRecipeView",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private ItemStack modifyStack(ItemStack stack) {
        MinecraftServer server = Polydex2EIV.getServer();
        if (server != null) {
            DynamicRegistryManager.Immutable registryManager = server.getRegistryManager();
            return PolymerItemUtils.getRealItemStack(stack, registryManager);
        }
        return stack;
    }

    @Inject(method = "openRecipeView", at = @At("HEAD"), cancellable = true)
    public void before(ItemStack stack, ItemViewOverlay.ItemViewOpenType openType, CallbackInfo ci) {
        try {
            if (!stack.isEmpty()) {
                ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
                if (clientPlayer != null) {
                    Class<ItemViewOverlay.ItemViewOpenType> clazz = ItemViewOverlay.ItemViewOpenType.class;
                    Field recipeProviderField = null;

                    recipeProviderField = clazz.getDeclaredField("recipeProvider");
                    recipeProviderField.setAccessible(true);

                    Object object = recipeProviderField.get(openType);
                    Class<?> rpfClass = object.getClass();
                    Method retrieveRecipes = rpfClass.getDeclaredMethod("retrieveRecipes", ItemStack.class);
                    retrieveRecipes.setAccessible(true);

                    if (FabricLoader.getInstance().isModLoaded("polydex") && PolymerItemUtils.isPolymerServerItem(stack)) {
                        Item item = stack.getItem();
                        Identifier id = Registries.ITEM.getId(item);
                        String stringId = id.toString();
                        String packetId = openType == ItemViewOverlay.ItemViewOpenType.INPUT ? "on_click_eiv_stack_input" : "on_click_eiv_stack_result";
                        CustomBytePayloadClient.Sender.sendLargePayload(packetId, stringId.getBytes(StandardCharsets.UTF_8));
                        ci.cancel();
                    }
                    List<IEivViewRecipe> foundRecipes = (List<IEivViewRecipe>) retrieveRecipes.invoke(object, stack);
                    if (!foundRecipes.isEmpty()) {
                        Screen parent = MinecraftClient.getInstance().currentScreen;
                        if (parent instanceof RecipeViewScreen) {
                            RecipeViewScreen viewScreen = (RecipeViewScreen) parent;
                            parent = ((RecipeViewMenu) viewScreen.getScreenHandler()).getParentScreen();
                        }

                        MinecraftClient.getInstance().setScreen(new RecipeViewScreen(new RecipeViewMenu(parent, 0, clientPlayer.getInventory(), foundRecipes, stack, openType == ItemViewOverlay.ItemViewOpenType.RESULT ? SlotContent.Type.RESULT : SlotContent.Type.INGREDIENT), clientPlayer.getInventory(), Text.empty()));
                    }

                }
            }
            ci.cancel();
        } catch (Exception ignored) {
        }
    }

}
