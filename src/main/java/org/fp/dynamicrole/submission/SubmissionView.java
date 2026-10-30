package org.fp.dynamicrole.submission;

import java.math.BigDecimal;
import java.time.Instant;

public record SubmissionView(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        String studentUsername,
        String content,
        Instant submittedAt,
        String submittedAtText,
        BigDecimal score,
        String feedback) {
}
