package dao.neo4j.interfaces;

import model.neo4j.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository {

    List<AppointmentWithDetails> listByPatient(String patientPN, boolean includeHistory) throws Exception;

    List<AppointmentWithDetails> listByDoctor(String doctorID, boolean includeHistory) throws Exception;

    String getAppointmentDetails(String appointmentID) throws Exception;

    Appointment createAppointment(Appointment appointment, String patientPN, String doctorID) throws Exception;

    Appointment updateAppointment(String appointmentID, LocalDateTime newStarts, LocalDateTime newEnds, String newReason) throws Exception;

    void deleteAppointment(String appointmentID) throws Exception;

    record AppointmentWithDetails(
            String appointmentID,
            LocalDateTime starts,
            LocalDateTime ends,
            String reason,
            String patientPN,
            String patientFirstName,
            String patientLastName,
            String doctorID,
            String doctorFirstName,
            String doctorLastName
    ) {}
}