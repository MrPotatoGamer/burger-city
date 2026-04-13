package game.ui;

import game.core.TimeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * UI panel for controlling game time speed and displaying current game time.
 */
public class TimeControlPanel extends JPanel {

    private final TimeManager timeManager;
    private JLabel timeDisplay;
    private JButton pauseButton;
    private JButton normalButton;
    private JButton fastButton;
    private JButton veryFastButton;

    private static final Color BG_COLOR = new Color(40, 40, 48);
    private static final Color BUTTON_ACTIVE = new Color(80, 200, 120);
    private static final Color BUTTON_NORMAL = new Color(70, 70, 80);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color TIME_COLOR = new Color(255, 215, 80);

    public TimeControlPanel(TimeManager timeManager) {
        this.timeManager = timeManager;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(4, 8, 4, 8));

        // Time display label
        timeDisplay = new JLabel(getTimeDisplayText());
        timeDisplay.setFont(new Font("SansSerif", Font.BOLD, 13));
        timeDisplay.setForeground(TIME_COLOR);
        timeDisplay.setBorder(new EmptyBorder(0, 0, 0, 12));
        add(timeDisplay);

        // Speed control buttons
        pauseButton = createSpeedButton("⏸", TimeManager.TimeSpeed.PAUSED);
        normalButton = createSpeedButton("▶", TimeManager.TimeSpeed.NORMAL);
        fastButton = createSpeedButton("▶▶", TimeManager.TimeSpeed.FAST);
        veryFastButton = createSpeedButton("▶▶▶", TimeManager.TimeSpeed.VERY_FAST);

        add(pauseButton);
        add(normalButton);
        add(fastButton);
        add(veryFastButton);

        // Initialize button states
        updateButtonStates();
    }

    private JButton createSpeedButton(String label, TimeManager.TimeSpeed speed) {
        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_NORMAL);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(55, 28));
        button.setToolTipText(speed.getDisplayName());
        
        button.addActionListener(e -> {
            timeManager.setSpeed(speed);
            updateButtonStates();
        });
        
        return button;
    }

    /**
     * Updates the time display and button states. Call this periodically from the game loop.
     */
    public void refresh() {
        timeDisplay.setText(getTimeDisplayText());
        updateButtonStates();
    }

    private void updateButtonStates() {
        TimeManager.TimeSpeed current = timeManager.getCurrentSpeed();
        
        pauseButton.setBackground(current == TimeManager.TimeSpeed.PAUSED ? BUTTON_ACTIVE : BUTTON_NORMAL);
        normalButton.setBackground(current == TimeManager.TimeSpeed.NORMAL ? BUTTON_ACTIVE : BUTTON_NORMAL);
        fastButton.setBackground(current == TimeManager.TimeSpeed.FAST ? BUTTON_ACTIVE : BUTTON_NORMAL);
        veryFastButton.setBackground(current == TimeManager.TimeSpeed.VERY_FAST ? BUTTON_ACTIVE : BUTTON_NORMAL);
    }

    private String getTimeDisplayText() {
        return "⏱ " + timeManager.getFormattedGameTime() + " | " + timeManager.getCurrentSpeed().getDisplayName();
    }
}
