import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
    private JComboBox<String> userTypeCombo;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private CRUD_User crud = new CRUD_User();

    public LoginFrame() {
        setTitle("Food Ordering System Login");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(5, 2, 10, 10));

        // Initialize components
        userTypeCombo = new JComboBox<>(new String[]{"Customer", "Vendor", "Delivery Runner", "Admin"});
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        // Add components to frame
        add(new JLabel("User Type:"));
        add(userTypeCombo);
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        add(registerButton);

        // Login button action
        loginButton.addActionListener(e -> {
            String userType = (String) userTypeCombo.getSelectedItem();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (crud.authenticateUser(userType, username, password)) {
                JOptionPane.showMessageDialog(this, "Login successful!");
                openDashboard(userType, username);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
            }
        });

        // Registration button action (only for admin)
        registerButton.addActionListener(e -> {
            if (!userTypeCombo.getSelectedItem().equals("Admin")) {
                JOptionPane.showMessageDialog(this, "Only administrators can register users!");
                return;
            }
            new RegistrationFrame();
        });

        setVisible(true);
    }

    private void openDashboard(String userType, String username) {
        switch (userType) {
            case "Customer":
                new CustomerDashboard(username);
                break;
            case "Vendor":
                new VendorDashboard(username);
                break;
            case "Delivery Runner":
                new RunnerDashboard(username);
                break;
            case "Admin":
                new AdminDashboard();
                break;
        }
    }

    public static void main(String[] args) {
        Panel.checkFiles(); // Initialize required files
        new LoginFrame();
    }
}