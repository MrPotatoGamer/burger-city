package game.resource;

public class MeatPatty extends Resource {

	public MeatPatty() {
		super(ResourceType.MEAT_PATTY);
	}

	public MeatPatty(int amount) {
		super(ResourceType.MEAT_PATTY, amount);
	}
}