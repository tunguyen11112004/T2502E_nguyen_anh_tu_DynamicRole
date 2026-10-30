package org.fp.dynamicrole.course;

import org.fp.dynamicrole.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assignment_students")
@IdClass(AssignmentStudentKey.class)
public class AssignmentStudent {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    protected AssignmentStudent() {
    }

    public AssignmentStudent(Assignment assignment, AppUser student) {
        this.assignment = assignment;
        this.student = student;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public AppUser getStudent() {
        return student;
    }
}
