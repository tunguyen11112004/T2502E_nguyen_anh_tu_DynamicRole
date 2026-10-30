package org.fp.dynamicrole.task;

import java.time.LocalDate;

public record TaskView(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        String ownerUsername) {

    public static TaskView from(Task task) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getOwner().getUsername());
    }
}

