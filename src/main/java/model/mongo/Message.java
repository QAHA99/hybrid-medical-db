package model.mongo;

import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Message {
    private ObjectId id;
    private ObjectId conversationId;
    private String senderId;  // doctorID, patientPN, or RC0001
    private String text;
    private Date createdAt;
    private List<AttachmentMetadata> attachments;

    // Constructors
    public Message() {
        this.createdAt = new Date();
        this.attachments = new ArrayList<>();
    }

    public Message(ObjectId conversationId, String senderId, String text) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = new Date();
        this.attachments = new ArrayList<>();
    }

    // Getters
    public ObjectId getId() {return id;}
    public ObjectId getConversationId() {return conversationId;}
    public String getSenderId() {return senderId;}
    public String getText() {return text;}
    public Date getCreatedAt() {return createdAt;}
    public List<AttachmentMetadata> getAttachments() {return attachments;}

    // Setters
    public void setId(ObjectId id) {this.id = id;}
    public void setConversationId(ObjectId conversationId) {this.conversationId = conversationId;}
    public void setSenderId(String senderId) {this.senderId = senderId;}
    public void setText(String text) {this.text = text;}
    public void setCreatedAt(Date createdAt) {this.createdAt = createdAt;}
    public void setAttachments(List<AttachmentMetadata> attachments) {this.attachments = attachments;}

    // To String
    @Override
    public String toString() {
        String preview;
        if (text.length() > 30) {
            preview = text.substring(0, 30) + "...";
        } else {
            preview = text;
        }
        return String.format("Message{sender='%s', text='%s', attachments=%d, time=%s}",
                senderId, preview, attachments.size(), createdAt);
    }
}