package game.vehicle;

import game.resource.ResourceType;

public class AdvancedTruck extends Vehicle {

	public AdvancedTruck() {
		super();
		this.speed = 2;
		this.capacity = 35;
		this.maintenanceCost = 4;
	}

	@Override
	protected boolean canCarry(ResourceType type) {
		return type != ResourceType.PASSENGERS;
	}
}
