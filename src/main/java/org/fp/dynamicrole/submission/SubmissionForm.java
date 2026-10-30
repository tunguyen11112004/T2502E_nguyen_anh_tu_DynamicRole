package org.fp.dynamicrole.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmissionForm {

    @NotBlank(message = "Vui lòng nhập nội dung hoặc liên kết bài làm")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String content = "";

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
