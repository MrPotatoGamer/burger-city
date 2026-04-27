package game.map;

import game.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndustryProfileTest {

    @Test
    void testConstructorWithValidInputsAndOutputs() {
        Map<ResourceType, Integer> inputs = Map.of(ResourceType.WHEAT, 2);
        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.5);

        assertNotNull(profile);
        assertEquals(0.5, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testConstructorWithEmptyMaps() {
        Map<ResourceType, Integer> inputs = Map.of();
        Map<ResourceType, Integer> outputs = Map.of();
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 1.0);

        assertNotNull(profile);
        assertTrue(profile.getInputsPerUnit().isEmpty());
        assertTrue(profile.getOutputsPerUnit().isEmpty());
        assertEquals(1.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testConstructorWithNullInputsThrowsException() {
        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        assertThrows(NullPointerException.class, () -> {
            new IndustryProfile(null, outputs, 0.5);
        });
    }

    @Test
    void testConstructorWithNullOutputsThrowsException() {
        Map<ResourceType, Integer> inputs = Map.of(ResourceType.WHEAT, 2);
        assertThrows(NullPointerException.class, () -> {
            new IndustryProfile(inputs, null, 0.5);
        });
    }

    @Test
    void testConstructorIgnoresNullKeys() {
        Map<ResourceType, Integer> inputs = new HashMap<>();
        inputs.put(null, 5);
        inputs.put(ResourceType.WHEAT, 2);

        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.5);

        assertTrue(profile.getInputsPerUnit().containsKey(ResourceType.WHEAT));
        assertEquals(2, profile.getInputsPerUnit().get(ResourceType.WHEAT));
    }

    @Test
    void testConstructorIgnoresNullValues() {
        Map<ResourceType, Integer> inputs = new HashMap<>();
        inputs.put(ResourceType.WHEAT, null);
        inputs.put(ResourceType.MEAT, 3);

        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.5);

        assertFalse(profile.getInputsPerUnit().containsKey(ResourceType.WHEAT));
        assertTrue(profile.getInputsPerUnit().containsKey(ResourceType.MEAT));
    }

    @Test
    void testConstructorIgnoresNegativeValues() {
        Map<ResourceType, Integer> inputs = Map.of(ResourceType.WHEAT, -5);
        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.5);

        assertFalse(profile.getInputsPerUnit().containsKey(ResourceType.WHEAT));
    }

    @Test
    void testConstructorIgnoresZeroValues() {
        Map<ResourceType, Integer> inputs = Map.of(ResourceType.WHEAT, 0);
        Map<ResourceType, Integer> outputs = Map.of(ResourceType.BREAD, 1);
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.5);

        assertFalse(profile.getInputsPerUnit().containsKey(ResourceType.WHEAT));
    }

    @Test
    void testConstructorClampsNegativeBaseUnitsPerSecond() {
        Map<ResourceType, Integer> inputs = Map.of();
        Map<ResourceType, Integer> outputs = Map.of();
        IndustryProfile profile = new IndustryProfile(inputs, outputs, -1.0);

        assertEquals(0.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testGetInputsPerUnitReturnsUnmodifiableCopy() {
        Map<ResourceType, Integer> inputs = new HashMap<>();
        inputs.put(ResourceType.WHEAT, 2);

        IndustryProfile profile = new IndustryProfile(inputs, Map.of(), 0.5);
        Map<ResourceType, Integer> retrievedInputs = profile.getInputsPerUnit();

        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedInputs.put(ResourceType.MEAT, 3);
        });
    }

    @Test
    void testGetOutputsPerUnitReturnsUnmodifiableCopy() {
        Map<ResourceType, Integer> outputs = new HashMap<>();
        outputs.put(ResourceType.BREAD, 1);

        IndustryProfile profile = new IndustryProfile(Map.of(), outputs, 0.5);
        Map<ResourceType, Integer> retrievedOutputs = profile.getOutputsPerUnit();

        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedOutputs.put(ResourceType.HAMBURGER, 2);
        });
    }

    @Test
    void testInputsPerUnitPreservesValues() {
        Map<ResourceType, Integer> inputs = Map.of(
                ResourceType.WHEAT, 2,
                ResourceType.MEAT, 3
        );
        IndustryProfile profile = new IndustryProfile(inputs, Map.of(), 0.5);

        assertEquals(2, profile.getInputsPerUnit().get(ResourceType.WHEAT));
        assertEquals(3, profile.getInputsPerUnit().get(ResourceType.MEAT));
    }

    @Test
    void testOutputsPerUnitPreservesValues() {
        Map<ResourceType, Integer> outputs = Map.of(
                ResourceType.BREAD, 5,
                ResourceType.HAMBURGER, 2
        );
        IndustryProfile profile = new IndustryProfile(Map.of(), outputs, 0.5);

        assertEquals(5, profile.getOutputsPerUnit().get(ResourceType.BREAD));
        assertEquals(2, profile.getOutputsPerUnit().get(ResourceType.HAMBURGER));
    }

    @Test
    void testBaseUnitsPerSecondWithZero() {
        IndustryProfile profile = new IndustryProfile(Map.of(), Map.of(), 0.0);
        assertEquals(0.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testBaseUnitsPerSecondWithLargeValue() {
        IndustryProfile profile = new IndustryProfile(Map.of(), Map.of(), 1000.0);
        assertEquals(1000.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testBaseUnitsPerSecondWithSmallPositiveValue() {
        IndustryProfile profile = new IndustryProfile(Map.of(), Map.of(), 0.001);
        assertEquals(0.001, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testMultipleInputsAndOutputs() {
        Map<ResourceType, Integer> inputs = Map.of(
                ResourceType.BREAD, 1,
                ResourceType.MEAT_PATTY, 1
        );
        Map<ResourceType, Integer> outputs = Map.of(
                ResourceType.HAMBURGER, 1
        );
        IndustryProfile profile = new IndustryProfile(inputs, outputs, 0.18);

        assertEquals(2, profile.getInputsPerUnit().size());
        assertEquals(1, profile.getOutputsPerUnit().size());
        assertEquals(1, profile.getInputsPerUnit().get(ResourceType.BREAD));
        assertEquals(1, profile.getInputsPerUnit().get(ResourceType.MEAT_PATTY));
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.HAMBURGER));
    }

    @Test
    void testOriginalMapModificationDoesNotAffectProfile() {
        Map<ResourceType, Integer> inputs = new HashMap<>();
        inputs.put(ResourceType.WHEAT, 2);

        IndustryProfile profile = new IndustryProfile(inputs, Map.of(), 0.5);
        inputs.put(ResourceType.MEAT, 5);

        assertFalse(profile.getInputsPerUnit().containsKey(ResourceType.MEAT));
        assertEquals(1, profile.getInputsPerUnit().size());
    }
}
