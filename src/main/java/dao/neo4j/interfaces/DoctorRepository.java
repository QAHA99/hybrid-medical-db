package dao.neo4j.interfaces;

public interface DoctorRepository {

    String findByID(String doctorID) throws Exception;

    String searchByName(String firstName, String lastName) throws Exception;

    String getDoctorClinicID(String doctorID) throws Exception;
}