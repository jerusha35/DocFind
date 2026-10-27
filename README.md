# DocuFind — Offline Personal Search Engine

**DocuFind** is a secure, responsive, offline desktop document search engine built using **Java Swing**, **Oracle Database 19c / 12c+**, **JDBC**, and **Java NIO**.

---

## 1. Key Features
- **Offline & Private**: Runs completely on Windows locally without external cloud dependencies or web APIs.
- **Role-Based Authentication**: Secure login & registration with PBKDF2WithHmacSHA256 (65,536 iterations + cryptographic salt). Supports `ADMIN` and `USER` roles.
- **Oracle SQL Backed**: Fully persistent schema storing documents, extracted text (CLOBs), sha256 checksums, saved searches, search history, and error logs.
- **Multi-Format Indexer**: Indexes `.txt`, `.md`, `.java`, `.log`, `.csv`, `.properties`, `.xml`, `.json`, `.html`, `.htm`, `.sql`.
- **Search Engine**: Exact phrase, all keywords, any keyword, filename-only, and extension filtering. Contextual snippet extraction and normalized relevance ranking.
- **Document Actions**: Open File (`Desktop.open`), Open Containing Folder (Explorer integration), copy path, and check for stale modified files.
- **File Export**: CSV export with formula injection protection (`=, +, -, @`), text search reports, and indexing error logs.
- **Admin Panel**: Account activation/deactivation, aggregate error logs inspection, and system metrics.

---

## 2. Prerequisites
1. **Windows OS** (Windows 10 / 11 / Server).
2. **Java 21 LTS** (Installed at system level, `java` and `javac` accessible in PATH).
3. **Oracle Database 19c or 12c+** (Local service `OracleServiceORCL` running on port `1521`).
4. **Oracle JDBC Driver**: Placed in `lib/ojdbc8.jar` (already configured in this repository).

---

## 3. Configuration

DocuFind reads settings from `config/application.properties`.
```properties
db.url=jdbc:oracle:thin:@localhost:1521:orcl
db.user=system
db.password=livewire

app.name=DocuFind - Offline Personal Search Engine
app.version=1.0.0
app.max_file_size_mb=25
app.preview_char_limit=3000
app.search_max_results=500
```

> **Security Note**: In this learning project, the `SYSTEM` account is configured as requested. For production environments, create a dedicated user schema (`DOCUFIND_USER`) with standard data access privileges.

---

## 4. How to Compile and Run

### Step 1: Compile the Application
In PowerShell, execute:
```powershell
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
$srcFiles = (Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName)
javac -encoding UTF-8 -cp "lib/ojdbc8.jar" -d bin $srcFiles
```

### Step 2: Run DocuFind Desktop Application
```powershell
java -cp "bin;lib/ojdbc8.jar" com.docufind.Main
```

### Step 3: Run Automated Test Suites
- **Core Unit Verification (PBKDF2, CSV Injection, SHA-256, Relevance)**:
  ```powershell
  javac -encoding UTF-8 -cp "bin;lib/ojdbc8.jar" -d bin src\test\java\com\docufind\CoreLogicVerification.java
  java -cp "bin;lib/ojdbc8.jar" com.docufind.CoreLogicVerification
  ```

- **Live Oracle Database Connection & Schema Test**:
  ```powershell
  javac -encoding UTF-8 -cp "bin;lib/ojdbc8.jar" -d bin src\test\java\com\docufind\TestDB.java
  java -cp "bin;lib/ojdbc8.jar" com.docufind.TestDB
  ```

- **End-to-End Workflow Test (Register -> Login -> Index -> Search)**:
  ```powershell
  javac -encoding UTF-8 -cp "bin;lib/ojdbc8.jar" -d bin src\test\java\com\docufind\EndToEndWorkflowTest.java
  java -cp "bin;lib/ojdbc8.jar" com.docufind.EndToEndWorkflowTest
  ```

---

## 5. First-Admin Setup
1. Launch the application.
2. Click **Create New Account**.
3. Register your desired username and password. The very first user registered is automatically granted the **ADMIN** role.
4. Subsequent registered users will be assigned the standard **USER** role.
