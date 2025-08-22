package cosacosa.medimate.controller;

import cosacosa.medimate.dto.PrescriptionListItemResponse;
import cosacosa.medimate.dto.PrescriptionRequest;
import cosacosa.medimate.dto.PrescriptionResponse;
import cosacosa.medimate.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrescriptionResponse> postPrescription(@ModelAttribute PrescriptionRequest form) {
        try {
            PrescriptionResponse result = prescriptionService.createPrescription(form);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            // 콘솔에 에러 로그를 남겨서 원인을 파악하기 쉽게 합니다.
            e.printStackTrace();
            // 클라이언트에게는 서버 내부 오류가 발생했다고 알려줍니다.
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionListItemResponse>> getPrescriptionList() {
        List<PrescriptionListItemResponse> result = prescriptionService.readPrescriptionList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescription(@PathVariable Long prescriptionId) {
        PrescriptionResponse result = prescriptionService.readPrescription();
        return ResponseEntity.ok(result);
    }
}
