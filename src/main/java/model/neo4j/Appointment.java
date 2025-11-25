package model.neo4j;

import java.time.LocalDateTime;

public class Appointment {

    // Instance Variables
    private final String appointmentID;
    private LocalDateTime starts;
    private LocalDateTime ends;
    private String reason;

    // Constructor
    public Appointment(String appointmentID, LocalDateTime starts, LocalDateTime ends, String reason) {
        this.appointmentID = appointmentID;
        this.starts = starts;
        this.ends = ends;
        this.reason = reason;
    }

    // Getters
    public String getAppointmentID() {return appointmentID;}
    public LocalDateTime getStarts() {return starts;}
    public LocalDateTime getEnds() {return ends;}
    public String getReason() {return reason;}

    // Setters
    public void setStarts(LocalDateTime starts) {this.starts = starts;}
    public void setEnds(LocalDateTime ends) {this.ends = ends;}
    public void setReason(String reason) {this.reason = reason;}
}
