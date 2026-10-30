package org.fp.dynamicrole.course;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentStudentKey implements Serializable {

    private Long assignment;
    private Long student;

    public AssignmentStudentKey() {
    }

    public AssignmentStudentKey(Long assignment, Long student) {
        this.assignment = assignment;
        this.student = student;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentStudentKey key)) {
            return false;
        }
        return Objects.equals(assignment, key.assignment) && Objects.equals(student, key.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignment, student);
    }
}
