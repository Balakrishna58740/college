import java.io.*;
import java.util.ArrayList;

public class CRUD_User {
    // Authenticate user
    public boolean authenticateUser(String userType, String username, String password) {
        String filename = getFilename(userType);
        ArrayList<String> lines = returnFileLines(filename);

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("Username: " + username) &&
                    lines.get(i + 1).equals("Password: " + password)) {
                return true;
            }
        }
        return false;
    }

    // Check if user exists
    public boolean userExists(String userType, String username) {
        String filename = getFilename(userType);
        ArrayList<String> lines = returnFileLines(filename);

        for (String line : lines) {
            if (line.startsWith("Username: " + username)) {
                return true;
            }
        }
        return false;
    }

    // Create user
    public boolean createUser(String userType, String username, String password, String displayName) {
        String filename = getFilename(userType);

        if (userExists(userType, username)) {
            System.err.println("User already exists!");
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write("Username: " + username + "\n");
            writer.write("Password: " + password + "\n");
            writer.write("DisplayName: " + displayName + "\n");
            writer.write("***\n");
            return true;
        } catch (IOException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    // Update user
    public boolean updateUser(String userType, String oldUsername, String newUsername, String newPassword, String newDisplayName) {
        String filename = getFilename(userType);
        ArrayList<String> lines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    String currentUser = line.substring(10).trim();
                    if (currentUser.equals(oldUsername)) {
                        lines.add("Username: " + newUsername);
                        lines.add("Password: " + newPassword);
                        lines.add("DisplayName: " + newDisplayName);
                        lines.add("***");
                        reader.readLine(); // Skip Password
                        reader.readLine(); // Skip DisplayName
                        reader.readLine(); // Skip ***
                        found = true;
                        continue;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return false;
        }

        return writeFile(filename, lines) && found;
    }

    // Delete user
    public boolean deleteUser(String userType, String username) {
        String filename = getFilename(userType);
        ArrayList<String> lines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    String currentUser = line.substring(10).trim();
                    if (currentUser.equals(username)) {
                        reader.readLine(); // Skip Password
                        reader.readLine(); // Skip DisplayName
                        reader.readLine(); // Skip ***
                        found = true;
                        continue;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return false;
        }

        return writeFile(filename, lines) && found;
    }

    // Get all users
    public ArrayList<String[]> getAllUsers(String userType) {
        String filename = getFilename(userType);
        ArrayList<String[]> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    String[] user = new String[3];
                    user[0] = line.substring(10).trim();
                    user[1] = reader.readLine().substring(10).trim();
                    user[2] = reader.readLine().substring(12).trim();
                    users.add(user);
                    reader.readLine(); // Skip ***
                }
            }
        } catch (IOException | StringIndexOutOfBoundsException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return users;
    }

    // Write file helper method
    private boolean writeFile(String filename, ArrayList<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            return false;
        }
    }

    // Read file helper method
    private ArrayList<String> returnFileLines(String filename) {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
        }
        return lines;
    }

    // Get filename based on user type
    private String getFilename(String userType) {
        return userType.replace(" ", "_") + "_Credentials.txt";
    }
}
