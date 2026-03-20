package game.resource;

public class Meat extends Resource {

	public Meat() {
		super(ResourceType.MEAT);
	}

	public Meat(int amount) {
		super(ResourceType.MEAT, amount);
	}
}