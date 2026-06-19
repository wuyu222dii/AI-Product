package com.aipm.cowriting.application.service;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMaterialStorageService {

    private final Path root = Paths.get(System.getProperty("user.dir"), "uploaded-materials");

    public String store(UUID workspaceId, MultipartFile file) {
        try {
            Files.createDirectories(root.resolve(workspaceId.toString()));
            String safeName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            Path path = root.resolve(workspaceId.toString()).resolve(safeName);
            file.transferTo(path);
            return path.toString();
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "文件保存失败"
            );
        }
    }

    public Path resolve(String storagePath) {
        return Paths.get(storagePath);
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
