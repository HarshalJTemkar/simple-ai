package harshal.temkar.ai.service.rag;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.DocumentUploadResponse;

public interface IDocumentService {
    
    DocumentUploadResponse uploadAndIndex(MultipartFile file, String metadata);
    
    void deleteDocument(Long documentId);
    
    List<DocumentEntity> getAllDocuments();
    
    DocumentEntity getDocumentById(Long documentId);
}