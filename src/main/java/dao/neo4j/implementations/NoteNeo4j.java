package dao.neo4j.implementations;

import dao.neo4j.interfaces.NoteRepository;
import model.neo4j.Note;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.Map;
import java.util.NoSuchElementException;

public class NoteNeo4j implements NoteRepository {

    private final Driver driver;

    public NoteNeo4j(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void addNote(Note note, String doctorID, String aboutType, String aboutID) throws Exception {
        if (note == null || note.getNodeID() == null || note.getNodeID().isBlank()) {
            throw new IllegalArgumentException("Note and noteID must not be null or blank");
        }
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }
        if (aboutType == null || aboutType.isBlank()) {
            throw new IllegalArgumentException("aboutType must not be blank");
        }
        if (aboutID == null || aboutID.isBlank()) {
            throw new IllegalArgumentException("aboutID must not be blank");
        }

        String relationshipType;
        String targetLabel;
        String targetProperty;

        switch (aboutType.toUpperCase()) {
            case "APPOINTMENT":
                relationshipType = "ABOUT_APPOINTMENT";
                targetLabel = "Appointment";
                targetProperty = "appointmentID";
                break;
            case "OBSERVATION":
                relationshipType = "ABOUT_OBSERVATION";
                targetLabel = "Observation";
                targetProperty = "observationID";
                break;
            case "DIAGNOSIS":
                relationshipType = "ABOUT_DIAGNOSIS";
                targetLabel = "Diagnosis";
                targetProperty = "diagnosisID";
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid aboutType: " + aboutType + ". Must be APPOINTMENT, OBSERVATION, or DIAGNOSIS"
                );
        }

        String checkCypher = String.format("""
            MATCH (dr:Doctor {doctorID: $doctorID})
            MATCH (target:%s {%s: $aboutID})
            RETURN dr, target
            """, targetLabel, targetProperty);

        String createCypher = String.format("""
            MATCH (dr:Doctor {doctorID: $doctorID})
            MATCH (target:%s {%s: $aboutID})
            CREATE (nt:Note {noteID: $noteID, description: $description})
            CREATE (nt)-[:AUTHORED_BY]->(dr)
            CREATE (nt)-[:%s]->(target)
            RETURN nt
            """, targetLabel, targetProperty, relationshipType);

        try (Session session = driver.session()) {
            Result checkResult = session.run(checkCypher, Map.of(
                    "doctorID", doctorID,
                    "aboutID", aboutID
            ));

            if (!checkResult.hasNext()) {
                throw new NoSuchElementException(
                        "Doctor " + doctorID + " or " + aboutType + " " + aboutID + " not found"
                );
            }

            session.run(createCypher, Map.of(
                    "noteID", note.getNodeID(),
                    "description", note.getDescription(),
                    "doctorID", doctorID,
                    "aboutID", aboutID
            ));
        }
    }

    @Override
    public void updateNote(String noteID, String newDescription, String doctorID) throws Exception {
        if (noteID == null || noteID.isBlank()) {
            throw new IllegalArgumentException("noteID must not be blank");
        }
        if (newDescription == null || newDescription.isBlank()) {
            throw new IllegalArgumentException("newDescription must not be blank");
        }
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String checkCypher = """
            MATCH (nt:Note {noteID: $noteID})-[:AUTHORED_BY]->(dr:Doctor)
            RETURN dr.doctorID AS authorID
            """;

        String updateCypher = """
            MATCH (nt:Note {noteID: $noteID})
            SET nt.description = $newDescription
            RETURN nt
            """;

        try (Session session = driver.session()) {
            Result checkResult = session.run(checkCypher, Map.of("noteID", noteID));

            if (!checkResult.hasNext()) {
                throw new NoSuchElementException("Note not found with ID: " + noteID);
            }

            String authorID = checkResult.next().get("authorID").asString();

            if (!authorID.equals(doctorID)) {
                throw new IllegalArgumentException(
                        "Doctor " + doctorID + " is not authorized to update note " + noteID +
                                " (authored by " + authorID + ")"
                );
            }

            session.run(updateCypher, Map.of(
                    "noteID", noteID,
                    "newDescription", newDescription
            ));
        }
    }

    @Override
    public void deleteNote(String noteID, String doctorID) throws Exception {
        if (noteID == null || noteID.isBlank()) {
            throw new IllegalArgumentException("noteID must not be blank");
        }
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String checkCypher = """
            MATCH (nt:Note {noteID: $noteID})-[:AUTHORED_BY]->(dr:Doctor)
            RETURN dr.doctorID AS authorID
            """;

        String deleteCypher = """
            MATCH (nt:Note {noteID: $noteID})
            OPTIONAL MATCH (at:Attachment)-[:ATTACHED_TO]->(nt)
            DETACH DELETE at, nt
            """;

        try (Session session = driver.session()) {
            Result checkResult = session.run(checkCypher, Map.of("noteID", noteID));

            if (!checkResult.hasNext()) {
                throw new NoSuchElementException("Note not found with ID: " + noteID);
            }

            String authorID = checkResult.next().get("authorID").asString();

            if (!authorID.equals(doctorID)) {
                throw new IllegalArgumentException(
                        "Doctor " + doctorID + " is not authorized to delete note " + noteID +
                                " (authored by " + authorID + ")"
                );
            }

            session.run(deleteCypher, Map.of("noteID", noteID));
        }
    }

    @Override
    public String getNoteDetails(String noteID) throws Exception {
        if (noteID == null || noteID.isBlank()) {
            throw new IllegalArgumentException("noteID must not be blank");
        }

        String cypher = """
            MATCH (nt:Note {noteID: $noteID})-[:AUTHORED_BY]->(dr:Doctor)
            OPTIONAL MATCH (nt)-[:ABOUT_APPOINTMENT]->(ap:Appointment)-[:FOR_PATIENT]->(p:Patient)
            OPTIONAL MATCH (nt)-[:ABOUT_OBSERVATION]->(ob:Observation)
            OPTIONAL MATCH (nt)-[:ABOUT_DIAGNOSIS]->(dg:Diagnosis)
            RETURN nt, dr, ap, ob, dg, p
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("noteID", noteID));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No note found with ID: " + noteID);
            }

            Record row = result.next();
            var nt = row.get("nt").asNode();
            var dr = row.get("dr").asNode();

            String output = String.format(
                    "Note ID: %s\nDescription: %s\nAuthor: Dr. %s %s (ID: %s)\n",
                    nt.get("noteID").asString(),
                    nt.get("description").asString(),
                    dr.get("firstName").asString(),
                    dr.get("lastName").asString(),
                    dr.get("doctorID").asString()
            );

            if (!row.get("ap").isNull()) {
                var ap = row.get("ap").asNode();
                var p = row.get("p").asNode();
                output += String.format("About: Appointment %s on %s for %s %s",
                        ap.get("appointmentID").asString(),
                        ap.get("starts").asLocalDateTime(),
                        p.get("firstName").asString(),
                        p.get("lastName").asString());
            } else if (!row.get("ob").isNull()) {
                var ob = row.get("ob").asNode();
                output += String.format("About: Observation %s at %s",
                        ob.get("observationID").asString(),
                        ob.get("observedAt").asLocalDateTime());
            } else if (!row.get("dg").isNull()) {
                var dg = row.get("dg").asNode();
                output += String.format("About: Diagnosis %s (Severity: %s)",
                        dg.get("diagnosisID").asString(),
                        dg.get("severity").asString());
            }

            return output;
        }
    }
}