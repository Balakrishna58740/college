import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CustomerDashboard extends JFrame {
    private String username;
    private DefaultTableModel menuModel, cartModel, orderModel;
    private Map<String, Double> cartItems = new HashMap<>();
    private double currentCredit;

    public CustomerDashboard(String username) {
        this.username = username;
        this.currentCredit = getCustomerCredit();
        initializeUI();
        loadVendorMenus();
        loadOrderHistory();
    }

    private void initializeUI() {
        setTitle("Customer Dashboard - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu Panel
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuModel = new DefaultTableModel(new Object[]{"Vendor", "Item", "Price"}, 0);
        JTable menuTable = new JTable(menuModel);
        menuPanel.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        // Cart Panel
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartModel = new DefaultTableModel(new Object[]{"Item", "Quantity", "Price"}, 0);
        JTable cartTable = new JTable(cartModel);
        JButton addToCartBtn = new JButton("Add to Cart");
        JButton placeOrderBtn = new JButton("Place Order");
        JButton cancelOrderBtn = new JButton("Cancel Order");

        addToCartBtn.addActionListener(e -> addToCart(menuTable));
        placeOrderBtn.addActionListener(e -> placeOrder());
        cancelOrderBtn.addActionListener(e -> clearCart());

        JPanel cartControls = new JPanel();
        cartControls.add(addToCartBtn);
        cartControls.add(placeOrderBtn);
        cartControls.add(cancelOrderBtn);

        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartPanel.add(cartControls, BorderLayout.SOUTH);

        // Order History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        orderModel = new DefaultTableModel(new Object[]{"Order ID", "Items", "Total", "Status"}, 0);
        JTable orderTable = new JTable(orderModel);
        historyPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // TabbedPane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Menu", menuPanel);
        tabbedPane.addTab("Cart", cartPanel);
        tabbedPane.addTab("Order History", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Credit Display
        JLabel creditLabel = new JLabel("Current Credit: RM " + currentCredit);
        add(creditLabel, BorderLayout.NORTH);

        setVisible(true);
    }

    private double getCustomerCredit() {
        ArrayList<String> lines = Panel.returnFileLines("Customer_Credits.txt");
        for (String line : lines) {
            if (line.startsWith("Username: " + username)) {
                return Double.parseDouble(line.split(": ")[2].trim());
            }
        }
        return 0.0;
    }

    private void loadVendorMenus() {
        ArrayList<String> lines = Panel.returnFileLines("Vendor_Menu_Details.txt");
        String currentVendor = "";

        for (String line : lines) {
            if (line.startsWith("Vendor Username: ")) {
                currentVendor = line.split(": ")[1];
            } else if (!line.isEmpty() && !line.equals("*** EOF ***")) {
                String[] parts = line.split(", ");
                menuModel.addRow(new Object[]{
                        currentVendor,
                        parts[0].split(": ")[1],
                        Double.parseDouble(parts[1].split(": ")[1])
                });
            }
        }
    }

    private void addToCart(JTable menuTable) {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) return;

        String item = (String) menuTable.getValueAt(selectedRow, 1);
        double price = (double) menuTable.getValueAt(selectedRow, 2);

        String quantity = JOptionPane.showInputDialog("Enter quantity for " + item);
        try {
            int qty = Integer.parseInt(quantity);
            if (qty > 0) {
                cartItems.put(item, cartItems.getOrDefault(item, 0.0) + qty * price);
                updateCartDisplay();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity!");
        }
    }

    private void updateCartDisplay() {
        cartModel.setRowCount(0);
        cartItems.forEach((item, total) -> {
            cartModel.addRow(new Object[]{item, total / getItemPrice(item), total});
        });
    }

    private double getItemPrice(String item) {
        for (int i = 0; i < menuModel.getRowCount(); i++) {
            if (menuModel.getValueAt(i, 1).equals(item)) {
                return (double) menuModel.getValueAt(i, 2);
            }
        }
        return 0.0;
    }

    private void clearCart() {
        cartItems.clear();
        cartModel.setRowCount(0);
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        double total = cartItems.values().stream().mapToDouble(Double::doubleValue).sum();
        int deliveryOption = JOptionPane.showConfirmDialog(
                this,
                "Add delivery service? (+RM 2.00)",
                "Delivery Option",
                JOptionPane.YES_NO_OPTION
        );

        if (deliveryOption == JOptionPane.YES_OPTION) {
            total += 2.0;
        }

        if (currentCredit < total) {
            JOptionPane.showMessageDialog(this, "Insufficient credit!");
            return;
        }

        // Process payment
        currentCredit -= total;
        updateCreditFile();

        // Create order
        int orderId = new Random().nextInt(9000) + 1000;
        String items = String.join(", ", cartItems.keySet());

        // Save to order history
        String orderEntry = String.format("%d, %s, %.2f, %s, Pending",
                orderId, items, total, username);
        Panel.writeToFile("Order_History.txt", orderEntry);

        // Notify vendor
        String vendor = getFirstVendor();
        String orderInfo = String.format("OrderID: %d, Vendor: %s, Customer: %s, Items: %s, Total: %.2f, Status: Pending",
                orderId, vendor, username, items, total);
        Panel.writeToFile("Order_Info.txt", orderInfo);

        JOptionPane.showMessageDialog(this, "Order placed successfully! Order ID: " + orderId);
        clearCart();
        loadOrderHistory();
    }

    private String getFirstVendor() {
        for (int i = 0; i < menuModel.getRowCount(); i++) {
            if (menuModel.getValueAt(i, 1).equals(cartItems.keySet().iterator().next())) {
                return (String) menuModel.getValueAt(i, 0);
            }
        }
        return "Unknown Vendor";
    }

    private void updateCreditFile() {
        ArrayList<String> lines = Panel.returnFileLines("Customer_Credits.txt");
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Username: " + username)) {
                lines.set(i, String.format("Username: %s, Credit: %.2f", username, currentCredit));
                break;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Customer_Credits.txt"))) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating credit!");
        }
    }

    private void loadOrderHistory() {
        orderModel.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Order_History.txt");
        for (String line : lines) {
            String[] parts = line.split(", ");
            if (parts.length >= 5 && parts[3].equals(username)) {
                orderModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[4]});
            }
        }
    }

    public static void main(String[] args) {
        // For testing
        new CustomerDashboard("test_customer");
    }
}