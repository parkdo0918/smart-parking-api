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

    // 번호판에서 숫자만 추출 (중간 한글 위치의 숫자 제거)
    // 예: "1571 4895" -> "157 4895" (중간 1은 한글 "고"를 잘못 읽은 것)
    private static final Pattern LICENSE_PLATE_PATTERN =
            Pattern.compile("([0-9]{2,3})[0-9]?\\s+([0-9]{4})");

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
        // 이미지 분석 요청 (한국어 OCR 설정)
        ImageAnalysisOptions options = new ImageAnalysisOptions()
                .setLanguage("ko");  // 한국어로 OCR 수행하여 "고", "가" 등 한글 인식

        ImageAnalysisResult result = client.analyze(
                BinaryData.fromBytes(file.getBytes()),
                Arrays.asList(VisualFeatures.READ),  // OCR 기능 사용
                options
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
            String recognizedText = allText.toString().trim();
            log.info("전체 인식된 텍스트: '{}'", recognizedText);

            String licensePlate = extractLicensePlate(recognizedText);
            if (licensePlate != null) {
                log.info("번호판 인식 성공: {}", licensePlate);
                return licensePlate;
            } else {
                log.warn("번호판 패턴 매칭 실패 - 인식된 텍스트: '{}'", recognizedText);
            }
        }

        log.warn("번호판 인식 실패 - OCR 결과 없음");
        return null;
    }

    /**
     * 텍스트에서 번호판 패턴 추출 (숫자만)
     */
    private String extractLicensePlate(String text) {
        Matcher matcher = LICENSE_PLATE_PATTERN.matcher(text);
        if (matcher.find()) {
            String num1 = matcher.group(1);
            String num2 = matcher.group(2);
            String licensePlate = num1 + " " + num2;
            log.info("번호판 숫자 추출 성공: {} + {} = {}", num1, num2, licensePlate);
            return licensePlate;
        }

        return null;
    }

}
