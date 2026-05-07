# simple-ai

A **Spring Boot 3.5 + Spring AI 1.1** reference application that exposes a production-style REST API for **chat, streaming chat, conversation history, prompt engineering, and Retrieval-Augmented Generation (RAG)** over your own documents.

It supports multiple LLM providers (**Ollama** for local models and **Anthropic Claude** for cloud), uses **PostgreSQL + pgvector** as the vector store, and ships with Swagger UI, SSE streaming, conversation caching, and a prompt-template framework.

---

## ✨ Features

- 🤖 **Multi-provider LLM support** — Ollama (local) and Anthropic (cloud), pluggable via `AIModel` / `AIProvider` enums and a `ModelSelector`.
- 💬 **Chat API** — synchronous chat with session-based conversation context.
- 📡 **Streaming chat** — Server-Sent Events (SSE) over both `POST` and `GET` (browser `EventSource` friendly), powered by Project Reactor.
- 📚 **RAG pipeline** — upload PDF / DOCX / TXT, parse with **Apache Tika**, chunk, embed via Ollama (`nomic-embed-text`, 768-dim), store in **pgvector**, retrieve top-K with cosine similarity, and answer with citations.
- 🧠 **Prompt engineering service** — system / task / RAG templates, prompt builder, prompt optimizer (whitespace trim, length limits), token counter.
- 🗂 **Conversation memory** — Caffeine-backed cache with TTL, per-session message cap, summary endpoint.
- 🆔 **Correlation-ID interceptor** for request tracing.
- 🛡 **Global exception handler** with typed error codes (`AiException`, `RateLimitException`, `StreamingException`, `ValidationException`).
- 📖 **OpenAPI / Swagger UI** auto-generated from controllers.
- 🚀 **Tuned Tomcat** (200 threads, 10k connections), HTTP response compression, HikariCP pooling, JPA batching.

---

## 🧱 Tech Stack

| Layer            | Technology                                                     |
|------------------|----------------------------------------------------------------|
| Language         | Java **21**                                                    |
| Framework        | Spring Boot **3.5.11**                                         |
| AI               | Spring AI **1.1.2** (Ollama, Anthropic, Transformers)          |
| Vector Store     | PostgreSQL + **pgvector** (768-dim, cosine distance)           |
| Streaming        | Spring WebFlux + Reactor (SSE only)                            |
| Persistence      | Spring Data JPA / Hibernate, HikariCP                          |
| Cache            | Caffeine + Spring Cache abstraction                            |
| Document parsing | Apache **Tika 3.2.3** (PDF, DOCX, TXT)                         |
| API docs         | springdoc-openapi 2.6                                          |
| Build            | Maven (Spring Boot Maven Plugin)                               |

---

## 📁 Project Structure

```
simple-ai/
├── pom.xml
├── src/main/java/harshal/temkar/ai/
│   ├── AiApplication.java                # Spring Boot entry point
│   ├── config/                           # ChatClient, Cache, OpenAPI, WebMvc, *Properties
│   ├── controller/chat/                  # REST controllers (Chat, Streaming, RAG, Conversation, Prompt, Model)
│   ├── exception/                        # Typed exceptions + GlobalExceptionHandler
│   ├── interceptor/                      # CorrelationIdInterceptor
│   ├── model/
│   │   ├── chat/                         # DTOs, JPA entities (Document, DocumentChunk), enums (AIModel, AIProvider)
│   │   └── conversation/                 # ConversationContext / Message / Summary
│   ├── repository/                       # JPA + in-memory conversation repos
│   ├── service/
│   │   ├── chat/                         # IChatService + ChatServiceImpl, ModelSelector
│   │   ├── conversation/                 # Conversation memory service
│   │   ├── prompt/                       # PromptService, PromptOptimizer
│   │   └── rag/                          # DocumentService, EmbeddingService, RAGService
│   └── util/                             # DocumentParser, TextChunker, PromptBuilder, PromptTemplateLoader, TokenCounter
└── src/main/resources/
    ├── application.yaml
    └── templates/prompts/{system,task,rag}/   # Prompt templates (rag-prompt.txt, etc.)
```

---

## ✅ Prerequisites

- **Java 21**
- **Maven 3.9+**
- **PostgreSQL 14+** with the **`pgvector`** extension
- **Ollama** running locally (default `http://localhost:11434`) with required models pulled:
  ```bash
  ollama pull nomic-embed-text   # required for embeddings (768-dim)
  ollama pull llama3             # default chat model
  ```
- *(Optional)* **Anthropic API key** if you want to enable Claude

---

## ⚙️ Configuration

All settings live in `src/main/resources/application.yaml` and can be overridden via environment variables.

### Environment variables

| Variable                     | Default                                          | Description                          |
|------------------------------|--------------------------------------------------|--------------------------------------|
| `AI_OLLAMA_URL`              | `http://localhost:11434`                         | Ollama base URL                      |
| `AI_OLLAMA_EMBEDDING_MODEL`  | `nomic-embed-text`                               | Embedding model                      |
| `ANTHROPIC_ENABLED`          | `false`                                          | Toggle Anthropic provider            |
| `ANTHROPIC_API_KEY`          | `ignore`                                         | Anthropic API key                    |
| `ANTHROPIC_BASE_URL`         | `https://api.anthropic.com`                      | Anthropic base URL                   |
| `DEFAULT_AI_PROVIDER_MODEL`  | `OLLAMA_LLAMA3`                                  | Default model identifier             |
| `AI_RETRY_ATTEMPT`           | `3`                                              | Spring AI retry max attempts         |
| `POSTGRES_URL`               | `jdbc:postgresql://localhost:5432/simple_ai`     | JDBC URL                             |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `postgres` / `postgres`                 | DB credentials                       |
| `HIKARI_MAX_POOL`            | `20`                                             | Max DB pool size                     |

### Database setup

```sql
CREATE DATABASE simple_ai;
\c simple_ai
CREATE EXTENSION IF NOT EXISTS vector;
```

The pgvector schema is auto-initialized on startup (`spring.ai.vectorstore.pgvector.initialize-schema=true`) with **768 dimensions** and **cosine distance**. JPA tables are auto-created via `ddl-auto: update`.

### RAG defaults

| Property              | Value      |
|-----------------------|------------|
| Chunk size            | 500        |
| Chunk overlap         | 50         |
| Strategy              | `sentence` |
| Top-K retrieval       | 5          |
| Similarity threshold  | 0.5        |
| Search type           | similarity |

### Conversation cache

| Property                  | Value     |
|---------------------------|-----------|
| Max sessions              | 1000      |
| TTL                       | 60 min    |
| Max messages per session  | 50        |

### Prompt optimization

- Max prompt length: **8000**
- Max system prompt length: **2000**
- Max context length: **6000**
- Trim whitespace + remove empty lines

---

## 🚀 Getting Started

### 1. Clone & build
```bash
git clone <your-repo-url>
cd simple-ai
mvn clean install
```

### 2. Start dependencies
- Start PostgreSQL (with `pgvector` extension created)
- Start Ollama and pull `nomic-embed-text` + `llama3`

### 3. Run the application
```bash
mvn spring-boot:run
```

App will be available at:

```
http://localhost:8090/simple-ai
```

### 4. Open Swagger UI
```
http://localhost:8090/simple-ai/swagger-ui.html
```

---

## 🔌 REST API Overview

All endpoints are prefixed with the context path `/simple-ai`.

### Chat — `/api/v1/chat`
| Method | Path           | Description                                  |
|--------|----------------|----------------------------------------------|
| POST   | `/api/v1/chat` | Send a chat message, get a JSON response     |

### Streaming Chat — `/api/v1/chat/stream` (SSE, `text/event-stream`)
| Method | Path                   | Description                              |
|--------|------------------------|------------------------------------------|
| POST   | `/api/v1/chat/stream`  | Programmatic streaming via JSON body     |
| GET    | `/api/v1/chat/stream`  | Browser `EventSource` (query params)     |

### Conversations — `/api/v1/conversations`
| Method | Path                                  | Description                  |
|--------|---------------------------------------|------------------------------|
| GET    | `/api/v1/conversations/{sessionId}`           | Full conversation context  |
| GET    | `/api/v1/conversations/{sessionId}/summary`   | Conversation summary       |
| DELETE | `/api/v1/conversations/{sessionId}`           | Delete a session           |
| DELETE | `/api/v1/conversations`                       | Clear all sessions         |

### Documents / RAG — `/api/v1/documents`
| Method | Path                            | Description                                            |
|--------|---------------------------------|--------------------------------------------------------|
| POST   | `/api/v1/documents/upload`      | Upload PDF/DOCX/TXT and index into pgvector            |
| GET    | `/api/v1/documents`             | List all indexed documents                             |
| GET    | `/api/v1/documents/{id}`        | Get a document by ID                                   |
| DELETE | `/api/v1/documents/{id}`        | Delete a document and its embeddings                   |
| POST   | `/api/v1/documents/search`      | Retrieve similar chunks for a query (`?query=`)        |
| POST   | `/api/v1/documents/ask`         | Ask with RAG (cited answer, optional `sessionId`)      |

### Prompts — `/api/v1/prompts`
| Method | Path                              | Description                              |
|--------|-----------------------------------|------------------------------------------|
| GET    | `/api/v1/prompts/system/{role}`   | Get a system prompt for a `PromptRole`   |
| POST   | `/api/v1/prompts/optimize`        | Optimize a system + user prompt          |
| POST   | `/api/v1/prompts/build`           | Build a fully optimized prompt context   |

### Models — `/api/v1/models`
| Method | Path                                            | Description                                  |
|--------|-------------------------------------------------|----------------------------------------------|
| GET    | `/api/v1/models/providers`                      | List available providers                     |
| GET    | `/api/v1/models/providers/{provider}/available` | Check if a provider is reachable             |
| GET    | `/api/v1/models/providers/info`                 | All `AIModel`s with enabled / accessible info|

---

## 🧪 Quick Examples

### Chat
```bash
curl -X POST http://localhost:8090/simple-ai/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: demo-1" \
  -d '{"message":"Hello, who are you?","sessionId":"sess-1"}'
```

### Streaming chat (browser SSE)
```js
const es = new EventSource(
  "http://localhost:8090/simple-ai/api/v1/chat/stream?message=Tell%20me%20a%20joke&sessionId=sess-1"
);
es.onmessage = (e) => console.log(JSON.parse(e.data));
```

### Upload a document for RAG
```bash
curl -X POST http://localhost:8090/simple-ai/api/v1/documents/upload \
  -F "file=@./mydoc.pdf" \
  -F "metadata=manual"
```

### Ask with RAG
```bash
curl -X POST "http://localhost:8090/simple-ai/api/v1/documents/ask?query=What%20is%20policy%20X&sessionId=sess-1"
```

---

## 🛠 Notes & Caveats

- `spring.jpa.hibernate.ddl-auto=update` is convenient for dev; review for production.
- `ANTHROPIC_API_KEY` defaults to the literal `ignore` so the app boots without Claude — set a real key and `ANTHROPIC_ENABLED=true` to use it.
- The embedding dimension (**768**) is tied to `nomic-embed-text`. If you change the embedding model, update `spring.ai.vectorstore.pgvector.dimensions` accordingly and rebuild the schema.
- SSE streaming uses `spring-boot-starter-webflux` alongside `spring-boot-starter-web` (MVC remains the primary stack).

---

## 📄 License

No license specified yet. Add one (e.g., MIT or Apache-2.0) before publishing.

---

## 🤝 Contributing

Issues and pull requests are welcome. Please include a clear description and, where applicable, tests.
