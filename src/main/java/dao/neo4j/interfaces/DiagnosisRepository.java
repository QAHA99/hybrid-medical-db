package dao.neo4j.interfaces;

import model.neo4j.Diagnosis;
import model.enums.Severity;

import java.time.LocalDateTime;
import java.util.List;

public interface DiagnosisRepository {

    List<DiagnosisWithContext> listByPatientAndSeverity(String patientPN, Severity severity) throws Exception;

    String getDiagnosisDetails(String diagnosisID) throws Exception;

    Diagnosis createDiagnosis(Diagnosis diagnosis, String observationID) throws Exception;

    Diagnosis updateDiagnosis(String diagnosisID, Severity newSeverity, String newDetails) throws Exception;

    void deleteDiagnosis(String diagnosisID) throws Exception;

    record DiagnosisWithContext(
            String diagnosisID,
            String severity,
            String details,
            String patientPN,
            String patientFirstName,
            String patientLastName,
            String doctorID,
            String doctorFirstName,
            String doctorLastName,
            String appointmentID,
            LocalDateTime diagnosedAt,
            String observationID,
            String observationText
    ) {}
}