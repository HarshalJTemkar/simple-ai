package harshal.temkar.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import harshal.temkar.ai.model.chat.DocumentChunkEntity;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {
	
    List<DocumentChunkEntity> findByDocumentId(Long documentId);
    
    Optional<DocumentChunkEntity> findByVectorStoreId(String vectorStoreId);
}