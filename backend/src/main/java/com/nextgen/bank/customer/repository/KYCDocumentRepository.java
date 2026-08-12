package com.nextgen.bank.customer.repository;

import com.nextgen.bank.customer.domain.KYCDocument;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KYCDocumentRepository extends JpaRepository<KYCDocument, UUID> {

    List<KYCDocument> findByCustomerId(UUID customerId);

    Optional<KYCDocument> findByCustomerIdAndDocumentType(UUID customerId, DocumentType documentType);

    boolean existsByDocumentTypeAndDocumentNumberEnc(DocumentType documentType, String documentNumberEnc);
}
