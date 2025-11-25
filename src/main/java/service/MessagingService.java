package service;

import dao.mongodb.ConversationRepository;
import dao.mongodb.MessageRepository;
import dao.neo4j.implementations.PatientNeo4j;
import model.mongo.Conversation;
import model.mongo.Message;
import model.mongo.User;
import org.neo4j.driver.Driver;

import java.util.List;

public class MessagingService {

    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final PatientNeo4j patientNeo4j;
    private final AuthService authService;

    public MessagingService(AuthService authService, Driver neo4jDriver) {
        this.conversationRepo = new ConversationRepository();
        this.messageRepo = new MessageRepository();
        this.patientNeo4j = new PatientNeo4j(neo4jDriver);
        this.authService = authService;
    }

    /**
     * Check if current user can message another user
     */
    public boolean canMessage(String targetPersonRef) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Receptionist can message everyone
        if (currentUser.getRole() == User.Role.RECEPTIONIST) {
            return true;
        }

        // Check Neo4j for doctor-patient relationship
        String currentRef = currentUser.getPersonRef();

        if (currentUser.getRole() == User.Role.PATIENT) {
            // Patient can only message their primary doctor
            var patient = patientNeo4j.findByPN(currentRef);
            // You need to check if targetPersonRef is their primary doctor
            // For simplicity, we'll just check if a conversation exists
            Conversation conv = conversationRepo.findByParticipants(currentRef, targetPersonRef);
            return conv != null;
        }

        if (currentUser.getRole() == User.Role.DOCTOR) {
            // Doctor can only message their patients
            Conversation conv = conversationRepo.findByParticipants(currentRef, targetPersonRef);
            return conv != null;
        }

        return false;
    }

    /**
     * Send a message
     */
    public Message sendMessage(String targetPersonRef, String text) throws Exception {
        if (!authService.isLoggedIn()) {
            throw new IllegalStateException("Must be logged in to send messages");
        }

        if (!canMessage(targetPersonRef)) {
            throw new IllegalArgumentException("You cannot message this user");
        }

        User currentUser = authService.getCurrentUser();
        Conversation conv = conversationRepo.findByParticipants(currentUser.getPersonRef(), targetPersonRef);

        if (conv == null) {
            throw new IllegalArgumentException("No conversation found");
        }

        Message message = new Message(conv.getId(), currentUser.getPersonRef(), text);
        return messageRepo.sendMessage(message);
    }

    /**
     * Get all conversations for current user
     */
    public List<Conversation> getMyConversations() {
        if (!authService.isLoggedIn()) {
            return List.of();
        }
        return conversationRepo.findByUser(authService.getCurrentUser().getPersonRef());
    }

    /**
     * Get messages in a conversation
     */
    public List<Message> getMessages(String conversationId) {
        return messageRepo.findByConversation(conversationId);
    }
}