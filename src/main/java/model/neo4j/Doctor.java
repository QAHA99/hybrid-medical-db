package model.neo4j;

public class Doctor {

    // Instance variables
    private String doctorID;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    // Constructor
    public Doctor(String doctorID, String firstName, String lastName, String phoneNumber) {
        this.doctorID = doctorID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public Doctor() {
    }

    // Getters
    public String getDoctorID() {return doctorID;}
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public String getPhoneNumber() {return phoneNumber;}

    // Setters
    public void setDoctorID(String doctorID) {this.doctorID = doctorID;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}
}
