package model.neo4j;

import model.enums.Severity;

public class Diagnosis {

    // Instance Variables
    private final String diagnosisID;
    private Severity severity;
    private String details;

    // Constructor
    public Diagnosis(String diagnosisID, String severity, String details) {
        this.diagnosisID = diagnosisID;
        this.severity = Severity.fromString(severity);
        this.details = details;
    }

    // Getters
    public String getDiagnosisID() {return diagnosisID;}
    public Severity getSeverity() {return severity;}
    public String getDetails() {return details;}

    // Setters
    public void setSeverity(Severity severity) {this.severity = severity;}
    public void setDetails(String details) {this.details = details;}
}
