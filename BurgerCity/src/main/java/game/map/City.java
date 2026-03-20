package game.map;

import game.resource.ResourceInventory;
import game.resource.ResourceType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class City {

    private String name;
    private int population;
    private int originX;
    private int originY;
    private int width;
    private int height;

    // Passengers waiting to be picked up from this city.
    private final ResourceInventory waiting;

    // Goods demanded by this city (backlog to be delivered).
    private final ResourceInventory demandBacklog;

    // Smoothly varying rates.
    private double passengersPerSecond;
    private double passengerRemainder;
    private final EnumMap<ResourceType, Double> goodsPerSecond;
    private final EnumMap<ResourceType, Double> goodsRemainder;
    private final Random rng;

    /** Minimum 3x3 */
    public City(String name, int originX, int originY, int width, int height) {
        this.name = name;
        this.originX = originX;
        this.originY = originY;
        this.width = Math.max(width, 3);
        this.height = Math.max(height, 3);
        this.population = Math.max(0, this.width * this.height * 120);

        this.waiting = new ResourceInventory();
        this.demandBacklog = new ResourceInventory();
        this.passengersPerSecond = Math.max(0.01, population / 50_000.0);
        this.passengerRemainder = 0.0;

        this.goodsPerSecond = new EnumMap<>(ResourceType.class);
        this.goodsRemainder = new EnumMap<>(ResourceType.class);
        // Cities demand finished goods; keep it simple for now.
        this.goodsPerSecond.put(ResourceType.HAMBURGER, Math.max(0.005, population / 200_000.0));

        this.rng = new Random((originX * 83492791L) ^ (originY * 15485863L) ^ (long) name.hashCode());
    }

    public String getName() { return name; }
    public int getPopulation() { return population; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public ResourceInventory getWaiting() { return waiting; }
    public ResourceInventory getDemandBacklog() { return demandBacklog; }
    public double getPassengersPerSecond() { return passengersPerSecond; }
    public Map<ResourceType, Double> getGoodsPerSecond() { return Map.copyOf(goodsPerSecond); }

    public boolean occupies(int x, int y) {
        return x >= originX && x < originX + width
            && y >= originY && y < originY + height;
    }

    /**
     * Economy tick: generates passengers and grows goods demand backlog.
     */
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0) return;

        updateRates(deltaSeconds);

        passengerRemainder += passengersPerSecond * deltaSeconds;
        int newPassengers = (int) Math.floor(passengerRemainder);
        if (newPassengers > 0) {
            waiting.add(ResourceType.PASSENGERS, newPassengers);
            passengerRemainder -= newPassengers;
        }

        for (var e : goodsPerSecond.entrySet()) {
            ResourceType type = e.getKey();
            double rate = Math.max(0.0, e.getValue());
            double rem = goodsRemainder.getOrDefault(type, 0.0) + rate * deltaSeconds;
            int add = (int) Math.floor(rem);
            if (add > 0) {
                demandBacklog.add(type, add);
                rem -= add;
            }
            goodsRemainder.put(type, rem);
        }
    }

    /**
     * Load passengers (or other waiting cargo) from the city.
     */
    public int load(ResourceType type, int maxAmount) {
        return waiting.removeUpTo(type, maxAmount);
    }

    /**
     * Deliver demanded goods into the city.
     * @return accepted amount (limited by current demand backlog)
     */
    public int deliver(ResourceType type, int amount) {
        if (type == ResourceType.PASSENGERS) {
            // Minimal model: any city can accept passengers.
            return Math.max(0, amount);
        }
        return demandBacklog.removeUpTo(type, amount);
    }

    // Backwards-compatible stubs (used by earlier skeleton).
    public void generatePassengers() {
        update(1.0);
    }

    public void acceptGoods() {
        // Goods acceptance is done via deliver(type, amount).
    }

    private void updateRates(double deltaSeconds) {
        // Passengers: slow drift with small noise.
        double pull = ((population / 50_000.0) - passengersPerSecond) * (0.02 * deltaSeconds);
        double noise = (rng.nextDouble() * 2.0 - 1.0) * (0.01 * deltaSeconds);
        passengersPerSecond = clamp(passengersPerSecond + pull + noise, 0.002, 0.25);

        // Goods demand: slow drift, tied to population.
        for (var type : goodsPerSecond.keySet()) {
            double base = switch (type) {
                case HAMBURGER -> population / 200_000.0;
                default -> 0.0;
            };
            double cur = goodsPerSecond.getOrDefault(type, 0.0);
            double gp = ((base) - cur) * (0.02 * deltaSeconds);
            double gn = (rng.nextDouble() * 2.0 - 1.0) * (0.008 * deltaSeconds);
            goodsPerSecond.put(type, clamp(cur + gp + gn, 0.0, 0.12));
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}