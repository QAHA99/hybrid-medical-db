package cli;

import dao.neo4j.implementations.AppointmentNeo4j;
import dao.neo4j.implementations.PatientNeo4j;
import model.mongo.User;
import service.AuthService;
import service.MessagingService;
import org.neo4j.driver.Driver;

import java.util.List;

import static dao.neo4j.implementations.AppointmentNeo4j.AppointmentWithDetails;

public class PatientMenuCLI extends CLIMenu {
    private final AuthService authService;
    private final Driver neo4jDriver;
    private final PatientNeo4j patientRepo;
    private final AppointmentNeo4j appointmentRepo;
    private final MessagingCLI messagingCLI;

    public PatientMenuCLI(AuthService authService, Driver neo4jDriver, MessagingService messagingService) {
        this.authService = authService;
        this.neo4jDriver = neo4jDriver;
        this.patientRepo = new PatientNeo4j(neo4jDriver);
        this.appointmentRepo = new AppointmentNeo4j(neo4jDriver);
        this.messagingCLI = new MessagingCLI(authService, messagingService);
    }

    /**
     * View patient's own summary
     */
    public void viewMySummary() {
        try {
            User currentUser = authService.getCurrentUser();
            String patientPN = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY SUMMARY");

            String summary = patientRepo.getPatientSummary(patientPN);
            System.out.println("\n" + summary);

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load summary: " + e.getMessage());
            pauseForUser();
        }
    }

    /**
     * View patient's appointments
     */
    public void viewMyAppointments() {
        while (true) {
            try {
                User currentUser = authService.getCurrentUser();
                String patientPN = currentUser.getPersonRef();

                clearScreen();
                printHeader("MY APPOINTMENTS");

                // Ask if user wants to include history
                boolean includeHistory = getConfirmation("\nInclude past appointments?");

                List<AppointmentWithDetails> appointments = appointmentRepo.listByPatient(patientPN, includeHistory);

                if (appointments.isEmpty()) {
                    printInfo("No appointments found.");
                    pauseForUser();
                    return;
                }

                // Display appointments
                printDivider();
                System.out.println(String.format("%-12s %-20s %-30s %-20s",
                        "ID", "Date & Time", "Doctor", "Reason"));
                printDivider();

                for (AppointmentWithDetails apt : appointments) {
                    String doctorName = "Dr. " + apt.doctorFirstName() + " " + apt.doctorLastName();
                    String dateTime = formatDateTime(apt.starts());
                    String reason = apt.reason();
                    if (reason.length() > 30) {
                        reason = reason.substring(0, 27) + "...";
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
                    viewAppointmentDetails(appointments);
                } else if (choice == 0) {
                    return;
                } else {
                    printError("Invalid option.");
                    pauseForUser();
                }

            } catch (Exception e) {
                printError("Failed to load appointments: " + e.getMessage());
                pauseForUser();
                return;
            }
        }
    }

    /**
     * View detailed information about a specific appointment
     */
    private void viewAppointmentDetails(List<AppointmentWithDetails> appointments) {
        String appointmentID = getStringInput("\nEnter Appointment ID: ");

        if (appointmentID.isEmpty()) {
            printError("Appointment ID cannot be empty.");
            pauseForUser();
            return;
        }

        try {
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
     * Show messaging menu
     */
    public void showMessagingMenu() {
        messagingCLI.showMessagingMenu();
    }
}