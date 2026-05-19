package com.inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginFrame() {
        setTitle("Smart Inventory - Secured Login");
        setSize(400, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(new GradientPaint(0, 0, new Color(15, 25, 50), getWidth(), getHeight(), new Color(30, 60, 120)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setPreferredSize(new Dimension(400, 100));
        headerPanel.setLayout(new GridBagLayout());
        
        JLabel titleLabel = new JLabel("Auth Portal");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(userLabel, gbc);

        userField = new JTextField();
        userField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        userField.setMargin(new Insets(8, 8, 8, 8));
        gbc.gridy = 1;
        mainPanel.add(userField, gbc);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridy = 2;
        mainPanel.add(passLabel, gbc);

        passField = new JPasswordField();
        passField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        passField.setMargin(new Insets(8, 8, 8, 8));
        passField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        });
        gbc.gridy = 3;
        mainPanel.add(passField, gbc);

        JButton loginButton = new JButton("Secure Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        loginButton.setBackground(new Color(0x2E7D32));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setMargin(new Insets(10, 10, 10, 10));
        loginButton.addActionListener(e -> attemptLogin());
        
        gbc.gridy = 4;
        gbc.insets = new Insets(25, 5, 10, 5);
        mainPanel.add(loginButton, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void attemptLogin() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword()).trim();
        
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Username and Password.");
            return;
        }

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT role FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, u);
            stmt.setString(2, p);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                dispose();
                Main.launchMainApp(role);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.");
                passField.setText("");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database connection error.");
        }
    }
}
