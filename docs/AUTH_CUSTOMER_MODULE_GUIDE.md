# NextGen Digital Banking — Auth & Customer Module Guide

This guide provides a comprehensive developer and reviewer reference for the **Authentication & Customer Onboarding** domain module of the NextGen Digital Banking Platform.

---

## 1. Module Overview

The **Auth & Customer** module handles identity creation, secure authentication, cryptographic self-activation, public customer numbering, customer profile onboarding, and compliance KYC verification.

### Key Functional Responsibilities:
1. **Customer Identity & Authentication**: Self-service customer registration, password complexity validation, 6-digit numeric OTP generation/verification, and JWT authentication.
2. **Customer Onboarding & Profile**: Identity establishment, age validation ($\ge 18$), nominee allocation ($100.00\%$), unique public customer identifier generation (`CUST-XXXXXXXX`), and address management.
3. **Customer KYC Submission**: KYC document uploads (PAN, Aadhaar, Passport, Voter ID) via multipart or JSON payloads, duplicate detection, and transition to pending compliance review.
4. **Staff & Employee Lifecycle**: Admin zero-knowledge staff provisioning, 32-byte tokenized self-service activation (`/staff/activate`), unactivated account isolation, and staff login.
5. **Staff KYC Review Queue**: Active compliance queue (`/api/v1/staff/kyc/pending`), customer detail inspection, document review, and one-click KYC approval/rejection without manual UUID entry.

---

## 2. Customer Registration & OTP Workflow

```
Customer opens /register
        ↓
Inputs Username, Email, Password, Confirm Password
        ↓
Clicks "Create Account"
        ↓
Backend saves User (role=CUSTOMER) & emits UserRegisteredEvent to Outbox
        ↓
Backend auto-generates 6-digit cryptographic OTP (SHA-256 hash in auth_otps)
        ↓
CustomerOtpNotificationEvent dispatched in-memory AFTER_COMMIT
        ↓
Frontend transitions to "Verify Your Email" screen
        ↓
Customer enters 6-digit OTP & clicks "Verify OTP" (calls POST /api/v1/auth/otp/verify)
        ↓
OTP verified atomically & marked is_used=true in DB
        ↓
Frontend displays "Email Verified" success screen → Customer proceeds to /login
```

### Relevant API Endpoints:
* `POST /api/v1/auth/register` — Public customer account creation.
* `POST /api/v1/auth/otp/generate` — Manual or programmatic OTP trigger.
* `POST /api/v1/auth/otp/verify` — Atomically verifies 6-digit OTP against SHA-256 hash.
* `POST /api/v1/auth/otp/resend` — Invalidates prior active OTPs, generates a fresh OTP, and resets the 60-second cooldown timer.

### Development Mode Behavior (`APP_DEV_MODE=true`):
* The backend logs the generated OTP directly to the terminal console:
  ```
  [DEV FALLBACK - DO NOT USE IN PRODUCTION] Generated OTP for [john.doe@example.com] (CUSTOMER_REGISTRATION): [353354]
  ```
* The frontend displays an instructional badge guiding developers to copy the code from the backend logs.
* **Production Boundary**: In production (`APP_DEV_MODE=false`), fallback logs are completely suppressed. The separate **Notification** module subscribes to `CustomerOtpNotificationEvent` to send real SMS/Emails.

---

## 3. Customer Login

* **URL Route**: `/login` (Default root path `/` also routes here for unauthenticated visitors).
* **API Endpoint**: `POST /api/v1/auth/customer/login`
* **Flow**:
  1. Customer enters `username` and `password`.
  2. Backend validates credentials against BCrypt password hash.
  3. Enforces 5-attempt brute-force protection (locks account for 15 minutes on 5 consecutive failures).
  4. Returns JWT `accessToken` (15-minute validity) and `refreshToken` (7-day validity).
  5. Frontend stores JWT in secure memory/storage and renders the authenticated Customer Banking Portal.

---

## 4. Customer Profile & Public Customer Number

* **URL Route**: `/` (Authenticated Customer Portal $\rightarrow$ Profile Tab).
* **API Endpoints**:
  * `POST /api/v1/customers/profile` — Creates customer profile.
  * `GET /api/v1/customers/profile` — Retrieves profile for authenticated user.
* **Business Invariants**:
  * **BR-CUST-001**: One customer profile per user account.
  * **BR-CUST-003**: Age restriction (Must be $\ge 18$ years old).
  * **BR-CUST-004**: Nominee allocation sum must equal exactly `100.00%`.
* **Public Customer Number (`CUST-XXXXXXXX`)**:
  * Generated using Crockford's unambiguous Base-32 alphabet (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`).
  * Displayed on all customer cards, profile headers, and staff review queues.
  * Internal database relationships across Account, Card, and Loan modules continue to safely use canonical UUIDs (`customerId`).

---

## 5. KYC Document Submission

* **URL Route**: Profile Tab $\rightarrow$ "Submit KYC Documents" modal.
* **API Endpoints**:
  * `POST /api/v1/customers/kyc` (Supports `multipart/form-data` with actual file upload, or `application/json` with document metadata).
* **Flow**:
  1. Customer selects document type (`PAN`, `AADHAAR`, `PASSPORT`, `VOTER_ID`) and enters the document number.
  2. Uploads the document file (PDF, JPG, PNG).
  3. File is saved to local storage (`data/kyc-uploads`) via `KycDocumentStorageService`.
  4. Document number is stored as a one-way SHA-256 hash (`document_number_enc`) to prevent PII exposure while enforcing platform-wide duplicate document detection.
  5. Customer `kycStatus` is set to `PENDING`, and `KYCSubmittedEvent` is written to the Outbox.

---

## 6. Staff Login & Zero-Knowledge Self-Activation

### A. Staff Provisioning (Admin Action):
1. Admin opens `/` $\rightarrow$ "Staff & Employee Management" tab.
2. Fills in Staff Username and Corporate Email $\rightarrow$ clicks "Provision Staff Account".
3. Backend creates user with role `BANK_STAFF`, `isActive = false`, and an unmatchable placeholder password hash.
4. Generates a 32-byte `SecureRandom` token (256-bit entropy) and stores only its SHA-256 hash in `auth_activation_tokens`.
5. Emits `StaffActivationNotificationEvent` containing the activation URL: `http://localhost:5173/staff/activate?token=<raw-token>`.

### B. Development Activation Flow:
1. In `APP_DEV_MODE=true`, the backend console prints:
   ```
   [DEV FALLBACK - DO NOT USE IN PRODUCTION] Generated Staff Activation Link for [sarah_staff] (sarah@bank.com): [http://localhost:5173/staff/activate?token=eaa8c324...]
   ```
2. Developer copies the link or extracts the 64-character token.

### C. Self-Service Staff Activation:
1. Opening `/staff/activate?token=...` automatically pre-fills the activation token field.
2. Employee establishes their password (min 8 chars, uppercase, lowercase, digit, special char) and confirms it.
3. Submits `POST /api/v1/auth/staff/activate`.
4. Token is atomically verified and marked `is_used = true`. The user account is activated (`isActive = true`).
5. Redirects to `/staff/login`.

### D. Staff Login:
* **URL Route**: `/staff/login` (Accessible via switcher on login page or direct URL).
* **API Endpoint**: `POST /api/v1/auth/staff/login`
* Enforces role `BANK_STAFF` or `ADMIN`. Rejects unactivated or customer accounts.

---

## 7. Staff KYC Review Queue

* **URL Route**: Staff Portal $\rightarrow$ "KYC Verification Queue" tab.
* **API Endpoints**:
  * `GET /api/v1/staff/kyc/pending` — Retrieves all pending customer submissions.
  * `GET /api/v1/staff/kyc/{customerId}` — Retrieves full customer profile, addresses, and documents.
  * `POST /api/v1/staff/kyc/verify` — Submits review decision (`VERIFIED` or `REJECTED`).

### Review Workflow (Zero Manual UUID Entry):
1. Staff views the active queue showing customer names, public numbers (`CUST-XXXXXXXX`), document types, and submission timestamps.
2. Staff clicks **"Review KYC"** on any row.
3. An interactive modal displays the customer's personal details, registered address, and uploaded KYC document.
4. Staff enters compliance remarks and clicks **"Approve KYC"** or **"Reject KYC"**.
5. Backend atomically updates customer status, marks documents `APPROVED`/`REJECTED`, writes `KYCApprovedEvent` / `KYCRejectedEvent` to Outbox, and refreshes the queue.

---

## 8. Admin Staff Management

* **URL Route**: Admin Portal $\rightarrow$ "Staff & Employee Management" tab.
* **API Endpoints**:
  * `POST /api/v1/admin/staff` — Provisions new employee account.
  * `GET /api/v1/admin/staff` — Retrieves roster of bank staff.
  * `POST /api/v1/admin/staff/{userId}/resend-activation` — Atomically invalidates old activation tokens and dispatches a fresh activation link.
* **UI Features**:
  * Real-time roster table displaying username, email, role, and activation status (`Active` vs `Unactivated`).
  * "Resend Link" button for unactivated staff accounts.

---

## 9. Development & Test Setup

### A. Environment Configuration (`.env`):
```env
# JWT Secret (at least 32 characters / 256 bits)
JWT_SECRET=YourStrongLocalDevelopmentJwtSecretKeyAtLeast32BytesLong123!

# PostgreSQL Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nextgen_bank
SPRING_DATASOURCE_USERNAME=bank_admin
SPRING_DATASOURCE_PASSWORD=bank_password

# Admin Bootstrap (creates default admin on startup)
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin_bootstrap
BOOTSTRAP_ADMIN_EMAIL=admin@nextgenbank.com
BOOTSTRAP_ADMIN_PASSWORD=Admin@123456

# Local Development Mode
APP_DEV_MODE=true
FRONTEND_URL=http://localhost:5173
KYC_UPLOAD_DIR=./data/kyc-uploads
```

### B. Startup Commands:
1. **Start Backend**:
   ```bash
   mvn -f backend/pom.xml spring-boot:run
   ```
2. **Start Frontend**:
   ```bash
   cd frontend
   npm run dev
   ```
3. **Run Automated Tests**:
   ```bash
   mvn -f backend/pom.xml test
   cd frontend && npm run build
   ```

---

## 10. Security & Architecture Invariants

1. **Zero Plaintext Token / OTP Persistence**:
   * Staff activation tokens (32 bytes / 256 bits) and Customer OTPs (6 digits) are **never** stored in plaintext in the database or transactional Outbox. Only SHA-256 hashes are persisted.
2. **Post-Commit Event Semantics**:
   * Ephemeral notification events (`CustomerOtpNotificationEvent`, `StaffActivationNotificationEvent`) are dispatched via `TransactionSynchronization.afterCommit()`. A failure in notification delivery cannot roll back database transactions.
3. **Decoupled Notification Boundary**:
   * Auth contains zero dependencies on JavaMailSender, SMTP, SendGrid, SES, Mailgun, or SMS providers. Notification delivery is delegated entirely to the external Notification module.
4. **Development Fallback Isolation**:
   * Console logging of OTPs and activation links is strictly gated behind `app.dev-mode: true` (`APP_DEV_MODE=true`) and is completely suppressed in production (`APP_DEV_MODE=false`).
5. **IDOR & Object-Level Security**:
   * Customer endpoints derive identity directly from the authenticated JWT context (`authenticatedUserId`). Customers cannot view or modify data belonging to other customers.

---

## 11. Quick End-to-End Demo Checklist

Follow this sequence during a demo to showcase the entire Auth + Customer module:

- [ ] **1. Customer Registration & OTP**:
  - Navigate to `http://localhost:5173/register`.
  - Enter username `alice_customer`, email `alice@example.com`, password `Password123!`.
  - Click **"Create Account"** $\rightarrow$ UI transitions to "Verify Your Email".
  - Copy the 6-digit OTP printed in the backend console (`[DEV FALLBACK]`).
  - Enter the OTP $\rightarrow$ Click **"Verify OTP"** $\rightarrow$ UI confirms verification $\rightarrow$ Click **"Proceed to Sign In"**.

- [ ] **2. Customer Login & Profile Setup**:
  - Log in as `alice_customer` / `Password123!`.
  - On the Customer Portal, click **"Complete Profile Onboarding"**.
  - Fill in Name (`Alice Smith`), DOB ($\ge 18$), Phone (`9876543210`), Street, City, State, PIN, and Nominee ($100\%$).
  - Click **"Save Customer Profile"** $\rightarrow$ Profile appears with public number `CUST-XXXXXXXX` and `KYC Status: PENDING`.

- [ ] **3. Customer KYC Submission**:
  - Click **"Submit KYC Documents"**.
  - Select `PAN`, enter document number `ABCDE1234F`, select a document file.
  - Click **"Upload & Submit Document"** $\rightarrow$ Status changes to `Submitted (Pending Review)`.
  - Click **"Sign Out"**.

- [ ] **4. Admin Provisions Staff Account**:
  - Open `/staff/login` $\rightarrow$ Sign in as `admin_bootstrap` / `Admin@123456`.
  - In the "Staff & Employee Management" tab, enter Username `bob_staff` and Email `bob@nextgenbank.com`.
  - Click **"Provision Staff Account"** $\rightarrow$ Account appears as `Unactivated` in the roster table.
  - Check the backend console to view the generated activation URL (`/staff/activate?token=...`).
  - Sign out of admin.

- [ ] **5. Staff Self-Service Activation & Login**:
  - Paste the activation URL into the browser (`http://localhost:5173/staff/activate?token=...`).
  - Notice the token is pre-filled. Enter new password `StaffPassword123!` and confirm.
  - Click **"Activate Staff Account"** $\rightarrow$ Success confirmation is shown $\rightarrow$ Click **"Proceed to Staff Sign In"**.
  - Sign in as `bob_staff` / `StaffPassword123!`.

- [ ] **6. Staff KYC Verification Queue**:
  - In the Staff Portal, click the **"KYC Verification Queue"** tab.
  - Locate `Alice Smith` (`CUST-XXXXXXXX`) in the pending list.
  - Click **"Review KYC"** $\rightarrow$ Inspect personal information, address, and document details in the modal.
  - Enter remarks `"NSDL PAN verification verified"` and click **"Approve KYC"**.
  - Notice the item is removed from the pending queue.
  - Sign out of staff.

- [ ] **7. Verified Customer Portal Check**:
  - Sign back in as `alice_customer` / `Password123!`.
  - Notice the Customer Profile header now displays a green `KYC Verified` badge.
