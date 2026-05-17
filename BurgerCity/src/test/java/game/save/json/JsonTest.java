package game.save.json;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    @Test
    void parse_supportsObjectsArraysAndPrimitives() {
        Object parsed = Json.parse(" { \"a\": 1, \"b\": [true, false, null, \"x\"], \"c\": {\"d\": -2.5} } ");
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = assertInstanceOf(Map.class, parsed);

        assertEquals(1.0, (Double) obj.get("a"));
        List<?> arr = assertInstanceOf(List.class, obj.get("b"));
        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE, null, "x"), arr);

        @SuppressWarnings("unchecked")
        Map<String, Object> c = assertInstanceOf(Map.class, obj.get("c"));
        assertEquals(-2.5, (Double) c.get("d"));
    }

    @Test
    void parse_supportsUnicodeAndSlashEscapes() {
        assertEquals("a/b", Json.parse("\"a\\/b\""));
        assertEquals("\u263A", Json.parse("\"\\u263A\""));
    }

    @Test
    void parse_supportsExponentNumbers() {
        assertEquals(100.0, (Double) Json.parse("1e2"));
        assertEquals(-0.01, (Double) Json.parse("-1E-2"), 1e-12);
    }

    @Test
    void parse_rejectsTrailingContent() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":1} trailing"));
    }

    @Test
    void parse_rejectsInvalidEscape() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"\\x\""));
    }

    @Test
    void parse_rejectsUnterminatedString() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"abc"));
    }

    @Test
    void stringify_escapesAndRoundTripsStrings() {
        String original = "quote=\" backslash=\\ newline=\n tab=\t unicode=\u263A";

        LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
        obj.put("s", original);
        obj.put("n", 1.0);
        String json = Json.stringify(obj);

        Object parsed = Json.parse(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedObj = assertInstanceOf(Map.class, parsed);
        assertEquals(original, parsedObj.get("s"));
        assertEquals(1.0, (Double) parsedObj.get("n"));
    }

    @Test
    void stringify_escapesControlCharsUsingUnicodeEscape() {
        String withControl = "A" + ((char) 0x1F) + "B";
        String json = Json.stringify(withControl);
        assertTrue(json.contains("\\u001f"));
        assertEquals(withControl, Json.parse(json));
    }

    @Test
    void stringify_writesIntegerLikeDoublesWithoutDecimal() {
        assertEquals("1", Json.stringify(1.0));
        assertEquals("-2", Json.stringify(-2.0));
        assertEquals("3.5", Json.stringify(3.5));
    }

    @Test
    void stringify_writesNaNAndInfinityAsNull() {
        assertEquals("null", Json.stringify(Double.NaN));
        assertEquals("null", Json.stringify(Double.POSITIVE_INFINITY));
        assertEquals("null", Json.stringify(Double.NEGATIVE_INFINITY));
    }

    @Test
    void stringify_rejectsNonStringObjectKeys() {
        Map<Object, Object> bad = new HashMap<>();
        bad.put(1, "x");
        assertThrows(IllegalArgumentException.class, () -> Json.stringify(bad));
    }

    @Test
    void stringify_rejectsUnsupportedTypes() {
        assertThrows(IllegalArgumentException.class, () -> Json.stringify(new Object()));
    }
}
