package com.inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Vector;

public class InventoryScreen extends JPanel {
    private JTextField nameField, priceField, stockField;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedProductId = -1;
    private String userRole;

    public InventoryScreen(String role) {
        this.userRole = role;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(nameLabel);
        nameField = new JTextField();
        nameField.setMargin(new Insets(5, 5, 5, 5));
        inputPanel.add(nameField);
        
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(priceLabel);
        priceField = new JTextField();
        priceField.setMargin(new Insets(5, 5, 5, 5));
        inputPanel.add(priceField);
        
        JLabel stockLabel = new JLabel("Stock:");
        stockLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(stockLabel);
        stockField = new JTextField();
        stockField.setMargin(new Insets(5, 5, 5, 5));
        inputPanel.add(stockField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton addButton = new JButton("Add Product");
        addButton.setBackground(new Color(0x2E7D32));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addButton.addActionListener(e -> addProduct());
        buttonPanel.add(addButton);
        
        JButton updateButton = new JButton("Update");
        updateButton.setBackground(new Color(0x1565C0));
        updateButton.setForeground(Color.WHITE);
        updateButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        updateButton.addActionListener(e -> updateProduct());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.setBackground(new Color(0xC62828));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deleteButton.addActionListener(e -> deleteProduct());
        buttonPanel.add(deleteButton);

        if ("cashier".equalsIgnoreCase(userRole)) {
            updateButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }

        JButton exportButton = new JButton("Export CSV");
        exportButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        exportButton.addActionListener(e -> exportCSV());
        buttonPanel.add(exportButton);

        inputPanel.add(new JLabel("")); 
        inputPanel.add(buttonPanel);

        add(inputPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Stock"}, 0);
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        table.getTableHeader().setBackground(new Color(30, 30, 30));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Object stockVal = table.getModel().getValueAt(row, 3);
                    if (stockVal instanceof Integer && (Integer) stockVal < 5) {
                        c.setBackground(new Color(0xFF5252));
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(UIManager.getColor(row % 2 == 0 ? "Table.background" : "Table.alternateRowColor"));
                        c.setForeground(UIManager.getColor("Table.foreground"));
                    }
                }
                return c;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                selectedProductId = (int) tableModel.getValueAt(row, 0);
                nameField.setText(tableModel.getValueAt(row, 1).toString());
                priceField.setText(tableModel.getValueAt(row, 2).toString());
                stockField.setText(tableModel.getValueAt(row, 3).toString());
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadProducts();
    }

    private void exportCSV() {
        try (PrintWriter pw = new PrintWriter(new File("inventory.csv"))) {
            pw.println("ID,Name,Price,Stock");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                pw.println(tableModel.getValueAt(i, 0) + "," +
                           tableModel.getValueAt(i, 1) + "," +
                           tableModel.getValueAt(i, 2) + "," +
                           tableModel.getValueAt(i, 3));
            }
            JOptionPane.showMessageDialog(this, "Exported to inventory.csv");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export error");
        }
    }

    private void addProduct() {
        try {
            String name = nameField.getText();
            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty.");
                return;
            }
            double price = Double.parseDouble(priceField.getText());
            int stock = Integer.parseInt(stockField.getText());

            try (Connection conn = DbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, price, stock) VALUES (?, ?, ?)")) {
                stmt.setString(1, name);
                stmt.setDouble(2, price);
                stmt.setInt(3, stock);
                stmt.executeUpdate();
                loadProducts();
                clearFields();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for Price and Stock.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void updateProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to update.");
            return;
        }
        try {
            String name = nameField.getText();
            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty.");
                return;
            }
            double price = Double.parseDouble(priceField.getText());
            int stock = Integer.parseInt(stockField.getText());

            try (Connection conn = DbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE products SET name = ?, price = ?, stock = ? WHERE id = ?")) {
                stmt.setString(1, name);
                stmt.setDouble(2, price);
                stmt.setInt(3, stock);
                stmt.setInt(4, selectedProductId);
                stmt.executeUpdate();
                loadProducts();
                clearFields();
                selectedProductId = -1;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for Price and Stock.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void deleteProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.");
            return;
        }
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            stmt.setInt(1, selectedProductId);
            stmt.executeUpdate();
            loadProducts();
            clearFields();
            selectedProductId = -1;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void loadProducts() {
        tableModel.setRowCount(0);
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getDouble("price"));
                row.add(rs.getInt("stock"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        nameField.setText("");
        priceField.setText("");
        stockField.setText("");
        table.clearSelection();
    }
}
