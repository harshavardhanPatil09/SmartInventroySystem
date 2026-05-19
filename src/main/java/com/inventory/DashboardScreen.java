package com.inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class DashboardScreen extends JPanel {
    private JLabel revenueLabel;
    private JLabel transactionsLabel;
    private JPanel chartContainer;

    public DashboardScreen() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        summaryPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        revenueLabel = new JLabel("Total Revenue: ₹0.00");
        revenueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        revenueLabel.setForeground(new Color(0x2E7D32));
        
        transactionsLabel = new JLabel("Total Transactions: 0");
        transactionsLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        transactionsLabel.setForeground(new Color(0x1565C0));

        summaryPanel.add(revenueLabel);
        summaryPanel.add(transactionsLabel);

        add(summaryPanel, BorderLayout.NORTH);

        chartContainer = new JPanel(new BorderLayout());
        add(chartContainer, BorderLayout.CENTER);

        loadAnalytics();
    }

    private void loadAnalytics() {
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rsRev = stmt.executeQuery("SELECT SUM(total_price) FROM sales_history");
            if (rsRev.next()) {
                double total = rsRev.getDouble(1);
                revenueLabel.setText("Total Revenue: ₹" + String.format("%.2f", total));
            }

            ResultSet rsTrans = stmt.executeQuery("SELECT COUNT(DISTINCT receipt_id) FROM sales_history WHERE receipt_id IS NOT NULL");
            if (rsTrans.next()) {
                int count = rsTrans.getInt(1);
                transactionsLabel.setText("Total Transactions: " + count);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            ResultSet rsTop = stmt.executeQuery("SELECT p_name, SUM(quantity_sold) as total_qty FROM sales_history GROUP BY p_name ORDER BY total_qty DESC LIMIT 5");
            while (rsTop.next()) {
                dataset.addValue(rsTop.getInt("total_qty"), "Units Sold", rsTop.getString("p_name"));
            }

            JFreeChart barChart = ChartFactory.createBarChart(
                "Top 5 Selling Products",
                "Product",
                "Units Sold",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
            );

            chartContainer.removeAll();
            ChartPanel chartPanel = new ChartPanel(barChart);
            chartContainer.add(chartPanel, BorderLayout.CENTER);
            chartContainer.revalidate();
            chartContainer.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Analytics Error");
        }
    }
}
