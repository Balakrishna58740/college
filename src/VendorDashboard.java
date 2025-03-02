import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VendorDashboard extends JFrame {
    private String username;
    private JTabbedPane tabbedPane;
    private DefaultTableModel menuModel, orderModel, historyModel, reviewModel;
    private JTable menuTable, orderTable, historyTable, reviewTable;
    private JTextField itemNameField, priceField;

    public VendorDashboard(String username) {
        this.username = username;
        initializeUI();
        loadMenu();
        loadOrders();
        loadOrderHistory();
        loadReviews();
    }

    private void initializeUI() {
        setTitle("Vendor Dashboard - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Menu Management Panel
        JPanel menuPanel = createMenuPanel();
        tabbedPane.addTab("Manage Menu", menuPanel);

        // Current Orders Panel
        JPanel orderPanel = createOrderPanel();
        tabbedPane.addTab("Current Orders", orderPanel);

        // Order History Panel
        JPanel historyPanel = createHistoryPanel();
        tabbedPane.addTab("Order History", historyPanel);

        // Reviews Panel
        JPanel reviewPanel = createReviewPanel();
        tabbedPane.addTab("Customer Reviews", reviewPanel);

        // Revenue Dashboard Panel
        JPanel revenuePanel = createRevenuePanel();
        tabbedPane.addTab("Revenue Dashboard", revenuePanel);

        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Table for menu items
        menuModel = new DefaultTableModel(new Object[]{"Item Name", "Price (RM)"}, 0);
        menuTable = new JTable(menuModel);
        JScrollPane scrollPane = new JScrollPane(menuTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Form for adding/editing menu items
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit Menu Item"));

        formPanel.add(new JLabel("Item Name:"));
        itemNameField = new JTextField();
        formPanel.add(itemNameField);

        formPanel.add(new JLabel("Price (RM):"));
        priceField = new JTextField();
        formPanel.add(priceField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Item");
        JButton editButton = new JButton("Update Selected");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addMenuItem());
        editButton.addActionListener(e -> editMenuItem());
        deleteButton.addActionListener(e -> deleteMenuItem());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        formPanel.add(buttonPanel);

        panel.add(formPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Table for current orders
        orderModel = new DefaultTableModel(new Object[]{"Order ID", "Customer", "Items", "Total (RM)", "Status"}, 0);
        orderTable = new JTable(orderModel);
        JScrollPane scrollPane = new JScrollPane(orderTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons for order actions
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton acceptButton = new JButton("Accept Order");
        JButton cancelButton = new JButton("Decline Order");
        JButton readyButton = new JButton("Order Ready");

        acceptButton.addActionListener(e -> updateOrderStatus("Accepted"));
        cancelButton.addActionListener(e -> updateOrderStatus("Declined"));
        readyButton.addActionListener(e -> updateOrderStatus("Ready"));

        buttonPanel.add(acceptButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(readyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // ComboBox for filtering
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> periodCombo = new JComboBox<>(new String[]{"Daily", "Monthly", "Quarterly", "Yearly"});
        filterPanel.add(new JLabel("View period:"));
        filterPanel.add(periodCombo);

        JButton refreshButton = new JButton("Apply Filter");
        refreshButton.addActionListener(e -> filterOrderHistory((String) periodCombo.getSelectedItem()));
        filterPanel.add(refreshButton);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Table for order history
        historyModel = new DefaultTableModel(new Object[]{"Order ID", "Date", "Customer", "Items", "Total (RM)", "Status"}, 0);
        historyTable = new JTable(historyModel);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Table for reviews
        reviewModel = new DefaultTableModel(new Object[]{"Order ID", "Customer", "Rating", "Comment", "Date"}, 0);
        reviewTable = new JTable(reviewModel);
        JScrollPane scrollPane = new JScrollPane(reviewTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRevenuePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Summary Statistics
        JPanel statsPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Revenue Statistics"));

        Map<String, Object> stats = calculateRevenueStats();

        statsPanel.add(new JLabel("Total Orders:"));
        statsPanel.add(new JLabel(String.valueOf(stats.getOrDefault("totalOrders", 0))));

        statsPanel.add(new JLabel("Total Revenue:"));
        statsPanel.add(new JLabel(String.format("RM %.2f", stats.getOrDefault("totalRevenue", 0.0))));

        statsPanel.add(new JLabel("Average Order Value:"));
        statsPanel.add(new JLabel(String.format("RM %.2f", stats.getOrDefault("avgOrderValue", 0.0))));

        statsPanel.add(new JLabel("Top Selling Item:"));
        statsPanel.add(new JLabel(stats.getOrDefault("topItem", "None").toString()));

        statsPanel.add(new JLabel("Average Customer Rating:"));
        statsPanel.add(new JLabel(String.format("%.1f/5", stats.getOrDefault("avgRating", 0.0))));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Add placeholder for chart
        JPanel chartPanel = new JPanel();
        chartPanel.setBorder(BorderFactory.createTitledBorder("Revenue Chart"));
        chartPanel.setPreferredSize(new Dimension(600, 300));

        JLabel chartPlaceholder = new JLabel("Revenue chart visualization would be shown here");
        chartPlaceholder.setHorizontalAlignment(JLabel.CENTER);
        chartPanel.add(chartPlaceholder);

        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Vendor_Menu_Details.txt");
        boolean inVendorSection = false;

        for (String line : lines) {
            if (line.startsWith("Vendor Username: " + username)) {
                inVendorSection = true;
            } else if (line.startsWith("Vendor Username: ") && inVendorSection) {
                break;
            } else if (inVendorSection && line.contains(", Price: ")) {
                String[] parts = line.split(", ");
                String itemName = parts[0].split(": ")[1];
                double price = Double.parseDouble(parts[1].split(": ")[1]);
                menuModel.addRow(new Object[]{itemName, price});
            }
        }
    }

    private void addMenuItem() {
        String itemName = itemNameField.getText().trim();
        String priceText = priceField.getText().trim();

        if (itemName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Price must be greater than zero");
                return;
            }

            // Add to file
            String entry = "Item: " + itemName + ", Price: " + price;
            boolean vendorFound = false;
            ArrayList<String> lines = Panel.returnFileLines("Vendor_Menu_Details.txt");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Vendor_Menu_Details.txt"))) {
                for (String line : lines) {
                    writer.write(line + "\n");
                    if (line.equals("Vendor Username: " + username)) {
                        vendorFound = true;
                        writer.write(entry + "\n");
                    }
                }

                if (!vendorFound) {
                    writer.write("Vendor Username: " + username + "\n");
                    writer.write(entry + "\n");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error updating menu file");
                return;
            }

            // Update table
            menuModel.addRow(new Object[]{itemName, price});
            itemNameField.setText("");
            priceField.setText("");

            JOptionPane.showMessageDialog(this, "Menu item added successfully");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid price");
        }
    }

    private void editMenuItem() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit");
            return;
        }

        String itemName = itemNameField.getText().trim();
        String priceText = priceField.getText().trim();

        if (itemName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Price must be greater than zero");
                return;
            }

            // Get old item name
            String oldItemName = (String) menuTable.getValueAt(selectedRow, 0);

            // Update file
            ArrayList<String> lines = Panel.returnFileLines("Vendor_Menu_Details.txt");
            boolean inVendorSection = false;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Vendor_Menu_Details.txt"))) {
                for (String line : lines) {
                    if (line.startsWith("Vendor Username: " + username)) {
                        inVendorSection = true;
                        writer.write(line + "\n");
                    } else if (line.startsWith("Vendor Username: ") && inVendorSection) {
                        inVendorSection = false;
                        writer.write(line + "\n");
                    } else if (inVendorSection && line.startsWith("Item: " + oldItemName + ", Price: ")) {
                        writer.write("Item: " + itemName + ", Price: " + price + "\n");
                    } else {
                        writer.write(line + "\n");
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error updating menu file");
                return;
            }

            // Update table
            menuModel.setValueAt(itemName, selectedRow, 0);
            menuModel.setValueAt(price, selectedRow, 1);
            itemNameField.setText("");
            priceField.setText("");

            JOptionPane.showMessageDialog(this, "Menu item updated successfully");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid price");
        }
    }

    private void deleteMenuItem() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete");
            return;
        }

        String itemName = (String) menuTable.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete " + itemName + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Update file
            ArrayList<String> lines = Panel.returnFileLines("Vendor_Menu_Details.txt");
            boolean inVendorSection = false;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Vendor_Menu_Details.txt"))) {
                for (String line : lines) {
                    if (line.startsWith("Vendor Username: " + username)) {
                        inVendorSection = true;
                        writer.write(line + "\n");
                    } else if (line.startsWith("Vendor Username: ") && inVendorSection) {
                        inVendorSection = false;
                        writer.write(line + "\n");
                    } else if (inVendorSection && line.startsWith("Item: " + itemName + ", Price: ")) {
                        // Skip this line (delete the item)
                    } else {
                        writer.write(line + "\n");
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error updating menu file");
                return;
            }

            // Update table
            menuModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Menu item deleted successfully");
        }
    }

    private void loadOrders() {
        orderModel.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Order_Info.txt");

        for (String line : lines) {
            if (line.contains("Vendor: " + username) &&
                    (line.contains("Status: Pending") ||
                            line.contains("Status: Accepted") ||
                            line.contains("Status: Ready"))) {

                String[] parts = line.split(", ");
                String orderId = parts[0].split(": ")[1];
                String customer = parts[2].split(": ")[1];
                String items = parts[3].split(": ")[1];
                double total = Double.parseDouble(parts[4].split(": ")[1]);
                String status = parts[5].split(": ")[1];

                orderModel.addRow(new Object[]{orderId, customer, items, total, status});
            }
        }
    }

    private void updateOrderStatus(String newStatus) {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order");
            return;
        }

        String orderId = orderTable.getValueAt(selectedRow, 0).toString();

        // Update Order_Info.txt
        ArrayList<String> lines = Panel.returnFileLines("Order_Info.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Order_Info.txt"))) {
            for (String line : lines) {
                if (line.startsWith("OrderID: " + orderId)) {
                    // Handle different formats in the line
                    if (line.contains("Status: ")) {
                        line = line.replaceAll("Status: [^,]+", "Status: " + newStatus);
                    } else {
                        // If status field doesn't exist, append it
                        line += ", Status: " + newStatus;
                    }
                }
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating order status: " + e.getMessage());
        }

        // Update Order_History.txt if status is Declined or Ready
        if (newStatus.equals("Declined") || newStatus.equals("Ready")) {
            updateOrderHistory(orderId, newStatus);
        }

        // Update table
        orderModel.setValueAt(newStatus, selectedRow, 4);
        JOptionPane.showMessageDialog(this, "Order status updated to " + newStatus);

        // Refresh orders if needed
        if (newStatus.equals("Declined") || newStatus.equals("Ready")) {
            loadOrders();
            loadOrderHistory();
        }
    }

    private void updateOrderHistory(String orderId, String newStatus) {
        ArrayList<String> lines = Panel.returnFileLines("Order_History.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Order_History.txt"))) {
            for (String line : lines) {
                if (line.startsWith(orderId + ", ")) {
                    String[] parts = line.split(", ");
                    StringBuilder newLine = new StringBuilder();

                    for (int i = 0; i < parts.length - 1; i++) {
                        newLine.append(parts[i]).append(", ");
                    }
                    newLine.append(newStatus);

                    writer.write(newLine.toString() + "\n");
                } else {
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating order history");
        }
    }

    private void loadOrderHistory() {
        historyModel.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Order_History.txt");
        ArrayList<String> orderInfoLines = Panel.returnFileLines("Order_Info.txt");

        for (String line : lines) {
            String[] parts = line.split(", ");
            String orderId = parts[0];

            // Find matching order in Order_Info.txt to get vendor
            for (String infoLine : orderInfoLines) {
                if (infoLine.contains("OrderID: " + orderId) && infoLine.contains("Vendor: " + username)) {
                    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // In reality, date should be stored in the order
                    String customer = parts[3];
                    String items = parts[1];
                    double total = Double.parseDouble(parts[2]);
                    String status = parts[4];

                    historyModel.addRow(new Object[]{orderId, date, customer, items, total, status});
                    break;
                }
            }
        }
    }

    private void filterOrderHistory(String period) {
        // In a real application, we would filter based on dates
        // For this sample, we'll just show a message
        JOptionPane.showMessageDialog(this, "Filtering by " + period + " period");
        // The actual implementation would filter the historyModel based on the period
    }

    private void loadReviews() {
        reviewModel.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Reviews.txt");

        for (String line : lines) {
            if (line.contains("Vendor: " + username)) {
                String[] parts = line.split(", ");
                String orderId = parts[0].split(": ")[1];
                String customer = parts[1].split(": ")[1];
                int rating = Integer.parseInt(parts[2].split(": ")[1]);
                String comment = parts[3].split(": ")[1];
                String date = parts[4].split(": ")[1];

                reviewModel.addRow(new Object[]{orderId, customer, rating, comment, date});
            }
        }
    }

    private Map<String, Object> calculateRevenueStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> itemCount = new HashMap<>();

        // Calculate from order history
        ArrayList<String> lines = Panel.returnFileLines("Order_History.txt");
        ArrayList<String> orderInfoLines = Panel.returnFileLines("Order_Info.txt");

        double totalRevenue = 0.0;
        int orderCount = 0;

        for (String line : lines) {
            String[] parts = line.split(", ");
            String orderId = parts[0];

            // Find matching order in Order_Info.txt to get vendor
            for (String infoLine : orderInfoLines) {
                if (infoLine.contains("OrderID: " + orderId) &&
                        infoLine.contains("Vendor: " + username) &&
                        (infoLine.contains("Status: Ready") || infoLine.contains("Status: Completed"))) {

                    double total = Double.parseDouble(parts[2]);
                    totalRevenue += total;
                    orderCount++;

                    // Count items for top-selling
                    String items = parts[1];
                    for (String item : items.split(",")) {
                        String itemTrim = item.trim();
                        itemCount.put(itemTrim, itemCount.getOrDefault(itemTrim, 0) + 1);
                    }

                    break;
                }
            }
        }

        // Find top selling item
        String topItem = "None";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : itemCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topItem = entry.getKey();
            }
        }

        // Calculate average rating
        double totalRating = 0.0;
        int ratingCount = 0;
        ArrayList<String> reviewLines = Panel.returnFileLines("Reviews.txt");

        for (String line : reviewLines) {
            if (line.contains("Vendor: " + username)) {
                String[] parts = line.split(", ");
                totalRating += Double.parseDouble(parts[2].split(": ")[1]);
                ratingCount++;
            }
        }

        stats.put("totalOrders", (double) orderCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("avgOrderValue", orderCount > 0 ? totalRevenue / orderCount : 0.0);
        stats.put("topItem", topItem); // Now storing the String directly
        stats.put("avgRating", ratingCount > 0 ? totalRating / ratingCount : 0.0);

        return stats;
    }

    public static void main(String[] args) {
        // For testing
        SwingUtilities.invokeLater(() -> new VendorDashboard("vendor1"));
    }
}