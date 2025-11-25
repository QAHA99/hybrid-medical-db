import cli.AuthCLI;
import cli.MainMenuCLI;
import config.Neo4jConfig;
import config.MongoConfig;
import service.AuthService;
import org.neo4j.driver.Driver;

public class Main {

    public static void main(String[] args) {
        Driver neo4jDriver = null;

        try {

            System.out.println("============================================");
            System.out.println("  CLINIC MANAGEMENT SYSTEM");
            System.out.println("============================================");
            System.out.println("\nConnecting to databases...");

            neo4jDriver = Neo4jConfig.getDriver();
            MongoConfig.getDatabase(); // Initialize MongoDB

            System.out.println("✓ Connected to Neo4j");
            System.out.println("✓ Connected to MongoDB");
            System.out.println("\nSystem ready!");

            // Initialize services
            AuthService authService = new AuthService();
            AuthCLI authCLI = new AuthCLI(authService);
            MainMenuCLI mainMenu = new MainMenuCLI(authService, neo4jDriver);

            // Main application loop
            while (true) {
                // Show login screen
                if (!authCLI.showLoginScreen()) {
                    // User chose to exit
                    break;
                }

                // Show appropriate menu based on role
                mainMenu.show();

                // After logout, loop continues to login screen
            }

            System.out.println("\n============================================");
            System.out.println("  Thank you for using Clinic Management System!");
            System.out.println("============================================\n");

        } catch (Exception e) {
            System.err.println("\n✗ Fatal error: " + e.getMessage());
            System.err.println("\nPlease check your database connections and try again.");
            e.printStackTrace();
        } finally {
            // Cleanup
            System.out.println("\nClosing database connections...");
            if (neo4jDriver != null) {
                Neo4jConfig.close();
                System.out.println("✓ Neo4j connection closed");
            }
            MongoConfig.close();
            System.out.println("✓ MongoDB connection closed");
            System.out.println("\nGoodbye!");
        }
    }
}