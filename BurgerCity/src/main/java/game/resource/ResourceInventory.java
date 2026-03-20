package game.resource;

import java.util.EnumMap;
import java.util.Map;

/**
 * Simple storage for resources by type.
 *
 * Note: This is intentionally minimal and side-effecting to fit the current codebase.
 */
public class ResourceInventory {

    private final EnumMap<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);

    public int get(ResourceType type) {
        return amounts.getOrDefault(type, 0);
    }

    public Map<ResourceType, Integer> asUnmodifiableMap() {
        return Map.copyOf(amounts);
    }

    public void add(ResourceType type, int amount) {
        if (amount <= 0) return;
        amounts.put(type, get(type) + amount);
    }

    /**
     * Removes up to {@code amount} from inventory.
     * @return how much was actually removed
     */
    public int removeUpTo(ResourceType type, int amount) {
        if (amount <= 0) return 0;
        int have = get(type);
        int removed = Math.min(have, amount);
        if (removed == 0) return 0;
        int left = have - removed;
        if (left == 0) {
            amounts.remove(type);
        } else {
            amounts.put(type, left);
        }
        return removed;
    }

    public boolean hasAtLeast(ResourceType type, int amount) {
        return get(type) >= amount;
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }
}
