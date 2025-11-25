package model.neo4j;

public class Clinic {

    // Instance Variables
    private final String clinicId;
    private String name;
    private String adress;
    private String phoneNumber;

    // Constructor
    public Clinic(String clinicId, String name, String adress, String phoneNumber) {
        this.clinicId = clinicId;
        this.name = name;
        this.adress = adress;
        this.phoneNumber = phoneNumber;
    }

    // Getters
    public String getClinicId() {return clinicId;}
    public String getName() {return name;}
    public String getAdress() {return adress;}
    public String getPhoneNumber() {return phoneNumber;}
}
