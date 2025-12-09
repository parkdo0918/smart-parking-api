package com.example.smartparkingapi.parking.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class BlobStorageService { //CCTV 이미지 → Azure Blob Storage 업로드

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private BlobContainerClient containerClient; // Azure Blob Storage의 컨테이너(폴더)를 다루는 클라이언트

    /**
     1. Azure와 연결 (connectionString 사용)
     2. cctv-images 컨테이너 찾기
     3. 컨테이너 없으면 생성 (최초 1회만)
     */
    @PostConstruct // 스프링 시작 시 자동으로 실행
    public void init() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // 컨테이너 없으면 생성
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Blob 컨테이너 생성됨: {}", containerName);
        }
    }

    /**
     * 이미지 파일을 Blob Storage에 업로드
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String blobName = UUID.randomUUID().toString() + extension;

        // Blob에 업로드
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        String imageUrl = blobClient.getBlobUrl();
        log.info("이미지 업로드 완료: {}", imageUrl);

        return imageUrl;
    }

    /**
     * Blob URL로 이미지 삭제
     */
    public void deleteImage(String blobUrl) {
        String blobName = blobUrl.substring(blobUrl.lastIndexOf("/") + 1);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (blobClient.exists()) {
            blobClient.delete();
            log.info("이미지 삭제 완료: {}", blobName);
        }
    }
}