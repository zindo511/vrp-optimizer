package vn.ttcs.vrp.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.ttcs.vrp.dto.response.UploadResponse;
import vn.ttcs.vrp.exception.FileStorageException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.service.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FILE-SERVICE")
public class FileServiceImpl implements FileService {

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Đã tạo hoặc tìm thấy kho lưu trữ ảnh tại: {}", fileStorageLocation);
        } catch (Exception e) {
            log.error("Could not initialize file store directory.", e);
            throw new RuntimeException("Could not initialize file store directory.", e);
        }
    }
    @Override
    public UploadResponse storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống không có dữ liệu!");
        }

        // Lấy extension từ tên file gốc
        String originalFilename = Paths.get(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "image.jpg").getFileName().toString();

        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }

        // validate extension
        List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        String generatedFileName = UUID.randomUUID() + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(generatedFileName);
            try(InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileDownloadUrl = serverUrl + "/api/files/" + generatedFileName;

            log.info("Lưu thành công ảnh: {}", generatedFileName);
            return UploadResponse.builder()
                    .fileName(generatedFileName)
                    .fileUrl(fileDownloadUrl)
                    .build();

        } catch (IOException ex) {
            log.error("Lưu file thất bại: {}", generatedFileName, ex);
            throw new FileStorageException("Không lưu được: " + generatedFileName + ".Vui lòng thử lại", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File bị mất rồi: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
