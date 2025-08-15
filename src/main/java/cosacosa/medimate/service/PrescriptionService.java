package cosacosa.medimate.service;

import cosacosa.medimate.domain.Prescription;
import cosacosa.medimate.dto.PrescriptionListItemResponse;
import cosacosa.medimate.dto.PrescriptionRequest;
import cosacosa.medimate.dto.PrescriptionResponse;
import cosacosa.medimate.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionResponse createPrescription(PrescriptionRequest dto) {
        Prescription prescription = prescriptionRepository.findById(1L).orElseThrow(() -> new RuntimeException("해당 id의 prescription을 찾을 수 없습니다."));
        // AI 로직
        PrescriptionResponse response = new PrescriptionResponse(
                prescription.getId(),
                prescription.getTitle(),
                prescription.getContent(),
                prescription.getKoreanContent(),
                prescription.getCreatedAt(),
                prescription.getImages()
        );
        return response;
    }

    public List<PrescriptionListItemResponse> readPrescriptionList() {
        List<PrescriptionListItemResponse> response = prescriptionRepository.findAll()
                .stream().map(prescription -> new PrescriptionListItemResponse(
                        prescription.getId(),
                        prescription.getTitle(),
                        prescription.getCreatedAt()
                )).toList();
        return response;
    }

    public PrescriptionResponse readPrescription() {
        Prescription prescription = prescriptionRepository.findById(1L).orElseThrow(() -> new RuntimeException("해당 id의 prescription을 찾을 수 없습니다."));
        PrescriptionResponse response = new PrescriptionResponse(
                prescription.getId(),
                prescription.getTitle(),
                prescription.getContent(),
                prescription.getKoreanContent(),
                prescription.getCreatedAt(),
                prescription.getImages()
        );
        return response;
    }
}
