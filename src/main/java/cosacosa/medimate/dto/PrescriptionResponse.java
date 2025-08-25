package cosacosa.medimate.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PrescriptionResponse {
    private Long id;
    private String title;
    private String content;
    private String koreanContent;
    private String contentMd;
    private String koreanContentMd;
    private LocalDate createdAt;

    public PrescriptionResponse(Long id, String title, String content, String koreanContent, String contentMd, String koreanContentMd, LocalDate createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.koreanContent = koreanContent;
        this.contentMd = contentMd;
        this.koreanContentMd = koreanContentMd;
        this.createdAt = createdAt;
    }
}
