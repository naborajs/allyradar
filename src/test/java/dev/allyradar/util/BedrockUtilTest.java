package dev.allyradar.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockUtilTest {

    @Test
    void detectsBedrockPlayerByPrefix() {
        final UUID uuid = UUID.randomUUID();
        assertTrue(BedrockUtil.isBedrockUuid(uuid, ".Aloneboy18k"));
        assertTrue(BedrockUtil.isBedrockUuid(uuid, "*GamerTag"));
        assertTrue(BedrockUtil.isBedrockUuid(uuid, "_BedrockUser"));

        assertFalse(BedrockUtil.isBedrockUuid(uuid, "Naborajs"));
        assertFalse(BedrockUtil.isBedrockUuid(uuid, "VandalRuin"));
        assertFalse(BedrockUtil.isBedrockUuid(uuid, "ASLIXSensei"));
    }

    @Test
    void nullInputsFailSafely() {
        assertFalse(BedrockUtil.isBedrockUuid(null, null));
        assertFalse(BedrockUtil.isBedrockUuid(UUID.randomUUID(), null));
        assertFalse(BedrockUtil.isBedrockUuid(null, "Player"));
    }
}

