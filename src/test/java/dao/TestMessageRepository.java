package dao;

import dao.mongodb.ConversationRepository;
import dao.mongodb.MessageRepository;
import model.mongo.Conversation;
import model.mongo.Message;

import java.util.List;

public class TestMessageRepository {
    public static void main(String[] args) {

        System.out.println("=== Testing MessageRepository ===\n");

        ConversationRepository convRepo = new ConversationRepository();
        MessageRepository msgRepo = new MessageRepository();

        // Test 1: Get messages from existing conversation
        System.out.println("Test 1: Get messages from DR0001 and 19900101-1234 conversation");
        Conversation conv = convRepo.findByParticipants("DR0001", "19900101-1234");
        if (conv != null) {
            String convId = conv.getId().toString();
            System.out.println("Conversation ID: " + convId);

            List<Message> messages = msgRepo.findByConversation(convId);
            System.out.println("Found " + messages.size() + " messages:");
            for (Message msg : messages) {
                System.out.println("  - " + msg);
            }
        }

        // Test 2: Send a new message
        System.out.println("\nTest 2: Send a new message");
        if (conv != null) {
            Message newMsg = new Message(conv.getId(), "19900101-1234", "Tack för hjälpen doktor!");
            Message saved = msgRepo.sendMessage(newMsg);
            System.out.println("Message sent: " + saved);

            // Verify it was saved
            List<Message> updatedMessages = msgRepo.findByConversation(conv.getId().toString());
            System.out.println("Total messages now: " + updatedMessages.size());
        }
    }
}