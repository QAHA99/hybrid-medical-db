package dao;

import service.AuthService;

public class TestAuthService {
    public static void main(String[] args) {

        System.out.println("=== Testing AuthService ===\n");

        AuthService auth = new AuthService();

        // Test 1: Login as doctor
        System.out.println("Test 1: Login as doctor");
        boolean success = auth.login("DR0001", "password123");
        System.out.println("Login successful: " + success);
        System.out.println("Current user: " + auth.getCurrentUser());
        System.out.println("Is doctor: " + auth.isDoctor());

        // Test 2: Logout
        System.out.println("\nTest 2: Logout");
        auth.logout();
        System.out.println("Is logged in: " + auth.isLoggedIn());

        // Test 3: Login as patient
        System.out.println("\nTest 3: Login as patient");
        auth.login("19900101-1234", "password123");
        System.out.println("Current user: " + auth.getCurrentUser());
        System.out.println("Is patient: " + auth.isPatient());

        // Test 4: Login as receptionist
        System.out.println("\nTest 4: Logout and login as receptionist");
        auth.logout();
        auth.login("RC0001", "reception123");
        System.out.println("Current user: " + auth.getCurrentUser());
        System.out.println("Is receptionist: " + auth.isReceptionist());

        // Test 5: Wrong password
        System.out.println("\nTest 5: Wrong password");
        auth.logout();
        boolean failed = auth.login("DR0001", "wrongpassword");
        System.out.println("Login successful: " + failed);
    }
}