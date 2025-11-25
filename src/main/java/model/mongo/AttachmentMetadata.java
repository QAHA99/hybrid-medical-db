package model.mongo;

import org.bson.types.ObjectId;

public class AttachmentMetadata {
    private ObjectId fileId;
    private String filename;
    private String contentType;
    private long fileSize;

    // Constructors
    public AttachmentMetadata() {}

    public AttachmentMetadata(ObjectId fileId, String filename, String contentType, long fileSize) {
        this.fileId = fileId;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    // Getters
    public ObjectId getFileId() {return fileId;}
    public String getFilename() {return filename;}
    public String getContentType() {return contentType;}
    public long getFileSize() {return fileSize;}

    // Setters
    public void setFileId(ObjectId fileId) {this.fileId = fileId;}
    public void setFilename(String filename) {this.filename = filename;}
    public void setContentType(String contentType) {this.contentType = contentType;}
    public void setFileSize(long fileSize) {this.fileSize = fileSize;}

    // To String
    @Override
    public String toString() {
        return String.format("Attachment{filename='%s', type='%s', size=%d bytes}",
                filename, contentType, fileSize);
    }
}