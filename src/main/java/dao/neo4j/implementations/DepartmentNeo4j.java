package dao.neo4j.implementations;

import dao.neo4j.interfaces.DepartmentRepository;
import model.neo4j.Doctor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DepartmentNeo4j implements DepartmentRepository {

    private final Driver driver;

    public DepartmentNeo4j (Driver driver) {this.driver = driver;}

    @Override
    public List<DepartmentsWithDoctors> listByClinic(String clinicID) {

        String cypher = """
                MATCH (dp:Department)-[:IN_CLINIC]->(cl:Clinic {clinicID: $clinicID})
                MATCH (dr:Doctor)-[:WORKS_IN]->(dp)
                RETURN cl.clinicID AS clinicID, dp.departmentID AS departmentID, dp.name AS departmentName,
                       dr.doctorID AS doctorID, dr.firstName AS firstName, dr.lastName AS lastName, dr.phoneNumber AS phoneNumber
                ORDER BY dp.departmentID, dr.lastName
                """;

        // Group doctors by department (a query returns one row per doctor)
        Map<String, DepartmentsWithDoctors> deptMap = new LinkedHashMap<>();

        try (Session session = driver.session()) {
            // Execute a query with a clinicID parameter
            Result result = session.run(cypher, Map.of("clinicID", clinicID));

            // Loop through each row returned by the query
            for (Record row : result.list()) {
                // Get the department ID from this row
                String deptID = row.get("departmentID").asString();

                if (!deptMap.containsKey(deptID)) {
                    DepartmentsWithDoctors dept = new DepartmentsWithDoctors (
                            row.get("clinicID").asString(),
                            deptID,
                            row.get("departmentName").asString(),
                            new ArrayList<>()
                    );
                    deptMap.put(deptID, dept);
                }

                Doctor doctor = new Doctor(
                        row.get("clinicID").asString(),
                        row.get("firstName").asString(),
                        row.get("lastName").asString(),
                        row.get("phoneNumber").asString()
                );
                deptMap.get(deptID).doctors().add(doctor);
            }
        }
        return new ArrayList<>(deptMap.values());
    }
}
