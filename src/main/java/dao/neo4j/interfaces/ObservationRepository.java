package dao.neo4j.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import model.neo4j.Observation;

public interface ObservationRepository {

    List<ObservationWithContext> listByPatient(String patientPN) throws Exception;

    String getObservationDetails(String observationID) throws Exception;

    Observation createObservation(Observation observation, String appointmentID) throws Exception;

    Observation updateObservation(String observationID, LocalDateTime newObservedAt, String newText) throws Exception;

    void deleteObservation(String observationID) throws Exception;

    record ObservationWithContext(
            String observationID,
            LocalDateTime observedAt,
            String text,
            String patientPN,
            String patientFirstName,
            String patientLastName,
            String doctorID,
            String doctorFirstName,
            String doctorLastName,
            String appointmentID
    ) {}
}