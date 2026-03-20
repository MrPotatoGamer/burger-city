package game.vehicle;

import game.resource.ResourceType;

public class Bus extends Vehicle {

	public Bus() {
		super();
		this.speed = 3;
		this.capacity = 30;
		this.maintenanceCost = 2;
	}

	@Override
	protected boolean canCarry(ResourceType type) {
		return type == ResourceType.PASSENGERS;
	}
}