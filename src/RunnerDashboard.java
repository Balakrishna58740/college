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

    private void loadTasks() {
        pendingTasksModel.setRowCount(0);
        activeTasksModel.setRowCount(0);
        completedTasksModel.setRowCount(0);

        List<DeliveryTask> allTasks = loadTasksFromFile();

        for (DeliveryTask task : allTasks) {
            switch (task.getStatus()) {
                case "Pending":
                    if (task.getAssignedRunner() == null ||
                            task.getAssignedRunner().isEmpty() ||
                            task.getAssignedRunner().equals(username)) {
                        pendingTasksModel.addRow(new Object[]{
                                task.getOrderId(),
                                task.getCustomer(),
                                task.getItems(),
                                task.getLocation(),
                                task.getOrderTime(),
                                String.format("RM %.2f", task.getFee())
                        });
                    }
                    break;
                case "Accepted":
                case "Picked Up":
                    if (task.getAssignedRunner().equals(username)) {
                        activeTasksModel.addRow(new Object[]{
                                task.getOrderId(),
                                task.getCustomer(),
                                task.getItems(),
                                task.getLocation(),
                                task.getOrderTime(),
                                task.getStatus(),
                                String.format("RM %.2f", task.getFee())
                        });
                    }
                    break;
                case "Delivered":
                    if (task.getAssignedRunner().equals(username)) {
                        completedTasksModel.addRow(new Object[]{
                                task.getOrderId(),
                                task.getCustomer(),
                                task.getItems(),
                                task.getLocation(),
                                task.getCompletionTime(),
                                String.format("RM %.2f", task.getFee())
                        });
                    }
                    break;
            }
        }
    }

    private List<DeliveryTask> loadTasksFromFile() {
        List<DeliveryTask> tasks = new ArrayList<>();
        ArrayList<String> lines = Panel.returnFileLines("Delivery_Tasks.txt");

        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.equals("*** EOF ***")) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    DeliveryTask task = new DeliveryTask();
                    for (String part : parts) {
                        String[] keyValue = part.trim().split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();

                            switch (key) {
                                case "OrderID":
                                    task.setOrderId(value);
                                    break;
                                case "Customer":
                                    task.setCustomer(value);
                                    break;
                                case "Items":
                                    task.setItems(value);
                                    break;
                                case "Location":
                                    task.setLocation(value);
                                    break;
                                case "OrderTime":
                                    task.setOrderTime(value);
                                    break;
                                case "CompletionTime":
                                    task.setCompletionTime(value);
                                    break;
                                case "Status":
                                    task.setStatus(value);
                                    break;
                                case "Fee":
                                    task.setFee(Double.parseDouble(value));
                                    break;
                                case "AssignedRunner":
                                    task.setAssignedRunner(value);
                                    break;
                            }
                        }
                    }
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    private void acceptTask() {
        int selectedRow = pendingTasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to accept!");
            return;
        }

        String orderId = (String) pendingTasksModel.getValueAt(selectedRow, 0);
        updateTaskInFile(orderId, "Accepted", username);

        JOptionPane.showMessageDialog(this, "Task accepted successfully!");
        loadTasks();
    }

    private void declineTask() {
        int selectedRow = pendingTasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to decline!");
            return;
        }

        String orderId = (String) pendingTasksModel.getValueAt(selectedRow, 0);
        // If the task is already assigned to this runner, mark it as declined
        updateTaskInFile(orderId, "Pending", "");

        JOptionPane.showMessageDialog(this, "Task declined!");
        loadTasks();
    }

    private void updateTaskStatus(String newStatus) {
        int selectedRow = activeTasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to update!");
            return;
        }

        String orderId = (String) activeTasksModel.getValueAt(selectedRow, 0);

        // If task is marked as delivered, add completion time
        String completionTime = "";
        if (newStatus.equals("Delivered")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            completionTime = sdf.format(new Date());
        }

        updateTaskInFile(orderId, newStatus, username, completionTime);

        // Update order status in Order_Info.txt as well
        updateOrderStatus(orderId, newStatus);

        JOptionPane.showMessageDialog(this, "Task status updated to: " + newStatus);
        loadTasks();

        if (newStatus.equals("Delivered")) {
            calculateEarnings();
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
                // Parse the existing line
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

                // If CompletionTime wasn't in the original line but we need to add it
                if (!line.contains("CompletionTime") && !completionTime.isEmpty()) {
                    updatedLine.append("CompletionTime: ").append(completionTime);
                }

                // Remove trailing comma and space if present
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
                // Handle different formats in the line
                if (line.contains("Status: ")) {
                    line = line.replaceAll("Status: [^,]+", "Status: " + newStatus);
                } else {
                    // If status field doesn't exist, append it
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
    }

    private void calculateEarnings() {
        totalEarnings = 0.0;
        String selectedPeriod = (String) reportPeriodCombo.getSelectedItem();

        List<DeliveryTask> completedTasks = loadTasksFromFile().stream()
                .filter(task -> task.getStatus().equals("Delivered") &&
                        task.getAssignedRunner() != null &&
                        task.getAssignedRunner().equals(username))
                .collect(Collectors.toList());

        // Apply time filter based on selected period
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
                // No filtering needed
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
                    // If there's an error parsing the date, include the task
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
        // Test entry point
        SwingUtilities.invokeLater(() -> new RunnerDashboard("test_runner").setVisible(true));
    }
}