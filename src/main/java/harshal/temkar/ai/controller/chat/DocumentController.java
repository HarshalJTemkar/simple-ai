package harshal.temkar.ai.controller.chat;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import harshal.temkar.ai.model.chat.AIModel;
import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.DocumentUploadResponse;
import harshal.temkar.ai.model.chat.RAGContext;
import harshal.temkar.ai.service.rag.IDocumentService;
import harshal.temkar.ai.service.rag.IRAGService;
import harshal.temkar.ai.service.rag.RagOptions;
import harshal.temkar.ai.config.UploadProperties;
import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "RAG document upload and management")
public class DocumentController {

	private final IDocumentService documentService;
	private final IRAGService ragService;
	private final UploadProperties uploadProperties;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload and index document", description = "Upload PDF/TXT/DOCX and create vector embeddings")
	public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam MultipartFile file,
			@RequestParam(required = false) @Size(max = 500, message = "Metadata cannot exceed 500 characters") String metadata) {

		log.info("Document upload request: {}", file.getOriginalFilename());
		validateUpload(file);
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
	@Operation(summary = "Search documents", description = "Retrieve similar document chunks. Supports topK and similarityThreshold overrides.")
	public ResponseEntity<RAGContext> searchDocuments(
			@RequestParam @NotBlank(message = "Query cannot be blank") @Size(max = 2000, message = "Query cannot exceed 2000 characters") String query,
			@RequestParam(required = false) @Min(1) @Max(50) Integer topK,
			@RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold) {
		log.info("Document search request: query='{}', topK={}, threshold={}", query, topK, similarityThreshold);
		RAGContext context = ragService.retrieveContext(query, RagOptions.builder()
				.topK(topK)
				.similarityThreshold(similarityThreshold)
				.build());
		return ResponseEntity.ok(context);
	}

	@PostMapping("/ask")
	@Operation(summary = "Ask with RAG",
		description = "Query with document context and citations. Optional overrides: providerModel, temperature, maxTokens, topK, similarityThreshold.")
	public ResponseEntity<CitedResponse> askWithRAG(
			@RequestParam @NotBlank(message = "Query cannot be blank") @Size(max = 2000, message = "Query cannot exceed 2000 characters") String query,
			@RequestParam(required = false) @Size(max = 100, message = "Session ID cannot exceed 100 characters") String sessionId,
			@RequestParam(required = false) AIModel providerModel,
			@RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
			@RequestParam(required = false) @Min(1) @Max(8192) Integer maxTokens,
			@RequestParam(required = false) @Min(1) @Max(50) Integer topK,
			@RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold) {

		log.info("RAG query request: query='{}', sessionId={}, providerModel={}, topK={}, threshold={}",
				query, sessionId, providerModel, topK, similarityThreshold);

		RagOptions options = RagOptions.builder()
				.providerModel(providerModel)
				.temperature(temperature)
				.maxTokens(maxTokens)
				.topK(topK)
				.similarityThreshold(similarityThreshold)
				.build();

		CitedResponse response = ragService.askWithRAG(query, sessionId, options);
		return ResponseEntity.ok(response);
	}

	private void validateUpload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new AiException(ErrorCode.VALIDATION_ERROR, "Uploaded file is empty");
		}
		long max = uploadProperties.getMaxFileSizeBytes();
		if (file.getSize() > max) {
			throw new AiException(ErrorCode.FILE_TOO_LARGE,
					"File size " + file.getSize() + " bytes exceeds limit of " + max + " bytes");
		}
		String contentType = file.getContentType();
		if (contentType == null
				|| uploadProperties.getAllowedContentTypes().stream().noneMatch(contentType::equalsIgnoreCase)) {
			throw new AiException(ErrorCode.UNSUPPORTED_FILE_TYPE,
					"Content-Type '" + contentType + "' is not allowed. Allowed: "
							+ uploadProperties.getAllowedContentTypes());
		}
	}
}