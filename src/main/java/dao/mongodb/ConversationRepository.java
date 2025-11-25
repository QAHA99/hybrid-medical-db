package dao.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import config.MongoConfig;
import model.mongo.Conversation;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConversationRepository {

    private final MongoCollection<Document> collection;

    public ConversationRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("conversations");
    }

    /**
     * Find conversation by ID
     */
    public Conversation findById(String conversationId) {
        ObjectId id = new ObjectId(conversationId);
        Document doc = collection.find(Filters.eq("_id", id)).first();
        if (doc == null) {
            return null;
        }
        return documentToConversation(doc);
    }

    /**
     * Find conversation between two participants
     */

    public Conversation findByParticipants(String personRef1, String personRef2) {
        // Try both orderings since we don't know the exact order in MongoDB
        Document doc = collection.find(
                Filters.or(
                        Filters.eq("participants", List.of(personRef1, personRef2)),
                        Filters.eq("participants", List.of(personRef2, personRef1))
                )
        ).first();

        if (doc == null) {
            return null;
        }
        return documentToConversation(doc);
    }

    /**
     * Get all conversations for a specific user (by personRef)
     */
    public List<Conversation> findByUser(String personRef) {
        List<Conversation> conversations = new ArrayList<>();

        // Find conversations where personRef is in the participants array
        for (Document doc : collection.find(Filters.in("participants", personRef))) {
            conversations.add(documentToConversation(doc));
        }

        return conversations;
    }

    /**
     * Get all conversations
     */
    public List<Conversation> findAll() {
        List<Conversation> conversations = new ArrayList<>();
        for (Document doc : collection.find()) {
            conversations.add(documentToConversation(doc));
        }
        return conversations;
    }

    /**
     * Convert MongoDB Document to Conversation object
     */
    private Conversation documentToConversation(Document doc) {
        Conversation conversation = new Conversation();
        conversation.setId(doc.getObjectId("_id"));
        conversation.setParticipants(doc.getList("participants", String.class));
        conversation.setLastMessageAt(doc.getDate("lastMessageAt"));
        return conversation;
    }

    /**
     * Convert Conversation object to MongoDB Document
     */
    private Document conversationToDocument(Conversation conversation) {
        Document doc = new Document();
        doc.append("participants", conversation.getParticipants());
        doc.append("lastMessageAt", conversation.getLastMessageAt() != null ? conversation.getLastMessageAt() : new Date());
        return doc;
    }
}