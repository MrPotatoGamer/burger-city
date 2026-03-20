package game.resource;

import java.util.Objects;

/**
 * A typed quantity of cargo (goods or passengers).
 */
public class Resource {

    private final ResourceType type;
    private int amount;

    public Resource(ResourceType type) {
        this(type, 0);
    }

    public Resource(ResourceType type, int amount) {
        this.type = Objects.requireNonNull(type, "type");
        this.amount = Math.max(amount, 0);
    }

    public ResourceType getType() {
        return type;
    }

    public String getName() {
        return type.getDisplayName();
    }

    public int getAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    public void add(int delta) {
        if (delta <= 0) return;
        amount += delta;
    }

    /**
     * Removes up to {@code delta} units.
     * @return how much was actually removed
     */
    public int removeUpTo(int delta) {
        if (delta <= 0) return 0;
        int removed = Math.min(amount, delta);
        amount -= removed;
        return removed;
    }
}