package com.example.smartparkingapi.parking.service;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.DetectedTextBlock;
import com.azure.ai.vision.imageanalysis.models.DetectedTextLine;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisOptions;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisResult;
import com.azure.ai.vision.imageanalysis.models.VisualFeatures;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ComputerVisionService {

    @Value("${azure.computer-vision.endpoint}")
    private String endpoint;

    @Value("${azure.computer-vision.key}")
    private String key;

    private ImageAnalysisClient client;

    // 한국 차량 번호판 패턴 (예: 12가1234, 서울12가1234)
    private static final Pattern LICENSE_PLATE_PATTERN =
            Pattern.compile("([가-힣]{0,2})?\\s*([0-9]{2,3})\\s*([가-힣])\\s*([0-9]{4})");

    @PostConstruct
    public void init() {
        client = new ImageAnalysisClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
        log.info("Computer Vision 클라이언트 초기화 완료");
    }

    /**
     * 이미지에서 차량 번호판 텍스트 추출
     * @param file 분석할 이미지 파일
     * @return 인식된 번호판 문자열 (없으면 null)
     */
    public String recognizeLicensePlate(MultipartFile file) throws IOException {
        // 이미지 분석 요청
        ImageAnalysisResult result = client.analyze(
                BinaryData.fromBytes(file.getBytes()),
                Arrays.asList(VisualFeatures.READ),  // OCR 기능 사용
                new ImageAnalysisOptions()
        );

        // 텍스트 추출
        if (result.getRead() != null && result.getRead().getBlocks() != null) {
            StringBuilder allText = new StringBuilder();

            for (DetectedTextBlock block : result.getRead().getBlocks()) {
                for (DetectedTextLine line : block.getLines()) {
                    allText.append(line.getText()).append(" ");
                    log.debug("인식된 텍스트: {}", line.getText());
                }
            }

            // 번호판 패턴 매칭
            String licensePlate = extractLicensePlate(allText.toString());
            if (licensePlate != null) {
                log.info("번호판 인식 성공: {}", licensePlate);
                return licensePlate;
            }
        }

        log.warn("번호판 인식 실패");
        return null;
    }

    /**
     * 텍스트에서 번호판 패턴 추출
     */
    private String extractLicensePlate(String text) {
        Matcher matcher = LICENSE_PLATE_PATTERN.matcher(text.replaceAll("\\s+", ""));

        if (matcher.find()) {
            String region = matcher.group(1) != null ? matcher.group(1) : "";
            String num1 = matcher.group(2);
            String letter = matcher.group(3);
            String num2 = matcher.group(4);

            return (region + num1 + letter + num2).trim();
        }

        return null;
    }

    /**
     * 이미지에서 차량 감지 여부 확인 (확장용)
     */
    public boolean detectVehicle(MultipartFile file) throws IOException {
        ImageAnalysisResult result = client.analyze(
                BinaryData.fromBytes(file.getBytes()),
                Arrays.asList(VisualFeatures.OBJECTS),
                new ImageAnalysisOptions()
        );

        if (result.getObjects() != null) {
            return result.getObjects().getValues().stream()
                    .anyMatch(obj ->
                            obj.getTags().stream()
                                    .anyMatch(tag ->
                                            tag.getName().equalsIgnoreCase("car") ||
                                                    tag.getName().equalsIgnoreCase("vehicle") ||
                                                    tag.getName().equalsIgnoreCase("truck")
                                    )
                    );
        }

        return false;
    }
}
