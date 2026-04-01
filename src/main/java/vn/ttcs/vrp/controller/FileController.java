package vn.ttcs.vrp.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.response.UploadResponse;
import vn.ttcs.vrp.service.FileService;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file")MultipartFile file
    ) {
        UploadResponse uploadResponse = fileService.storeFile(file);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Tải ảnh lên hệ thống thành công", uploadResponse
        ));
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName, HttpServletRequest request
    ) {
        // load ảnh từ thư mục lên
        Resource resource = fileService.loadFileAsResource(fileName);

        String contentType;

        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
