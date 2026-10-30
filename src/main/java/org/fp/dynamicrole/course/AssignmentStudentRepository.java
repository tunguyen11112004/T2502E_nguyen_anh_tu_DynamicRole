package org.fp.dynamicrole.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentStudentRepository extends JpaRepository<AssignmentStudent, AssignmentStudentKey> {

    boolean existsByAssignment_IdAndStudent_Username(Long assignmentId, String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AssignmentStudent link where link.assignment.id = :assignmentId")
    void deleteByAssignmentId(@Param("assignmentId") Long assignmentId);
}
