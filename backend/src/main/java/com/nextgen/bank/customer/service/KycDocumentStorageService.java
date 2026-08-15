package com.nextgen.bank.customer.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface KycDocumentStorageService {

    String storeDocument(MultipartFile file, UUID customerId);
}
