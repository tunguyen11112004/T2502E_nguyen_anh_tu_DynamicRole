package org.fp.dynamicrole.course;

import org.fp.dynamicrole.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false, updatable = false)
    private AppUser lecturer;

    @OneToMany(mappedBy = "assignment")
    private Set<AssignmentStudent> enrollments = new HashSet<>();

    protected Assignment() {
    }

    public Assignment(AppUser lecturer, String title, String description, Instant deadline, AssignmentStatus status) {
        this.lecturer = lecturer;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
    }

    public void changeDetails(String title, String description, Instant deadline) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
    }

    public void open() {
        this.status = AssignmentStatus.OPEN;
    }

    public void close() {
        this.status = AssignmentStatus.CLOSED;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public AppUser getLecturer() {
        return lecturer;
    }
}
