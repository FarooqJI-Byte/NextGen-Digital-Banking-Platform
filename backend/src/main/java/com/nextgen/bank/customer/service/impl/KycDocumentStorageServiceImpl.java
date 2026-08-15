package com.nextgen.bank.customer.service.impl;

import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.customer.service.KycDocumentStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class KycDocumentStorageServiceImpl implements KycDocumentStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg"
    );

    private final Path uploadRoot;

    public KycDocumentStorageServiceImpl(@Value("${app.storage.kyc-upload-dir:./data/kyc-uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeDocument(MultipartFile file, UUID customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Document file cannot be empty", HttpStatus.BAD_REQUEST, "EMPTY_KYC_FILE");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("Document file size exceeds 10 MB limit", HttpStatus.BAD_REQUEST, "KYC_FILE_TOO_LARGE");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("Invalid document filename", HttpStatus.BAD_REQUEST, "INVALID_KYC_FILENAME");
        }

        // Prevent path traversal
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("Malicious path sequence in filename", HttpStatus.BAD_REQUEST, "INVALID_KYC_FILENAME");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    "Unsupported file format: ." + extension + ". Allowed: PDF, JPG, JPEG, PNG",
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_KYC_FILE_FORMAT"
            );
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                    "Unsupported MIME type: " + contentType + ". Allowed: PDF, JPG, PNG",
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_KYC_MIME_TYPE"
            );
        }

        try {
            Path customerDir = uploadRoot.resolve(customerId.toString()).normalize();
            if (!customerDir.startsWith(uploadRoot)) {
                throw new BusinessException("Security violation: Invalid storage path", HttpStatus.BAD_REQUEST, "INVALID_STORAGE_PATH");
            }

            Files.createDirectories(customerDir);

            String secureFilename = UUID.randomUUID() + "_" + System.currentTimeMillis() + "." + extension;
            Path destinationPath = customerDir.resolve(secureFilename).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "kyc-storage/" + customerId + "/" + secureFilename;
        } catch (IOException e) {
            throw new BusinessException("Failed to store KYC document securely: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "KYC_STORAGE_ERROR");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}
