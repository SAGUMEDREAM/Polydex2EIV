package cc.thonly.polydex2eiv.mixin;

import de.crafty.eiv.common.overlay.ItemFilters;
import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import eu.pb4.polymer.core.impl.client.InternalClientRegistry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;

@Mixin(ItemFilters.class)
public class ItemFiltersMixin {
    @Inject(method = "defaultFilter", at = @At("RETURN"), cancellable = true, remap = false)
    private static void modifyItemStacks(String query, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> list = cir.getReturnValue();
        Iterator<ClientPolymerItem> iterator = ClientPolymerItem.REGISTRY.stream().iterator();
        while (iterator.hasNext()) {
            ClientPolymerItem next = iterator.next();
            list.add(next.visualStack());
        }
        cir.setReturnValue(list);
    }
}
