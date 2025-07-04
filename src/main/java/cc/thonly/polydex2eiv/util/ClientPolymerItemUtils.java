package cc.thonly.polydex2eiv.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Optional;

public class ClientPolymerItemUtils {
    public static boolean isPolymerItem(ItemStack itemStack) {
        return getPolymerStackId(itemStack).isPresent();
    }

    public static Optional<String> getPolymerStackId(ItemStack itemStack) {
        NbtComponent customData = getCustomData(itemStack);
        if (customData == null) {
            return Optional.empty();
        }
        NbtCompound nbtCompound = customData.copyNbt();
        NbtElement element = nbtCompound.get("$polymer:stack");
        if (!(element instanceof NbtCompound polymerStack)) {
            return Optional.empty();
        }
        if (polymerStack.contains("id")) {
            return polymerStack.getString("id");
        }
        return Optional.empty();
    }

    public static NbtComponent getCustomData(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.CUSTOM_DATA);
    }
}
