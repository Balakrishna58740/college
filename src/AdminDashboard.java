import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;



public class AdminDashboard extends JFrame {
    private final CRUD_User crud = new CRUD_User();
    private JTable userTable;
    private UserTableModel tableModel;
    private JComboBox<String> userTypeCombo;
    private String username;


    // Modified constructor
    public AdminDashboard() {
        initializeUI();
        setupTable();
        setupEventListeners();
        setLocationRelativeTo(null); // Center window
    }

    // Updated initializeUI() method
    private void initializeUI() {
        setTitle("Admin Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Changed to dispose
        setLayout(new BorderLayout());

        // User type selection
        JPanel topPanel = new JPanel();
        userTypeCombo = new JComboBox<>(new String[]{"Vendor", "Customer", "Delivery Runner"});
        topPanel.add(new JLabel("User Type:"));
        topPanel.add(userTypeCombo);
        add(topPanel, BorderLayout.NORTH);

        // Table
        tableModel = new UserTableModel();
        userTable = new JTable(tableModel);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Modify control panel in initializeUI()
        JPanel controlPanel = new JPanel();
        addButton(controlPanel, "Add", this::addUser);
        addButton(controlPanel, "Edit", this::editUser);
        addButton(controlPanel, "Delete", this::deleteUser);
        addButton(controlPanel, "Top-Up Credit", this::topUpCredit);
        addButton(controlPanel, "Logout", e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void addButton(JPanel panel, String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    private void setupTable() {
        userTypeCombo.addActionListener(e -> refreshTable());
        refreshTable();
    }

    private void refreshTable() {
        String type = (String) userTypeCombo.getSelectedItem();
        tableModel.setUsers(crud.getAllUsers(getFilename(type)));
    }

    private String getFilename(String userType) {
        return userType.replace(" ", "_") + "_Credentials.txt";
    }

    // Remove the window closing listener in setupEventListeners()
    private void setupEventListeners() {
        // Remove this entire WindowAdapter
    }

    private void addUser(ActionEvent e) {
        String[] userData = showUserDialog("Add New User");
        if (userData != null) {
            boolean success = crud.createUser(
                    getFilename((String) userTypeCombo.getSelectedItem()),
                    userData[0], userData[1], userData[2]
            );
            if (success) refreshTable();
        }
    }

    private void editUser(ActionEvent e) {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user!");
            return;
        }
        String oldUsername = (String) tableModel.getValueAt(selectedRow, 0);
        String[] userData = showUserDialog("Edit User");
        if (userData != null) {
            boolean success = crud.updateUser(
                    getFilename((String) userTypeCombo.getSelectedItem()),
                    oldUsername, userData[0], userData[1], userData[2]
            );
            if (success) refreshTable();
        }
    }

    private void deleteUser(ActionEvent e) {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user!");
            return;
        }
        String username = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete user " + username + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = crud.deleteUser(
                    getFilename((String) userTypeCombo.getSelectedItem()),
                    username
            );
            if (success) refreshTable();
        }
    }

    // New method for credit top-up
    private void topUpCredit(ActionEvent e) {
        JTextField usernameField = new JTextField();
        JTextField amountField = new JTextField();

        Object[] message = {
                "Customer Username:", usernameField,
                "Top-Up Amount (RM):", amountField
        };
        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Credit Top-Up",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String amountStr = amountField.getText().trim();

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new NumberFormatException();

                // Update credit file
                ArrayList<String> lines = Panel.returnFileLines("Customer_Credits.txt");
                boolean found = false;

                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("Username: " + username)) {
                        double current = Double.parseDouble(lines.get(i).split(", Credit: ")[1]);
                        lines.set(i, String.format("Username: %s, Credit: %.2f",
                                username, current + amount));
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    lines.add(String.format("Username: %s, Credit: %.2f", username, amount));
                }

                Panel.writeFile("Customer_Credits.txt", lines);
                JOptionPane.showMessageDialog(this, "Credit updated successfully!");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount! Enter positive numbers only");
            }
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

    private String[] showUserDialog(String title) {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField displayNameField = new JTextField();
        Object[] message = {
                "Username:", usernameField,
                "Password:", passwordField,
                "Display Name:", displayNameField
        };
        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION
        );
        if (option == JOptionPane.OK_OPTION) {
            return new String[]{
                    usernameField.getText().trim(),
                    new String(passwordField.getPassword()).trim(),
                    displayNameField.getText().trim()
            };
        }
        return null;
    }

    class UserTableModel extends AbstractTableModel {
        private ArrayList<String[]> users = new ArrayList<>();
        private final String[] columns = {"Username", "Password", "Display Name"};

        public void setUsers(ArrayList<String[]> users) {
            this.users = users;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return users.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return users.get(row)[col];
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboard().setVisible(true));
    }
}