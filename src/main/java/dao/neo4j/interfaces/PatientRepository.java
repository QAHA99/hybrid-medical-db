package dao.neo4j.interfaces;

import model.neo4j.Patient;
import java.util.List;

public interface PatientRepository {

    Patient createPatient(String patientPN, String firstName, String lastName,
                          String sex, String phoneNumber, String doctorID) throws Exception;

    Patient updatePatient(String patientPN, String firstName, String lastName,
                          String sex, String phoneNumber, String doctorID) throws Exception;

    String deletePatient(String patientPN, boolean confirmed) throws Exception;

    Patient findByPN(String patientPN) throws Exception;

    List<PatientWithDoctor> searchByName(String firstName, String lastName) throws Exception;

    List<Patient> listByDoctor(String doctorID) throws Exception;

    List<PatientWithDoctor> listByClinic(String clinicID) throws Exception;

    String getPatientSummary(String patientPN) throws Exception;

    record PatientWithDoctor(
            String patientPN,
            String firstName,
            String lastName,
            String sex,
            String primaryDoctorID,
            String doctorFirstName,
            String doctorLastName,
            String clinicName
    ) {}
}