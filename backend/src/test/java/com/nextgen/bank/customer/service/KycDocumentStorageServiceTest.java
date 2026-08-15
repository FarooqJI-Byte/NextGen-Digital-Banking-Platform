package com.nextgen.bank.customer.service;

import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.customer.service.impl.KycDocumentStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KycDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private KycDocumentStorageService storageService;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        storageService = new KycDocumentStorageServiceImpl(tempDir.toString());
    }

    @Test
    @DisplayName("Should successfully store valid PDF document file")
    void testStoreValidPdfDocument() {
        byte[] pdfContent = "%PDF-1.4 test document content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                pdfContent
        );

        String reference = storageService.storeDocument(file, customerId);

        assertNotNull(reference);
        assertTrue(reference.startsWith("kyc-storage/" + customerId + "/"));
        assertTrue(reference.endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should successfully store valid JPG / PNG document file")
    void testStoreValidImageDocument() {
        byte[] imgContent = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10 };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_front.png",
                "image/png",
                imgContent
        );

        String reference = storageService.storeDocument(file, customerId);

        assertNotNull(reference);
        assertTrue(reference.startsWith("kyc-storage/" + customerId + "/"));
        assertTrue(reference.endsWith(".png"));
    }

    @Test
    @DisplayName("Should reject empty document file")
    void testRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> storageService.storeDocument(file, customerId));
        assertEquals("EMPTY_KYC_FILE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject unsupported file format / executable")
    void testRejectUnsupportedFileFormat() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "malicious bytes".getBytes()
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> storageService.storeDocument(file, customerId));
        assertEquals("UNSUPPORTED_KYC_FILE_FORMAT", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject file exceeding 10 MB limit")
    void testRejectOversizedFile() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "huge_document.pdf",
                "application/pdf",
                largeContent
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> storageService.storeDocument(file, customerId));
        assertEquals("KYC_FILE_TOO_LARGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject filename with path traversal sequence")
    void testRejectPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                "test".getBytes()
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> storageService.storeDocument(file, customerId));
        assertEquals("INVALID_KYC_FILENAME", ex.getErrorCode());
    }
}
