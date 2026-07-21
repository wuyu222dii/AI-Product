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

    private final Path root = Paths.get(System.getProperty("user.dir"), "uploaded-materials")
            .toAbsolutePath().normalize();
    private final CurrentUserService currentUserService;

    public LocalMaterialStorageService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public String store(UUID workspaceId, MultipartFile file) {
        try {
            Path workspaceDirectory = root
                    .resolve(currentUserService.userId().toString())
                    .resolve(workspaceId.toString())
                    .normalize();
            requireInsideRoot(workspaceDirectory);
            Files.createDirectories(workspaceDirectory);
            String safeName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            Path path = workspaceDirectory.resolve(safeName).normalize();
            requireInsideRoot(path);
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
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文件不存在");
        }
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        requireInsideRoot(path);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文件不存在");
        }
        return path;
    }

    private void requireInsideRoot(Path path) {
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文件不存在");
        }
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
