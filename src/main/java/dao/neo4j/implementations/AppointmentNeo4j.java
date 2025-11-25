package dao.neo4j.implementations;

import dao.neo4j.interfaces.AppointmentRepository;
import model.neo4j.Appointment;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class AppointmentNeo4j implements AppointmentRepository {

    private final Driver driver;

    public AppointmentNeo4j(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<AppointmentWithDetails> listByPatient(String patientPN, boolean includeHistory) throws Exception {
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment)-[:FOR_PATIENT]->(p:Patient {patientPN: $patientPN})
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)
            RETURN ap, p, dr
            ORDER BY ap.starts ASC
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("patientPN", patientPN));
            List<AppointmentWithDetails> appointments = new ArrayList<>();

            for (Record row : result.list()) {
                var ap = row.get("ap").asNode();
                var p = row.get("p").asNode();
                var dr = row.get("dr").asNode();

                appointments.add(new AppointmentWithDetails(
                        ap.get("appointmentID").asString(),
                        ap.get("starts").asLocalDateTime(),
                        ap.get("ends").asLocalDateTime(),
                        ap.get("reason").asString(),
                        p.get("patientPN").asString(),
                        p.get("firstName").asString(),
                        p.get("lastName").asString(),
                        dr.get("doctorID").asString(),
                        dr.get("firstName").asString(),
                        dr.get("lastName").asString()
                ));
            }

            if (appointments.isEmpty()) {
                throw new NoSuchElementException("No appointments found for patient: " + patientPN);
            }

            return appointments;
        }
    }

    @Override
    public List<AppointmentWithDetails> listByDoctor(String doctorID, boolean includeHistory) throws Exception {
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment)-[:WITH_DOCTOR]->(dr:Doctor {doctorID: $doctorID})
            MATCH (ap)-[:FOR_PATIENT]->(p:Patient)
            RETURN ap, p, dr
            ORDER BY ap.starts ASC
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("doctorID", doctorID));
            List<AppointmentWithDetails> appointments = new ArrayList<>();

            for (Record row : result.list()) {
                var ap = row.get("ap").asNode();
                var p = row.get("p").asNode();
                var dr = row.get("dr").asNode();

                appointments.add(new AppointmentWithDetails(
                        ap.get("appointmentID").asString(),
                        ap.get("starts").asLocalDateTime(),
                        ap.get("ends").asLocalDateTime(),
                        ap.get("reason").asString(),
                        p.get("patientPN").asString(),
                        p.get("firstName").asString(),
                        p.get("lastName").asString(),
                        dr.get("doctorID").asString(),
                        dr.get("firstName").asString(),
                        dr.get("lastName").asString()
                ));
            }

            if (appointments.isEmpty()) {
                throw new NoSuchElementException("No appointments found for doctor: " + doctorID);
            }

            return appointments;
        }
    }

    @Override
    public String getAppointmentDetails(String appointmentID) throws Exception {
        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            MATCH (ap)-[:FOR_PATIENT]->(p:Patient)
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)-[:WORKS_IN]->(dp:Department)-[:IN_CLINIC]->(cl:Clinic)
            RETURN ap, p, dr, dp, cl
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("appointmentID", appointmentID));

            if (!result.hasNext()) {
                throw new NoSuchElementException("No appointment found with ID: " + appointmentID);
            }

            Record row = result.next();
            var ap = row.get("ap").asNode();
            var p = row.get("p").asNode();
            var dr = row.get("dr").asNode();
            var dp = row.get("dp").asNode();
            var cl = row.get("cl").asNode();

            return String.format(
                    "Appointment ID: %s\n" +
                            "Date & Time: %s to %s\n" +
                            "Reason: %s\n" +
                            "Patient: %s %s (PN: %s)\n" +
                            "Doctor: Dr. %s %s (%s)\n" +
                            "Department: %s\n" +
                            "Clinic: %s",
                    ap.get("appointmentID").asString(),
                    ap.get("starts").asLocalDateTime(),
                    ap.get("ends").asLocalDateTime(),
                    ap.get("reason").asString(),
                    p.get("firstName").asString(),
                    p.get("lastName").asString(),
                    p.get("patientPN").asString(),
                    dr.get("firstName").asString(),
                    dr.get("lastName").asString(),
                    dr.get("phoneNumber").asString("N/A"),
                    dp.get("name").asString("Unknown"),
                    cl.get("name").asString("Unknown")
            );
        }
    }

    @Override
    public Appointment createAppointment(Appointment appointment, String patientPN, String doctorID) throws Exception {
        if (appointment == null) {
            throw new IllegalArgumentException("appointment must not be null");
        }
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }
        if (appointment.getStarts() == null || appointment.getEnds() == null) {
            throw new IllegalArgumentException("appointment start and end times must not be null");
        }
        if (appointment.getEnds().isBefore(appointment.getStarts()) || appointment.getEnds().equals(appointment.getStarts())) {
            throw new IllegalArgumentException("appointment end time must be after start time");
        }

        String checkExistsCypher = """
            MATCH (p:Patient {patientPN: $patientPN})
            MATCH (dr:Doctor {doctorID: $doctorID})
            RETURN p, dr
            """;

        String checkOverlapCypher = """
            MATCH (existing:Appointment)-[:WITH_DOCTOR]->(dr:Doctor {doctorID: $doctorID})
            WHERE existing.starts < $ends AND existing.ends > $starts
            RETURN COUNT(existing) AS overlapCount
            """;

        String createCypher = """
            MATCH (p:Patient {patientPN: $patientPN})
            MATCH (dr:Doctor {doctorID: $doctorID})
            CREATE (ap:Appointment {
                appointmentID: $appointmentID,
                starts: $starts,
                ends: $ends,
                reason: $reason
            })
            CREATE (ap)-[:FOR_PATIENT]->(p)
            CREATE (ap)-[:WITH_DOCTOR]->(dr)
            RETURN ap
            """;

        try (Session session = driver.session()) {
            Result existsResult = session.run(checkExistsCypher, Map.of(
                    "patientPN", patientPN,
                    "doctorID", doctorID
            ));

            if (!existsResult.hasNext()) {
                throw new NoSuchElementException("Patient or doctor not found");
            }

            Result overlapResult = session.run(checkOverlapCypher, Map.of(
                    "doctorID", doctorID,
                    "starts", appointment.getStarts(),
                    "ends", appointment.getEnds()
            ));

            if (overlapResult.hasNext() && overlapResult.next().get("overlapCount").asInt() > 0) {
                throw new IllegalArgumentException("Time slot overlaps with existing appointment");
            }

            Result createResult = session.run(createCypher, Map.of(
                    "appointmentID", appointment.getAppointmentID(),
                    "starts", appointment.getStarts(),
                    "ends", appointment.getEnds(),
                    "reason", appointment.getReason(),
                    "patientPN", patientPN,
                    "doctorID", doctorID
            ));

            var ap = createResult.next().get("ap").asNode();
            return new Appointment(
                    ap.get("appointmentID").asString(),
                    ap.get("starts").asLocalDateTime(),
                    ap.get("ends").asLocalDateTime(),
                    ap.get("reason").asString()
            );
        }
    }

    @Override
    public Appointment updateAppointment(String appointmentID, LocalDateTime newStarts,
                                         LocalDateTime newEnds, String newReason) throws Exception {
        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }
        if (newStarts == null && newEnds == null && newReason == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }
        if (newStarts != null && newEnds != null) {
            if (newEnds.isBefore(newStarts) || newEnds.equals(newStarts)) {
                throw new IllegalArgumentException("end time must be after start time");
            }
        }

        String getExistingCypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            MATCH (ap)-[:WITH_DOCTOR]->(dr:Doctor)
            RETURN ap, dr.doctorID AS doctorID
            """;

        String checkOverlapCypher = """
            MATCH (other:Appointment)-[:WITH_DOCTOR]->(dr:Doctor {doctorID: $doctorID})
            WHERE other.appointmentID <> $appointmentID
              AND other.starts < $ends AND other.ends > $starts
            RETURN COUNT(other) AS overlapCount
            """;

        String updateCypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            SET ap.starts = COALESCE($newStarts, ap.starts),
                ap.ends = COALESCE($newEnds, ap.ends),
                ap.reason = COALESCE($newReason, ap.reason)
            RETURN ap
            """;

        try (Session session = driver.session()) {
            Result existingResult = session.run(getExistingCypher, Map.of("appointmentID", appointmentID));

            if (!existingResult.hasNext()) {
                throw new NoSuchElementException("Appointment not found with ID: " + appointmentID);
            }

            Record existingRow = existingResult.next();
            var existingAp = existingRow.get("ap").asNode();
            String doctorID = existingRow.get("doctorID").asString();

            LocalDateTime finalStarts = newStarts != null ? newStarts : existingAp.get("starts").asLocalDateTime();
            LocalDateTime finalEnds = newEnds != null ? newEnds : existingAp.get("ends").asLocalDateTime();

            if (finalEnds.isBefore(finalStarts) || finalEnds.equals(finalStarts)) {
                throw new IllegalArgumentException("end time must be after start time");
            }

            Result overlapResult = session.run(checkOverlapCypher, Map.of(
                    "appointmentID", appointmentID,
                    "doctorID", doctorID,
                    "starts", finalStarts,
                    "ends", finalEnds
            ));

            if (overlapResult.hasNext() && overlapResult.next().get("overlapCount").asInt() > 0) {
                throw new IllegalArgumentException("New time slot overlaps with existing appointment");
            }

            Result updateResult = session.run(updateCypher, Map.of(
                    "appointmentID", appointmentID,
                    "newStarts", newStarts,
                    "newEnds", newEnds,
                    "newReason", newReason
            ));

            var ap = updateResult.next().get("ap").asNode();
            return new Appointment(
                    ap.get("appointmentID").asString(),
                    ap.get("starts").asLocalDateTime(),
                    ap.get("ends").asLocalDateTime(),
                    ap.get("reason").asString()
            );
        }
    }

    @Override
    public void deleteAppointment(String appointmentID) throws Exception {
        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }

        String cypher = """
            MATCH (ap:Appointment {appointmentID: $appointmentID})
            DETACH DELETE ap
            RETURN COUNT(ap) AS deletedCount
            """;

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("appointmentID", appointmentID));

            if (result.hasNext()) {
                int deletedCount = result.next().get("deletedCount").asInt();
                if (deletedCount == 0) {
                    throw new NoSuchElementException("No appointment found with ID: " + appointmentID);
                }
            } else {
                throw new NoSuchElementException("No appointment found with ID: " + appointmentID);
            }
        }
    }
}