package cosacosa.medimate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosacosa.medimate.domain.Prescription;
import cosacosa.medimate.dto.PrescriptionListItemResponse;
import cosacosa.medimate.dto.PrescriptionRequest;
import cosacosa.medimate.dto.PrescriptionResponse;
import cosacosa.medimate.repository.PrescriptionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final FileUploadService fileUploadService;

    private final WebClient openAiWebClient;
    private final WebClient upstageWebClient;

    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;
    @Value("${upstage.api.key}")
    private String upstageApiKey;

    public PrescriptionResponse createPrescription(PrescriptionRequest dto) throws IOException {
        String imageUrl = fileUploadService.uploadFile(dto.getImage());
        log.info("Uploaded prescription image to S3: {}", imageUrl);

        try {
            log.info("Step 1: Extracting medicine names using Upstage API...");
            String base64Image = encodeImageToBase64(dto.getImage());
            Map<String, Object> extractionRequestBody = createUpstageExtractionRequest(base64Image);

            String extractionResponse = callUpstageApi(extractionRequestBody);
            List<String> medicineNameList = parseMedicineNamesFromUpstageResponse(extractionResponse);

            if (medicineNameList == null || medicineNameList.isEmpty()) {
                throw new RuntimeException("처방전 이미지에서 약품명을 추출하지 못했습니다.");
            }

            String medicineNames = String.join("\n", medicineNameList);
            log.info("Successfully extracted medicine names:\n{}", medicineNames);


            log.info("Step 2: Generating detailed information for extracted names...");
            String detailsPrompt = createDetailsPrompt(medicineNames, dto.getLanguage());
            Map<String, Object> textRequestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", detailsPrompt))
            );
            String detailsResponse = callOpenAiApi(textRequestBody);
            String fullContent = parseContentFromApiResponse(detailsResponse, "OpenAI");


            log.info("Step 3: Parsing final response and saving to DB...");
            List<String> parsedContents = parseAiResponse(fullContent, dto.getLanguage());

            Prescription prescription = new Prescription(
                    "처방전 약품 상세 정보",
                    parsedContents.get(0),
                    parsedContents.get(1)
            );
            prescriptionRepository.save(prescription);

            return new PrescriptionResponse(
                    prescription.getId(),
                    prescription.getTitle(),
                    prescription.getContent(),
                    prescription.getKoreanContent(),
                    prescription.getCreatedAt()
            );

        } catch (Exception e) {
            log.error("Failed to create prescription: {}", e.getMessage());
            throw new RuntimeException("처방전 정보 생성에 실패했습니다.", e);
        }
    }


    public List<PrescriptionListItemResponse> readPrescriptionList() {
        return prescriptionRepository.findAll()
                .stream().map(prescription -> new PrescriptionListItemResponse(
                        prescription.getId(),
                        prescription.getTitle(),
                        prescription.getCreatedAt()
                )).collect(Collectors.toList());
    }

    public PrescriptionResponse readPrescription() {
        Prescription prescription = prescriptionRepository.findById(1L).orElseThrow(() -> new RuntimeException("해당 id의 prescription을 찾을 수 없습니다."));
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getTitle(),
                prescription.getContent(),
                prescription.getKoreanContent(),
                prescription.getCreatedAt()
        );
    }

    private String encodeImageToBase64(MultipartFile file) throws IOException {
        return Base64.getEncoder().encodeToString(file.getBytes());
    }

    private Map<String, Object> createUpstageExtractionRequest(String base64Data) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "medicine", Map.of(
                                "type", "array",
                                "description", "처방전 사진에서 얻은 환자가 처방 받은 의약품들의 이름을 담은 리스트",
                                "items", Map.of("type", "string")
                        )
                )
        );

        return Map.of(
                "model", "information-extract",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:application/octet-stream;base64," + base64Data)
                                )
                        ))
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "document_schema",
                                "schema", schema
                        )
                )
        );
    }

    private List<String> parseMedicineNamesFromUpstageResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            JsonNode contentNode = objectMapper.readTree(content);
            JsonNode medicineNode = contentNode.path("medicine");

            if (medicineNode.isArray()) {
                return objectMapper.convertValue(medicineNode, List.class);
            }
            return Collections.emptyList();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Upstage API response", e);
            throw new RuntimeException("Upstage API 응답 파싱 실패", e);
        }
    }

    private String createDetailsPrompt(String medicineNames, String language) {
        // ... (기존과 동일, 변경 없음)
        String targetLanguage = "english".equalsIgnoreCase(language) || "chinese".equalsIgnoreCase(language) ? language : "english";
        return String.format("""
        당신은 한국의 약학 정보에 능통한 전문 약사입니다. 아래 '의약품 목록'에 명시된 한국 의약품에 대한 상세 정보를 생성해 주세요. 
        출력은 반드시 지정된 두 섹션으로만 구성하며, 각 섹션의 헤더/필드명/순서/들여쓰기를 정확히 준수해야 합니다.

        의약품 목록:
        %s

        절대 준수사항:
        1) 의약품 목록에 있는 품목만 다루고, 목록의 순서를 그대로 유지합니다. 추가/누락/재배열 금지.
        2) 각 의약품에 대해 아래 6개 필드를 정확한 영문/한글 레이블로 모두 포함하고, 필드 순서를 반드시 지킵니다.
           영어 섹션: Efficacy, Usage, Precautions, Drug Interactions, Side Effects, Storage
           한국어 섹션: 효능, 사용법, 주의사항, 약물 상호작용, 부작용, 보관법
        3) 각 필드의 설명은 한 줄 이상으로 작성하되, 설명 줄 앞에는 탭 문자(\\t) 하나만 사용해 들여쓰기합니다. 공백 들여쓰기 금지.
        4) 불필요한 문장, 요약, 서문/맺음말, 번호/목록 기호(-, • 등), 링크, 인용, 참고문헌, 경고 배지, 마크다운(해시태그 #, 별표 *)를 절대 포함하지 마세요.
        5) 복용법/용량은 일반적 권장 범위로 간결히 기술하고, 개인 맞춤/진단/처방 유도 문구는 쓰지 않습니다.
        6) 확실하지 않은 정보는 추정하지 말고 영어 섹션에는 "N/A", 한국어 섹션에는 "정보 없음"이라고만 기재합니다.
        7) 의약품 이름은 제공된 표기 그대로 사용하고, 번역/괄호 병기는 하지 않습니다.
        8) 각 의약품 블록 사이에는 빈 줄 하나만 두고, 섹션 헤더·필드명·콜론 뒤의 공백 규칙을 지킵니다.

        출력 형식(이 형식을 정확히 복제할 것):
        --- %s response ---
        [Medicine Name 1]
        Efficacy:
            ...
        Usage:
            ...
        Precautions:
            ...
        Drug Interactions:
            ...
        Side Effects:
            ...
        Storage:
            ...

        --- korean response ---
        [의약품명 1]
        효능:
            ...
        사용법:
            ...
        주의사항:
            ...
        약물 상호작용:
            ...
        부작용:
            ...
        보관법:
            ...

        위 형식을 모든 의약품에 대해 반복 출력하세요. 형식 외의 어떤 내용도 출력하지 마세요.
        """, medicineNames, targetLanguage, targetLanguage);
    }

    private String parseContentFromApiResponse(String response, String apiType) {
        try {
            return objectMapper.readTree(response).path("choices").get(0).path("message").path("content").asText();
        } catch (IOException e) {
            log.error("Failed to parse {} API response", apiType, e);
            throw new RuntimeException(apiType + " 응답 파싱 실패", e);
        }
    }

    private List<String> parseAiResponse(String text, String language) {
        String foreignContent = "";
        String koreanContent = "";

        String delimiter = "--- korean response ---";
        String[] parts = text.split(delimiter);

        if (parts.length > 1) {
            String langHeader = String.format("--- %s response ---",
                    language);
            foreignContent = parts[0].replace(langHeader, "").trim();
            koreanContent = parts[1].trim();
        } else {
            log.warn("AI response format was not as expected. Storing full content in koreanContent.");
            koreanContent = text.trim();
        }
        return List.of(foreignContent, koreanContent);
    }

    private String callOpenAiApi(Map<String, Object> requestBody) {
        return openAiWebClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.error("OpenAI API error: {}", clientResponse.statusCode());
                    return clientResponse.createException();
                })
                .bodyToMono(String.class)
                .block();
    }

    private String callUpstageApi(Map<String, Object> requestBody) {
        return upstageWebClient.post()
                .uri("/v1/information-extraction/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + upstageApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.error("Upstage API error: {}", clientResponse.statusCode());
                    return clientResponse.createException();
                })
                .bodyToMono(String.class)
                .block();
    }
}