package cc.thonly.polydex2eiv.mixin;

import cc.thonly.polydex2eiv.util.ClientPolymerItemUtils;
import de.crafty.eiv.common.overlay.ItemFilters;
import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import eu.pb4.polymer.core.impl.client.InternalClientRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Mixin(ItemFilters.class)
public class ItemFiltersMixin {
    @Inject(method = "defaultFilter", at = @At("RETURN"), cancellable = true, remap = false)
    private static void modifyItemStacks(String query, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> list = cir.getReturnValue();
        Iterator<ClientPolymerItem> iterator = ClientPolymerItem.REGISTRY.stream().iterator();
        while (iterator.hasNext()) {
            ClientPolymerItem next = iterator.next();
            ItemStack itemStack = next.visualStack();
            if (!ClientPolymerItemUtils.isPolymerItem(itemStack)) {
                continue;
            }
            Optional<String> polymerStackId = ClientPolymerItemUtils.getPolymerStackId(itemStack);
            if (polymerStackId.isEmpty()) {
                continue;
            }
            if (Registries.ITEM.get(Identifier.of(polymerStackId.get())) != Items.AIR) {
                continue;
            }
            list.add(itemStack);
        }
        cir.setReturnValue(list);
    }
}
