package org.fp.dynamicrole.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TaskForm {

    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title = "";

    @NotNull
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description = "";

    @NotNull(message = "Vui lòng chọn hạn hoàn thành")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate = LocalDate.now();

    @NotNull(message = "Vui lòng chọn trạng thái")
    private TaskStatus status = TaskStatus.TODO;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Hạn hợp lệ nếu không trước hôm nay, hoặc nếu giữ nguyên hạn hiện có
     * (current = null khi tạo mới). Cho phép đánh dấu DONE việc đã quá hạn.
     */
    public boolean isDueDateAcceptable(LocalDate current, LocalDate today) {
        return dueDate == null
                || !dueDate.isBefore(today)
                || dueDate.equals(current);
    }

    public static TaskForm from(TaskView task) {
        TaskForm f = new TaskForm();
        f.title = task.title();
        f.description = task.description();
        f.dueDate = task.dueDate();
        f.status = task.status();
        return f;
    }
}

