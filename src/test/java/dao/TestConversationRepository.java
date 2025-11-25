package dao;

import dao.mongodb.ConversationRepository;
import model.mongo.Conversation;

import java.util.List;

public class TestConversationRepository {
    public static void main(String[] args) {

        System.out.println("=== Testing ConversationRepository ===\n");

        ConversationRepository convRepo = new ConversationRepository();

        // Test 1: Find conversations for a doctor
        System.out.println("Test 1: Find conversations for DR0001");
        List<Conversation> doctorConvs = convRepo.findByUser("DR0001");
        System.out.println("Found " + doctorConvs.size() + " conversations:");
        for (Conversation conv : doctorConvs) {
            System.out.println("  - " + conv);
        }

        // Test 2: Find conversations for a patient
        System.out.println("\nTest 2: Find conversations for patient 19900101-1234");
        List<Conversation> patientConvs = convRepo.findByUser("19900101-1234");
        System.out.println("Found " + patientConvs.size() + " conversations:");
        for (Conversation conv : patientConvs) {
            System.out.println("  - " + conv);
        }

        // Test 3: Find conversations for receptionist
        System.out.println("\nTest 3: Find conversations for RC0001");
        List<Conversation> receptionistConvs = convRepo.findByUser("RC0001");
        System.out.println("Found " + receptionistConvs.size() + " conversations:");
        for (Conversation conv : receptionistConvs) {
            System.out.println("  - " + conv);
        }

        // Test 4: Find specific conversation between two users
        System.out.println("\nTest 4: Find conversation between DR0001 and 19900101-1234");
        Conversation specificConv = convRepo.findByParticipants("DR0001", "19900101-1234");
        if (specificConv != null) {
            System.out.println("Found: " + specificConv);
        } else {
            System.out.println("Conversation not found");
        }
    }
}