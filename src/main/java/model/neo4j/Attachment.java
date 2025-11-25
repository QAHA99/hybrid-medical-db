package model.neo4j;

public class Attachment {

    // Instance Variables
    private final String attachmentID;
    private String title;
    private String type;

    // Constructor
    public Attachment(String attachmentID, String title, String type) {
        this.attachmentID = attachmentID;
        this.title = title;
        this.type = type;
    }

    // Getter
    public String getAttachmentID() {return attachmentID;}
    public String getTitle() {return title;}
    public String getType() {return type;}

    // Setters
    public void setTitle(String title) {this.title = title;}
    public void setType(String type) {this.type = type;}
}