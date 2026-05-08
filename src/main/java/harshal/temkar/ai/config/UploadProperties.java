package harshal.temkar.ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configurable file upload constraints for RAG document ingestion.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Hard cap (in bytes) enforced at the controller layer (defense-in-depth on top of multipart limits). */
    private long maxFileSizeBytes = 25L * 1024 * 1024; // 25 MiB

    /** Allowed MIME types for ingestion. */
    private List<String> allowedContentTypes = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "text/markdown");
}
