import java.io.*;
import java.util.ArrayList;

public class CRUD_User {
    public boolean authenticateUser(String userType, String username, String password) {
        ArrayList<String> lines = returnFileLines(getFilename(userType));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("Username: " + username.trim())) {
                if ((i+1) < lines.size() &&
                        lines.get(i+1).trim().equals("Password: " + password.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean userExists(String userType, String username) {
        ArrayList<String> lines = returnFileLines(getFilename(userType));
        for (String line : lines) {
            if (line.startsWith("Username: " + username)) {
                return true;
            }
        }
        return false;
    }

    public boolean createUser(String userType, String username, String password, String displayName) {
        String filename = getFilename(userType);
        if (userExists(userType, username)) return false;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write("Username: " + username + "\n");
            writer.write("Password: " + password + "\n");
            writer.write("DisplayName: " + displayName + "\n");
            writer.write("***\n");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean updateUser(String userType, String oldUsername, String newUsername, String newPassword, String newDisplayName) {
        String filename = getFilename(userType);
        ArrayList<String> lines = new ArrayList<>();
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    if (line.substring(10).trim().equals(oldUsername)) {
                        lines.add("Username: " + newUsername);
                        lines.add("Password: " + newPassword);
                        lines.add("DisplayName: " + newDisplayName);
                        lines.add("***");
                        reader.readLine(); reader.readLine(); reader.readLine();
                        found = true;
                        continue;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }
        return writeFile(filename, lines) && found;
    }

    public boolean deleteUser(String userType, String username) {
        String filename = getFilename(userType);
        ArrayList<String> lines = new ArrayList<>();
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    if (line.substring(10).trim().equals(username)) {
                        reader.readLine(); reader.readLine(); reader.readLine();
                        found = true;
                        continue;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }
        return writeFile(filename, lines) && found;
    }

    public ArrayList<String[]> getAllUsers(String userType) {
        ArrayList<String[]> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(getFilename(userType)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    String[] user = new String[3];
                    user[0] = line.substring(10).trim();
                    user[1] = reader.readLine().substring(10).trim();
                    user[2] = reader.readLine().substring(12).trim();
                    users.add(user);
                    reader.readLine();
                }
            }
        } catch (IOException | StringIndexOutOfBoundsException e) {
            return users;
        }
        return users;
    }

    private boolean writeFile(String filename, ArrayList<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private ArrayList<String> returnFileLines(String filename) {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {}
        return lines;
    }

    private String getFilename(String userType) {
        return userType.replace(" ", "_") + "_Credentials.txt";
    }
}
