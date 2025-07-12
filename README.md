# 📄 AI-Driven PDF Q\&A Service

An AI-powered backend application built with Spring Boot that enables users to upload PDF documents and intelligently interact with them using natural language questions. It leverages Spring AI, Ollama, and Apache PDFBox to extract, chunk, embed, and highlight relevant answers from PDFs.

---

## ✨ Features

### 📄 PDF Upload & Processing

* Upload PDF documents
* Extract text and chunk them using PDFBox
* Compute SHA-256 hash for content-based deduplication/versioning
* Store chunks and metadata for retrieval and summarization

### 🔍 Smart Question Answering

* Ask questions on entire document or by page/section
* Uses **RAG (Retrieval-Augmented Generation)** to fetch relevant chunks and pass them to LLM
* Context-aware answers using top-k relevant chunks
* Uses Spring AI + Ollama for response generation (chat + embedding models)
* Multi-document QnA with aggregated chunk matching

### 🧠 Summarization

* Summarize full documents or specific sections
* Caches generated summaries in in-memory repository to avoid recomputation
* Uses `ChatClient` with prompt injection to handle LLM interaction

### 🖍 PDF Highlighting

* Highlight answer-related phrases in the original PDF
* Uses `PDFBox` + custom `PositionAwareStripper` for word position extraction

### 🧵 Vector Search

* Semantic search on PDF content using Spring AI’s VectorStore (e.g., SimpleVectorStore)
* Metadata filtering supported via `docId`, `pageNumber`, `section` in chunk metadata
* Fast top-K similarity matching using embedding vector search

### 🧪 Simple Text-Based Search API

* Retrieve top matching chunks for arbitrary query
* Renders text, score, and document ID for review/debugging
  
---

## 🛠 Tech Stack

### ☕ Backend
* **Spring Boot**
* **Spring AI** (Chat + VectorStore)
* **Ollama** (locally running `llama3.2:latest` model for chat & embedding)
* **Apache PDFBox** for PDF parsing and highlighting
* **VectorStore (SimpleVectorStore/Chroma/Redis)**
* **Lombok** for reducing boilerplate
  
---

## 📁 Project Structure

```bash
src/
├── controller/         # REST API endpoints (upload, qa, search, highlight, summary)
├── dto/                # Request/response DTOs
├── service/            # Core services (PDF, QnA, AI, highlight, summarization)
├── repository/         # Summary repository abstraction
├── config/             # Any custom config if added later
└── resources/
    └── application.properties
```

## 🔐 Configuration

### `application.properties`

```properties
spring.application.name=pdfqa
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Ollama configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2:latest
spring.ai.ollama.embedding.model=llama3.2:latest

# Vector store
spring.ai.vectorstore.chroma.enabled=false

server.port=8081

# Logging
logging.level.com.example.pdfqa=DEBUG
logging.level.org.springframework.ai=DEBUG
logging.level.root=INFO

# PDF Storage
pdf.storage-dir=pdfs
```

### Maven Dependencies (`pom.xml`)

* `spring-boot-starter-web`
* `spring-ai-ollama-spring-boot-starter`
* `spring-ai-redis-store`
* `spring-ai-core`
* `pdfbox`
* `lombok`

---

## ⚙ Setup & Run Locally

### 1. Prerequisites

* Java 17+
* Maven
* Ollama with LLaMA3 model running locally

### 2. Clone the Repository

```bash
git clone https://github.com/your-username/pdfqa.git
cd pdfqa
```

### 3. Configure Application

Update `application.properties` with appropriate Ollama settings (already preconfigured above).

### 4. Run Ollama

* Ollama: `ollama run llama3.2`

### 5. Run the Spring Boot App

```bash
./mvnw spring-boot:run
```

## 📌 API Endpoints

### 📁 Document APIs

| Method | Endpoint          | Description                          |
|--------|-------------------|--------------------------------------|
| POST   | `/api/pdf/upload` | Upload and embed a PDF file          |
| GET    | `/api/pdf/docs`   | List available uploaded document IDs |

---

### 🤖 AI Q&A APIs

| Method | Endpoint                 | Description                                                     |
|--------|--------------------------|-----------------------------------------------------------------|
| GET    | `/qa/ask-smart`          | Ask a smart question with fallback to most relevant document    |
| POST   | `/qa/ask-by-section`     | Ask a question filtered by docId, page number, or section title |
| POST   | `/api/pdf/multi-doc-ask` | Ask across multiple PDFs using combined vector search           |

---

### ✨ Highlight APIs

| Method | Endpoint              | Description                                    |
|--------|-----------------------|------------------------------------------------|
| POST   | `/api/pdf/highlight`  | Highlight text spans in PDF based on an answer |
| POST   | `/highlight/question` | Highlight answer text derived from a question  |

---

### 🧠 Summarization APIs

| Method | Endpoint             | Description                                             |
|--------|----------------------|---------------------------------------------------------|
| GET    | `/api/pdf/summarize` | Summarize entire document or a section using top-K hits |
| GET    | `/api/pdf/keypoints` | Extract important keypoints from a document             |

---

### 🔍 Search APIs

| Method | Endpoint          | Description                                  |
|--------|-------------------|----------------------------------------------|
| GET    | `/api/pdf/search` | Search document chunks via vector similarity |


---

## 🤖 AI Integration

Using [`llama3.2:latest`](https://ollama.com/library/llama3) via **Ollama**, wrapped in Spring AI:

* Q\&A with document context
* Chunk-wise summarization
* Vector similarity search powered by Spring AI 'EmbeddingModel'

---

## 🧠 Core Services Implemented

* `PdfService` - extract, chunk, hash
* `QnaService` - answer questions (simple or filtered)
* `QnAHighlightService` - extract key phrases from answers for highlighting
* `SummarizationService` - cache + summarize docs
* `PdfHighlightService` + `PositionAwareStripper` - find & annotate text spans
* `SearchService` - vector-based search by similarity
