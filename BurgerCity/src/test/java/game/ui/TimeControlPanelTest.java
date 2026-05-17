package game.ui;

import game.core.TimeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeControlPanelTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void clickingSpeedButtons_updatesTimeManagerSpeed_andRefreshUpdatesLabel() {
        TimeManager tm = new TimeManager();
        TimeControlPanel panel = new TimeControlPanel(tm);
        panel.setSize(400, 60);

        List<JButton> buttons = findAll(panel, JButton.class);
        assertTrue(buttons.size() >= 4, "Expected at least 4 speed buttons");

        JButton paused = findButtonByTooltip(buttons, TimeManager.TimeSpeed.PAUSED.getDisplayName());
        JButton normal = findButtonByTooltip(buttons, TimeManager.TimeSpeed.NORMAL.getDisplayName());
        JButton fast = findButtonByTooltip(buttons, TimeManager.TimeSpeed.FAST.getDisplayName());
        JButton veryFast = findButtonByTooltip(buttons, TimeManager.TimeSpeed.VERY_FAST.getDisplayName());

        assertNotNull(paused);
        assertNotNull(normal);
        assertNotNull(fast);
        assertNotNull(veryFast);

        fast.doClick();
        assertEquals(TimeManager.TimeSpeed.FAST, tm.getCurrentSpeed());

        paused.doClick();
        assertEquals(TimeManager.TimeSpeed.PAUSED, tm.getCurrentSpeed());

        veryFast.doClick();
        assertEquals(TimeManager.TimeSpeed.VERY_FAST, tm.getCurrentSpeed());

        normal.doClick();
        assertEquals(TimeManager.TimeSpeed.NORMAL, tm.getCurrentSpeed());

        panel.refresh();
        List<JLabel> labels = findAll(panel, JLabel.class);
        JLabel timeLabel = labels.stream().filter(l -> l.getText() != null && l.getText().startsWith("⏱ ")).findFirst().orElse(null);
        assertNotNull(timeLabel, "Expected a time display label");
        assertTrue(timeLabel.getText().contains(TimeManager.TimeSpeed.NORMAL.getDisplayName()));
    }

    private static JButton findButtonByTooltip(List<JButton> buttons, String tooltip) {
        for (JButton b : buttons) {
            if (tooltip.equals(b.getToolTipText())) return b;
        }
        return null;
    }

    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                result.add(type.cast(c));
            }
            if (c instanceof Container child) {
                result.addAll(findAll(child, type));
            }
        }
        return result;
    }
}
