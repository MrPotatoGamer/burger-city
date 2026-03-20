package game.vehicle;

import game.resource.ResourceType;

public class Truck extends Vehicle {

	public Truck() {
		super();
		this.speed = 2;
		this.capacity = 20;
		this.maintenanceCost = 3;
	}

	@Override
	protected boolean canCarry(ResourceType type) {
		return type != ResourceType.PASSENGERS;
	}
}