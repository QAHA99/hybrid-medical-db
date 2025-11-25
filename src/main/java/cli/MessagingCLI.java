package cli;

import dao.mongodb.ConversationRepository;
import dao.mongodb.MessageRepository;
import dao.mongodb.UserRepository;
import model.mongo.AttachmentMetadata;
import model.mongo.Conversation;
import model.mongo.Message;
import model.mongo.User;
import service.AuthService;
import service.MessagingService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class MessagingCLI extends CLIMenu {
    private final AuthService authService;
    private final MessagingService messagingService;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private static final String DOWNLOAD_PATH = "/Users/qusaialhajali/Downloads/";

    public MessagingCLI(AuthService authService, MessagingService messagingService) {
        this.authService = authService;
        this.messagingService = messagingService;
        this.conversationRepo = new ConversationRepository();
        this.messageRepo = new MessageRepository();
        this.userRepo = new UserRepository();
    }

    /**
     * Show messaging menu
     */
    public void showMessagingMenu() {
        while (true) {
            clearScreen();
            printHeader("MESSAGING");

            printMenuOptions(
                    "View Conversations",
                    "Send Message"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    viewConversations();
                    break;
                case 2:
                    sendNewMessage();
                    break;
                case 0:
                    return;
                default:
                    printError("Invalid option.");
                    pauseForUser();
            }
        }
    }

    /**
     * View all conversations for current user
     */
    private void viewConversations() {
        try {
            List<Conversation> conversations = messagingService.getMyConversations();

            if (conversations.isEmpty()) {
                printInfo("You have no conversations.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("MY CONVERSATIONS");

            // Display conversations with participant names
            String[] conversationOptions = new String[conversations.size()];
            for (int i = 0; i < conversations.size(); i++) {
                Conversation conv = conversations.get(i);
                String otherPersonRef = getOtherParticipant(conv);
                User otherUser = userRepo.findByPersonRef(otherPersonRef);
                String otherName = getUserDisplayName(otherUser);
                conversationOptions[i] = otherName + " (Last: " + formatDate(conv.getLastMessageAt()) + ")";
            }

            int choice = selectFromList("Select Conversation", conversationOptions);

            if (choice == 0) {
                return;
            }

            // Open selected conversation
            Conversation selectedConv = conversations.get(choice - 1);
            openConversation(selectedConv);

        } catch (Exception e) {
            printError("Failed to load conversations: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Open a conversation and show messages
     */
    private void openConversation(Conversation conversation) {
        while (true) {
            try {
                clearScreen();

                // Get other participant name
                String otherPersonRef = getOtherParticipant(conversation);
                User otherUser = userRepo.findByPersonRef(otherPersonRef);
                String otherName = getUserDisplayName(otherUser);

                printHeader("CONVERSATION WITH " + otherName);

                // Load messages
                List<Message> messages = messageRepo.findByConversation(conversation.getId().toString());

                if (messages.isEmpty()) {
                    printInfo("No messages yet.");
                } else {
                    printDivider();
                    displayMessages(messages);
                }

                printDivider();
                printMenuOptions(
                        "Send Message",
                        "Send Message with Attachment",
                        "Download Attachment",
                        "Refresh"
                );

                int choice = getIntInput("");

                switch (choice) {
                    case 1:
                        sendMessageInConversation(conversation, otherPersonRef);
                        break;
                    case 2:
                        sendMessageWithAttachment(conversation, otherPersonRef);
                        break;
                    case 3:
                        downloadAttachment(messages);
                        break;
                    case 4:
                        // Refresh - loop continues
                        break;
                    case 0:
                        return;
                    default:
                        printError("Invalid option.");
                        pauseForUser();
                }

            } catch (Exception e) {
                printError("Error: " + e.getMessage());
                pauseForUser();
                return;
            }
        }
    }

    /**
     * Display all messages in a conversation
     */
    private void displayMessages(List<Message> messages) {
        User currentUser = authService.getCurrentUser();
        SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, HH:mm");

        for (Message msg : messages) {
            boolean isMine = msg.getSenderId().equals(currentUser.getPersonRef());
            String sender = isMine ? "You" : getSenderName(msg.getSenderId());
            String time = timeFormat.format(msg.getCreatedAt());

            System.out.println("\n[" + time + "] " + sender + ":");
            System.out.println("  " + msg.getText());

            // Show attachments if any
            if (!msg.getAttachments().isEmpty()) {
                System.out.println("  📎 Attachments:");
                for (AttachmentMetadata att : msg.getAttachments()) {
                    System.out.println("    - " + att.getFilename() + " (" + formatFileSize(att.getFileSize()) + ")");
                }
            }
        }
    }

    /**
     * Send a text message in current conversation
     */
    private void sendMessageInConversation(Conversation conversation, String recipientRef) {
        String text = getStringInput("\nEnter message: ");

        if (text.isEmpty()) {
            printError("Message cannot be empty.");
            pauseForUser();
            return;
        }

        try {
            Message message = new Message(conversation.getId(), authService.getCurrentUser().getPersonRef(), text);
            messageRepo.sendMessage(message);
            printSuccess("Message sent!");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Send a message with file attachment
     */
    private void sendMessageWithAttachment(Conversation conversation, String recipientRef) {
        String text = getStringInput("\nEnter message: ");

        if (text.isEmpty()) {
            printError("Message cannot be empty.");
            pauseForUser();
            return;
        }

        String filePath = getStringInput("Enter full path to file: ");

        File file = new File(filePath);
        if (!file.exists()) {
            printError("File not found: " + filePath);
            pauseForUser();
            return;
        }

        try {
            // Upload file to GridFS
            String filename = file.getName();
            String contentType = guessContentType(filename);
            AttachmentMetadata attachment = messageRepo.uploadFile(filePath, filename, contentType);

            // Create message with attachment
            Message message = new Message(conversation.getId(), authService.getCurrentUser().getPersonRef(), text);
            message.getAttachments().add(attachment);
            messageRepo.sendMessage(message);

            printSuccess("Message with attachment sent!");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Download an attachment from messages
     */
    private void downloadAttachment(List<Message> messages) {
        // Collect all attachments
        int attachmentCount = 0;
        for (Message msg : messages) {
            attachmentCount += msg.getAttachments().size();
        }

        if (attachmentCount == 0) {
            printInfo("No attachments in this conversation.");
            pauseForUser();
            return;
        }

        // Build attachment list
        clearScreen();
        printHeader("SELECT ATTACHMENT TO DOWNLOAD");

        int index = 1;
        for (Message msg : messages) {
            for (AttachmentMetadata att : msg.getAttachments()) {
                System.out.println(index + ". " + att.getFilename() + " (" + formatFileSize(att.getFileSize()) + ")");
                index++;
            }
        }
        System.out.println("0. Cancel");

        int choice = getIntInput("\nSelect attachment: ");

        if (choice == 0 || choice > attachmentCount) {
            return;
        }

        // Find selected attachment
        index = 1;
        AttachmentMetadata selectedAttachment = null;
        for (Message msg : messages) {
            for (AttachmentMetadata att : msg.getAttachments()) {
                if (index == choice) {
                    selectedAttachment = att;
                    break;
                }
                index++;
            }
            if (selectedAttachment != null) break;
        }

        if (selectedAttachment == null) {
            printError("Attachment not found.");
            pauseForUser();
            return;
        }

        // Download to specified path
        try {
            String downloadFilePath = DOWNLOAD_PATH + selectedAttachment.getFilename();
            messageRepo.downloadFile(selectedAttachment.getFileId(), downloadFilePath);
            printSuccess("Downloaded to: " + downloadFilePath);
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to download: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Send a new message to someone
     */
    private void sendNewMessage() {
        String recipientRef = getStringInput("\nEnter recipient ID (doctorID or patientPN): ");

        if (recipientRef.isEmpty()) {
            printError("Recipient ID cannot be empty.");
            pauseForUser();
            return;
        }

        String text = getStringInput("Enter message: ");

        if (text.isEmpty()) {
            printError("Message cannot be empty.");
            pauseForUser();
            return;
        }

        try {
            messagingService.sendMessage(recipientRef, text);
            printSuccess("Message sent!");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Get the other participant in a conversation
     */
    private String getOtherParticipant(Conversation conversation) {
        String myRef = authService.getCurrentUser().getPersonRef();
        for (String participant : conversation.getParticipants()) {
            if (!participant.equals(myRef)) {
                return participant;
            }
        }
        return myRef; // Should never happen
    }

    /**
     * Get sender display name
     */
    private String getSenderName(String senderId) {
        try {
            User sender = userRepo.findByPersonRef(senderId);
            return getUserDisplayName(sender);
        } catch (Exception e) {
            return senderId;
        }
    }

    /**
     * Get user display name
     */
    private String getUserDisplayName(User user) {
        if (user.getRole() == User.Role.RECEPTIONIST) {
            return user.getName();
        } else {
            return user.getFirstName() + " " + user.getLastName();
        }
    }

    /**
     * Guess content type from filename
     */
    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        return "application/octet-stream";
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}