# DocuFind Oracle Database Setup & Troubleshooting Guide

## 1. Database Connection Requirements
- **Database Engine**: Oracle Database 19c / 12c+
- **Host**: `localhost` (default)
- **Port**: `1521` (default)
- **Service Name / SID**: `orcl` (configurable)
- **Default Learning Credentials**:
  - Username: `system`
  - Password: `livewire`

> **Note on Security**: In production environments, never run an application using the `SYSTEM` administrative account. Create a dedicated application schema (e.g., `DOCUFIND_USER`) with standard table creation and data access grants.

## 2. Applying Schema Script
You can apply `database/schema.sql` using Oracle SQL*Plus or SQL Developer.

Using SQL*Plus in PowerShell/Command Prompt:
```powershell
sqlplus system/livewire@localhost:1521/orcl @database/schema.sql
```
or via standard SQL*Plus connection:
```cmd
sqlplus system/livewire@orcl
SQL> @database/schema.sql
```

The schema script creates the necessary sequences, tables (`APP_USERS`, `INDEXED_FOLDERS`, `DOCUMENTS`, `SAVED_SEARCHES`, `SEARCH_HISTORY`, `INDEXING_LOGS`), indexes, and foreign keys with idempotent blocks (`IF SQLCODE != -955`).

## 3. Configuration
The application reads its settings from `config/application.properties`.
If your Oracle database uses a pluggable database (PDB) service name (such as `ORCLPDB1` or `XEPDB1`), update the JDBC URL:
```properties
db.url=jdbc:oracle:thin:@localhost:1521/orclpdb1
db.user=system
db.password=livewire
```

## 4. Oracle Backup & Recovery Guidelines

### Database Backup
Do not treat a simple CSV metadata export as an Oracle database backup. For actual database backups, use Oracle Data Pump (`expdp`) or RMAN:
```cmd
expdp system/livewire@orcl schemas=SYSTEM include=TABLE:\"IN\(\'APP_USERS\',\'INDEXED_FOLDERS\',\'DOCUMENTS\',\'SAVED_SEARCHES\',\'SEARCH_HISTORY\',\'INDEXING_LOGS\'\)\" directory=DATA_PUMP_DIR dumpfile=docufind_backup.dmp logfile=docufind_export.log
```

### Database Recovery
To restore tables and records using Oracle Data Pump (`impdp`):
```cmd
impdp system/livewire@orcl directory=DATA_PUMP_DIR dumpfile=docufind_backup.dmp logfile=docufind_import.log table_exists_action=replace
```

## 5. Troubleshooting Common Connection Issues
- **`ORA-12541: TNS:no listener`**: The Oracle TNS Listener service (`OracleOraDB19Home1TNSListener`) is stopped. Start it via `Start-Service OracleOraDB19Home1TNSListener` or Windows Services Manager.
- **`ORA-12514: TNS:listener does not currently know of service requested`**: Verify your service name using `lsnrctl status`. You may need to change `orcl` to your PDB service name.
- **`ORA-01017: invalid username/password`**: Verify that the password for user `system` matches `livewire` in `config/application.properties`.
