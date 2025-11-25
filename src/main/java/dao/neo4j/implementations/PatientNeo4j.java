package dao.neo4j.implementations;

import dao.neo4j.interfaces.PatientRepository;
import model.neo4j.Patient;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;

public class PatientNeo4j implements PatientRepository {

    private final Driver driver;

    public PatientNeo4j(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Patient createPatient(String patientPN, String firstName, String lastName,
                                 String sex, String phoneNumber, String doctorID) throws Exception {
        String cypher = """
            MATCH (dr:Doctor {doctorID: $doctorID})-[:WORKS_IN]->(:Department)-[:IN_CLINIC]->(cl:Clinic)
            CREATE (p:Patient {
                patientPN: $patientPN, 
                firstName: $firstName, 
                lastName: $lastName, 
                sex: $sex, 
                phoneNumber: $phoneNumber
            })
            CREATE (p)-[:HAS_PRIMARY_DOCTOR]->(dr)
            CREATE (p)-[:REGISTERED_AT]->(cl)
            RETURN p.patientPN AS patientPN, p.firstName AS firstName, p.lastName AS lastName, p.sex AS sex
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "patientPN", patientPN,
                    "firstName", firstName,
                    "lastName", lastName,
                    "sex", sex,
                    "phoneNumber", phoneNumber,
                    "doctorID", doctorID
            ));

            if (!result.hasNext()) throw new IllegalArgumentException("Doctor not found: " + doctorID);

            Record row = result.next();
            return new Patient(
                    row.get("patientPN").asString(),
                    row.get("firstName").asString(),
                    row.get("lastName").asString(),
                    row.get("sex").asString()
            );
        }
    }

    @Override
    public Patient updatePatient(String patientPN, String firstName, String lastName,
                                 String sex, String phoneNumber, String doctorID) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("patientPN", patientPN);

        StringBuilder cypher = new StringBuilder("MATCH (p:Patient {patientPN: $patientPN})\n");

        if (firstName != null && !firstName.isBlank()) {
            cypher.append("SET p.firstName = $firstName\n");
            params.put("firstName", firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            cypher.append("SET p.lastName = $lastName\n");
            params.put("lastName", lastName);
        }
        if (sex != null && !sex.isBlank()) {
            cypher.append("SET p.sex = $sex\n");
            params.put("sex", sex);
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            cypher.append("SET p.phoneNumber = $phoneNumber\n");
            params.put("phoneNumber", phoneNumber);
        }

        if (doctorID != null && !doctorID.isBlank()) {
            cypher.append("""
                WITH p
                OPTIONAL MATCH (p)-[r:HAS_PRIMARY_DOCTOR|REGISTERED_AT]->()
                DELETE r
                WITH p
                MATCH (dr:Doctor {doctorID: $doctorID})-[:WORKS_IN]->(:Department)-[:IN_CLINIC]->(cl:Clinic)
                CREATE (p)-[:HAS_PRIMARY_DOCTOR]->(dr)
                CREATE (p)-[:REGISTERED_AT]->(cl)
                """);
            params.put("doctorID", doctorID);
        }

        cypher.append("RETURN p.patientPN AS patientPN, p.firstName AS firstName, p.lastName AS lastName, p.sex AS sex");

        try (Session session = driver.session()) {
            Result result = session.run(cypher.toString(), params);
            if (!result.hasNext()) throw new NoSuchElementException("Patient not found: " + patientPN);
            Record row = result.next();
            return new Patient(
                    row.get("patientPN").asString(),
                    row.get("firstName").asString(),
                    row.get("lastName").asString(),
                    row.get("sex").asString()
            );
        }
    }

    @Override
    public String deletePatient(String patientPN, boolean confirmed) throws Exception {
        String summaryCypher = """
            MATCH (p:Patient {patientPN: $patientPN})
            OPTIONAL MATCH (ap:Appointment)-[:FOR_PATIENT]->(p)
            OPTIONAL MATCH (ap)-[:HAS_OBSERVATION]->(ob:Observation)
            OPTIONAL MATCH (ap)-[:HAS_DIAGNOSIS]->(dg:Diagnosis)
            OPTIONAL MATCH (nt:Note)-[:ABOUT_APPOINTMENT]->(ap)
            OPTIONAL MATCH (nt2:Note)-[:ABOUT_OBSERVATION]->(ob)
            OPTIONAL MATCH (nt3:Note)-[:ABOUT_DIAGNOSIS]->(dg)
            OPTIONAL MATCH (at:Attachment)-[:ATTACHED_TO]->(nt)
            OPTIONAL MATCH (at2:Attachment)-[:ATTACHED_TO]->(nt2)
            OPTIONAL MATCH (at3:Attachment)-[:ATTACHED_TO]->(nt3)
            RETURN p.firstName AS firstName, p.lastName AS lastName,
                   count(DISTINCT ap) AS apps, count(DISTINCT ob) AS obs, 
                   count(DISTINCT dg) AS diags, count(DISTINCT nt) + count(DISTINCT nt2) + count(DISTINCT nt3) AS notes, 
                   count(DISTINCT at) + count(DISTINCT at2) + count(DISTINCT at3) AS atts
            """;

        try (Session session = driver.session()) {
            Result r = session.run(summaryCypher, Map.of("patientPN", patientPN));
            if (!r.hasNext()) throw new NoSuchElementException("Patient not found: " + patientPN);

            Record s = r.next();
            String firstName = s.get("firstName").asString();
            String lastName = s.get("lastName").asString();
            int apps = s.get("apps").asInt();
            int obs = s.get("obs").asInt();
            int diags = s.get("diags").asInt();
            int notes = s.get("notes").asInt();
            int atts = s.get("atts").asInt();

            if (!confirmed) {
                return String.format(
                        "⚠️  WARNING: Deleting %s %s (PN: %s)\n\nWill CASCADE DELETE:\n  • %d Appointment(s)\n  • %d Observation(s)\n  • %d Diagnosis(es)\n  • %d Note(s)\n  • %d Attachment(s)\n\nCall again with confirmed=true to proceed",
                        firstName, lastName, patientPN, apps, obs, diags, notes, atts
                );
            }

            String deleteCypher = """
                MATCH (p:Patient {patientPN: $patientPN})
                OPTIONAL MATCH (ap:Appointment)-[:FOR_PATIENT]->(p)
                OPTIONAL MATCH (ap)-[:HAS_OBSERVATION]->(ob:Observation)
                OPTIONAL MATCH (ap)-[:HAS_DIAGNOSIS]->(dg:Diagnosis)
                OPTIONAL MATCH (nt:Note)-[:ABOUT_APPOINTMENT]->(ap)
                OPTIONAL MATCH (nt2:Note)-[:ABOUT_OBSERVATION]->(ob)
                OPTIONAL MATCH (nt3:Note)-[:ABOUT_DIAGNOSIS]->(dg)
                OPTIONAL MATCH (at:Attachment)-[:ATTACHED_TO]->(nt)
                OPTIONAL MATCH (at2:Attachment)-[:ATTACHED_TO]->(nt2)
                OPTIONAL MATCH (at3:Attachment)-[:ATTACHED_TO]->(nt3)
                DETACH DELETE at, at2, at3, nt, nt2, nt3, ob, dg, ap, p
                """;

            session.run(deleteCypher, Map.of("patientPN", patientPN));

            return String.format(
                    "✓ Successfully deleted %s %s (PN: %s) and %d related records",
                    firstName, lastName, patientPN, apps + obs + diags + notes + atts
            );
        }
    }

    @Override
    public Patient findByPN(String patientPN) throws Exception {
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        String cypher = """
            MATCH (p:Patient {patientPN: $patientPN})
            RETURN p.patientPN AS patientPN,
                   p.firstName AS firstName,
                   p.lastName AS lastName,
                   p.sex AS sex
            LIMIT 1
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("patientPN", patientPN));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No patient found with PN: " + patientPN);
            }

            Record row = result.next();
            return new Patient(
                    row.get("patientPN").asString(),
                    row.get("firstName").asString(),
                    row.get("lastName").asString(),
                    row.get("sex").asString()
            );
        }
    }

    @Override
    public List<PatientWithDoctor> searchByName(String firstName, String lastName) throws Exception {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Both firstName and lastName must be provided");
        }

        String cypher = """
            MATCH (p:Patient)
            WHERE toLower(p.firstName) = toLower($firstName) 
              AND toLower(p.lastName) = toLower($lastName)
            OPTIONAL MATCH (p)-[:HAS_PRIMARY_DOCTOR]->(dr:Doctor)
            OPTIONAL MATCH (dr)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN p.patientPN AS patientPN,
                   p.firstName AS firstName,
                   p.lastName AS lastName,
                   p.sex AS sex,
                   dr.doctorID AS doctorID,
                   dr.firstName AS doctorFirstName,
                   dr.lastName AS doctorLastName,
                   cl.name AS clinicName
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "firstName", firstName,
                    "lastName", lastName
            ));

            List<PatientWithDoctor> patients = new ArrayList<>();

            for (Record row : result.list()) {
                patients.add(new PatientWithDoctor(
                        row.get("patientPN").asString(),
                        row.get("firstName").asString(),
                        row.get("lastName").asString(),
                        row.get("sex").asString(),
                        row.get("doctorID").asString(null),
                        row.get("doctorFirstName").asString(null),
                        row.get("doctorLastName").asString(null),
                        row.get("clinicName").asString(null)
                ));
            }

            if (patients.isEmpty()) {
                throw new NoSuchElementException(
                        "No patients found with name: " + firstName + " " + lastName
                );
            }

            return patients;
        }
    }

    @Override
    public List<Patient> listByDoctor(String doctorID) throws Exception {
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String cypher = """
            MATCH (p:Patient)-[:HAS_PRIMARY_DOCTOR]->(dr:Doctor {doctorID: $doctorID})
            RETURN p.patientPN AS patientPN,
                   p.firstName AS firstName,
                   p.lastName AS lastName,
                   p.sex AS sex
            ORDER BY p.lastName, p.firstName
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("doctorID", doctorID));

            List<Patient> patients = new ArrayList<>();

            for (Record row : result.list()) {
                patients.add(new Patient(
                        row.get("patientPN").asString(),
                        row.get("firstName").asString(),
                        row.get("lastName").asString(),
                        row.get("sex").asString()
                ));
            }

            if (patients.isEmpty()) {
                throw new NoSuchElementException(
                        "No patients found for doctor: " + doctorID
                );
            }

            return patients;
        }
    }

    @Override
    public List<PatientWithDoctor> listByClinic(String clinicID) throws Exception {
        if (clinicID == null || clinicID.isBlank()) {
            throw new IllegalArgumentException("clinicID must not be blank");
        }

        String cypher = """
            MATCH (cl:Clinic {clinicID: $clinicID})
            MATCH (p:Patient)-[:HAS_PRIMARY_DOCTOR]->(dr:Doctor)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl)
            RETURN p.patientPN AS patientPN,
                   p.firstName AS firstName,
                   p.lastName AS lastName,
                   p.sex AS sex,
                   dr.doctorID AS doctorID,
                   dr.firstName AS doctorFirstName,
                   dr.lastName AS doctorLastName,
                   cl.name AS clinicName
            ORDER BY p.lastName, p.firstName
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("clinicID", clinicID));

            List<PatientWithDoctor> patients = new ArrayList<>();

            for (Record row : result.list()) {
                patients.add(new PatientWithDoctor(
                        row.get("patientPN").asString(),
                        row.get("firstName").asString(),
                        row.get("lastName").asString(),
                        row.get("sex").asString(),
                        row.get("doctorID").asString(),
                        row.get("doctorFirstName").asString(),
                        row.get("doctorLastName").asString(),
                        row.get("clinicName").asString()
                ));
            }

            if (patients.isEmpty()) {
                throw new NoSuchElementException(
                        "No patients found in clinic: " + clinicID
                );
            }

            return patients;
        }
    }

    @Override
    public String getPatientSummary(String patientPN) throws Exception {
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        String cypher = """
            MATCH (p:Patient {patientPN: $patientPN})
            OPTIONAL MATCH (p)-[:HAS_PRIMARY_DOCTOR]->(dr:Doctor)
            OPTIONAL MATCH (dr)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN p.patientPN AS patientPN,
                   p.firstName AS firstName,
                   p.lastName AS lastName,
                   p.sex AS sex,
                   dr.doctorID AS doctorID,
                   dr.firstName AS doctorFirstName,
                   dr.lastName AS doctorLastName,
                   dr.phoneNumber AS doctorPhone,
                   dp.name AS departmentName,
                   cl.name AS clinicName
            LIMIT 1
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("patientPN", patientPN));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No patient found with PN: " + patientPN);
            }

            Record row = result.next();

            String doctorInfo = "No primary doctor assigned";
            if (!row.get("doctorID").isNull()) {
                doctorInfo = String.format(
                        "Dr. %s %s (%s) - Dept: %s, Clinic: %s",
                        row.get("doctorFirstName").asString(""),
                        row.get("doctorLastName").asString(""),
                        row.get("doctorPhone").asString("N/A"),
                        row.get("departmentName").asString("Unknown"),
                        row.get("clinicName").asString("Unknown")
                );
            }

            return String.format(
                    "Patient: %s %s (PN: %s, Sex: %s)\nPrimary Doctor: %s",
                    row.get("firstName").asString(""),
                    row.get("lastName").asString(""),
                    row.get("patientPN").asString(""),
                    row.get("sex").asString(""),
                    doctorInfo
            );
        }
    }
}