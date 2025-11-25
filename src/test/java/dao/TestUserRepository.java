package dao;

import dao.mongodb.UserRepository;
import model.mongo.User;

public class TestUserRepository {
    public static void main(String[] args) {

        System.out.println("=== Testing MongoDB Connection ===\n");

        // Create repository
        UserRepository userRepo = new UserRepository();

        // Test 1: Find user by username
        System.out.println("Test 1: Find doctor by username");
        User doctor = userRepo.findByUsername("DR0001");
        if (doctor != null) {
            System.out.println("Found: " + doctor);
        } else {
            System.out.println("User not found");
        }

        System.out.println("\nTest 2: Authenticate user");
        User authenticated = userRepo.authenticate("DR0001", "password123");
        if (authenticated != null) {
            System.out.println("Authentication successful: " + authenticated);
        } else {
            System.out.println("Authentication failed");
        }

        System.out.println("\nTest 3: Find receptionist");
        User receptionist = userRepo.findByUsername("RC0001");
        if (receptionist != null) {
            System.out.println("Found: " + receptionist);
        } else {
            System.out.println("Receptionist not found");
        }

        System.out.println("\nTest 4: List all users");
        for (User u : userRepo.findAll()) {
            System.out.println("  - " + u);
        }
    }
}