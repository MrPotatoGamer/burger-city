package game.vehicle;

import game.resource.ResourceType;

public class AdvancedBus extends Vehicle {

	public AdvancedBus() {
		super();
		this.speed = 3;
		this.capacity = 50;
		this.maintenanceCost = 3;
	}

	@Override
	protected boolean canCarry(ResourceType type) {
		return type == ResourceType.PASSENGERS;
	}
}
