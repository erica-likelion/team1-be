package cosacosa.medimate.controller;

import cosacosa.medimate.dto.PrescriptionListItemResponse;
import cosacosa.medimate.dto.PrescriptionRequest;
import cosacosa.medimate.dto.PrescriptionResponse;
import cosacosa.medimate.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrescriptionResponse> postPrescription(@ModelAttribute PrescriptionRequest form) {
        PrescriptionResponse result = prescriptionService.createPrescription(form);
        return ResponseEntity.ok(result);
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
