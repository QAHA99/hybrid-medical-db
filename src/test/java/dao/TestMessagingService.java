package dao;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import service.AuthService;
import service.MessagingService;
import model.mongo.Conversation;
import model.mongo.Message;

import java.util.List;

public class TestMessagingService {
    public static void main(String[] args) throws Exception {

        // Connect to Neo4j (replace with your credentials)
        Driver neo4jDriver = GraphDatabase.driver(
                "neo4j+s://423bcb80.databases.neo4j.io",
                AuthTokens.basic("neo4j", "dPLxUBqct9iyrv-35nq6x8SnQbesDy9prQnCUxRM0UE")
        );

        System.out.println("=== Testing MessagingService ===\n");

        AuthService auth = new AuthService();
        MessagingService messaging = new MessagingService(auth, neo4jDriver);

        // Test 1: Patient tries to message their primary doctor
        System.out.println("Test 1: Patient Karin messages her doctor DR0001");
        auth.login("19900101-1234", "password123");
        boolean canMessage = messaging.canMessage("DR0001");
        System.out.println("Can message: " + canMessage);

        if (canMessage) {
            Message sent = messaging.sendMessage("DR0001", "Test message from patient");
            System.out.println("Message sent: " + sent);
        }

        // Test 2: Patient tries to message wrong doctor
        System.out.println("\nTest 2: Patient tries to message DR0002 (not their doctor)");
        boolean canMessageWrong = messaging.canMessage("DR0002");
        System.out.println("Can message: " + canMessageWrong);

        // Test 3: Receptionist can message anyone
        System.out.println("\nTest 3: Receptionist messages a doctor");
        auth.logout();
        auth.login("RC0001", "reception123");
        boolean receptionistCan = messaging.canMessage("DR0001");
        System.out.println("Can message: " + receptionistCan);

        // Test 4: Get my conversations
        System.out.println("\nTest 4: Get receptionist's conversations");
        List<Conversation> convs = messaging.getMyConversations();
        System.out.println("Found " + convs.size() + " conversations");

        neo4jDriver.close();
    }
}