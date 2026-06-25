package ru.syntaxteam.teamstorage.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class InventorySerializer {
    private InventorySerializer() {
    }

    public static byte[] serialize(ItemStack[] contents) {
        return ItemStack.serializeItemsAsBytes(contents);
    }

    public static ItemStack[] deserialize(byte[] data, int expectedSize) {
        if (data == null || data.length == 0) {
            return new ItemStack[expectedSize];
        }

        try {
            validateCurrentFormatSize(data);
            return resizeSafely(ItemStack.deserializeItemsFromBytes(data), expectedSize);
        } catch (IllegalArgumentException currentFormatFailure) {
            // Existing installations used Bukkit object streams. Read them once and save in the
            // current ItemStack byte format on the next inventory write.
            return deserializeLegacy(data, expectedSize);
        }
    }

    private static void validateCurrentFormatSize(byte[] data) {
        if (data.length < 5 || data[0] != 1) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            input.readByte();
            int storedSize = input.readInt();
            if (storedSize < 0 || storedSize > 54) {
                throw new IllegalStateException("Invalid stored inventory size: " + storedSize);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read inventory header.", exception);
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack[] deserializeLegacy(byte[] data, int expectedSize) {
        try (ByteArrayInputStream inputBytes = new ByteArrayInputStream(data);
             BukkitObjectInputStream input = new BukkitObjectInputStream(inputBytes)) {
            int storedSize = input.readInt();
            if (storedSize < 0 || storedSize > 54) {
                throw new IllegalStateException("Invalid stored inventory size: " + storedSize);
            }
            ItemStack[] storedContents = new ItemStack[storedSize];
            for (int slot = 0; slot < storedSize; slot++) {
                storedContents[slot] = (ItemStack) input.readObject();
            }
            return resizeSafely(storedContents, expectedSize);
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot deserialize inventory.", exception);
        }
    }

    private static ItemStack[] resizeSafely(ItemStack[] storedContents, int expectedSize) {
        if (storedContents.length > 54) {
            throw new IllegalStateException("Invalid stored inventory size: " + storedContents.length);
        }

        ItemStack[] contents = new ItemStack[expectedSize];
        System.arraycopy(storedContents, 0, contents, 0, Math.min(storedContents.length, expectedSize));
        for (int slot = expectedSize; slot < storedContents.length; slot++) {
            ItemStack item = storedContents[slot];
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                throw new IllegalStateException(
                        "Storage size was reduced from " + storedContents.length + " to " + expectedSize
                                + " while removed slots still contain items."
                );
            }
        }
        return contents;
    }
}
