package cosacosa.medimate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;

// 목록 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecheckListItemResponseDto {
    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
}
