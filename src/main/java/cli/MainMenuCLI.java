package cli;

import model.mongo.User;
import service.AuthService;
import service.MessagingService;
import org.neo4j.driver.Driver;

public class MainMenuCLI extends CLIMenu {
    private final AuthService authService;
    private final Driver neo4jDriver;
    private final MessagingService messagingService;

    // Submenu instances
    private PatientMenuCLI patientMenu;
    private DoctorMenuCLI doctorMenu;
    private ReceptionistMenuCLI receptionistMenu;

    public MainMenuCLI(AuthService authService, Driver neo4jDriver) {
        this.authService = authService;
        this.neo4jDriver = neo4jDriver;
        this.messagingService = new MessagingService(authService, neo4jDriver);
    }

    /**
     * Show main menu based on user role
     */
    public void show() {
        User currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            printError("No user logged in.");
            return;
        }

        switch (currentUser.getRole()) {
            case PATIENT:
                showPatientMenu();
                break;
            case DOCTOR:
                showDoctorMenu();
                break;
            case RECEPTIONIST:
                showReceptionistMenu();
                break;
            default:
                printError("Unknown user role.");
        }
    }

    /**
     * Patient main menu
     */
    private void showPatientMenu() {
        if (patientMenu == null) {
            patientMenu = new PatientMenuCLI(authService, neo4jDriver, messagingService);
        }

        while (authService.isLoggedIn()) {
            clearScreen();
            User user = authService.getCurrentUser();
            printHeader("PATIENT MENU - " + user.getFirstName() + " " + user.getLastName());

            printMenuOptions(
                    "View My Summary",
                    "View My Appointments",
                    "Messages",
                    "Logout"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    patientMenu.viewMySummary();
                    break;
                case 2:
                    patientMenu.viewMyAppointments();
                    break;
                case 3:
                    patientMenu.showMessagingMenu();
                    break;
                case 4:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        printSuccess("Logged out successfully.");
                        pauseForUser();
                        return;
                    }
                    break;
                case 0:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        return;
                    }
                    break;
                default:
                    printError("Invalid option. Please try again.");
                    pauseForUser();
            }
        }
    }

    /**
     * Doctor main menu
     */
    private void showDoctorMenu() {
        if (doctorMenu == null) {
            doctorMenu = new DoctorMenuCLI(authService, neo4jDriver, messagingService);
        }

        while (authService.isLoggedIn()) {
            clearScreen();
            User user = authService.getCurrentUser();
            printHeader("DOCTOR MENU - Dr. " + user.getFirstName() + " " + user.getLastName());

            printMenuOptions(
                    "View My Patients",
                    "View Patient Summary",
                    "View My Appointments",
                    "Manage Appointments",
                    "Manage Observations",
                    "Manage Diagnoses",
                    "Manage Notes",
                    "Messages",
                    "Logout"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    doctorMenu.viewMyPatients();
                    break;
                case 2:
                    doctorMenu.viewPatientSummary();
                    break;
                case 3:
                    doctorMenu.viewMyAppointments();
                    break;
                case 4:
                    doctorMenu.manageAppointments();
                    break;
                case 5:
                    doctorMenu.manageObservations();
                    break;
                case 6:
                    doctorMenu.manageDiagnoses();
                    break;
                case 7:
                    doctorMenu.manageNotes();
                    break;
                case 8:
                    doctorMenu.showMessagingMenu();
                    break;
                case 9:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        printSuccess("Logged out successfully.");
                        pauseForUser();
                        return;
                    }
                    break;
                case 0:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        return;
                    }
                    break;
                default:
                    printError("Invalid option. Please try again.");
                    pauseForUser();
            }
        }
    }

    /**
     * Receptionist main menu
     */
    private void showReceptionistMenu() {
        if (receptionistMenu == null) {
            receptionistMenu = new ReceptionistMenuCLI(authService, neo4jDriver, messagingService);
        }

        while (authService.isLoggedIn()) {
            clearScreen();
            printHeader("RECEPTIONIST MENU - " + authService.getCurrentUser().getName());

            printMenuOptions(
                    "View All Patients",
                    "View All Doctors",
                    "View All Appointments",
                    "View Patient Summary",
                    "View Medical Data (Read-Only)",
                    "Messages",
                    "Logout"
            );

            int choice = getIntInput("");

            switch (choice) {
                case 1:
                    receptionistMenu.viewAllPatients();
                    break;
                case 2:
                    receptionistMenu.viewAllDoctors();
                    break;
                case 3:
                    receptionistMenu.viewAllAppointments();
                    break;
                case 4:
                    receptionistMenu.viewPatientSummary();
                    break;
                case 5:
                    receptionistMenu.viewMedicalData();
                    break;
                case 6:
                    receptionistMenu.showMessagingMenu();
                    break;
                case 7:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        printSuccess("Logged out successfully.");
                        pauseForUser();
                        return;
                    }
                    break;
                case 0:
                    if (new AuthCLI(authService).confirmLogout()) {
                        authService.logout();
                        return;
                    }
                    break;
                default:
                    printError("Invalid option. Please try again.");
                    pauseForUser();
            }
        }
    }
}