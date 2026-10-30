package org.fp.dynamicrole.submission;

import org.fp.dynamicrole.course.Assignment;
import org.fp.dynamicrole.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private AppUser student;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(length = 2000)
    private String feedback;

    protected Submission() {
    }

    public Submission(Assignment assignment, AppUser student, String content, Instant submittedAt) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = submittedAt;
    }

    public void resubmit(String content, Instant submittedAt) {
        this.content = content;
        this.submittedAt = submittedAt;
    }

    public void grade(BigDecimal score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public AppUser getStudent() {
        return student;
    }

    public String getContent() {
        return content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
