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
    private String image = ""; // 의약품 공공 api에서 가져온 이미지 파일의 url

    public PrescriptionResponse(Long id, String title, String content, String koreanContent, LocalDate createdAt, String image) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.koreanContent = koreanContent;
        this.createdAt = createdAt;
        this.image = image;
    }
}
