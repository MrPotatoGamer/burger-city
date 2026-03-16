package game.vehicle;

import game.route.Route;
import game.resource.Resource;
import game.building.Garage;

public class Vehicle {

    protected int speed;
    protected int capacity;
    protected int maintenanceCost;
    protected int age;
    protected int timeSinceMaintenance;

    protected Route route;
    protected Resource currentCargo;
    protected Garage garage;

    public void transport() {}

    public boolean needsMaintenance() {
        return false;
    }

    public void goToGarage() {}
}