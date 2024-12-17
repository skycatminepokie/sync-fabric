package dev.kir.sync.item;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SimpleInventory implements Inventory, Nameable {
    public static final Codec<SimpleInventory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().xmap((List<ItemStack> list) -> createDefaultedList(list, 32), (defaultedList) -> defaultedList).fieldOf("main").forGetter(SimpleInventory::getMain),
            ItemStack.CODEC.listOf().xmap((List<ItemStack> list) -> createDefaultedList(list, 4), (defaultedList) -> defaultedList).fieldOf("armor").forGetter(SimpleInventory::getArmor),
            ItemStack.CODEC.listOf().xmap((List<ItemStack> list) -> createDefaultedList(list, 1), (defaultedList) -> defaultedList).fieldOf("offHand").forGetter(SimpleInventory::getOffHand),
            Codec.INT.fieldOf("selectedSlot").forGetter(SimpleInventory::getSelectedSlot)
    ).apply(instance, SimpleInventory::new));
    public final DefaultedList<ItemStack> main;
    public final DefaultedList<ItemStack> armor;
    public final DefaultedList<ItemStack> offHand;
    private final List<DefaultedList<ItemStack>> combinedInventory;
    public int selectedSlot;
    private int changeCount;

    public SimpleInventory(DefaultedList<ItemStack> main, DefaultedList<ItemStack> armor, DefaultedList<ItemStack> offHand, int selectedSlot) {
        this.main = main;
        this.armor = armor;
        this.offHand = offHand;
        this.combinedInventory = ImmutableList.of(main, armor, offHand);
        this.selectedSlot = selectedSlot;
    }
    public SimpleInventory() {
        this.main = DefaultedList.ofSize(36, ItemStack.EMPTY);
        this.armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
        this.offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
        this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
    }

    private static DefaultedList<ItemStack> createDefaultedList(List<ItemStack> list, int size) {
        DefaultedList<ItemStack> ret = DefaultedList.ofSize(size, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            ret.set(i, list.get(i));
        }
        return ret;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    public void clear() {
        for (DefaultedList<ItemStack> itemStacks : this.combinedInventory) {
            itemStacks.clear();
        }
    }

    public void clone(Inventory other) {
        int thisSize = this.size();
        int otherSize = other.size();
        for (int i = 0; i < thisSize; ++i) {
            this.setStack(i, i < otherSize ? other.getStack(i) : ItemStack.EMPTY);
        }

        if (other instanceof PlayerInventory playerInventory) {
            this.selectedSlot = playerInventory.selectedSlot;
        } else if (other instanceof SimpleInventory simpleInventory) {
            this.selectedSlot = simpleInventory.selectedSlot;
        }
    }

    public void copyTo(Inventory other) {
        int thisSize = this.size();
        int otherSize = other.size();
        for (int i = 0; i < otherSize; ++i) {
            other.setStack(i, i < thisSize ? this.getStack(i) : ItemStack.EMPTY);
        }

        if (other instanceof PlayerInventory playerInventory) {
            playerInventory.selectedSlot = this.selectedSlot;
        } else if (other instanceof SimpleInventory simpleInventory) {
            simpleInventory.selectedSlot = this.selectedSlot;
        }
    }

    public DefaultedList<ItemStack> getArmor() {
        return armor;
    }

    public int getChangeCount() {
        return this.changeCount;
    }

    public DefaultedList<ItemStack> getMain() {
        return main;
    }

    @Override
    public Text getName() {
        return Text.of("container.inventory");
    }

    public DefaultedList<ItemStack> getOffHand() {
        return offHand;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    @Override
    public ItemStack getStack(int slot) {
        for (DefaultedList<ItemStack> inventory : this.combinedInventory) {
            if (slot < inventory.size()) {
                return inventory.get(slot);
            }
            slot -= inventory.size();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return this.combinedInventory.stream().flatMap(Collection::stream).allMatch(ItemStack::isEmpty);
    }

    @Override
    public void markDirty() {
        ++this.changeCount;
    }

    public void readNbt(NbtList nbtList, RegistryWrapper.WrapperLookup lookup) {
        this.main.clear();
        this.armor.clear();
        this.offHand.clear();

        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack itemStack = ItemStack.fromNbtOrEmpty(lookup, nbtCompound);
            if (!itemStack.isEmpty()) {
                if (j < this.main.size()) {
                    this.main.set(j, itemStack);
                } else if (j >= 100 && j < this.armor.size() + 100) {
                    this.armor.set(j - 100, itemStack);
                } else if (j >= 150 && j < this.offHand.size() + 150) {
                    this.offHand.set(j - 150, itemStack);
                }
            }
        }

    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        for (DefaultedList<ItemStack> inventory : this.combinedInventory) {
            if (slot < inventory.size()) {
                return !inventory.get(slot).isEmpty() ? Inventories.splitStack(inventory, slot, amount) : ItemStack.EMPTY;
            }
            slot -= inventory.size();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        for (DefaultedList<ItemStack> inventory : this.combinedInventory) {
            if (slot < inventory.size()) {
                if (!inventory.get(slot).isEmpty()) {
                    ItemStack itemStack = inventory.get(slot);
                    inventory.set(slot, ItemStack.EMPTY);
                    return itemStack;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            slot -= inventory.size();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        for (DefaultedList<ItemStack> inventory : this.combinedInventory) {
            if (slot < inventory.size()) {
                inventory.set(slot, stack);
                return;
            }
            slot -= inventory.size();
        }
    }

    @Override
    public int size() {
        return this.main.size() + this.armor.size() + this.offHand.size();
    }

    public NbtList writeNbt(NbtList nbtList, RegistryWrapper.WrapperLookup lookup) {
        for (Map.Entry<DefaultedList<ItemStack>, Integer> inventoryInfo : Map.of(this.main, 0, this.armor, 100, this.offHand, 150).entrySet()) {
            DefaultedList<ItemStack> inventory = inventoryInfo.getKey();
            int delta = inventoryInfo.getValue();
            for (int i = 0; i < inventory.size(); ++i) {
                if (!inventory.get(i).isEmpty()) {
                    NbtCompound compound = new NbtCompound();
                    compound.putByte("Slot", (byte) (i + delta));
                    nbtList.add(inventory.get(i).encode(lookup, compound));
                }
            }
        }
        return nbtList;
    }
}
