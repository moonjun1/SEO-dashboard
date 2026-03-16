package com.seodashboard.api.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReportCreateRequest(

        @NotBlank(message = "Report type is required")
        String type,

        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotNull(message = "Period start is required")
        LocalDate periodStart,

        @NotNull(message = "Period end is required")
        LocalDate periodEnd
) {

    public String titleOrDefault() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return type + " Report (" + periodStart + " ~ " + periodEnd + ")";
    }
}
