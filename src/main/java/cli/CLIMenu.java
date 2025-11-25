package cli;

import java.util.Scanner;

public class CLIMenu {
    protected static final Scanner scanner = new Scanner(System.in);

    /**
     * Print a header with border
     */
    protected void printHeader(String title) {
        int width = 60;
        System.out.println("\n" + "=".repeat(width));
        System.out.println(centerText(title, width));
        System.out.println("=".repeat(width));
    }

    /**
     * Print a section divider
     */
    protected void printDivider() {
        System.out.println("-".repeat(60));
    }

    /**
     * Center text within a given width
     */
    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    /**
     * Print menu options
     */
    protected void printMenuOptions(String... options) {
        System.out.println();
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i]);
        }
        System.out.println("0. Back");
        System.out.print("\nSelect option: ");
    }

    /**
     * Get integer input with validation
     */
    protected int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Get string input with validation
     */
    protected String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Get yes/no confirmation
     */
    protected boolean getConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Please enter 'y' or 'n'.");
        }
    }

    /**
     * Print success message
     */
    protected void printSuccess(String message) {
        System.out.println("\n✓ " + message);
    }

    /**
     * Print error message
     */
    protected void printError(String message) {
        System.out.println("\n✗ Error: " + message);
    }

    /**
     * Print info message
     */
    protected void printInfo(String message) {
        System.out.println("\nℹ " + message);
    }

    /**
     * Pause and wait for user to press Enter
     */
    protected void pauseForUser() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clear screen effect (print newlines)
     */
    protected void clearScreen() {
        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
    }

    /**
     * Display numbered list and get selection
     */
    protected int selectFromList(String title, String[] items) {
        printHeader(title);
        for (int i = 0; i < items.length; i++) {
            System.out.println((i + 1) + ". " + items[i]);
        }
        System.out.println("0. Cancel");

        while (true) {
            int choice = getIntInput("\nSelect: ");
            if (choice >= 0 && choice <= items.length) {
                return choice;
            }
            System.out.println("Invalid selection. Try again.");
        }
    }

    /**
     * Format date for display
     */
    protected String formatDate(java.util.Date date) {
        if (date == null) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    /**
     * Format LocalDateTime for display
     */
    protected String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }
}