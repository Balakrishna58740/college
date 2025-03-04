import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
    private JComboBox<String> userTypeCombo;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private CRUD_User crud = new CRUD_User();

    public LoginFrame() {
        setTitle("Food Ordering System Login");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Set frame background color to light gray
        getContentPane().setBackground(new Color(245, 245, 245));

        // Initialize components
        userTypeCombo = new JComboBox<>(new String[]{"Customer", "Vendor", "Delivery Runner", "Admin"});
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);

        // Set uniform size for fields
        Dimension fieldSize = new Dimension(200, 30);
        userTypeCombo.setPreferredSize(fieldSize);
        usernameField.setPreferredSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);

        // Initialize components using the modified method
        initComponents();

        // Center the frame on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Create main panel with vertical BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        mainPanel.setBackground(new Color(245, 245, 245));

        // User Type panel
        JPanel userTypePanel = createFieldPanel("User Type:", userTypeCombo);

        // Add spacing
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(userTypePanel);

        // Add spacing
        mainPanel.add(Box.createVerticalStrut(30));

        // Username panel
        JPanel usernamePanel = createFieldPanel("Username:", usernameField);
        mainPanel.add(usernamePanel);

        // Add spacing
        mainPanel.add(Box.createVerticalStrut(30));

        // Password panel
        JPanel passwordPanel = createFieldPanel("Password:", passwordField);
        mainPanel.add(passwordPanel);

        // Add spacing
        mainPanel.add(Box.createVerticalStrut(40));

        // Login button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setBackground(new Color(245, 245, 245));

        JButton loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(150, 35));
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.addActionListener(new LoginListener());

        buttonPanel.add(loginButton);
        mainPanel.add(buttonPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createFieldPanel(String labelText, JComponent field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setBackground(new Color(245, 245, 245));

        // Create label with right alignment
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setPreferredSize(new Dimension(120, 30));
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        // Add components with spacing
        panel.add(label);
        panel.add(Box.createHorizontalStrut(15));  // Add some space between label and field
        panel.add(field);

        return panel;
    }

    // Login action listener class
    private class LoginListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String userType = (String) userTypeCombo.getSelectedItem();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (crud.authenticateUser(userType, username, password)) {
                JOptionPane.showMessageDialog(LoginFrame.this, "Login successful!");
                openDashboard(userType, username);
                dispose();
            } else {
                JOptionPane.showMessageDialog(LoginFrame.this, "Invalid credentials!");
            }
        }
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
                new AdminDashboard().setVisible(true); // Explicitly set visibility
                break;
        }
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            Panel.checkFiles(); // Initialize required files
            new LoginFrame();
        });
    }
}