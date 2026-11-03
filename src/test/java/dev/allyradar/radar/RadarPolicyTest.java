package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.VisibilityMode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarPolicyTest {
    private final UUID viewer = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final PlayerPreferences receiving = new PlayerPreferences(VisibilityMode.TEAM, true);

    @Test
    void privateAlwaysHidesTarget() {
        assertFalse(can(VisibilityMode.PRIVATE, true, true, true));
    }

    @Test
    void teamVisibilityRequiresBothMembershipAndTeamRadar() {
        assertTrue(can(VisibilityMode.TEAM, true, true, false));
        assertFalse(can(VisibilityMode.TEAM, false, true, false));
        assertFalse(can(VisibilityMode.TEAM, true, false, false));
    }

    @Test
    void publicVisibilityRequiresServerOptIn() {
        assertTrue(can(VisibilityMode.PUBLIC, false, false, true));
        assertFalse(can(VisibilityMode.PUBLIC, false, false, false));
    }

    @Test
    void receiverCanDisableAllRadar() {
        assertFalse(RadarPolicy.canTrack(viewer, target, new PlayerPreferences(VisibilityMode.TEAM, false),
                new PlayerPreferences(VisibilityMode.PUBLIC, true), false, false, true));
    }

    private boolean can(final VisibilityMode targetVisibility, final boolean sameTeam,
                        final boolean teamRadar, final boolean publicRadar) {
        return RadarPolicy.canTrack(viewer, target, receiving,
                new PlayerPreferences(targetVisibility, true), sameTeam, teamRadar, publicRadar);
    }
}
