package harshal.temkar.ai.service.rag;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.DocumentChunkEntity;
import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.DocumentUploadResponse;
import harshal.temkar.ai.repository.DocumentChunkRepository;
import harshal.temkar.ai.repository.DocumentRepository;
import harshal.temkar.ai.util.DocumentParser;
import harshal.temkar.ai.util.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    @Override
    @Transactional
    public DocumentUploadResponse uploadAndIndex(MultipartFile file, String metadata) {
        try {
            log.info("Processing document upload: {}", file.getOriginalFilename());
            
            // Parse document
            String content = documentParser.parseDocument(file);
            
            // Chunk text
            List<String> chunks = textChunker.chunkText(content);
            
            // Save document entity
            DocumentEntity document = DocumentEntity.builder()
                    .filename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .chunkCount(chunks.size())
                    .uploadedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .build();
            
            document = documentRepository.save(document);
            log.debug("Saved document entity with ID: {}", document.getId());
            
            // Create and store vector embeddings
            List<Document> documents = createDocuments(chunks, document);
            vectorStore.add(documents);
            
            // Save chunk metadata
            saveChunkMetadata(documents, document.getId());
            
            log.info("Successfully indexed document: {} ({} chunks)", 
                     file.getOriginalFilename(), chunks.size());
            
            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(file.getOriginalFilename())
                    .chunksCreated(chunks.size())
                    .status("SUCCESS")
                    .message("Document uploaded and indexed successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to upload and index document: {}", file.getOriginalFilename(), e);
            throw new AiException(ErrorCode.INTERNAL_ERROR, 
                "Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("Deleting document: {}", documentId);
        
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AiException(ErrorCode.VALIDATION_ERROR, 
                    "Document not found: " + documentId));
        
        // Delete chunks from vector store
        List<DocumentChunkEntity> chunks = chunkRepository.findByDocumentId(documentId);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunkEntity::getVectorStoreId)
                .collect(Collectors.toList());
        
        vectorStore.delete(vectorIds);
        
        // Delete from database
        chunkRepository.deleteAll(chunks);
        documentRepository.delete(document);
        
        log.info("Deleted document and {} chunks", chunks.size());
    }

    @Override
    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Override
    public DocumentEntity getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new AiException(ErrorCode.VALIDATION_ERROR, 
                    "Document not found: " + documentId));
    }

    private List<Document> createDocuments(List<String> chunks, DocumentEntity documentEntity) {
        return chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", documentEntity.getId());
                    metadata.put("filename", documentEntity.getFilename());
                    metadata.put("chunkIndex", chunks.indexOf(chunk));
                    
                    String id = UUID.randomUUID().toString();
                    return new Document(id, chunk, metadata);
                })
                .collect(Collectors.toList());
    }

    private void saveChunkMetadata(List<Document> documents, Long documentId) {
        documents.forEach(doc -> {
            DocumentChunkEntity chunk = DocumentChunkEntity.builder()
                    .documentId(documentId)
                    .chunkIndex((Integer) doc.getMetadata().get("chunkIndex"))
                    .content(doc.getText())
                    .vectorStoreId(doc.getId())
                    .build();
            
            chunkRepository.save(chunk);
        });
    }
}