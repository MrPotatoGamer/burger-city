package game.ui;

import game.core.TimeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class TimeControlPanel extends JPanel {

    private final TimeManager timeManager;

    private JLabel timeDisplay;

    private JButton pauseButton;
    private JButton normalButton;
    private JButton fastButton;
    private JButton veryFastButton;

    private static final Color BG_COLOR = new Color(36, 36, 44);
    private static final Color PANEL_COLOR = new Color(50, 50, 60);

    private static final Color BUTTON_NORMAL = new Color(70, 70, 80);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color TIME_COLOR = new Color(255, 215, 80);

    public TimeControlPanel(TimeManager timeManager) {
        this.timeManager = timeManager;

        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(20, 20, 25)),
                new EmptyBorder(6, 10, 6, 10)
        ));

        // LEFT: Time display
        timeDisplay = new JLabel(getTimeDisplayText());
        timeDisplay.setFont(new Font("SansSerif", Font.BOLD, 14));
        timeDisplay.setForeground(TIME_COLOR);
        timeDisplay.setOpaque(true);
        timeDisplay.setBackground(PANEL_COLOR);
        timeDisplay.setBorder(new EmptyBorder(6, 12, 6, 12));

        add(timeDisplay, BorderLayout.WEST);

        // RIGHT: Speed buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);

        pauseButton = createSpeedButton("⏸", TimeManager.TimeSpeed.PAUSED);
        normalButton = createSpeedButton("▶", TimeManager.TimeSpeed.NORMAL);
        fastButton = createSpeedButton("⏩", TimeManager.TimeSpeed.FAST);
        veryFastButton = createSpeedButton("⏩⏩", TimeManager.TimeSpeed.VERY_FAST);

        buttonPanel.add(pauseButton);
        buttonPanel.add(normalButton);
        buttonPanel.add(fastButton);
        buttonPanel.add(veryFastButton);

        add(buttonPanel, BorderLayout.EAST);

        updateButtonStates();
    }

    private JButton createSpeedButton(String label, TimeManager.TimeSpeed speed) {
        JButton button = new JButton(label);

        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_NORMAL);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 100)));
        button.setPreferredSize(new Dimension(60, 30));
        button.setToolTipText(speed.getDisplayName());

        // Click action
        button.addActionListener(e -> {
            timeManager.setSpeed(speed);
            updateButtonStates();
        });

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (timeManager.getCurrentSpeed() != speed) {
                    button.setBackground(BUTTON_NORMAL.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateButtonStates();
            }
        });

        return button;
    }

    /**
     * Updates the time display and button states.
     * Call this periodically from the game loop.
     */
    public void refresh() {
        timeDisplay.setText(getTimeDisplayText());
        updateButtonStates();
    }

    private void updateButtonStates() {
        TimeManager.TimeSpeed current = timeManager.getCurrentSpeed();

        updateButtonColor(pauseButton, current == TimeManager.TimeSpeed.PAUSED, TimeManager.TimeSpeed.PAUSED);
        updateButtonColor(normalButton, current == TimeManager.TimeSpeed.NORMAL, TimeManager.TimeSpeed.NORMAL);
        updateButtonColor(fastButton, current == TimeManager.TimeSpeed.FAST, TimeManager.TimeSpeed.FAST);
        updateButtonColor(veryFastButton, current == TimeManager.TimeSpeed.VERY_FAST, TimeManager.TimeSpeed.VERY_FAST);
    }

    private void updateButtonColor(JButton button, boolean active, TimeManager.TimeSpeed speed) {
        if (active) {
            button.setBackground(getSpeedColor(speed));
        } else {
            button.setBackground(BUTTON_NORMAL);
        }
    }

    private Color getSpeedColor(TimeManager.TimeSpeed speed) {
        return switch (speed) {
            case PAUSED -> new Color(120, 120, 120);
            case NORMAL -> new Color(80, 200, 120);
            case FAST -> new Color(255, 200, 80);
            case VERY_FAST -> new Color(255, 120, 80);
        };
    }

    private String getTimeDisplayText() {
        return "⏱ " + timeManager.getFormattedGameTime() +
                "   |   " + timeManager.getCurrentSpeed().getDisplayName();
    }
}