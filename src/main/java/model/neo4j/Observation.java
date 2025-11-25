package model.neo4j;

import java.time.LocalDateTime;

public class Observation {

    // Instance variables
    private final String observationID;
    private LocalDateTime observedAt;
    private String text;

    // Constructor
    public Observation(String observationID, LocalDateTime observedAt, String text) {
        this.observationID = observationID;
        this.observedAt = observedAt;
        this.text = text;
    }

    // Getters
    public String getObservationID() {return observationID;}
    public LocalDateTime getObservedAt() {return observedAt;}
    public String getText() {return text;}

    // Setters
    public void setObservedAt(LocalDateTime observedAt) {this.observedAt = observedAt;}
    public void setText(String text) {this.text = text;}
}
