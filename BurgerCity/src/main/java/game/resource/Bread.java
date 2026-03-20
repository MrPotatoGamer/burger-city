package game.resource;

public class Bread extends Resource {

	public Bread() {
		super(ResourceType.BREAD);
	}

	public Bread(int amount) {
		super(ResourceType.BREAD, amount);
	}
}