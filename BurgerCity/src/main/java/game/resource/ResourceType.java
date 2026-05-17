package game.resource;

public enum ResourceType {
    WHEAT("Búza"),
    MEAT("Hús"),
    BREAD("Kenyer"),
    MEAT_PATTY("Húspogácsa"),
    HAMBURGER("Hamburger"),
    PASSENGERS("Utasok");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
