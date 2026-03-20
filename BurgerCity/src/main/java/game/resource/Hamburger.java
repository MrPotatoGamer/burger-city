package game.resource;

public class Hamburger extends Resource {

	public Hamburger() {
		super(ResourceType.HAMBURGER);
	}

	public Hamburger(int amount) {
		super(ResourceType.HAMBURGER, amount);
	}
}