package model.mongo;

import org.bson.types.ObjectId;
import java.util.Date;
import java.util.List;

public class Conversation {
    private ObjectId id;
    private List<String> participants;
    private Date lastMessageAt;

    // Constructors
    public Conversation() {this.lastMessageAt = new Date();}

    public Conversation(List<String> participants) {
        this.participants = participants;
        this.lastMessageAt = new Date();
    }

    // Getters 
    public ObjectId getId() {return id;}
    public List<String> getParticipants() {return participants;}
    public Date getLastMessageAt() {return lastMessageAt;}

    // Setters
    public void setId(ObjectId id) {this.id = id;}
    public void setParticipants(List<String> participants) {this.participants = participants;}
    public void setLastMessageAt(Date lastMessageAt) {this.lastMessageAt = lastMessageAt;}

    // To String
    @Override
    public String toString() {
        return String.format("Conversation{id=%s, participants=%s, lastMessage=%s}",
                id, participants, lastMessageAt);
    }
}