package cosacosa.medimate.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PrescriptionResponse {
    private Long id;
    private String title;
    private String content;
    private String koreanContent;
    private LocalDate createdAt;
    private String image; // 의약품 공공 api에서 가져온 이미지 파일의 url
}
