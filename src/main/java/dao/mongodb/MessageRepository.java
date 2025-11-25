package dao.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import config.MongoConfig;
import model.mongo.AttachmentMetadata;
import model.mongo.Message;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageRepository {

    private final MongoCollection<Document> collection;
    private final GridFSBucket gridFSBucket;

    public MessageRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("messages");
        this.gridFSBucket = MongoConfig.getGridFSBucket();
    }

    /**
     * Get all messages in a conversation (sorted by time)
     */
    public List<Message> findByConversation(String conversationId) {
        ObjectId convId = new ObjectId(conversationId);
        List<Message> messages = new ArrayList<>();

        for (Document doc : collection.find(Filters.eq("conversationId", convId))
                .sort(Sorts.ascending("createdAt"))) {
            messages.add(documentToMessage(doc));
        }

        return messages;
    }

    /**
     * Send a new message
     */
    public Message sendMessage(Message message) {
        Document doc = messageToDocument(message);
        collection.insertOne(doc);
        message.setId(doc.getObjectId("_id"));
        return message;
    }

    /**
     * Upload file to GridFS and return file metadata
     */
    public AttachmentMetadata uploadFile(String filePath, String filename, String contentType) throws Exception {
        try (InputStream streamToUploadFrom = new FileInputStream(filePath)) {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .metadata(new Document("contentType", contentType)
                            .append("uploadDate", new Date()));

            ObjectId fileId = gridFSBucket.uploadFromStream(filename, streamToUploadFrom, options);

            // Get file size
            long fileSize = new java.io.File(filePath).length();

            return new AttachmentMetadata(fileId, filename, contentType, fileSize);
        }
    }

    /**
     * Download file from GridFS
     */
    public void downloadFile(ObjectId fileId, String downloadPath) throws Exception {
        try (OutputStream streamToDownloadTo = new FileOutputStream(downloadPath)) {
            gridFSBucket.downloadToStream(fileId, streamToDownloadTo);
        }
    }

    /**
     * Delete file from GridFS
     */
    public void deleteFile(ObjectId fileId) {
        gridFSBucket.delete(fileId);
    }

    /**
     * Convert MongoDB Document to Message object
     */
    private Message documentToMessage(Document doc) {
        Message message = new Message();
        message.setId(doc.getObjectId("_id"));
        message.setConversationId(doc.getObjectId("conversationId"));
        message.setSenderId(doc.getString("senderId"));
        message.setText(doc.getString("text"));
        message.setCreatedAt(doc.getDate("createdAt"));

        // Handle attachments
        List<Document> attachmentDocs = doc.getList("attachments", Document.class, new ArrayList<>());
        List<AttachmentMetadata> attachments = new ArrayList<>();
        for (Document attDoc : attachmentDocs) {
            AttachmentMetadata att = new AttachmentMetadata();
            att.setFileId(attDoc.getObjectId("fileId"));
            att.setFilename(attDoc.getString("filename"));
            att.setContentType(attDoc.getString("contentType"));
            att.setFileSize(attDoc.getLong("fileSize"));
            attachments.add(att);
        }
        message.setAttachments(attachments);

        return message;
    }

    /**
     * Convert Message object to MongoDB Document
     */
    private Document messageToDocument(Message message) {
        Document doc = new Document();
        doc.append("conversationId", message.getConversationId());
        doc.append("senderId", message.getSenderId());
        doc.append("text", message.getText());
        doc.append("createdAt", message.getCreatedAt() != null ? message.getCreatedAt() : new Date());

        // Handle attachments
        List<Document> attachmentDocs = new ArrayList<>();
        for (AttachmentMetadata att : message.getAttachments()) {
            Document attDoc = new Document();
            attDoc.append("fileId", att.getFileId());
            attDoc.append("filename", att.getFilename());
            attDoc.append("contentType", att.getContentType());
            attDoc.append("fileSize", att.getFileSize());
            attachmentDocs.add(attDoc);
        }
        doc.append("attachments", attachmentDocs);

        return doc;
    }
}