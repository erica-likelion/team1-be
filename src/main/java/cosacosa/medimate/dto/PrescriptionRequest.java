package cosacosa.medimate.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PrescriptionRequest {
    private String language;
    private MultipartFile image;
}
