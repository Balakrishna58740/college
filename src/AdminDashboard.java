import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class AdminDashboard extends JFrame {
    private final CRUD_User crud = new CRUD_User();
    private JTable userTable;
    private UserTableModel tableModel;
    private JComboBox<String> userTypeCombo;

    public AdminDashboard() {
        initializeUI();
        setupTable();
        setupEventListeners();
    }

    private void initializeUI() {
        setTitle("Admin Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        // Control buttons
        JPanel controlPanel = new JPanel();
        addButton(controlPanel, "Add", this::addUser);
        addButton(controlPanel, "Edit", this::editUser);
        addButton(controlPanel, "Delete", this::deleteUser);
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

    private void setupEventListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
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
