package vn.ttcs.vrp.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import vn.ttcs.vrp.dto.response.UploadResponse;

public interface FileService {

    // lưu file vào thư mục trả về đuôi url của hệ thống
    UploadResponse storeFile(MultipartFile file);

    // mở file ảnh trên web/app
    Resource loadFileAsResource(String fileName);
}
