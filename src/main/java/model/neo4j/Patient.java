package model.neo4j;

import model.enums.SexOption;

public class Patient {

    // Instance Variables
    private final String patientPN;
    private String firstName;
    private String lastName;
    private SexOption sex;

    // Constructor
    public Patient(String patientPN, String firstName, String lastName, String sex) {
        this.patientPN = patientPN;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = SexOption.fromString(sex);
    }

    // Getters
    public String getPatientPN() {return patientPN;}
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public SexOption getSex() {return sex;}

    // Setters
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setSex(SexOption sex) {this.sex = sex;}
}
