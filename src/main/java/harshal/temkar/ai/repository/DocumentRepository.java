package harshal.temkar.ai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import harshal.temkar.ai.model.chat.DocumentEntity;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
	
    List<DocumentEntity> findByFilenameContaining(String filename);
}