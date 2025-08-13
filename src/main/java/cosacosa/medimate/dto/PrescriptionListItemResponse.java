package cosacosa.medimate.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PrescriptionListItemResponse {
    private Long id;
    private String title;
    private LocalDate createdAt;
}
