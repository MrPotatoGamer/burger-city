package game.map;

import game.resource.ResourceType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines what an industry consumes and produces.
 *
 * The simulation treats production as "units"; each unit consumes {@code inputsPerUnit}
 * and produces {@code outputsPerUnit}.
 */
public final class IndustryProfile {

    private final EnumMap<ResourceType, Integer> inputsPerUnit;
    private final EnumMap<ResourceType, Integer> outputsPerUnit;
    private final double baseUnitsPerSecond;

    public IndustryProfile(Map<ResourceType, Integer> inputsPerUnit,
                          Map<ResourceType, Integer> outputsPerUnit,
                          double baseUnitsPerSecond) {
        Objects.requireNonNull(inputsPerUnit, "inputsPerUnit");
        Objects.requireNonNull(outputsPerUnit, "outputsPerUnit");
        this.inputsPerUnit = new EnumMap<>(ResourceType.class);
        this.outputsPerUnit = new EnumMap<>(ResourceType.class);

        for (var e : inputsPerUnit.entrySet()) {
            if (e.getKey() == null) continue;
            int v = Math.max(0, e.getValue() == null ? 0 : e.getValue());
            if (v > 0) this.inputsPerUnit.put(e.getKey(), v);
        }
        for (var e : outputsPerUnit.entrySet()) {
            if (e.getKey() == null) continue;
            int v = Math.max(0, e.getValue() == null ? 0 : e.getValue());
            if (v > 0) this.outputsPerUnit.put(e.getKey(), v);
        }

        this.baseUnitsPerSecond = Math.max(0.0, baseUnitsPerSecond);
    }

    public Map<ResourceType, Integer> getInputsPerUnit() {
        return Map.copyOf(inputsPerUnit);
    }

    public Map<ResourceType, Integer> getOutputsPerUnit() {
        return Map.copyOf(outputsPerUnit);
    }

    public double getBaseUnitsPerSecond() {
        return baseUnitsPerSecond;
    }
}
