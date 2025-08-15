package cosacosa.medimate.dto;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class PrescriptionRequest {
    private String language;
    private MultipartFile image;
}
