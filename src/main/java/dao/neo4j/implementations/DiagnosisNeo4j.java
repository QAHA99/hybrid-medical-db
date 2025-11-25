package dao.neo4j.implementations;

import dao.neo4j.interfaces.DiagnosisRepository;
import model.neo4j.Diagnosis;
import model.enums.Severity;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class DiagnosisNeo4j implements DiagnosisRepository {

    private final Driver driver;

    public DiagnosisNeo4j(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<DiagnosisWithContext> listByPatientAndSeverity(String patientPN, Severity severity) throws Exception {
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }

        String cypher = """
            MATCH (ap:Appointment)-[:FOR_PATIENT]->(p:Patient {patientPN: $patientPN})
            MATCH (ap)-[:HAS_OBSERVATION]->(ob:Observation)
            MATCH (ob)-[:HAS_DIAGNOSIS]->(dg:Diagnosis {severity: $severity})
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)
            RETURN dg, p, dr, ap, ob
            ORDER BY ap.starts ASC
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "patientPN", patientPN,
                    "severity", severity.getLabel()
            ));

            List<DiagnosisWithContext> diagnoses = new ArrayList<>();

            for (Record row : result.list()) {
                var dg = row.get("dg").asNode();
                var p = row.get("p").asNode();
                var dr = row.get("dr").asNode();
                var ap = row.get("ap").asNode();
                var ob = row.get("ob").asNode();

                diagnoses.add(new DiagnosisWithContext(
                        dg.get("diagnosisID").asString(),
                        dg.get("severity").asString(),
                        dg.get("details").asString(),
                        p.get("patientPN").asString(),
                        p.get("firstName").asString(),
                        p.get("lastName").asString(),
                        dr.get("doctorID").asString(),
                        dr.get("firstName").asString(),
                        dr.get("lastName").asString(),
                        ap.get("appointmentID").asString(),
                        ap.get("starts").asLocalDateTime(),
                        ob.get("observationID").asString(),
                        ob.get("text").asString()
                ));
            }

            if (diagnoses.isEmpty()) {
                throw new NoSuchElementException(
                        "No diagnoses found for patient " + patientPN + " with severity: " + severity.getLabel()
                );
            }

            return diagnoses;
        }
    }

    @Override
    public String getDiagnosisDetails(String diagnosisID) throws Exception {
        if (diagnosisID == null || diagnosisID.isBlank()) {
            throw new IllegalArgumentException("diagnosisID must not be blank");
        }

        String cypher = """
            MATCH (ob:Observation)-[:HAS_DIAGNOSIS]->(dg:Diagnosis {diagnosisID: $diagnosisID})
            MATCH (ap:Appointment)-[:HAS_OBSERVATION]->(ob)
            MATCH (ap)-[:FOR_PATIENT]->(p:Patient)
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN dg, ob, ap, p, dr, dp, cl
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("diagnosisID", diagnosisID));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No diagnosis found with ID: " + diagnosisID);
            }

            Record row = result.next();
            var dg = row.get("dg").asNode();
            var ob = row.get("ob").asNode();
            var ap = row.get("ap").asNode();
            var p = row.get("p").asNode();
            var dr = row.get("dr").asNode();
            var dp = row.get("dp").asNode();
            var cl = row.get("cl").asNode();

            return String.format(
                    "Diagnosis ID: %s\n" +
                            "Severity: %s\n" +
                            "Details: %s\n\n" +
                            "Patient: %s %s (PN: %s)\n" +
                            "Doctor: Dr. %s %s (%s)\n" +
                            "Department: %s\n" +
                            "Clinic: %s\n\n" +
                            "Appointment: %s (Date: %s)\n" +
                            "Observation: %s\n" +
                            "Observation Text: %s",
                    dg.get("diagnosisID").asString(),
                    dg.get("severity").asString(),
                    dg.get("details").asString(),
                    p.get("firstName").asString(),
                    p.get("lastName").asString(),
                    p.get("patientPN").asString(),
                    dr.get("firstName").asString(),
                    dr.get("lastName").asString(),
                    dr.get("phoneNumber").asString("N/A"),
                    dp.get("name").asString("Unknown"),
                    cl.get("name").asString("Unknown"),
                    ap.get("appointmentID").asString(),
                    ap.get("starts").asLocalDateTime(),
                    ob.get("observationID").asString(),
                    ob.get("text").asString()
            );
        }
    }

    @Override
    public Diagnosis createDiagnosis(Diagnosis diagnosis, String observationID) throws Exception {
        if (diagnosis == null) {
            throw new IllegalArgumentException("diagnosis must not be null");
        }
        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }
        if (diagnosis.getSeverity() == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        if (diagnosis.getDetails() == null || diagnosis.getDetails().isBlank()) {
            throw new IllegalArgumentException("diagnosis details must not be blank");
        }

        String checkObservationCypher = """
            MATCH (ob:Observation {observationID: $observationID})
            RETURN ob
            """;

        String createCypher = """
            MATCH (ob:Observation {observationID: $observationID})
            CREATE (dg:Diagnosis {
                diagnosisID: $diagnosisID,
                severity: $severity,
                details: $details
            })
            CREATE (ob)-[:HAS_DIAGNOSIS]->(dg)
            RETURN dg
            """;

        try (Session session = driver.session()) {
            Result observationResult = session.run(checkObservationCypher, Map.of("observationID", observationID));

            if (!observationResult.hasNext()) {
                throw new NoSuchElementException("Observation not found with ID: " + observationID);
            }

            Result createResult = session.run(createCypher, Map.of(
                    "diagnosisID", diagnosis.getDiagnosisID(),
                    "severity", diagnosis.getSeverity().getLabel(),
                    "details", diagnosis.getDetails(),
                    "observationID", observationID
            ));

            var dg = createResult.next().get("dg").asNode();
            return new Diagnosis(
                    dg.get("diagnosisID").asString(),
                    dg.get("severity").asString(),
                    dg.get("details").asString()
            );
        }
    }

    @Override
    public Diagnosis updateDiagnosis(String diagnosisID, Severity newSeverity, String newDetails) throws Exception {
        if (diagnosisID == null || diagnosisID.isBlank()) {
            throw new IllegalArgumentException("diagnosisID must not be blank");
        }
        if (newSeverity == null && newDetails == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }
        if (newDetails != null && newDetails.isBlank()) {
            throw new IllegalArgumentException("diagnosis details must not be blank");
        }

        String checkExistsCypher = """
            MATCH (dg:Diagnosis {diagnosisID: $diagnosisID})
            RETURN dg
            """;

        String updateCypher = """
            MATCH (dg:Diagnosis {diagnosisID: $diagnosisID})
            SET dg.severity = COALESCE($newSeverity, dg.severity),
                dg.details = COALESCE($newDetails, dg.details)
            RETURN dg
            """;

        try (Session session = driver.session()) {
            Result existsResult = session.run(checkExistsCypher, Map.of("diagnosisID", diagnosisID));

            if (!existsResult.hasNext()) {
                throw new NoSuchElementException("Diagnosis not found with ID: " + diagnosisID);
            }

            String severityLabel = newSeverity != null ? newSeverity.getLabel() : null;

            Result updateResult = session.run(updateCypher, Map.of(
                    "diagnosisID", diagnosisID,
                    "newSeverity", severityLabel,
                    "newDetails", newDetails
            ));

            var dg = updateResult.next().get("dg").asNode();
            return new Diagnosis(
                    dg.get("diagnosisID").asString(),
                    dg.get("severity").asString(),
                    dg.get("details").asString()
            );
        }
    }

    @Override
    public void deleteDiagnosis(String diagnosisID) throws Exception {
        if (diagnosisID == null || diagnosisID.isBlank()) {
            throw new IllegalArgumentException("diagnosisID must not be blank");
        }

        String cypher = """
            MATCH (dg:Diagnosis {diagnosisID: $diagnosisID})
            DETACH DELETE dg
            RETURN COUNT(dg) AS deletedCount
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("diagnosisID", diagnosisID));

            if (result.hasNext()) {
                int deletedCount = result.next().get("deletedCount").asInt();
                if (deletedCount == 0) {
                    throw new NoSuchElementException("No diagnosis found with ID: " + diagnosisID);
                }
            } else {
                throw new NoSuchElementException("No diagnosis found with ID: " + diagnosisID);
            }
        }
    }
}