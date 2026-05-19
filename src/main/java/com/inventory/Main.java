package com.inventory;

import javax.swing.*;
import java.awt.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme;
import java.sql.Connection;

public class Main {
    private static boolean isDarkMode = false;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Table.alternateRowColor", new Color(0, 0, 0, 10));
        } catch (Exception ex) {
            System.err.println("Failed to initialize themes");
        }

        initDatabase();

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }

    public static void launchMainApp(String userRole) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Smart Inventory & Billing System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 750);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            JPanel headerPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setPaint(isDarkMode 
                        ? new GradientPaint(0, 0, new Color(15, 25, 50), getWidth(), getHeight(), new Color(30, 60, 120))
                        : new GradientPaint(0, 0, new Color(100, 150, 200), getWidth(), getHeight(), new Color(150, 200, 250)));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            headerPanel.setPreferredSize(new Dimension(1100, 80));
            headerPanel.setLayout(new BorderLayout());
            
            JLabel titleLabel = new JLabel("  Smart Inventory System", SwingConstants.LEFT);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(titleLabel, BorderLayout.CENTER);

            JButton themeToggle = new JButton("Toggle Theme");
            themeToggle.setFont(new Font("SansSerif", Font.BOLD, 14));
            themeToggle.setFocusPainted(false);
            themeToggle.setMargin(new Insets(10, 20, 10, 20));
            themeToggle.addActionListener(e -> {
                isDarkMode = !isDarkMode;
                try {
                    if (isDarkMode) {
                        UIManager.setLookAndFeel(new FlatMaterialDeepOceanIJTheme());
                        UIManager.put("Table.alternateRowColor", new Color(255, 255, 255, 10));
                    } else {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                        UIManager.put("Table.alternateRowColor", new Color(0, 0, 0, 10));
                    }
                    SwingUtilities.updateComponentTreeUI(frame);
                } catch (Exception ex) { }
            });

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
            rightPanel.setOpaque(false);
            rightPanel.add(themeToggle);

            headerPanel.add(rightPanel, BorderLayout.EAST);
            frame.add(headerPanel, BorderLayout.NORTH);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 16));
            tabbedPane.addTab(" Inventory", new InventoryScreen(userRole));
            tabbedPane.addTab(" Billing", new BillingScreen());
            tabbedPane.addTab(" Analytics", new DashboardScreen());
            tabbedPane.addTab(" Receipt History", new ReceiptHistoryScreen());
            
            frame.add(tabbedPane, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    private static void initDatabase() {
        try (Connection conn = DbConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "stock INT NOT NULL)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS sales_history (" +
                    "s_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "p_id INT NOT NULL, " +
                    "p_name VARCHAR(255) NOT NULL, " +
                    "quantity_sold INT NOT NULL, " +
                    "total_price DOUBLE NOT NULL, " +
                    "sale_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (p_id) REFERENCES products(id) ON DELETE CASCADE)");
            
            try {
                stmt.execute("ALTER TABLE sales_history ADD COLUMN receipt_id VARCHAR(50)");
            } catch (Exception ignore) { }

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL)");
            
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'admin')");// user 1
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('cashier', 'cash123', 'cashier')");// user 2
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }	
}
