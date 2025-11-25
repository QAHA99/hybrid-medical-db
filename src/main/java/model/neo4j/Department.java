package model.neo4j;

public class Department {

    // Instance Variables
    private final String departmentID;
    private String name;

    // Constructor
    public Department(String departmentID, String name) {
        this.departmentID = departmentID;
        this.name = name;
    }

    // Getters
    public String getDepartmentID() {return departmentID;}
    public String getName() {return name;}
}
