package cosacosa.medimate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckListItemResponse {
    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
}
