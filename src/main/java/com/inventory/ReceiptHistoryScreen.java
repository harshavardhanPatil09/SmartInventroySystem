package com.inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.File;
import java.awt.Desktop;
import java.io.FileOutputStream;
import java.util.Date;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class ReceiptHistoryScreen extends JPanel {
    private DefaultListModel<String> historyModel;
    private JList<String> historyList;

    public ReceiptHistoryScreen() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Past Receipts"));

        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        refreshButton.addActionListener(e -> loadHistory());
        
        JButton printButton = new JButton("Re-print Receipt");
        printButton.setBackground(new Color(0x1565C0));
        printButton.setForeground(Color.WHITE);
        printButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        printButton.addActionListener(e -> reprintReceipt());

        JButton openButton = new JButton("Open Receipt");
        openButton.setBackground(new Color(0x388E3C)); // A green color to differentiate the open action
        openButton.setForeground(Color.WHITE);
        openButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        openButton.addActionListener(e -> openReceipt()); // Action to open the PDF in system's default viewer

        bottomPanel.add(refreshButton);
        bottomPanel.add(openButton);
        bottomPanel.add(printButton);
        add(bottomPanel, BorderLayout.SOUTH);

        loadHistory();
    }

    private void loadHistory() {
        historyModel.clear();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT receipt_id, MAX(sale_timestamp) as ts, SUM(total_price) as sum_total FROM sales_history WHERE receipt_id IS NOT NULL GROUP BY receipt_id ORDER BY ts DESC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("receipt_id");
                String ts = rs.getTimestamp("ts").toString();
                double total = rs.getDouble("sum_total");
                historyModel.addElement("Receipt ID: " + id + " | Date: " + ts + " | Total: \u20B9" + String.format("%.2f", total));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void reprintReceipt() {
        String selected = historyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a receipt.");
            return;
        }

        String targetId = selected.split(" \\| ")[0].replace("Receipt ID: ", "").trim();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT p_name, quantity_sold, total_price, sale_timestamp FROM sales_history WHERE receipt_id = ?")) {
            stmt.setString(1, targetId);
            ResultSet rs = stmt.executeQuery();
            
            // Check and create 'receipts' folder to store PDFs properly
            File receiptsDir = new File("receipts");
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs();
            }

            Document document = new Document();
            // Prefix the path with 'receipts/' folder path
            String filePath = "receipts/reprint_" + targetId + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph("SMART INVENTORY STORE (REPRINT)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph address = new Paragraph("1909 Panchashil Chowk, Tadiwala Road, \\nPune, India - 411001\\nPhone: +91 9325140055\\n\\n", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
            
            document.add(new Paragraph("Receipt ID: " + targetId, normalFont));
            document.add(new Paragraph("Date Generated: " + new Date().toString(), normalFont));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{ 4f, 1f, 2f });
            
            table.addCell(new PdfPCell(new Paragraph("Item", headerFont)));
            table.addCell(new PdfPCell(new Paragraph("Qty", headerFont)));
            
            PdfPCell totalHeader = new PdfPCell(new Paragraph("Total (\u20B9)", headerFont));
            totalHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalHeader);

            double batchTotal = 0;
            while (rs.next()) {
                String n = rs.getString("p_name");
                int q = rs.getInt("quantity_sold");
                double pPrice = rs.getDouble("total_price");
                batchTotal += pPrice;
                
                table.addCell(new PdfPCell(new Paragraph(n, normalFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(q), normalFont)));
                
                PdfPCell totalCell = new PdfPCell(new Paragraph(String.format("%.2f", pPrice), normalFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
            document.add(table);
            document.add(new Paragraph("\n"));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell grandTotalLabel = new PdfPCell(new Paragraph("Final Billed Amount:", headerFont));
            grandTotalLabel.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            summaryTable.addCell(grandTotalLabel);
            
            PdfPCell grandTotalVal = new PdfPCell(new Paragraph("\u20B9" + String.format("%.2f", batchTotal), headerFont));
            grandTotalVal.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            grandTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(grandTotalVal);
            
            document.add(summaryTable);
            
            Paragraph footer = new Paragraph("\nThank you for shopping with us!\nVisit Again", headerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            JOptionPane.showMessageDialog(this, "Reprinted! Generated reprint_" + targetId + ".pdf in 'receipts' folder.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Reprint failed: " + ex.getMessage());
        }
    }

    /**
     * Attempts to open the currently selected receipt by launching the system's default PDF viewer.
     * It looks for both original generated receipts and reprints inside the receipts folder.
     */
    private void openReceipt() {
        String selected = historyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a receipt from the list.");
            return;
        }

        // Extract the target receipt ID from string format: "Receipt ID: XYZ | Date: ..."
        String targetId = selected.split(" \\| ")[0].replace("Receipt ID: ", "").trim();
        
        // Define paths for both the original receipt and reprint
        File originalFile = new File("receipts/receipt_" + targetId + ".pdf");
        File reprintFile = new File("receipts/reprint_" + targetId + ".pdf");

        File fileToOpen = null;
        if (originalFile.exists()) {
            fileToOpen = originalFile;
        } else if (reprintFile.exists()) {
            fileToOpen = reprintFile;
        }

        // Output logic
        if (fileToOpen != null) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(fileToOpen); // Open with default PDF viewer
                } else {
                    JOptionPane.showMessageDialog(this, "Desktop integration is not supported on this OS. File located at: " + fileToOpen.getAbsolutePath());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open receipt: " + ex.getMessage());
            }
        } else {
            // Give user friendly error if neither file is generated yet
            JOptionPane.showMessageDialog(this, "Receipt PDF not found on disk! Please click 'Re-print Receipt' first.");
        }
    }
}
