package game.resource;

public class Passengers extends Resource {

    public Passengers() {
        super(ResourceType.PASSENGERS);
    }

    public Passengers(int amount) {
        super(ResourceType.PASSENGERS, amount);
    }
}
