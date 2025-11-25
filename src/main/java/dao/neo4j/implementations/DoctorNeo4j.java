package dao.neo4j.implementations;

import dao.neo4j.interfaces.DoctorRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.Map;
import java.util.NoSuchElementException;

public class DoctorNeo4j implements DoctorRepository {

    private final Driver driver;

    public DoctorNeo4j(Driver driver) { this.driver = driver; }

    @Override
    public String findByID(String doctorID) {
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String cypher = """
            MATCH (dr:Doctor {doctorID: $doctorID})
            MATCH (dr)-[:WORKS_IN]->(dp:Department)
            MATCH (dp)-[:IN_CLINIC]->(cl:Clinic)
            RETURN dr.firstName   AS FirstName,
                   dr.lastName    AS LastName,
                   dr.phoneNumber AS PhoneNumber,
                   dp.name        AS DepartmentName,
                   cl.name        AS ClinicName
            LIMIT 1
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("doctorID", doctorID));
            if (!result.hasNext()) {
                throw new NoSuchElementException("No doctor found with id " + doctorID);
            }
            Record row = result.next();
            return String.format(
                    "Dr. %s %s (%s) - Dept: %s, Clinic: %s",
                    row.get("FirstName").asString(""),
                    row.get("LastName").asString(""),
                    row.get("PhoneNumber").asString(""),
                    row.get("DepartmentName").asString(""),
                    row.get("ClinicName").asString("")
            );
        }
    }

    @Override
    public String searchByName(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Both firstName and lastName must be provided");
        }

        String cypher = """
        MATCH (d:Doctor {firstName: $firstName, lastName: $lastName})
        MATCH (d)-[:WORKS_IN]->(dp:Department)
        MATCH (dp)-[:IN_CLINIC]->(cl:Clinic)
        RETURN d.doctorID   AS DoctorID,
               d.firstName  AS FirstName,
               d.lastName   AS LastName,
               d.phoneNumber AS PhoneNumber,
               dp.name      AS DepartmentName,
               cl.name      AS ClinicName
        """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "firstName", firstName,
                    "lastName", lastName
            ));

            if (!result.hasNext()) {
                throw new NoSuchElementException(
                        "No doctor found for name '" + firstName + " " + lastName + "'"
                );
            }

            Record row = result.next();
            return String.format(
                    "%s — Dr. %s %s (%s) | Dept: %s | Clinic: %s",
                    row.get("DoctorID").asString(""),
                    row.get("FirstName").asString(""),
                    row.get("LastName").asString(""),
                    row.get("PhoneNumber").asString(""),
                    row.get("DepartmentName").asString(""),
                    row.get("ClinicName").asString("")
            );
        }
    }

    @Override
    public String getDoctorClinicID(String doctorID) throws Exception {
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String cypher = """
            MATCH (dr:Doctor {doctorID: $doctorID})-[:WORKS_IN]->(:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN cl.clinicID AS clinicID
            LIMIT 1
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("doctorID", doctorID));
            if (!result.hasNext()) {
                throw new NoSuchElementException("Doctor not found or not assigned to clinic: " + doctorID);
            }
            return result.next().get("clinicID").asString();
        }
    }
}