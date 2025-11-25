package cli;

import model.mongo.User;
import service.AuthService;

public class AuthCLI extends CLIMenu {
    private final AuthService authService;

    public AuthCLI(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Display login screen and authenticate user
     * @return true if login successful, false to exit
     */
    public boolean showLoginScreen() {
        clearScreen();
        printHeader("CLINIC MANAGEMENT SYSTEM - LOGIN");

        System.out.println("\nWelcome! Please log in to continue.");
        printDivider();

        while (true) {
            String username = getStringInput("\nUsername (or 'exit' to quit): ");

            if (username.equalsIgnoreCase("exit")) {
                return false;
            }

            if (username.isEmpty()) {
                printError("Username cannot be empty.");
                continue;
            }

            String password = getStringInput("Password: ");

            if (password.isEmpty()) {
                printError("Password cannot be empty.");
                continue;
            }

            // Attempt login
            if (authService.login(username, password)) {
                User currentUser = authService.getCurrentUser();
                clearScreen();
                printSuccess("Login successful!");
                printInfo("Welcome, " + getUserDisplayName(currentUser) + "!");
                pauseForUser();
                return true;
            } else {
                printError("Invalid username or password. Please try again.");
            }
        }
    }

    /**
     * Get display name for user
     */
    private String getUserDisplayName(User user) {
        if (user.getRole() == User.Role.RECEPTIONIST) {
            return user.getName();
        } else {
            return user.getFirstName() + " " + user.getLastName();
        }
    }

    /**
     * Display logout confirmation
     */
    public boolean confirmLogout() {
        return getConfirmation("\nAre you sure you want to logout?");
    }
}