<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK1]
@difficulty medium
@files [backend/src/main/java/com/cbrviewer/controller/AdminController.java, backend/src/main/java/com/cbrviewer/service/AdminUserService.java, backend/src/main/java/com/cbrviewer/service/LlmConfigService.java, backend/src/main/java/com/cbrviewer/service/AuditLogService.java, backend/src/main/java/com/cbrviewer/service/SystemLogService.java, backend/src/main/java/com/cbrviewer/model/LlmConfig.java, backend/src/main/java/com/cbrviewer/model/AuditLog.java, backend/src/main/java/com/cbrviewer/model/SystemLog.java, backend/src/main/java/com/cbrviewer/repository/LlmConfigRepository.java, backend/src/main/java/com/cbrviewer/repository/AuditLogRepository.java, backend/src/main/java/com/cbrviewer/repository/SystemLogRepository.java, backend/src/main/java/com/cbrviewer/util/EncryptionUtil.java, backend/src/main/java/com/cbrviewer/dto/AdminUserDto.java, backend/src/main/java/com/cbrviewer/dto/LlmConfigDto.java, backend/src/main/java/com/cbrviewer/dto/LogEntryDto.java]

# BLUEPRINT: TASK12

## 1. IDENTITY

### This Task IS:
- Creating AdminController with admin-only endpoints for users, files, logs, LLM config
- Creating AdminUserService for user CRUD (list, create, update, delete users)
- Creating LlmConfigService for API key management with encryption/decryption
- Creating EncryptionUtil for AES encryption of API keys
- Creating AuditLogService for recording user actions
- Creating SystemLogService for retrieving system logs
- Creating LlmConfig, AuditLog, SystemLog entities
- Implementing admin-only access control (403 for non-admins)

### This Task IS NOT:
- Creating admin frontend UI (TASK13)
- Implementing TTS logic (TASK8-10)
- Basic auth (TASK1 complete)
- Audit log integration into other controllers (TASK15)

### Anti-Hallucination Anchors:
- If admin endpoints unclear -> Follow AI_PROMPT.md:L297-306 API spec
- If LlmConfig schema unclear -> Follow AI_PROMPT.md:L231-240 exactly
- If encryption unclear -> Use AES-256 with secret key from config
- If admin role check unclear -> Check user.role == 'ADMIN'

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement admin frontend - belongs to TASK13
- [ ] DO NOT implement audit log writing in controllers - belongs to TASK15
- [ ] DO NOT implement file management admin - use existing FileController

**Architecture Guardrails:**
- [ ] DO NOT use JWT for admin tokens - same session auth as regular users
- [ ] DO NOT store encryption key in code - use application.yml

**Quality Guardrails:**
- [ ] DO NOT add complex role hierarchy - only USER and ADMIN
- [ ] DO NOT add complex log searching - basic filtering is sufficient

**Security Guardrails:**
- [ ] NEVER store API keys in plain text - encrypt in database (AI_PROMPT.md:L488)
- [ ] NEVER expose decrypted API keys in API responses - mask with asterisks
- [ ] NEVER allow non-admin users to access admin endpoints (403)

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Admin requirements (§4), API endpoints (§2)
- AI_PROMPT.md:L482-488 for admin acceptance criteria
- AI_PROMPT.md:L297-306 for admin API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L231-262 for llm_config, audit_logs, system_logs schemas
- AI_PROMPT.md:L617-625 for admin backend implementation guidance
- AI_PROMPT.md:L316-340 for REST controller pattern

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for Spring Security role-based access
- Context7 for AES encryption in Java

### Inherited From Dependencies:
- TASK1: Auth with User entity (includes role field), SecurityConfig
- TASK0: schema.sql with all admin tables

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK1 SecurityConfig exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/config/SecurityConfig.java` | File exists |
| Schema has llm_config table | `grep -q "CREATE TABLE llm_config" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| Schema has audit_logs table | `grep -q "CREATE TABLE audit_logs" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| Schema has system_logs table | `grep -q "CREATE TABLE system_logs" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| User entity has role | `grep -q "role" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/User.java` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| AdminController exists | AI_PROMPT.md:L67 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AdminController.java` | - |
| LlmConfigService exists | AI_PROMPT.md:L487 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/LlmConfigService.java` | - |
| EncryptionUtil exists | AI_PROMPT.md:L488 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/util/EncryptionUtil.java` | - |
| Admin endpoint protection | AI_PROMPT.md:L435 | AUTO | `grep -q "ADMIN" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AdminController.java` | - |
| API key encryption | AI_PROMPT.md:L488 | AUTO | `grep -q "encrypt" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/LlmConfigService.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| AdminController | CREATE | backend/src/main/java/com/cbrviewer/controller/AdminController.java | `test -f` |
| AdminUserService | CREATE | backend/src/main/java/com/cbrviewer/service/AdminUserService.java | `test -f` |
| LlmConfigService | CREATE | backend/src/main/java/com/cbrviewer/service/LlmConfigService.java | `test -f` |
| AuditLogService | CREATE | backend/src/main/java/com/cbrviewer/service/AuditLogService.java | `test -f` |
| SystemLogService | CREATE | backend/src/main/java/com/cbrviewer/service/SystemLogService.java | `test -f` |
| LlmConfig entity | CREATE | backend/src/main/java/com/cbrviewer/model/LlmConfig.java | `test -f` |
| AuditLog entity | CREATE | backend/src/main/java/com/cbrviewer/model/AuditLog.java | `test -f` |
| SystemLog entity | CREATE | backend/src/main/java/com/cbrviewer/model/SystemLog.java | `test -f` |
| EncryptionUtil | CREATE | backend/src/main/java/com/cbrviewer/util/EncryptionUtil.java | `test -f` |
| Repositories | CREATE | backend/src/main/java/com/cbrviewer/repository/*Repository.java | `ls` |
| DTOs | CREATE | backend/src/main/java/com/cbrviewer/dto/Admin*.java, LlmConfigDto.java, LogEntryDto.java | `ls` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md admin requirements
2. Verify TASK1 artifacts exist
3. Review admin table schemas

**Gate:** All pre-conditions verified, schemas understood

### Phase 2: Model Layer
1. Create LlmConfig.java entity with: id, provider, apiKeyEncrypted, modelName, isActive, priority, createdAt, updatedAt
2. Create AuditLog.java entity with: id, userId, action, entityType, entityId, details, ipAddress, createdAt
3. Create SystemLog.java entity with: id, level, message, stackTrace, createdAt
4. Create repositories for each entity
5. Create DTOs: AdminUserDto, LlmConfigDto (mask apiKey), LogEntryDto

**Gate:** Model classes compile, match schemas

### Phase 3: Encryption Utility
1. Create EncryptionUtil.java:
   - encrypt(String plainText, String secretKey) returning String (base64)
   - decrypt(String cipherText, String secretKey) returning String
   - Use AES-256-GCM for encryption
   - Read secret key from application.yml (app.encryption.key)
   - Handle encryption errors gracefully

**Gate:** Encryption util compiles, can encrypt/decrypt

### Phase 4: Services
1. Create AdminUserService.java:
   - getAllUsers() returning List<AdminUserDto>
   - createUser(username, email, password, role) - hash password, save
   - updateUser(id, updates) - update fields, re-hash if password changed
   - deleteUser(id) - remove user
2. Create LlmConfigService.java:
   - getAllConfigs() returning List<LlmConfigDto> (masked keys)
   - getActiveConfig(provider) - for OCR/TTS services
   - getDecryptedApiKey(id) - return decrypted key (internal use only)
   - updateConfig(id, config) - encrypt key, save
   - createConfig(config) - encrypt key, save
3. Create AuditLogService.java:
   - logAction(userId, action, entityType, entityId, details, ipAddress)
   - getLogs(filters) - filter by user, action, date range
4. Create SystemLogService.java:
   - log(level, message, stackTrace) - save log entry
   - getLogs(level, startDate, endDate) - filtered retrieval

**Gate:** Services compile, encryption integrated

### Phase 5: Controller Layer
1. Create AdminController.java with:
   - @PreAuthorize("hasRole('ADMIN')") or manual role check
   - GET /api/admin/users - list users
   - POST /api/admin/users - create user
   - PUT /api/admin/users/{id} - update user
   - DELETE /api/admin/users/{id} - delete user
   - GET /api/admin/logs - get system logs
   - GET /api/admin/audit - get audit logs
   - GET /api/admin/llm-config - get LLM configs
   - PUT /api/admin/llm-config/{id} - update LLM config
2. Return 403 for non-admin users

**Gate:** Endpoints defined, admin-only access enforced

### Phase 6: Security Configuration Update
1. Update SecurityConfig.java:
   - Add /api/admin/** to require ADMIN role
   - Ensure 403 returned for non-admin access

**Gate:** Admin endpoints protected

### Phase 7: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify encryption util works
4. Verify admin role check in controller
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Encryption key length | Using 256-bit AES key from config | HIGH | Standard AES-256 |
| U2 | Role-based security | Using @PreAuthorize or manual check | MEDIUM | Either approach works |
| U3 | API key masking | Show first/last 4 chars, mask middle | HIGH | Common pattern |
| U4 | Log pagination | Return last 100 by default | MEDIUM | Not specified |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| SecurityConfig.java | Add admin endpoint protection | - | Access control |
| application.yml | Add encryption.key config | EncryptionUtil | Encryption key |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| AdminController.java | All admin services | Admin REST endpoints |
| AdminUserService.java | UserRepository, AuthService | User management |
| LlmConfigService.java | LlmConfigRepository, EncryptionUtil | LLM config management |
| AuditLogService.java | AuditLogRepository | Audit logging |
| SystemLogService.java | SystemLogRepository | System logs |
| LlmConfig.java | - | LlmConfig entity |
| AuditLog.java | - | AuditLog entity |
| SystemLog.java | - | SystemLog entity |
| EncryptionUtil.java | - | Encryption/decryption |
| Repositories | Entities | Data access |
| DTOs | - | Transfer objects |

### Breaking Changes:
None - new endpoints and services
