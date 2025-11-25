package model.neo4j;

public class Note {

    // Instance Variables
    private final String nodeID;
    private String description;

    // Constructor
    public Note(String nodeID, String description) {
        this.nodeID = nodeID;
        this.description = description;
    }

    // Getters
    public String getNodeID() {return nodeID;}
    public String getDescription() {return description;}

    // Setters
    public void setDescription(String description) {this.description = description;}
}
