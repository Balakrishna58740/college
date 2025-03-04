import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Panel {

    // Initialize all required files
    public static void checkFiles() {
        createIfNotExists("Admin_Credentials.txt");
        createIfNotExists("Vendor_Credentials.txt");
        createIfNotExists("Customer_Credentials.txt");
        createIfNotExists("Delivery_Runner_Credentials.txt");
        createIfNotExists("Customer_Credits.txt");
        createIfNotExists("Vendor_Menu_Details.txt");
        createIfNotExists("Order_History.txt");
        createIfNotExists("Order_Info.txt");
        createIfNotExists("Notifications.txt");
        createIfNotExists("Delivery_Tasks.txt");
        createIfNotExists("Reviews.txt");
        createIfNotExists("Transaction_Receipts.txt");// Adding the Notifications file
    }

    // Create a file if it doesn't already exist
    private static void createIfNotExists(String filename) {
        try {
            File file = new File(filename);
            if (file.createNewFile()) {
                System.out.println("Created new file: " + filename);
            }
        } catch (IOException e) {
            System.out.println("Error creating file: " + filename);
        }
    }

    // Read all lines from a file
    public static ArrayList<String> returnFileLines(String filename) {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + filename);
        }
        return lines;
    }

    // Append to a file
    public static void writeToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error writing to file: " + filename);
        }
    }

    // Write all lines to a file (overwrites existing content)
    public static void writeFile(String filename, ArrayList<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + filename);
        }
    }

    // Modify specific line in a file
    public static void makeChangesToTheFile(String filename, int lineNumber, String newContent) {
        ArrayList<String> lines = returnFileLines(filename);
        if (lineNumber >= 0 && lineNumber < lines.size()) {
            lines.set(lineNumber, newContent);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (String line : lines) {
                    writer.write(line + "\n");
                }
            } catch (IOException e) {
                System.out.println("Error updating file: " + filename);
            }
        }
    }

    // Send notification with username, message, and type
    public static void sendNotification(String username, String message, String type) {
        String entry = String.format("User: %s, Message: %s, Type: %s, Time: %s",
                username,
                message,
                type,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );
        writeToFile("Notifications.txt", entry);
    }

    // Original panel display methods (keep for compatibility)
    public static void displayAdminPanel() {
        System.out.println("\n=== Admin Panel ===");
    }

    public static void displayVendorPanel() {
        System.out.println("\n=== Vendor Panel ===");
    }

    public static void displayCustomerPanel() {
        System.out.println("\n=== Customer Panel ===");
    }

    public static void displayDeliveryRunnerPanel() {
        System.out.println("\n=== Delivery Runner Panel ===");
    }

    // Main method for testing purposes
    public static void main(String[] args) {
        checkFiles();  // Initialize files
        sendNotification("admin", "New order placed", "info"); // Sending a notification
    }
}
