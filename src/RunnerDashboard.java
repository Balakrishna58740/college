import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RunnerDashboard extends JFrame {
    private String username;
    private DefaultTableModel pendingTasksModel, activeTasksModel, completedTasksModel;
    private JLabel earningsLabel;
    private JTabbedPane tabbedPane;
    private JComboBox<String> reportPeriodCombo;
    private JTable pendingTasksTable, activeTasksTable, completedTasksTable;
    private double totalEarnings = 0.0;


    public RunnerDashboard(String username) {
        this.username = username;
        initializeUI();
        loadTasks();
        calculateEarnings();
        initNotificationPanel();
    }

    private void initializeUI() {
        setTitle("Delivery Runner Dashboard - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel with earnings info
        JPanel topPanel = new JPanel(new BorderLayout());
        earningsLabel = new JLabel("Total Earnings: RM 0.00");
        earningsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        earningsLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 0));

        JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reportPeriodCombo = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly", "Yearly", "All Time"});
        reportPanel.add(new JLabel("View Earnings:"));
        reportPanel.add(reportPeriodCombo);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> calculateEarnings());
        reportPanel.add(refreshButton);

        topPanel.add(earningsLabel, BorderLayout.WEST);
        topPanel.add(reportPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Create tabs
        tabbedPane = new JTabbedPane();

        // Pending Tasks Tab
        JPanel pendingTasksPanel = new JPanel(new BorderLayout());
        pendingTasksModel = new DefaultTableModel(
                new Object[]{"Order ID", "Customer", "Items", "Location", "Time", "Fee"}, 0);
        pendingTasksTable = new JTable(pendingTasksModel);
        pendingTasksPanel.add(new JScrollPane(pendingTasksTable), BorderLayout.CENTER);

        JPanel pendingButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton acceptTaskButton = new JButton("Accept Task");
        JButton declineTaskButton = new JButton("Decline Task");
        acceptTaskButton.addActionListener(e -> acceptTask());
        declineTaskButton.addActionListener(e -> declineTask());
        pendingButtonPanel.add(acceptTaskButton);
        pendingButtonPanel.add(declineTaskButton);
        pendingTasksPanel.add(pendingButtonPanel, BorderLayout.SOUTH);

        // Active Tasks Tab
        JPanel activeTasksPanel = new JPanel(new BorderLayout());
        activeTasksModel = new DefaultTableModel(
                new Object[]{"Order ID", "Customer", "Items", "Location", "Time", "Status", "Fee"}, 0);
        activeTasksTable = new JTable(activeTasksModel);
        activeTasksPanel.add(new JScrollPane(activeTasksTable), BorderLayout.CENTER);

        JPanel activeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton pickupButton = new JButton("Mark as Picked Up");
        JButton deliveredButton = new JButton("Mark as Delivered");
        pickupButton.addActionListener(e -> updateTaskStatus("Picked Up"));
        deliveredButton.addActionListener(e -> updateTaskStatus("Delivered"));
        activeButtonPanel.add(pickupButton);
        activeButtonPanel.add(deliveredButton);
        activeTasksPanel.add(activeButtonPanel, BorderLayout.SOUTH);

        // Completed Tasks Tab (History)
        JPanel completedTasksPanel = new JPanel(new BorderLayout());
        completedTasksModel = new DefaultTableModel(
                new Object[]{"Order ID", "Customer", "Items", "Location", "Completion Time", "Fee"}, 0);
        completedTasksTable = new JTable(completedTasksModel);
        completedTasksPanel.add(new JScrollPane(completedTasksTable), BorderLayout.CENTER);

        // Add tabs to the tabbedPane
        tabbedPane.addTab("Pending Tasks", pendingTasksPanel);
        tabbedPane.addTab("Active Tasks", activeTasksPanel);
        tabbedPane.addTab("Task History", completedTasksPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel with refresh and logout
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshAllButton = new JButton("Refresh All");
        JButton logoutButton = new JButton("Logout");
        refreshAllButton.addActionListener(e -> {
            loadTasks();
            calculateEarnings();
        });
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        bottomPanel.add(refreshAllButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // New notification panel added to the EAST side
    private void initNotificationPanel() {
        JPanel notificationPanel = new JPanel(new BorderLayout());
        notificationPanel.setBorder(BorderFactory.createTitledBorder("Notifications"));

        DefaultTableModel notificationModel = new DefaultTableModel(
                new Object[]{"Time", "Message"}, 0
        );
        JTable notificationTable = new JTable(notificationModel);

        javax.swing.Timer timer = new javax.swing.Timer(5000, e -> {
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

    private void updateNotifications(DefaultTableModel model) {
        model.setRowCount(0);
        ArrayList<String> lines = Panel.returnFileLines("Notifications.txt");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for (String line : lines) {
            if (line.contains("User: " + this.username)) {
                String[] parts = line.split(", ");
                model.addRow(new Object[]{
                        sdf.format(new Date()),
                        parts[1].replace("Message: ", "")
                });
            }
        }
    }

    // In RunnerDashboard.java
    private void acceptTask() {
        int selectedRow = pendingTasksTable.getSelectedRow();
        String orderId = (String) pendingTasksModel.getValueAt(selectedRow, 0);
        updateTaskStatus(orderId, "Accepted"); // Updates Delivery_Tasks.txt
        Panel.sendNotification(customer, "Runner accepted your order", "DELIVERY_UPDATE");
    }

    // Add automatic runner assignment
    private void autoAssignRunner(String orderId) {
        ArrayList<String> runners = Panel.returnFileLines("Delivery_Runner_Credentials.txt");
        boolean assigned = false;
        for (String line : runners) {
            if (line.startsWith("Username: ")) {
                String runner = line.split(": ")[1];
                if (!isRunnerBusy(runner)) {
                    updateTaskInFile(orderId, "Assigned", runner);
                    assigned = true;
                    break;
                }
            }
        }
        if (!assigned) {
            String customer = getCustomerFromOrder(orderId);
            Panel.writeToFile("Notifications.txt", "Customer: " + customer +
                    ", Message: No available runners - please choose takeaway");
        }
    }

    // Helper method to determine if a runner is busy
    private boolean isRunnerBusy(String runner) {
        // Stub implementation: In a real system, check runner's current assignments.
        return false;
    }
    private void loadTasks() {
        // Load pending tasks from file or database
        List<DeliveryTask> pendingTasks = loadTasksFromFile("pending"); // Example: Load pending tasks
        updateTableModel(pendingTasksModel, pendingTasks);

        // Load active tasks from file or database
        List<DeliveryTask> activeTasks = loadTasksFromFile("active"); // Example: Load active tasks
        updateTableModel(activeTasksModel, activeTasks);

        // Load completed tasks from file or database
        List<DeliveryTask> completedTasks = loadTasksFromFile("completed"); // Example: Load completed tasks
        updateTableModel(completedTasksModel, completedTasks);
    }

    private List<DeliveryTask> loadTasksFromFile(String taskType) {
        List<DeliveryTask> tasks = new ArrayList<>();
        // Implement the logic to load tasks based on taskType (pending, active, completed) from the file or database
        // This can involve reading from files like "Delivery_Tasks.txt" and filtering based on task type
        return tasks;
    }

    private void updateTableModel(DefaultTableModel model, List<DeliveryTask> tasks) {
        model.setRowCount(0); // Clear existing rows
        for (DeliveryTask task : tasks) {
            model.addRow(new Object[]{
                    task.getOrderId(),
                    task.getCustomer(),
                    task.getItems(),
                    task.getLocation(),
                    task.getOrderTime(), // Change from task.getTime() to task.getOrderTime()
                    task.getFee()
            });
        }
    }

    // Helper method to retrieve customer name from Order_Info.txt based on orderId
    private String getCustomerFromOrder(String orderId) {
        ArrayList<String> lines = Panel.returnFileLines("Order_Info.txt");
        for (String line : lines) {
            if (line.contains("OrderID: " + orderId)) {
                String[] parts = line.split(", ");
                for (String part : parts) {
                    if (part.startsWith("Customer: ")) {
                        return part.split(": ")[1];
                    }
                }
            }
        }
        return "Unknown Customer";
    }

    private void declineTask() {
        int selectedRow = pendingTasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to decline!");
            return;
        }
        String orderId = (String) pendingTasksModel.getValueAt(selectedRow, 0);
        updateTaskInFile(orderId, "Pending", "");
        JOptionPane.showMessageDialog(this, "Task declined!");
        loadTasks();
    }

    private void updateTaskStatus(String orderId, String newStatus) {
        ArrayList<String> lines = Panel.returnFileLines("Delivery_Tasks.txt");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Delivery_Tasks.txt"))) {
            for (String line : lines) {
                if (line.contains("OrderID: " + orderId)) {
                    // Update status and timestamp
                    line = line.replaceAll("Status: [^,]+", "Status: " + newStatus);
                    if (newStatus.equals("Delivered")) {
                        line += ", CompletionTime: " + sdf.format(new Date());
                    }
                }
                writer.write(line + "\n");
            }
            // Notify customer
            String customer = getCustomerFromOrder(orderId);
            Panel.sendNotification(customer, "Order " + orderId + " status: " + newStatus, "DELIVERY_UPDATE");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating task status: " + e.getMessage());
        }
    }

    private void updateTaskInFile(String orderId, String newStatus, String runner) {
        updateTaskInFile(orderId, newStatus, runner, "");
    }

    private void updateTaskInFile(String orderId, String newStatus, String runner, String completionTime) {
        ArrayList<String> lines = Panel.returnFileLines("Delivery_Tasks.txt");
        ArrayList<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("OrderID:" + orderId)) {
                String[] parts = line.split(",");
                StringBuilder updatedLine = new StringBuilder();
                for (String part : parts) {
                    String[] keyValue = part.trim().split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        if (key.equals("Status")) {
                            updatedLine.append("Status: ").append(newStatus);
                        } else if (key.equals("AssignedRunner")) {
                            updatedLine.append("AssignedRunner: ").append(runner);
                        } else if (key.equals("CompletionTime") && !completionTime.isEmpty()) {
                            updatedLine.append("CompletionTime: ").append(completionTime);
                        } else {
                            updatedLine.append(part);
                        }
                    } else {
                        updatedLine.append(part);
                    }
                    updatedLine.append(", ");
                }
                if (!line.contains("CompletionTime") && !completionTime.isEmpty()) {
                    updatedLine.append("CompletionTime: ").append(completionTime);
                }
                String finalLine = updatedLine.toString();
                if (finalLine.endsWith(", ")) {
                    finalLine = finalLine.substring(0, finalLine.length() - 2);
                }
                updatedLines.add(finalLine);
            } else {
                updatedLines.add(line);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Delivery_Tasks.txt"))) {
            for (String line : updatedLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating task: " + e.getMessage());
        }
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        ArrayList<String> lines = Panel.returnFileLines("Order_Info.txt");
        ArrayList<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("OrderID: " + orderId)) {
                if (line.contains("Status: ")) {
                    line = line.replaceAll("Status: [^,]+", "Status: " + newStatus);
                } else {
                    line += ", Status: " + newStatus;
                }
            }
            updatedLines.add(line);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Order_Info.txt"))) {
            for (String line : updatedLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating order status: " + e.getMessage());
        }

        // Handle declined orders by refunding customer credit
        if (newStatus.equals("Declined")) {
            refundCustomerCredit(orderId);
        }

        // Send notification to customer
        String customer = getCustomerFromOrder(orderId);
        String notification = String.format("Order %s status changed to %s", orderId, newStatus);
        Panel.writeToFile("Notifications.txt", "Customer: " + customer + ", Message: " + notification);
    }

    // Method to refund customer credit if an order is declined
    private void refundCustomerCredit(String orderId) {
        ArrayList<String> orderLines = Panel.returnFileLines("Order_History.txt");
        for (String line : orderLines) {
            if (line.startsWith(orderId + ", ")) {
                double amount = Double.parseDouble(line.split(", ")[2]);
                String customer = line.split(", ")[3];

                ArrayList<String> creditLines = Panel.returnFileLines("Customer_Credits.txt");
                for (int i = 0; i < creditLines.size(); i++) {
                    if (creditLines.get(i).contains("Username: " + customer)) {
                        double current = Double.parseDouble(creditLines.get(i).split(", Credit: ")[1]);
                        creditLines.set(i, String.format("Username: %s, Credit: %.2f", customer, current + amount));
                        Panel.writeFile("Customer_Credits.txt", creditLines);
                        break;
                    }
                }
            }
        }
    }

    private void calculateEarnings() {
        totalEarnings = 0.0;
        String selectedPeriod = (String) reportPeriodCombo.getSelectedItem();
        List<DeliveryTask> completedTasks = loadTasksFromFile("completed").stream() // Specify "completed"
                .filter(task -> task.getStatus().equals("Delivered") &&
                        task.getAssignedRunner() != null &&
                        task.getAssignedRunner().equals(username))
                .collect(Collectors.toList());
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        switch (selectedPeriod) {
            case "Daily":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "Weekly":
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case "Monthly":
                cal.add(Calendar.MONTH, -1);
                break;
            case "Yearly":
                cal.add(Calendar.YEAR, -1);
                break;
            case "All Time":
                break;
        }
        Date filterDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (DeliveryTask task : completedTasks) {
            if (!selectedPeriod.equals("All Time")) {
                try {
                    Date completionDate = sdf.parse(task.getCompletionTime());
                    if (completionDate.before(filterDate)) {
                        continue;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing date: " + task.getCompletionTime());
                }
            }
            totalEarnings += task.getFee();
        }
        earningsLabel.setText(String.format("Total Earnings (%s): RM %.2f", selectedPeriod, totalEarnings));
    }

    // Inner class to represent a delivery task
    private class DeliveryTask {
        private String orderId;
        private String customer;
        private String items;
        private String location;
        private String orderTime;
        private String completionTime;
        private String status;
        private double fee;
        private String assignedRunner;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        public String getItems() { return items; }
        public void setItems(String items) { this.items = items; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getOrderTime() { return orderTime; }
        public void setOrderTime(String orderTime) { this.orderTime = orderTime; }
        public String getCompletionTime() { return completionTime; }
        public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
        public String getAssignedRunner() { return assignedRunner; }
        public void setAssignedRunner(String assignedRunner) { this.assignedRunner = assignedRunner; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RunnerDashboard("test_runner").setVisible(true));
    }
}
