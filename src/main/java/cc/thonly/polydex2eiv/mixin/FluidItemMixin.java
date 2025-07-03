package cc.thonly.polydex2eiv.mixin;

import de.crafty.eiv.common.recipe.item.FluidItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import xyz.nucleoid.packettweaker.PacketContext;

@Mixin(value = FluidItem.class, remap = false)
@Pseudo
public class FluidItemMixin implements PolymerItem {
    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

}
