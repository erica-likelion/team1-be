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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public Prescription get(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId).orElseThrow(() -> new RuntimeException("해당 처방전을 찾을 수 없습니다."));
    }

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

    public PrescriptionResponse readPrescription(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElseThrow(() -> new RuntimeException("해당 id의 prescription을 찾을 수 없습니다."));
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
        return String.format("""
        당신은 한국의 약학 정보에 능통한 전문 약사입니다. 처방전을 입력한 환자에게 처방받은 의약품들에 대해 쉼고 자세하게 설명해주는 글을 생성해야합니다.
반드시 아래 출력 틀 그대로 생성해야 합니다.
출력 시작 전후에 어떤 텍스트도 쓰지 마세요(설명/경고/요약/인용/코드블록 금지). 

의약품 목록:
%1$s

절대 준수사항:
1) 의약품 목록의 품목만, 주어진 순서 그대로 출력합니다(추가/누락/재배열 금지). %2$s 버전의 경우는 의약품 이름을 해당 언어로 번역해서 보여줘야합니다.
2) 한국어와 %2$s 두 버전의 생성물이 있어야합니다. 각각의 버전에는 다음 항목들이 포함됩니다.
   효능, 사용법, 주의사항, 약물 상호작용, 부작용, 보관법
   (약물 상호작용에는 같이 복용하면 위험할 수 있는 의약품들을 알려줘야합니다.)
3) 각 설명 줄은 탭 문자(\\t)로만 1단 들여쓰기 합니다(스페이스 들여쓰기 금지).
4) 마크다운/번호/불릿/링크/추가 코멘트/콘텐츠 확장 금지.
5) 의약품명은 입력 그대로 사용(번역/괄호 표기 금지).
6) 두 블록 사이에는 빈 줄 1개만, 그 외 불필요한 빈 줄 금지.

출력 예시:
<<<BEGIN_KO>>>
[의약품명 1]
효능:
\t...
사용법:
\t...
주의사항:
\t...
약물 상호작용:
\t...
부작용:
\t...
보관법:
\t...

[의약품명 2]
효능:
\t...
사용법:
\t...
주의사항:
\t...
약물 상호작용:
\t...
부작용:
\t...
보관법:
\t...
<<<END_KO>>>
<<<BEGIN_%2$s>>>
...(한국어 출력과 같은 형식)
<<<END_%2$s>>>
""", medicineNames, language.toUpperCase());
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
        if (text == null) return List.of("", "");

        String content = "";
        String koreanContent  = "";

        // DOTALL 모드로 줄바꿈 포함 매칭
        Pattern pattern = Pattern.compile(
                String.format("(?s)<<<BEGIN_KO>>>\\s*(.*?)\\s*<<<END_KO>>>.*?<<<BEGIN_%s>>>\\s*(.*?)\\s*<<<END_%s>>>", language.toUpperCase(), language.toUpperCase())
        );
        Matcher matcher = pattern.matcher(text.trim());

        if (matcher.find()) {
            koreanContent = matcher.group(1).trim();
            content  = matcher.group(2).trim();
        } else {
            // 센티넬이 없으면 전체를 koreanContent로 반환
            koreanContent = text.trim();
        }

        return List.of(content, koreanContent);
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