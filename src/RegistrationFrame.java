import javax.swing.*;
import java.awt.*;

public class RegistrationFrame extends JFrame {
    private JComboBox<String> userTypeCombo;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField displayNameField;
    private CRUD_User crud = new CRUD_User();

    public RegistrationFrame() {
        setTitle("User Registration (Admin Only)");
        setSize(400, 300);
        setLayout(new GridLayout(5, 2, 10, 10));

        userTypeCombo = new JComboBox<>(new String[]{"Customer", "Vendor", "Delivery Runner"});
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        displayNameField = new JTextField();
        JButton registerButton = new JButton("Register User");

        add(new JLabel("User Type:"));
        add(userTypeCombo);
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(new JLabel("Display Name:"));
        add(displayNameField);
        add(registerButton);

        registerButton.addActionListener(e -> {
            String userType = (String) userTypeCombo.getSelectedItem();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String displayName = displayNameField.getText();

            if (crud.createUser(userType, username, password, displayName)) {
                JOptionPane.showMessageDialog(this, "Registration successful!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed!");
            }
        });

        setVisible(true);
    }
}