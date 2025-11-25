package cli;

import dao.neo4j.implementations.*;
import model.enums.Severity;
import model.neo4j.*;
import model.mongo.User;
import service.AuthService;
import service.MessagingService;
import org.neo4j.driver.Driver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static dao.neo4j.implementations.AppointmentNeo4j.AppointmentWithDetails;
import static dao.neo4j.implementations.ObservationNeo4j.ObservationWithContext;
import static dao.neo4j.implementations.DiagnosisNeo4j.DiagnosisWithContext;

public class DoctorMenuCLI extends CLIMenu {
    private final AuthService authService;
    private final Driver neo4jDriver;
    private final PatientNeo4j patientRepo;
    private final AppointmentNeo4j appointmentRepo;
    private final ObservationNeo4j observationRepo;
    private final DiagnosisNeo4j diagnosisRepo;
    private final NoteNeo4j noteRepo;
    private final MessagingCLI messagingCLI;

    public DoctorMenuCLI(AuthService authService, Driver neo4jDriver, MessagingService messagingService) {
        this.authService = authService;
        this.neo4jDriver = neo4jDriver;
        this.patientRepo = new PatientNeo4j(neo4jDriver);
        this.appointmentRepo = new AppointmentNeo4j(neo4jDriver);
        this.observationRepo = new ObservationNeo4j(neo4jDriver);
        this.diagnosisRepo = new DiagnosisNeo4j(neo4jDriver);
        this.noteRepo = new NoteNeo4j(neo4jDriver);
        this.messagingCLI = new MessagingCLI(authService, messagingService);
    }

    /**
     * View doctor's patients (only their patients)
     */
    public void viewMyPatients() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY PATIENTS");

            List<model.neo4j.Patient> patients = patientRepo.listByDoctor(doctorID);

            if (patients.isEmpty()) {
                printInfo("You have no patients assigned.");
                pauseForUser();
                return;
            }

            printDivider();
            System.out.println(String.format("%-18s %-20s %-20s %-10s",
                    "Patient PN", "First Name", "Last Name", "Sex"));
            printDivider();

            for (model.neo4j.Patient patient : patients) {
                System.out.println(String.format("%-18s %-20s %-20s %-10s",
                        patient.getPatientPN(),
                        patient.getFirstName(),
                        patient.getLastName(),
                        patient.getSex().getLabel()
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
     * View summary of a specific patient (must be doctor's patient)
     */
    public void viewPatientSummary() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            // First show doctor's patients
            List<model.neo4j.Patient> patients = patientRepo.listByDoctor(doctorID);

            if (patients.isEmpty()) {
                printInfo("You have no patients assigned.");
                pauseForUser();
                return;
            }

            // Build selection list
            String[] patientOptions = new String[patients.size()];
            for (int i = 0; i < patients.size(); i++) {
                model.neo4j.Patient p = patients.get(i);
                patientOptions[i] = p.getPatientPN() + " - " + p.getFirstName() + " " + p.getLastName();
            }

            int choice = selectFromList("SELECT PATIENT", patientOptions);

            if (choice == 0) {
                return;
            }

            model.neo4j.Patient selectedPatient = patients.get(choice - 1);
            String patientPN = selectedPatient.getPatientPN();

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
     * View doctor's appointments
     */
    public void viewMyAppointments() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY APPOINTMENTS");

            boolean includeHistory = getConfirmation("\nInclude past appointments?");

            List<AppointmentWithDetails> appointments = appointmentRepo.listByDoctor(doctorID, includeHistory);

            if (appointments.isEmpty()) {
                printInfo("No appointments found.");
                pauseForUser();
                return;
            }

            printDivider();
            System.out.println(String.format("%-12s %-20s %-25s %-20s",
                    "ID", "Date & Time", "Patient", "Reason"));
            printDivider();

            for (AppointmentWithDetails apt : appointments) {
                String patientName = apt.patientFirstName() + " " + apt.patientLastName();
                String dateTime = formatDateTime(apt.starts());
                String reason = apt.reason();
                if (reason.length() > 20) {
                    reason = reason.substring(0, 17) + "...";
                }

                System.out.println(String.format("%-12s %-20s %-25s %-20s",
                        apt.appointmentID(),
                        dateTime,
                        patientName,
                        reason
                ));
            }

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load appointments: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Manage appointments (CRUD)
     */
    public void manageAppointments() {
        while (true) {
            clearScreen();
            printHeader("MANAGE APPOINTMENTS");

            printMenuOptions(
                    "Create Appointment",
                    "Update Appointment",
                    "Delete Appointment",
                    "View Appointment Details"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    createAppointment();
                    break;
                case 2:
                    updateAppointment();
                    break;
                case 3:
                    deleteAppointment();
                    break;
                case 4:
                    viewAppointmentDetails();
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
     * Create a new appointment
     */
    private void createAppointment() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("CREATE APPOINTMENT");

            // Get appointment ID
            String appointmentID = getStringInput("\nEnter Appointment ID (e.g., AP0001): ");
            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            // Select patient
            List<model.neo4j.Patient> patients = patientRepo.listByDoctor(doctorID);
            if (patients.isEmpty()) {
                printInfo("You have no patients assigned.");
                pauseForUser();
                return;
            }

            String[] patientOptions = new String[patients.size()];
            for (int i = 0; i < patients.size(); i++) {
                model.neo4j.Patient p = patients.get(i);
                patientOptions[i] = p.getPatientPN() + " - " + p.getFirstName() + " " + p.getLastName();
            }

            int patientChoice = selectFromList("SELECT PATIENT", patientOptions);
            if (patientChoice == 0) return;

            String patientPN = patients.get(patientChoice - 1).getPatientPN();

            // Get date and time
            System.out.println("\nEnter start date and time (format: yyyy-MM-dd HH:mm)");
            String startsStr = getStringInput("Start: ");

            System.out.println("\nEnter end date and time (format: yyyy-MM-dd HH:mm)");
            String endsStr = getStringInput("End: ");

            String reason = getStringInput("\nReason for visit: ");

            if (reason.isEmpty()) {
                printError("Reason cannot be empty.");
                pauseForUser();
                return;
            }

            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime starts = LocalDateTime.parse(startsStr, formatter);
            LocalDateTime ends = LocalDateTime.parse(endsStr, formatter);

            // Create appointment
            Appointment appointment = new Appointment(appointmentID, starts, ends, reason);
            appointmentRepo.createAppointment(appointment, patientPN, doctorID);

            printSuccess("Appointment created successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to create appointment: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Update an existing appointment
     */
    private void updateAppointment() {
        try {
            String appointmentID = getStringInput("\nEnter Appointment ID to update: ");

            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("UPDATE APPOINTMENT: " + appointmentID);

            System.out.println("\nLeave blank to keep current value.");

            String startsStr = getStringInput("\nNew start time (yyyy-MM-dd HH:mm) or blank: ");
            String endsStr = getStringInput("New end time (yyyy-MM-dd HH:mm) or blank: ");
            String reason = getStringInput("New reason or blank: ");

            LocalDateTime newStarts = null;
            LocalDateTime newEnds = null;
            String newReason = null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (!startsStr.isEmpty()) {
                newStarts = LocalDateTime.parse(startsStr, formatter);
            }

            if (!endsStr.isEmpty()) {
                newEnds = LocalDateTime.parse(endsStr, formatter);
            }

            if (!reason.isEmpty()) {
                newReason = reason;
            }

            if (newStarts == null && newEnds == null && newReason == null) {
                printInfo("No changes made.");
                pauseForUser();
                return;
            }

            appointmentRepo.updateAppointment(appointmentID, newStarts, newEnds, newReason);

            printSuccess("Appointment updated successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to update appointment: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Delete an appointment
     */
    private void deleteAppointment() {
        try {
            String appointmentID = getStringInput("\nEnter Appointment ID to delete: ");

            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            if (!getConfirmation("\nAre you sure you want to delete appointment " + appointmentID + "?")) {
                printInfo("Deletion cancelled.");
                pauseForUser();
                return;
            }

            appointmentRepo.deleteAppointment(appointmentID);

            printSuccess("Appointment deleted successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to delete appointment: " + e.getMessage());
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
     * Manage observations (CRUD)
     */
    public void manageObservations() {
        while (true) {
            clearScreen();
            printHeader("MANAGE OBSERVATIONS");

            printMenuOptions(
                    "Create Observation",
                    "View Patient Observations",
                    "Update Observation",
                    "Delete Observation",
                    "View Observation Details"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    createObservation();
                    break;
                case 2:
                    viewPatientObservations();
                    break;
                case 3:
                    updateObservation();
                    break;
                case 4:
                    deleteObservation();
                    break;
                case 5:
                    viewObservationDetails();
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
     * Create a new observation
     */
    private void createObservation() {
        try {
            clearScreen();
            printHeader("CREATE OBSERVATION");

            String observationID = getStringInput("\nEnter Observation ID (e.g., OB0001): ");
            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            String appointmentID = getStringInput("Enter Appointment ID: ");
            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nEnter observation date and time (format: yyyy-MM-dd HH:mm)");
            String observedAtStr = getStringInput("Date/Time: ");

            String text = getStringInput("\nObservation text: ");
            if (text.isEmpty()) {
                printError("Observation text cannot be empty.");
                pauseForUser();
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime observedAt = LocalDateTime.parse(observedAtStr, formatter);

            Observation observation = new Observation(observationID, observedAt, text);
            observationRepo.createObservation(observation, appointmentID);

            printSuccess("Observation created successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to create observation: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View observations for a patient
     */
    private void viewPatientObservations() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            List<model.neo4j.Patient> patients = patientRepo.listByDoctor(doctorID);
            if (patients.isEmpty()) {
                printInfo("You have no patients assigned.");
                pauseForUser();
                return;
            }

            String[] patientOptions = new String[patients.size()];
            for (int i = 0; i < patients.size(); i++) {
                model.neo4j.Patient p = patients.get(i);
                patientOptions[i] = p.getPatientPN() + " - " + p.getFirstName() + " " + p.getLastName();
            }

            int choice = selectFromList("SELECT PATIENT", patientOptions);
            if (choice == 0) return;

            String patientPN = patients.get(choice - 1).getPatientPN();

            List<ObservationWithContext> observations = observationRepo.listByPatient(patientPN);

            clearScreen();
            printHeader("OBSERVATIONS FOR " + patientPN);

            if (observations.isEmpty()) {
                printInfo("No observations found for this patient.");
                pauseForUser();
                return;
            }

            printDivider();
            for (ObservationWithContext obs : observations) {
                System.out.println("\nID: " + obs.observationID());
                System.out.println("Date: " + formatDateTime(obs.observedAt()));
                System.out.println("Appointment: " + obs.appointmentID());
                System.out.println("Text: " + obs.text());
                printDivider();
            }

            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load observations: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Update an observation
     */
    private void updateObservation() {
        try {
            String observationID = getStringInput("\nEnter Observation ID to update: ");

            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("UPDATE OBSERVATION: " + observationID);

            System.out.println("\nLeave blank to keep current value.");

            String observedAtStr = getStringInput("\nNew date/time (yyyy-MM-dd HH:mm) or blank: ");
            String text = getStringInput("New observation text or blank: ");

            LocalDateTime newObservedAt = null;
            String newText = null;

            if (!observedAtStr.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                newObservedAt = LocalDateTime.parse(observedAtStr, formatter);
            }

            if (!text.isEmpty()) {
                newText = text;
            }

            if (newObservedAt == null && newText == null) {
                printInfo("No changes made.");
                pauseForUser();
                return;
            }

            observationRepo.updateObservation(observationID, newObservedAt, newText);

            printSuccess("Observation updated successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to update observation: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Delete an observation
     */
    private void deleteObservation() {
        try {
            String observationID = getStringInput("\nEnter Observation ID to delete: ");

            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            if (!getConfirmation("\nAre you sure you want to delete observation " + observationID + "?")) {
                printInfo("Deletion cancelled.");
                pauseForUser();
                return;
            }

            observationRepo.deleteObservation(observationID);

            printSuccess("Observation deleted successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to delete observation: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View observation details
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
     * Manage diagnoses (CRUD)
     */
    public void manageDiagnoses() {
        while (true) {
            clearScreen();
            printHeader("MANAGE DIAGNOSES");

            printMenuOptions(
                    "Create Diagnosis",
                    "View Patient Diagnoses",
                    "Update Diagnosis",
                    "Delete Diagnosis",
                    "View Diagnosis Details"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    createDiagnosis();
                    break;
                case 2:
                    viewPatientDiagnoses();
                    break;
                case 3:
                    updateDiagnosis();
                    break;
                case 4:
                    deleteDiagnosis();
                    break;
                case 5:
                    viewDiagnosisDetails();
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
     * Create a new diagnosis
     */
    private void createDiagnosis() {
        try {
            clearScreen();
            printHeader("CREATE DIAGNOSIS");

            String diagnosisID = getStringInput("\nEnter Diagnosis ID (e.g., DG0001): ");
            if (diagnosisID.isEmpty()) {
                printError("Diagnosis ID cannot be empty.");
                pauseForUser();
                return;
            }

            String observationID = getStringInput("Enter Observation ID: ");
            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nSelect severity:");
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

            String details = getStringInput("\nDiagnosis details: ");
            if (details.isEmpty()) {
                printError("Details cannot be empty.");
                pauseForUser();
                return;
            }

            Diagnosis diagnosis = new Diagnosis(diagnosisID, severity.getLabel(), details);
            diagnosisRepo.createDiagnosis(diagnosis, observationID);

            printSuccess("Diagnosis created successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to create diagnosis: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View diagnoses for a patient by severity
     */
    private void viewPatientDiagnoses() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            List<model.neo4j.Patient> patients = patientRepo.listByDoctor(doctorID);
            if (patients.isEmpty()) {
                printInfo("You have no patients assigned.");
                pauseForUser();
                return;
            }

            String[] patientOptions = new String[patients.size()];
            for (int i = 0; i < patients.size(); i++) {
                model.neo4j.Patient p = patients.get(i);
                patientOptions[i] = p.getPatientPN() + " - " + p.getFirstName() + " " + p.getLastName();
            }

            int choice = selectFromList("SELECT PATIENT", patientOptions);
            if (choice == 0) return;

            String patientPN = patients.get(choice - 1).getPatientPN();

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
                System.out.println("Observation: " + diag.observationID());
                printDivider();
            }

            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load diagnoses: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Update a diagnosis
     */
    private void updateDiagnosis() {
        try {
            String diagnosisID = getStringInput("\nEnter Diagnosis ID to update: ");

            if (diagnosisID.isEmpty()) {
                printError("Diagnosis ID cannot be empty.");
                pauseForUser();
                return;
            }

            clearScreen();
            printHeader("UPDATE DIAGNOSIS: " + diagnosisID);

            System.out.println("\nLeave blank to keep current value.");

            System.out.println("\nNew severity (or 0 to keep current):");
            System.out.println("1. Hög (High)");
            System.out.println("2. Medel (Medium)");
            System.out.println("3. Låg (Low)");
            System.out.println("0. Keep current");

            int severityChoice = getIntInput("Choice: ");
            Severity newSeverity = null;

            switch (severityChoice) {
                case 1:
                    newSeverity = Severity.HÖG;
                    break;
                case 2:
                    newSeverity = Severity.MEDEL;
                    break;
                case 3:
                    newSeverity = Severity.LÅG;
                    break;
                case 0:
                    break;
                default:
                    printError("Invalid choice.");
                    pauseForUser();
                    return;
            }

            String details = getStringInput("\nNew details or blank: ");
            String newDetails = details.isEmpty() ? null : details;

            if (newSeverity == null && newDetails == null) {
                printInfo("No changes made.");
                pauseForUser();
                return;
            }

            diagnosisRepo.updateDiagnosis(diagnosisID, newSeverity, newDetails);

            printSuccess("Diagnosis updated successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to update diagnosis: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Delete a diagnosis
     */
    private void deleteDiagnosis() {
        try {
            String diagnosisID = getStringInput("\nEnter Diagnosis ID to delete: ");

            if (diagnosisID.isEmpty()) {
                printError("Diagnosis ID cannot be empty.");
                pauseForUser();
                return;
            }

            if (!getConfirmation("\nAre you sure you want to delete diagnosis " + diagnosisID + "?")) {
                printInfo("Deletion cancelled.");
                pauseForUser();
                return;
            }

            diagnosisRepo.deleteDiagnosis(diagnosisID);

            printSuccess("Diagnosis deleted successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to delete diagnosis: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View diagnosis details
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
     * Manage notes (CRUD)
     */
    public void manageNotes() {
        while (true) {
            clearScreen();
            printHeader("MANAGE NOTES");

            printMenuOptions(
                    "Add Note",
                    "Update Note",
                    "Delete Note",
                    "View Note Details"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    addNote();
                    break;
                case 2:
                    updateNote();
                    break;
                case 3:
                    deleteNote();
                    break;
                case 4:
                    viewNoteDetails();
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
     * Add a new note
     */
    private void addNote() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("ADD NOTE");

            String noteID = getStringInput("\nEnter Note ID (e.g., NT0001): ");
            if (noteID.isEmpty()) {
                printError("Note ID cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nNote about:");
            System.out.println("1. Appointment");
            System.out.println("2. Observation");
            System.out.println("3. Diagnosis");

            int typeChoice = getIntInput("Choice: ");
            String aboutType;

            switch (typeChoice) {
                case 1:
                    aboutType = "APPOINTMENT";
                    break;
                case 2:
                    aboutType = "OBSERVATION";
                    break;
                case 3:
                    aboutType = "DIAGNOSIS";
                    break;
                default:
                    printError("Invalid choice.");
                    pauseForUser();
                    return;
            }

            String aboutID = getStringInput("\nEnter " + aboutType + " ID: ");
            if (aboutID.isEmpty()) {
                printError(aboutType + " ID cannot be empty.");
                pauseForUser();
                return;
            }

            String description = getStringInput("\nNote description: ");
            if (description.isEmpty()) {
                printError("Description cannot be empty.");
                pauseForUser();
                return;
            }

            Note note = new Note(noteID, description);
            noteRepo.addNote(note, doctorID, aboutType, aboutID);

            printSuccess("Note added successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to add note: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Update a note
     */
    private void updateNote() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            String noteID = getStringInput("\nEnter Note ID to update: ");

            if (noteID.isEmpty()) {
                printError("Note ID cannot be empty.");
                pauseForUser();
                return;
            }

            String newDescription = getStringInput("New description: ");

            if (newDescription.isEmpty()) {
                printError("Description cannot be empty.");
                pauseForUser();
                return;
            }

            noteRepo.updateNote(noteID, newDescription, doctorID);

            printSuccess("Note updated successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to update note: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * Delete a note
     */
    private void deleteNote() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            String noteID = getStringInput("\nEnter Note ID to delete: ");

            if (noteID.isEmpty()) {
                printError("Note ID cannot be empty.");
                pauseForUser();
                return;
            }

            if (!getConfirmation("\nAre you sure you want to delete note " + noteID + "?")) {
                printInfo("Deletion cancelled.");
                pauseForUser();
                return;
            }

            noteRepo.deleteNote(noteID, doctorID);

            printSuccess("Note deleted successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to delete note: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View note details
     */
    private void viewNoteDetails() {
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