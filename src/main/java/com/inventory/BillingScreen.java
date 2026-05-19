package com.inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.util.Vector;
import java.util.Date;
import java.io.FileOutputStream;
import java.util.UUID;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class BillingScreen extends JPanel {
    private JComboBox<String> searchBox;
    private JTextField qtyField;
    private JTextField discountField;
    private JTable billTable;
    private DefaultTableModel billModel;
    private JLabel totalLabel;
    private double totalAmount = 0.0;
    private int selectedProductId = -1;

    public BillingScreen() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        inputPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel searchLabel = new JLabel("Search Product:");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(searchLabel);
        
        JLabel qtyLabel = new JLabel("Quantity:");
        qtyLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(qtyLabel);

        JLabel discountLabel = new JLabel("Discount (%):");
        discountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputPanel.add(discountLabel);
        
        inputPanel.add(new JLabel("")); 

        searchBox = new JComboBox<>();
        searchBox.setEditable(true);
        JTextField searchEditor = (JTextField) searchBox.getEditor().getEditorComponent();
        searchEditor.setMargin(new Insets(5, 5, 5, 5));
        searchEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN) {
                    searchProducts(searchEditor.getText());
                    searchBox.showPopup();
                }
            }
        });
        searchBox.addActionListener(e -> {
            String selected = (String) searchBox.getSelectedItem();
            if (selected != null && selected.contains("[ID: ")) {
                try {
                    String idStr = selected.substring(selected.indexOf("[ID: ") + 5, selected.indexOf("]"));
                    selectedProductId = Integer.parseInt(idStr);
                } catch (Exception ex) {
                    selectedProductId = -1;
                }
            }
        });

        inputPanel.add(searchBox);
        
        qtyField = new JTextField();
        qtyField.setMargin(new Insets(5, 5, 5, 5));
        inputPanel.add(qtyField);

        discountField = new JTextField("0");
        discountField.setMargin(new Insets(5, 5, 5, 5));
        inputPanel.add(discountField);
        
        JButton addItemButton = new JButton("Add to Bill");
        addItemButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addItemButton.addActionListener(e -> addItem());
        inputPanel.add(addItemButton);

        add(inputPanel, BorderLayout.NORTH);

        billModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Qty", "Subtotal"}, 0);
        billTable = new JTable(billModel);
        billTable.setRowHeight(30);
        billTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        billTable.getTableHeader().setBackground(new Color(30, 30, 30));
        billTable.getTableHeader().setForeground(Color.WHITE);
        billTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        
        add(new JScrollPane(billTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        totalLabel = new JLabel("Subtotal: \u20B90.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        
        JButton clearButton = new JButton("Clear Bill");
        clearButton.setBackground(new Color(0xC62828));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        clearButton.addActionListener(e -> clearBill());
        
        JButton checkoutButton = new JButton("Confirm Sale");
        checkoutButton.setBackground(new Color(0x2E7D32));
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        checkoutButton.addActionListener(e -> checkout());
        
        bottomPanel.add(totalLabel);
        bottomPanel.add(clearButton);
        bottomPanel.add(checkoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void clearBill() {
        billModel.setRowCount(0);
        totalAmount = 0.0;
        totalLabel.setText("Subtotal: \u20B90.00");
    }

    private void searchProducts(String keyword) {
        searchBox.removeAllItems();
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, price, stock FROM products WHERE name LIKE ?")) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String item = rs.getString("name") + " - \u20B9" + rs.getDouble("price") + " (Stock: " + rs.getInt("stock") + ") [ID: " + rs.getInt("id") + "]";
                searchBox.addItem(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addItem() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please search and select a valid product.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric quantity.");
            return;
        }

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name, price, stock FROM products WHERE id = ?")) {
            stmt.setInt(1, selectedProductId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int stock = rs.getInt("stock");
                if (stock >= qty) {
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    double subtotal = price * qty;
                    
                    Vector<Object> row = new Vector<>();
                    row.add(selectedProductId);
                    row.add(name);
                    row.add(price);
                    row.add(qty);
                    row.add(subtotal);
                    billModel.addRow(row);
                    
                    totalAmount += subtotal;
                    totalLabel.setText("Subtotal: \u20B9" + String.format("%.2f", totalAmount));
                    
                    searchBox.removeAllItems();
                    searchBox.setSelectedItem("");
                    qtyField.setText("");
                    selectedProductId = -1;
                } else {
                    JOptionPane.showMessageDialog(this, "Not enough stock! Available: " + stock);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Product not found.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void checkout() {
        if (billModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Bill is empty.");
            return;
        }

        double discountPct = 0;
        try {
            String dText = discountField.getText().trim();
            if (!dText.isEmpty()) {
                discountPct = Double.parseDouble(dText);
            }
            if (discountPct < 0 || discountPct > 100) {
                JOptionPane.showMessageDialog(this, "Discount must be between 0% and 100%.");
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Discount input format.");
            return;
        }

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            String receiptId = UUID.randomUUID().toString();

            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE products SET stock = stock - ? WHERE id = ?");
                 PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO sales_history (p_id, p_name, quantity_sold, total_price, receipt_id) VALUES (?, ?, ?, ?, ?)")) {
                
                for (int i = 0; i < billModel.getRowCount(); i++) {
                    int id = (int) billModel.getValueAt(i, 0);
                    String name = (String) billModel.getValueAt(i, 1);
                    int qty = (int) billModel.getValueAt(i, 3);
                    double subtotal = (double) billModel.getValueAt(i, 4);
                    
                    double lineDiscount = subtotal * (discountPct / 100);
                    double postDiscountSubtotal = subtotal - lineDiscount;
                    double finalLinePrice = postDiscountSubtotal + (postDiscountSubtotal * 0.05);

                    updateStmt.setInt(1, qty);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();

                    insertStmt.setInt(1, id);
                    insertStmt.setString(2, name);
                    insertStmt.setInt(3, qty);
                    insertStmt.setDouble(4, finalLinePrice);
                    insertStmt.setString(5, receiptId);
                    insertStmt.executeUpdate();
                }
                conn.commit();
                
                generatePDF(receiptId, discountPct);
                
                JOptionPane.showMessageDialog(this, "Checkout successful! receipt_" + receiptId + ".pdf generated.");
                clearBill();
                discountField.setText("0");
            } catch (Exception e) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Checkout failed! " + e.getMessage());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB exception: " + e.getMessage());
        }
    }

    private void generatePDF(String receiptId, double discountPct) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream("receipt_" + receiptId + ".pdf"));
            document.open();
            
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            
            Paragraph title = new Paragraph("SMART INVENTORY STORE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph address = new Paragraph(" 1909 Panchashil Chowk, Tadiwala Road, \nPune, India - 411001\nPhone: +91 9325140055\n\n", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
            
            document.add(new Paragraph("Receipt ID: " + receiptId, normalFont));
            document.add(new Paragraph("Date: " + new Date().toString(), normalFont));
            document.add(new Paragraph("\n"));
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{ 3f, 1f, 1.5f, 1.5f });
            
            table.addCell(new PdfPCell(new Paragraph("Item", headerFont)));
            table.addCell(new PdfPCell(new Paragraph("Qty", headerFont)));
            
            PdfPCell priceHeader = new PdfPCell(new Paragraph("Price (\u20B9)", headerFont));
            priceHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(priceHeader);
            
            PdfPCell totalHeader = new PdfPCell(new Paragraph("Total (\u20B9)", headerFont));
            totalHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalHeader);
            
            for (int i = 0; i < billModel.getRowCount(); i++) {
                String name = (String) billModel.getValueAt(i, 1);
                int qty = (int) billModel.getValueAt(i, 3);
                double price = (double) billModel.getValueAt(i, 2);
                double subtotal = (double) billModel.getValueAt(i, 4);
                
                table.addCell(new PdfPCell(new Paragraph(name, normalFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(qty), normalFont)));
                
                PdfPCell priceCell = new PdfPCell(new Paragraph(String.format("%.2f", price), normalFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(priceCell);
                
                PdfPCell totalCell = new PdfPCell(new Paragraph(String.format("%.2f", subtotal), normalFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
            document.add(table);
            
            document.add(new Paragraph("\n"));
            
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            PdfPCell subtotalLabel = new PdfPCell(new Paragraph("Subtotal:", normalFont));
            subtotalLabel.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            summaryTable.addCell(subtotalLabel);
            
            PdfPCell subtotalValue = new PdfPCell(new Paragraph("\u20B9" + String.format("%.2f", totalAmount), normalFont));
            subtotalValue.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(subtotalValue);
            
            double discountAmount = totalAmount * (discountPct / 100);
            if (discountAmount > 0) {
                PdfPCell discountLabelCell = new PdfPCell(new Paragraph("Discount (" + discountPct + "%):", normalFont));
                discountLabelCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                summaryTable.addCell(discountLabelCell);
                
                PdfPCell discountValCell = new PdfPCell(new Paragraph("-\u20B9" + String.format("%.2f", discountAmount), normalFont));
                discountValCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                discountValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                summaryTable.addCell(discountValCell);
            }
            
            double afterDiscount = totalAmount - discountAmount;
            double gst = afterDiscount * 0.05;
            
            PdfPCell gstLabelCell = new PdfPCell(new Paragraph("GST (5%):", normalFont));
            gstLabelCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            summaryTable.addCell(gstLabelCell);
            
            PdfPCell gstValCell = new PdfPCell(new Paragraph("\u20B9" + String.format("%.2f", gst), normalFont));
            gstValCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            gstValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(gstValCell);
            
            PdfPCell grandTotalLabel = new PdfPCell(new Paragraph("Grand Total:", headerFont));
            grandTotalLabel.setBorder(com.lowagie.text.Rectangle.TOP);
            summaryTable.addCell(grandTotalLabel);
            
            PdfPCell grandTotalVal = new PdfPCell(new Paragraph("\u20B9" + String.format("%.2f", (afterDiscount + gst)), headerFont));
            grandTotalVal.setBorder(com.lowagie.text.Rectangle.TOP);
            grandTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(grandTotalVal);
            
            document.add(summaryTable);
            
            Paragraph footer = new Paragraph("\nThank you for shopping with us!\nVisit Again", headerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "PDF Generation failed: " + e.getMessage());
        }
    }
}
