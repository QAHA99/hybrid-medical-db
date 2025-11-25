package dao;

import dao.neo4j.implementations.DepartmentNeo4j;
import dao.neo4j.interfaces.DepartmentRepository;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.List;
import java.util.Scanner;

public class DepartmentTest {
    public static void main(String[] args) {
        String dbUri = "neo4j+s://423bcb80.databases.neo4j.io";
        String dbUser = "neo4j";
        String dbPassword = "dPLxUBqct9iyrv-35nq6x8SnQbesDy9prQnCUxRM0UE";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a clinic: ");
        String input = scanner.nextLine();

        try (Driver driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword))){

            DepartmentNeo4j repo = new DepartmentNeo4j(driver);

            List<DepartmentRepository.DepartmentsWithDoctors> results = repo.listByClinic(input);

            System.out.println("\nFound " + results.size() + " departments");

            for (DepartmentRepository.DepartmentsWithDoctors dept : results) {
                System.out.println("\n" + dept.departmentName() + " - " + dept.doctors().size() + " doctors");
                for (var doctor : dept.doctors()) {
                    System.out.println("- " + doctor.getFirstName() + " " + doctor.getLastName());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        scanner.close();

    }
}
