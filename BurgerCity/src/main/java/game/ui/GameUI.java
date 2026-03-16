package game.ui;

import game.map.Map;

import javax.swing.*;
import java.awt.*;

public class GameUI extends JFrame {

    private MapRenderer mapRenderer;
    private Map map;

    public GameUI() {
        setTitle("Mini Transport Tycoon - BurgerCity");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Térkép létrehozása és betöltése
        map = new Map(20, 16);
        map.loadPredefined();

        mapRenderer = new MapRenderer(map);

        JScrollPane scrollPane = new JScrollPane(mapRenderer);
        add(scrollPane, BorderLayout.CENTER);

        // Állapotsáv
        JLabel statusBar = new JLabel(" Mini Transport Tycoon | BurgerCity");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameUI ui = new GameUI();
            ui.setVisible(true);
        });
    }
}