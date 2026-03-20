package game.core;

import game.resource.ResourceType;

/**
 * Minimal revenue table for deliveries.
 */
public final class ResourcePrices {

    private ResourcePrices() {}

    /** Revenue per delivered unit. */
    public static int revenuePerUnit(ResourceType type) {
        if (type == null) return 0;
        return switch (type) {
            case WHEAT -> 5;
            case MEAT -> 8;
            case BREAD -> 12;
            case MEAT_PATTY -> 15;
            case HAMBURGER -> 25;
            case PASSENGERS -> 10;
        };
    }
}
