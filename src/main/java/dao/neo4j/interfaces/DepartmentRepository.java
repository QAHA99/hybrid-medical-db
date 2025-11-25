package dao.neo4j.interfaces;

import model.neo4j.Doctor;

import java.util.List;

public interface DepartmentRepository {

    record DepartmentsWithDoctors (String clinicID, String departmentID, String departmentName,
                                   List<Doctor> doctors){};

    /** List the departments in the clinic and their respective doctors */
    List <DepartmentsWithDoctors> listByClinic (String clinicID);
}
