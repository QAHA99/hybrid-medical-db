package cli;

import dao.neo4j.implementations.*;
import model.enums.Severity;
import model.mongo.User;
import service.AuthService;
import service.MessagingService;
import org.neo4j.driver.Driver;

import java.util.List;

import static dao.neo4j.implementations.AppointmentNeo4j.AppointmentWithDetails;
import static dao.neo4j.implementations.PatientNeo4j.PatientWithDoctor;
import static dao.neo4j.implementations.DepartmentNeo4j.DepartmentsWithDoctors;
import static dao.neo4j.implementations.ObservationNeo4j.ObservationWithContext;
import static dao.neo4j.implementations.DiagnosisNeo4j.DiagnosisWithContext;

public class ReceptionistMenuCLI extends CLIMenu {
    private final AuthService authService;
    private final Driver neo4jDriver;
    private final PatientNeo4j patientRepo;
    private final DepartmentNeo4j departmentRepo;
    private final AppointmentNeo4j appointmentRepo;
    private final ObservationNeo4j observationRepo;
    private final DiagnosisNeo4j diagnosisRepo;
    private final NoteNeo4j noteRepo;
    private final MessagingCLI messagingCLI;

    // Receptionist has access to all clinics
    private static final String[] CLINIC_IDS = {"CL0001", "CL0002", "CL0003", "CL0004"};

    public ReceptionistMenuCLI(AuthService authService, Driver neo4jDriver, MessagingService messagingService) {
        this.authService = authService;
        this.neo4jDriver = neo4jDriver;
        this.patientRepo = new PatientNeo4j(neo4jDriver);
        this.departmentRepo = new DepartmentNeo4j(neo4jDriver);
        this.appointmentRepo = new AppointmentNeo4j(neo4jDriver);
        this.observationRepo = new ObservationNeo4j(neo4jDriver);
        this.diagnosisRepo = new DiagnosisNeo4j(neo4jDriver);
        this.noteRepo = new NoteNeo4j(neo4jDriver);
        this.messagingCLI = new MessagingCLI(authService, messagingService);
    }

    /**
     * View all patients across all clinics
     */
    public void viewAllPatients() {
        while (true) {
            clearScreen();
            printHeader("ALL PATIENTS");

            printMenuOptions(
                    "View All Patients (All Clinics)",
                    "View Patients by Clinic"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    viewAllPatientsAllClinics();
                    break;
                case 2:
                    viewPatientsByClinic();
                    break;
                case 0:
                    return;
                default:
                    printError("Invalid option.");
                    pauseForUser();
            }
        }
    }

    /**
     * View all patients from all clinics
     */
    private void viewAllPatientsAllClinics() {
        try {
            clearScreen();
            printHeader("ALL PATIENTS - ALL CLINICS");

            printDivider();
            System.out.println(String.format("%-18s %-20s %-20s %-10s %-30s %-20s",
                    "Patient PN", "First Name", "Last Name", "Sex", "Primary Doctor", "Clinic"));
            printDivider();

            // Loop through all clinics
            for (String clinicID : CLINIC_IDS) {
                try {
                    List<PatientWithDoctor> patients = patientRepo.listByClinic(clinicID);

                    for (PatientWithDoctor patient : patients) {
                        String doctorName = patient.doctorFirstName() != null
                                ? "Dr. " + patient.doctorFirstName() + " " + patient.doctorLastName()
                                : "None";
                        String clinicName = patient.clinicName() != null ? patient.clinicName() : "N/A";

                        System.out.println(String.format("%-18s %-20s %-20s %-10s %-30s %-20s",
                                patient.patientPN(),
                                patient.firstName(),
                                patient.lastName(),
                                patient.sex(),
                                doctorName,
                                clinicName
                        ));
                    }
                } catch (Exception e) {
                    // Continue if one clinic fails
                    System.out.println("Error loading clinic " + clinicID + ": " + e.getMessage());
                }
            }

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load patients: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View patients by specific clinic
     */
    private void viewPatientsByClinic() {
        try {
            String clinicID = getStringInput("\nEnter Clinic ID (e.g., CL0001): ");

            if (clinicID.isEmpty()) {
                printError("Clinic ID cannot be empty.");
                pauseForUser();
                return;
            }

            List<PatientWithDoctor> patients = patientRepo.listByClinic(clinicID);

            clearScreen();
            printHeader("PATIENTS IN CLINIC: " + clinicID);

            if (patients.isEmpty()) {
                printInfo("No patients found in this clinic.");
                pauseForUser();
                return;
            }

            printDivider();
            System.out.println(String.format("%-18s %-20s %-20s %-10s %-30s",
                    "Patient PN", "First Name", "Last Name", "Sex", "Primary Doctor"));
            printDivider();

            for (PatientWithDoctor patient : patients) {
                String doctorName = patient.doctorFirstName() != null
                        ? "Dr. " + patient.doctorFirstName() + " " + patient.doctorLastName()
                        : "None";

                System.out.println(String.format("%-18s %-20s %-20s %-10s %-30s",
                        patient.patientPN(),
                        patient.firstName(),
                        patient.lastName(),
                        patient.sex(),
                        doctorName
                ));
            }

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load patients: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View all doctors with their departments
     */
    public void viewAllDoctors() {
        try {
            String clinicID = getStringInput("\nEnter Clinic ID (e.g., CL0001): ");

            if (clinicID.isEmpty()) {
                printError("Clinic ID cannot be empty.");
                pauseForUser();
                return;
            }

            List<DepartmentsWithDoctors> departments = departmentRepo.listByClinic(clinicID);

            clearScreen();
            printHeader("DOCTORS IN CLINIC: " + clinicID);

            if (departments.isEmpty()) {
                printInfo("No departments found in this clinic.");
                pauseForUser();
                return;
            }

            for (DepartmentsWithDoctors dept : departments) {
                printDivider();
                System.out.println("\nDEPARTMENT: " + dept.departmentName() + " (" + dept.departmentID() + ")");
                printDivider();

                if (dept.doctors().isEmpty()) {
                    System.out.println("  No doctors in this department");
                } else {
                    for (model.neo4j.Doctor doctor : dept.doctors()) {
                        System.out.println("  • Dr. " + doctor.getFirstName() + " " + doctor.getLastName()
                                + " - " + doctor.getPhoneNumber());
                    }
                }
            }

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load doctors: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View all appointments
     */
    public void viewAllAppointments() {
        try {
            String patientPN = getStringInput("\nEnter Patient PN to view appointments: ");

            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("APPOINTMENTS FOR PATIENT: " + patientPN);

            boolean includeHistory = getConfirmation("\nInclude past appointments?");

            List<AppointmentWithDetails> appointments = appointmentRepo.listByPatient(patientPN, includeHistory);

            if (appointments.isEmpty()) {
                printInfo("No appointments found for this patient.");
                pauseForUser();
                return;
            }

            printDivider();
            System.out.println(String.format("%-12s %-20s %-30s %-20s",
                    "ID", "Date & Time", "Doctor", "Reason"));
            printDivider();

            for (AppointmentWithDetails apt : appointments) {
                String doctorName = "Dr. " + apt.doctorFirstName() + " " + apt.doctorLastName();
                String dateTime = formatDateTime(apt.starts());
                String reason = apt.reason();
                if (reason.length() > 20) {
                    reason = reason.substring(0, 17) + "...";
                }

                System.out.println(String.format("%-12s %-20s %-30s %-20s",
                        apt.appointmentID(),
                        dateTime,
                        doctorName,
                        reason
                ));
            }

            printDivider();

            printMenuOptions("View Appointment Details");

            int choice = getIntInput("");

            if (choice == 1) {
                viewAppointmentDetails();
            }

        } catch (Exception e) {
            printError("Failed to load appointments: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View appointment details
     */
    private void viewAppointmentDetails() {
        try {
            String appointmentID = getStringInput("\nEnter Appointment ID: ");

            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = appointmentRepo.getAppointmentDetails(appointmentID);

            clearScreen();
            printHeader("APPOINTMENT DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load appointment details: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View patient summary
     */
    public void viewPatientSummary() {
        try {
            String patientPN = getStringInput("\nEnter Patient PN: ");

            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("PATIENT SUMMARY");

            String summary = patientRepo.getPatientSummary(patientPN);
            System.out.println("\n" + summary);

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load patient summary: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View medical data (read-only)
     */
    public void viewMedicalData() {
        while (true) {
            clearScreen();
            printHeader("VIEW MEDICAL DATA (READ-ONLY)");

            printMenuOptions(
                    "View Observations",
                    "View Diagnoses",
                    "View Notes"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    viewObservations();
                    break;
                case 2:
                    viewDiagnoses();
                    break;
                case 3:
                    viewNotes();
                    break;
                case 0:
                    return;
                default:
                    printError("Invalid option.");
                    pauseForUser();
            }
        }
    }

    /**
     * View observations for a patient (read-only)
     */
    private void viewObservations() {
        try {
            String patientPN = getStringInput("\nEnter Patient PN: ");

            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            List<ObservationWithContext> observations = observationRepo.listByPatient(patientPN);

            clearScreen();
            printHeader("OBSERVATIONS FOR PATIENT: " + patientPN);

            if (observations.isEmpty()) {
                printInfo("No observations found for this patient.");
                pauseForUser();
                return;
            }

            printDivider();
            for (ObservationWithContext obs : observations) {
                System.out.println("\nID: " + obs.observationID());
                System.out.println("Date: " + formatDateTime(obs.observedAt()));
                System.out.println("Doctor: Dr. " + obs.doctorFirstName() + " " + obs.doctorLastName());
                System.out.println("Appointment: " + obs.appointmentID());
                System.out.println("Text: " + obs.text());
                printDivider();
            }

            printMenuOptions("View Observation Details");

            int choice = getIntInput("");

            if (choice == 1) {
                viewObservationDetails();
            }

        } catch (Exception e) {
            printError("Failed to load observations: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View observation details (read-only)
     */
    private void viewObservationDetails() {
        try {
            String observationID = getStringInput("\nEnter Observation ID: ");

            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = observationRepo.getObservationDetails(observationID);

            clearScreen();
            printHeader("OBSERVATION DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load observation details: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View diagnoses for a patient (read-only)
     */
    private void viewDiagnoses() {
        try {
            String patientPN = getStringInput("\nEnter Patient PN: ");

            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nSelect severity to filter:");
            System.out.println("1. Hög (High)");
            System.out.println("2. Medel (Medium)");
            System.out.println("3. Låg (Low)");

            int severityChoice = getIntInput("Choice: ");
            Severity severity;

            switch (severityChoice) {
                case 1:
                    severity = Severity.HÖG;
                    break;
                case 2:
                    severity = Severity.MEDEL;
                    break;
                case 3:
                    severity = Severity.LÅG;
                    break;
                default:
                    printError("Invalid severity choice.");
                    pauseForUser();
                    return;
            }

            List<DiagnosisWithContext> diagnoses = diagnosisRepo.listByPatientAndSeverity(patientPN, severity);

            clearScreen();
            printHeader("DIAGNOSES FOR " + patientPN + " - " + severity.getLabel());

            if (diagnoses.isEmpty()) {
                printInfo("No diagnoses found with this severity.");
                pauseForUser();
                return;
            }

            printDivider();
            for (DiagnosisWithContext diag : diagnoses) {
                System.out.println("\nID: " + diag.diagnosisID());
                System.out.println("Severity: " + diag.severity());
                System.out.println("Details: " + diag.details());
                System.out.println("Date: " + formatDateTime(diag.diagnosedAt()));
                System.out.println("Doctor: Dr. " + diag.doctorFirstName() + " " + diag.doctorLastName());
                System.out.println("Observation: " + diag.observationID());
                printDivider();
            }

            printMenuOptions("View Diagnosis Details");

            int choice = getIntInput("");

            if (choice == 1) {
                viewDiagnosisDetails();
            }

        } catch (Exception e) {
            printError("Failed to load diagnoses: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View diagnosis details (read-only)
     */
    private void viewDiagnosisDetails() {
        try {
            String diagnosisID = getStringInput("\nEnter Diagnosis ID: ");

            if (diagnosisID.isEmpty()) {
                printError("Diagnosis ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = diagnosisRepo.getDiagnosisDetails(diagnosisID);

            clearScreen();
            printHeader("DIAGNOSIS DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load diagnosis details: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View notes (read-only)
     */
    private void viewNotes() {
        try {
            String noteID = getStringInput("\nEnter Note ID: ");

            if (noteID.isEmpty()) {
                printError("Note ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = noteRepo.getNoteDetails(noteID);

            clearScreen();
            printHeader("NOTE DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load note details: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Show messaging menu
     */
    public void showMessagingMenu() {
        messagingCLI.showMessagingMenu();
    }
}