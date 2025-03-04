import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.Timer;
import java.util.Date;
import java.text.SimpleDateFormat;


public class CustomerDashboard extends JFrame {
    private String username;
    private DefaultTableModel menuModel, cartModel, orderModel;
    private Map<String, CartItem> cartItems = new HashMap<>();
    private double currentCredit;
    private JLabel creditLabel = new JLabel();
    private JTable orderTable;  // Add this
    private JTabbedPane tabbedPane;

    public CustomerDashboard(String username) {
        this.username = username;
        this.currentCredit = getCustomerCredit();
        initializeUI();
        loadVendorMenus();
        loadOrderHistory();
        setupOrderNotifications(); // Start order notification timer
    }

    // Inner class to represent items in the cart
    private class CartItem {
        int quantity;
        double price;

        CartItem(int quantity, double price) {
            this.quantity = quantity;
            this.price = price;
        }

        double getTotal() {
            return quantity * price;
        }
    }

    private void initializeUI() {
        setTitle("Customer Dashboard - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu Panel
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuModel = new DefaultTableModel(new Object[]{"Vendor", "Item", "Price (RM)"}, 0);
        JTable menuTable = new JTable(menuModel);
        JScrollPane menuScroll = new JScrollPane(menuTable);

        JButton addToCartBtn = new JButton("Add to Cart");
        addToCartBtn.addActionListener(e -> addToCart(menuTable));

        JPanel menuButtonPanel = new JPanel();
        menuButtonPanel.add(addToCartBtn);
        menuPanel.add(menuScroll, BorderLayout.CENTER);
        menuPanel.add(menuButtonPanel, BorderLayout.SOUTH);

        // Cart Panel
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartModel = new DefaultTableModel(new Object[]{"Item", "Quantity", "Unit Price", "Total"}, 0);
        JTable cartTable = new JTable(cartModel);

        JButton placeOrderBtn = new JButton("Place Order");
        JButton clearCartBtn = new JButton("Clear Cart");
        JButton editCartBtn = new JButton("Edit Quantity");
        JButton removeItemBtn = new JButton("Remove Item");

        placeOrderBtn.addActionListener(e -> placeOrder());
        clearCartBtn.addActionListener(e -> clearCart());
        editCartBtn.addActionListener(e -> editCartItem(cartTable));
        removeItemBtn.addActionListener(e -> removeCartItem(cartTable));

        JPanel cartControlPanel = new JPanel(new FlowLayout());
        cartControlPanel.add(placeOrderBtn);
        cartControlPanel.add(clearCartBtn);
        cartControlPanel.add(editCartBtn);
        cartControlPanel.add(removeItemBtn);

        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartPanel.add(cartControlPanel, BorderLayout.SOUTH);

        // Order History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        orderModel = new DefaultTableModel(new Object[]{"Order ID", "Items", "Total (RM)", "Status"}, 0);
        orderTable = new JTable(orderModel);  // Remove 'JTable' declaration
        historyPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // Tabbed Pane (modify this section)
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Menu", menuPanel);
        tabbedPane.addTab("Cart", cartPanel);
        tabbedPane.addTab("Order History", historyPanel);
        add(tabbedPane, BorderLayout.CENTER);

        // Credit Display
        creditLabel.setText("Current Credit: RM " + String.format("%.2f", currentCredit));
        JPanel creditPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        creditPanel.add(creditLabel);
        add(creditPanel, BorderLayout.NORTH);

        // Logout Panel
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        logoutPanel.add(logoutButton);
        add(logoutPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private double getCustomerCredit() {
        ArrayList<String> lines = Panel.returnFileLines("Customer_Credits.txt");
        for (String line : lines) {
            if (line.startsWith("Username: " + username)) {
                return Double.parseDouble(line.split(", Credit: ")[1].trim());
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
    private void reorderFromHistory() {
        if (orderTable == null) {
            JOptionPane.showMessageDialog(this, "Order history not initialized!");
            return;
        }
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order to reorder!");
            return;
        }

        String itemsStr = (String) orderModel.getValueAt(selectedRow, 1);
        String[] items = itemsStr.split("; ");

        cartItems.clear();
        for (String itemName : items) {
            for (int i = 0; i < menuModel.getRowCount(); i++) {
                if (menuModel.getValueAt(i, 1).equals(itemName)) {
                    double price = (Double) menuModel.getValueAt(i, 2);
                    cartItems.put(itemName, new CartItem(1, price));
                    break;
                }
            }
        }
        updateCartDisplay();
        tabbedPane.setSelectedIndex(1); // Switch to cart tab
    }
    private boolean hasReview(String orderId) {
        ArrayList<String> reviews = Panel.returnFileLines("Reviews.txt");
        for (String review : reviews) {
            if (review.contains("OrderID: " + orderId)) {
                return true;
            }
        }
        return false;
    }

    private void checkForOrderUpdates() {
        ArrayList<String> notifications = Panel.returnFileLines("Notifications.txt");
        for (String notification : notifications) {
            if (notification.contains("User: " + username) && notification.contains("Delivered")) {
                String orderId = notification.split("Order ")[1].split(" ")[0];
                if (!hasReview(orderId)) {
                    promptReview(orderId);
                }
            }
        }
    }

    private void promptReview(String orderId) {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JComboBox<Integer> rating = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JTextArea comment = new JTextArea(3,20);

        panel.add(new JLabel("Rating (1-5):"));
        panel.add(rating);
        panel.add(new JLabel("Comment:"));
        panel.add(new JScrollPane(comment));

        int result = JOptionPane.showConfirmDialog(this, panel, "Review Order "+orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String review = String.format("OrderID: %s, Customer: %s, Rating: %d, Comment: %s, Date: %s",
                    orderId, username, rating.getSelectedItem(), comment.getText().trim(),
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            Panel.writeToFile("Reviews.txt", review);
        }
    }

    private void initNotificationPanel() {
        JPanel notificationPanel = new JPanel(new BorderLayout());
        notificationPanel.setBorder(BorderFactory.createTitledBorder("Notifications"));

        DefaultTableModel notificationModel = new DefaultTableModel(
                new Object[]{"Time", "Message"}, 0
        );
        JTable notificationTable = new JTable(notificationModel);

        Timer timer = new Timer(5000, e -> {
            notificationModel.setRowCount(0);
            ArrayList<String> lines = Panel.returnFileLines("Notifications.txt");
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm:ss");

            for (String line : lines) {
                if (line.contains("User: " + username)) {
                    String[] parts = line.split(", ");
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .parse(parts[3].split(": ")[1]);
                        notificationModel.addRow(new Object[]{
                                displayFormat.format(date),
                                parts[1].split("Message: ")[1]
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace(); // Handle parsing errors
                    }
                }
            }
        });

        timer.start();
        notificationPanel.add(new JScrollPane(notificationTable), BorderLayout.CENTER);
        add(notificationPanel, BorderLayout.EAST);
    }

    private void addToCart(JTable menuTable) {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the menu!");
            return;
        }
        String item = (String) menuTable.getValueAt(selectedRow, 1);
        double price = (double) menuTable.getValueAt(selectedRow, 2);
        String quantityStr = JOptionPane.showInputDialog("Enter quantity for " + item + ":");
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 1) throw new NumberFormatException();
            if (cartItems.containsKey(item)) {
                CartItem existing = cartItems.get(item);
                existing.quantity += quantity;
            } else {
                cartItems.put(item, new CartItem(quantity, price));
            }
            updateCartDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity (1 or more)!");
        }
    }

    private void updateCartDisplay() {
        cartModel.setRowCount(0);
        cartItems.forEach((item, cartItem) -> {
            cartModel.addRow(new Object[]{
                    item,
                    cartItem.quantity,
                    String.format("%.2f", cartItem.price),
                    String.format("%.2f", cartItem.getTotal())
            });
        });
    }

    private void clearCart() {
        cartItems.clear();
        cartModel.setRowCount(0);
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty!");
            return;
        }

        double itemsTotal = cartItems.values().stream().mapToDouble(CartItem::getTotal).sum();
        int deliveryOption = JOptionPane.showConfirmDialog(
                this,
                "Add delivery service? (+RM 2.00)",
                "Delivery Option",
                JOptionPane.YES_NO_OPTION
        );

        int orderId = new Random().nextInt(9000) + 1000;
        String items = String.join("; ", cartItems.keySet());
        String vendor = getFirstVendor();

        boolean deliveryAssigned = false;
        double total = itemsTotal;

        // Handle delivery assignment
        if (deliveryOption == JOptionPane.YES_OPTION) {
            deliveryAssigned = autoAssignRunner(orderId);
            if (deliveryAssigned) {
                total += 2.0;
            } else {
                JOptionPane.showMessageDialog(this, "No available runners. Switching to takeaway.");
                deliveryOption = JOptionPane.NO_OPTION;
            }
        }

        // Check credit balance
        if (currentCredit < total) {
            JOptionPane.showMessageDialog(this,
                    "Insufficient credit!\nRequired: RM " + String.format("%.2f", total) +
                            "\nAvailable: RM " + String.format("%.2f", currentCredit));
            return;
        }

        // Write order details to files
        String orderEntry = String.format("%d, %s, %.2f, %s, Pending, %s",
                orderId, items, total, username, vendor);
        Panel.writeToFile("Order_History.txt", orderEntry);

        String orderInfo = String.format("OrderID: %d, Vendor: %s, Customer: %s, Items: %s, Total: %.2f, Status: Pending",
                orderId, vendor, username, items, total);
        Panel.writeToFile("Order_Info.txt", orderInfo);

        // Vendor notification
        String vendorNotification = String.format("New order %d from %s", orderId, username);
        Panel.writeToFile("Notifications.txt", "Vendor: " + vendor + ", Message: " + vendorNotification);

        currentCredit -= total;
        updateCreditFile();
        creditLabel.setText("Current Credit: RM " + String.format("%.2f", currentCredit));
        clearCart();
        loadOrderHistory();

        JOptionPane.showMessageDialog(this,
                "Order placed successfully!\nOrder ID: " + orderId +
                        "\nTotal: RM " + String.format("%.2f", total));
    }
    // Stub for automatic runner assignment
    // In CustomerDashboard.java
    private boolean autoAssignRunner(int orderId) {
        ArrayList<String> runners = Panel.returnFileLines("Delivery_Runner_Credentials.txt");
        for (String line : runners) {
            if (line.startsWith("Username: ")) {
                String runner = line.split(": ")[1].trim();
                if (!isRunnerBusy(runner)) {
                    // Assign task to runner
                    String taskEntry = String.format("OrderID: %d, Customer: %s, AssignedRunner: %s, Status: Assigned, Fee: 2.00",
                            orderId, username, runner);
                    Panel.writeToFile("Delivery_Tasks.txt", taskEntry);
                    Panel.sendNotification(runner, "New delivery task: Order " + orderId, "TASK_ASSIGNED");
                    return true;
                }
            }
        }
        return false; // No available runners
    }

    private boolean isRunnerBusy(String runner) {
        ArrayList<String> tasks = Panel.returnFileLines("Delivery_Tasks.txt");
        for (String task : tasks) {
            if (task.contains("AssignedRunner: " + runner) &&
                    (task.contains("Status: Assigned") || task.contains("Status: Picked Up"))) {
                return true;
            }
        }
        return false;
    }

    private String getFirstVendor() {
        if (menuModel.getRowCount() > 0) {
            return (String) menuModel.getValueAt(0, 0);
        }
        return "Unknown Vendor";
    }

    private void updateCreditFile() {
        ArrayList<String> lines = Panel.returnFileLines("Customer_Credits.txt");
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Username: " + username)) {
                lines.set(i, String.format("Username: %s, Credit: %.2f", username, currentCredit));
                found = true;
                break;
            }
        }
        if (!found) {
            lines.add(String.format("Username: %s, Credit: %.2f", username, currentCredit));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Customer_Credits.txt"))) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating credit information.");
        }
    }

    private void loadOrderHistory() {
        orderModel.setRowCount(0);
        ArrayList<String> orderHistory = Panel.returnFileLines("Order_History.txt");

        for (String order : orderHistory) {
            String[] parts = order.split(", ");
            if (parts.length >= 4) { // Ensures sufficient elements exist
                orderModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3]});
            }
        }
    }

    // Timer for order notifications
    private void setupOrderNotifications() {
        Timer timer = new Timer(5000, e -> checkForOrderUpdates());
        timer.start();
    }



    private void editCartItem(JTable cartTable) {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit!");
            return;
        }
        String item = (String) cartTable.getValueAt(selectedRow, 0);
        String newQuantityStr = JOptionPane.showInputDialog("Enter new quantity for " + item + ":");
        try {
            int newQuantity = Integer.parseInt(newQuantityStr);
            if (newQuantity < 1) throw new NumberFormatException();
            cartItems.get(item).quantity = newQuantity;
            updateCartDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity (1 or more)!");
        }
    }

    private void removeCartItem(JTable cartTable) {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove!");
            return;
        }
        String item = (String) cartTable.getValueAt(selectedRow, 0);
        cartItems.remove(item);
        updateCartDisplay();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CustomerDashboard("customer1"));
    }
}
