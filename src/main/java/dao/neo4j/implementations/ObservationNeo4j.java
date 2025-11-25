package dao.neo4j.implementations;

import dao.neo4j.interfaces.ObservationRepository;
import model.neo4j.Observation;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ObservationNeo4j implements ObservationRepository {

    private final Driver driver;

    public ObservationNeo4j(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<ObservationWithContext> listByPatient(String patientPN) throws Exception {
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment)-[:FOR_PATIENT]->(p:Patient {patientPN: $patientPN})
            MATCH (ap)-[:HAS_OBSERVATION]->(ob:Observation)
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)
            RETURN ob, p, dr, ap.appointmentID AS appointmentID
            ORDER BY ob.observedAt ASC
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("patientPN", patientPN));
            List<ObservationWithContext> observations = new ArrayList<>();

            for (Record row : result.list()) {
                var ob = row.get("ob").asNode();
                var p = row.get("p").asNode();
                var dr = row.get("dr").asNode();

                observations.add(new ObservationWithContext(
                        ob.get("observationID").asString(),
                        ob.get("observedAt").asLocalDateTime(),
                        ob.get("text").asString(),
                        p.get("patientPN").asString(),
                        p.get("firstName").asString(),
                        p.get("lastName").asString(),
                        dr.get("doctorID").asString(),
                        dr.get("firstName").asString(),
                        dr.get("lastName").asString(),
                        row.get("appointmentID").asString()
                ));
            }

            if (observations.isEmpty()) {
                throw new NoSuchElementException("No observations found for patient: " + patientPN);
            }

            return observations;
        }
    }

    @Override
    public String getObservationDetails(String observationID) throws Exception {
        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment)-[:HAS_OBSERVATION]->(ob:Observation {observationID: $observationID})
            MATCH (ap)-[:FOR_PATIENT]->(p:Patient)
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN ob, p, dr, dp, cl, ap
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("observationID", observationID));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No observation found with ID: " + observationID);
            }

            Record row = result.next();
            var ob = row.get("ob").asNode();
            var p = row.get("p").asNode();
            var dr = row.get("dr").asNode();
            var dp = row.get("dp").asNode();
            var cl = row.get("cl").asNode();
            var ap = row.get("ap").asNode();

            return String.format(
                    "Observation ID: %s\n" +
                            "Observed At: %s\n" +
                            "Text: %s\n\n" +
                            "Patient: %s %s (PN: %s)\n" +
                            "Doctor: Dr. %s %s (%s)\n" +
                            "Department: %s\n" +
                            "Clinic: %s\n\n" +
                            "Appointment: %s (Date: %s)",
                    ob.get("observationID").asString(),
                    ob.get("observedAt").asLocalDateTime(),
                    ob.get("text").asString(),
                    p.get("firstName").asString(),
                    p.get("lastName").asString(),
                    p.get("patientPN").asString(),
                    dr.get("firstName").asString(),
                    dr.get("lastName").asString(),
                    dr.get("phoneNumber").asString("N/A"),
                    dp.get("name").asString("Unknown"),
                    cl.get("name").asString("Unknown"),
                    ap.get("appointmentID").asString(),
                    ap.get("starts").asLocalDateTime()
            );
        }
    }

    @Override
    public Observation createObservation(Observation observation, String appointmentID) throws Exception {
        if (observation == null) {
            throw new IllegalArgumentException("observation must not be null");
        }
        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }
        if (observation.getObservedAt() == null) {
            throw new IllegalArgumentException("observedAt must not be null");
        }
        if (observation.getText() == null || observation.getText().isBlank()) {
            throw new IllegalArgumentException("observation text must not be blank");
        }

        String checkAppointmentCypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            RETURN ap
            """;

        String createCypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            CREATE (ob:Observation {
                observationID: $observationID,
                observedAt: $observedAt,
                text: $text
            })
            CREATE (ap)-[:HAS_OBSERVATION]->(ob)
            RETURN ob
            """;

        try (Session session = driver.session()) {
            Result appointmentResult = session.run(checkAppointmentCypher, Map.of("appointmentID", appointmentID));

            if (!appointmentResult.hasNext()) {
                throw new NoSuchElementException("Appointment not found with ID: " + appointmentID);
            }

            Result createResult = session.run(createCypher, Map.of(
                    "observationID", observation.getObservationID(),
                    "observedAt", observation.getObservedAt(),
                    "text", observation.getText(),
                    "appointmentID", appointmentID
            ));

            var ob = createResult.next().get("ob").asNode();
            return new Observation(
                    ob.get("observationID").asString(),
                    ob.get("observedAt").asLocalDateTime(),
                    ob.get("text").asString()
            );
        }
    }

    @Override
    public Observation updateObservation(String observationID, LocalDateTime newObservedAt, String newText) throws Exception {
        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }
        if (newObservedAt == null && newText == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }
        if (newText != null && newText.isBlank()) {
            throw new IllegalArgumentException("observation text must not be blank");
        }

        String checkExistsCypher = """
            MATCH (ob:Observation {observationID: $observationID})
            RETURN ob
            """;

        String updateCypher = """
            MATCH (ob:Observation {observationID: $observationID})
            SET ob.observedAt = COALESCE($newObservedAt, ob.observedAt),
                ob.text = COALESCE($newText, ob.text)
            RETURN ob
            """;

        try (Session session = driver.session()) {
            Result existsResult = session.run(checkExistsCypher, Map.of("observationID", observationID));

            if (!existsResult.hasNext()) {
                throw new NoSuchElementException("Observation not found with ID: " + observationID);
            }

            Result updateResult = session.run(updateCypher, Map.of(
                    "observationID", observationID,
                    "newObservedAt", newObservedAt,
                    "newText", newText
            ));

            var ob = updateResult.next().get("ob").asNode();
            return new Observation(
                    ob.get("observationID").asString(),
                    ob.get("observedAt").asLocalDateTime(),
                    ob.get("text").asString()
            );
        }
    }

    @Override
    public void deleteObservation(String observationID) throws Exception {
        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }

        String cypher = """
            MATCH (ob:Observation {observationID: $observationID})
            DETACH DELETE ob
            RETURN COUNT(ob) AS deletedCount
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("observationID", observationID));

            if (result.hasNext()) {
                int deletedCount = result.next().get("deletedCount").asInt();
                if (deletedCount == 0) {
                    throw new NoSuchElementException("No observation found with ID: " + observationID);
                }
            } else {
                throw new NoSuchElementException("No observation found with ID: " + observationID);
            }
        }
    }
}