package org.fp.dynamicrole.course;

import java.time.Instant;

public record AssignmentView(
        Long id,
        String title,
        String description,
        Instant deadline,
        String deadlineText,
        AssignmentStatus status,
        String lecturerUsername) {
}
