package ru.syntaxteam.teamstorage.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemDiff {
    private ItemDiff() {
    }

    public static List<Change> diff(ItemStack[] before, ItemStack[] after) {
        List<Entry> beforeEntries = aggregate(before);
        List<Entry> afterEntries = aggregate(after);
        List<Change> changes = new ArrayList<>();

        for (Entry afterEntry : afterEntries) {
            int beforeAmount = amountOf(beforeEntries, afterEntry.item);
            int delta = afterEntry.amount - beforeAmount;
            if (delta > 0) {
                changes.add(new Change(afterEntry.item.clone(), delta));
            }
        }

        for (Entry beforeEntry : beforeEntries) {
            int afterAmount = amountOf(afterEntries, beforeEntry.item);
            int delta = afterAmount - beforeEntry.amount;
            if (delta < 0) {
                changes.add(new Change(beforeEntry.item.clone(), delta));
            }
        }

        return changes;
    }

    private static List<Entry> aggregate(ItemStack[] contents) {
        List<Entry> entries = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }

            // Similar items are grouped so moving an item between city storage slots produces no log entry.
            Entry entry = find(entries, item);
            if (entry == null) {
                ItemStack normalized = item.clone();
                normalized.setAmount(1);
                entries.add(new Entry(normalized, item.getAmount()));
            } else {
                entry.amount += item.getAmount();
            }
        }
        return entries;
    }

    private static Entry find(List<Entry> entries, ItemStack item) {
        for (Entry entry : entries) {
            if (entry.item.isSimilar(item)) {
                return entry;
            }
        }
        return null;
    }

    private static int amountOf(List<Entry> entries, ItemStack item) {
        Entry entry = find(entries, item);
        return entry == null ? 0 : entry.amount;
    }

    private static final class Entry {
        private final ItemStack item;
        private int amount;

        private Entry(ItemStack item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }

    public record Change(ItemStack item, int amount) {
    }
}
