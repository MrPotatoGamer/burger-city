package game.resource;

public class Wheat extends Resource {

	public Wheat() {
		super(ResourceType.WHEAT);
	}

	public Wheat(int amount) {
		super(ResourceType.WHEAT, amount);
	}
}