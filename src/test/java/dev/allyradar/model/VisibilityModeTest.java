package dev.allyradar.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisibilityModeTest {
    @Test
    void parsingIsCaseInsensitive() {
        assertEquals(VisibilityMode.PRIVATE, VisibilityMode.parse("private"));
        assertEquals(VisibilityMode.TEAM, VisibilityMode.parse(" TEAM "));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> VisibilityMode.parse("friends"));
    }
}
