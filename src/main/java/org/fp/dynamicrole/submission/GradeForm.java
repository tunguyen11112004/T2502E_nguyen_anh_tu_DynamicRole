package org.fp.dynamicrole.submission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class GradeForm {

    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.00", message = "Điểm từ 0 đến 10")
    @DecimalMax(value = "10.00", message = "Điểm từ 0 đến 10")
    @Digits(integer = 2, fraction = 2, message = "Điểm tối đa 2 chữ số thập phân")
    private BigDecimal score;

    @NotBlank(message = "Vui lòng nhập nhận xét")
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    private String feedback = "";

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
