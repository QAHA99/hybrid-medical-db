package dao.neo4j.interfaces;

import model.neo4j.Note;

public interface NoteRepository {

    void addNote(Note note, String doctorID, String aboutType, String aboutID) throws Exception;

    void updateNote(String noteID, String newDescription, String doctorID) throws Exception;

    void deleteNote(String noteID, String doctorID) throws Exception;

    String getNoteDetails(String noteID) throws Exception;
}