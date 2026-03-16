package harshal.temkar.ai.controller.chat;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.DocumentUploadResponse;
import harshal.temkar.ai.model.chat.RAGContext;
import harshal.temkar.ai.service.rag.IDocumentService;
import harshal.temkar.ai.service.rag.IRAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "RAG document upload and management")
public class DocumentController {

	private final IDocumentService documentService;
	private final IRAGService ragService;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload and index document", description = "Upload PDF/TXT/DOCX and create vector embeddings")
	public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam MultipartFile file,
			@RequestParam(required = false) String metadata) {

		log.info("Document upload request: {}", file.getOriginalFilename());
		DocumentUploadResponse response = documentService.uploadAndIndex(file, metadata);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(summary = "List all documents")
	public ResponseEntity<List<DocumentEntity>> listDocuments() {
		return ResponseEntity.ok(documentService.getAllDocuments());
	}

	@GetMapping("/{documentId}")
	@Operation(summary = "Get document by ID")
	public ResponseEntity<DocumentEntity> getDocument(@PathVariable Long documentId) {
		return ResponseEntity.ok(documentService.getDocumentById(documentId));
	}

	@DeleteMapping("/{documentId}")
	@Operation(summary = "Delete document", description = "Delete document and its vector embeddings")
	public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
		log.info("Delete document request: {}", documentId);
		documentService.deleteDocument(documentId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/search")
	@Operation(summary = "Search documents", description = "Retrieve similar document chunks")
	public ResponseEntity<RAGContext> searchDocuments(@RequestParam String query) {
		log.info("Document search request: {}", query);
		RAGContext context = ragService.retrieveContext(query);
		return ResponseEntity.ok(context);
	}

	@PostMapping("/ask")
	@Operation(summary = "Ask with RAG", description = "Query with document context and citations")
	public ResponseEntity<CitedResponse> askWithRAG(@RequestParam String query,
			@RequestParam(required = false) String sessionId) {

		log.info("RAG query request: {}", query);
		CitedResponse response = ragService.askWithRAG(query, sessionId);
		return ResponseEntity.ok(response);
	}
}