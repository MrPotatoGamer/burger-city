package game.resource;

public enum ResourceType {
    WHEAT("Wheat"),
    MEAT("Meat"),
    BREAD("Bread"),
    MEAT_PATTY("Meat Patty"),
    HAMBURGER("Hamburger"),
    PASSENGERS("Passengers");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
