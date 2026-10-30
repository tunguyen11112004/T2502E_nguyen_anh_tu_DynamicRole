package org.fp.dynamicrole.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findByLecturer_Username(String username, Pageable pageable);

    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findByLecturer_UsernameAndStatus(String username, AssignmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "lecturer")
    Optional<Assignment> findByIdAndLecturer_Username(Long id, String username);

    @EntityGraph(attributePaths = "lecturer")
    Optional<Assignment> findByIdAndEnrollments_Student_UsernameAndStatusIn(
            Long id, String username, Collection<AssignmentStatus> statuses);

    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findByEnrollments_Student_UsernameAndStatus(
            String username, AssignmentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findByStatus(AssignmentStatus status, Pageable pageable);
}
