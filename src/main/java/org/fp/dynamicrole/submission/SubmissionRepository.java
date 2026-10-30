package org.fp.dynamicrole.submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @EntityGraph(attributePaths = {"assignment", "student"})
    Optional<Submission> findByAssignment_IdAndStudent_Username(Long assignmentId, String username);

    @EntityGraph(attributePaths = {"assignment", "assignment.lecturer", "student"})
    Optional<Submission> findByIdAndStudent_Username(Long id, String username);

    @EntityGraph(attributePaths = {"assignment", "assignment.lecturer", "student"})
    Optional<Submission> findByIdAndAssignment_IdAndAssignment_Lecturer_Username(
            Long id, Long assignmentId, String username);

    @EntityGraph(attributePaths = {"student", "assignment"})
    Page<Submission> findByAssignment_IdAndAssignment_Lecturer_Username(
            Long assignmentId, String username, Pageable pageable);

    long countByScoreIsNotNull();
}
