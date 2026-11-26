# Banking System — Security Implementation

## 1. Authentication

The application implements stateless JWT-based authentication using Spring Security.

- BCrypt password hashing with strength 12.
- JWT signing using HS256.
- Token validity of 30 minutes.
- Signature, issuer and expiration validation.
- User status verification.
- Environment-based JWT secret configuration.

## 2. User Roles

The application supports two roles:

- CUSTOMER: Access to personally owned bank accounts.
- ADMIN: Administrative access to banking operations and system reports.

Public registration creates CUSTOMER users only.

## 3. Authorization Matrix

| Operation | CUSTOMER | ADMIN |
|---|---|---|
| Create own account | Allowed | Denied |
| Administrative account creation | Denied | Allowed |
| View balance | Own accounts | All accounts |
| Deposit | Own accounts | All accounts |
| Withdraw | Own accounts | All accounts |
| Transfer | Own sender account | All accounts |
| Transaction history | Own accounts | All accounts |
| Account status management | Denied | Allowed |
| System reports | Denied | Allowed |

Unspecified endpoints are denied by default.

## 4. Account Ownership Protection

Account ownership is verified using the authenticated user's customer ID and the requested account ID.

Customer account creation derives the customer ID from the authenticated identity instead of accepting it from the request body.

Transfer authorization verifies ownership of the sender account while allowing transfers to other valid accounts.

## 5. Administrator Provisioning

Administrators are provisioned through an explicitly enabled startup initializer.

Required environment variables:

- BANKING_ADMIN_ENABLED
- BANKING_ADMIN_USERNAME
- BANKING_ADMIN_PASSWORD

Administrator provisioning is disabled by default and excluded from test and benchmark environments.

## 6. Security Error Responses

| HTTP Status | Meaning |
|---|---|
| 400 | Invalid request |
| 401 | Missing or invalid authentication |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 409 | Duplicate resource or transaction conflict |

## 7. Automated Testing

The project includes 50 automated tests:

- 29 core banking, concurrency, reporting and financial integrity tests.
- 8 authentication tests.
- 13 HTTP security integration tests.

Security testing covers password hashing, duplicate registration, invalid credentials, disabled users, JWT validation, role restrictions and unauthorized account access.

## 8. Current Limitations and Future Improvements

- Login rate limiting.
- JWT revocation and refresh tokens.
- Multi-factor authentication.
- Production-grade secret management.
- HTTPS deployment.
- Additional security audit logging.
- Service-level authorization enforcement.
- Additional token expiration and tampering tests.
- Administrator-side duplicate account error handling.
- Customer deposits simulate funding rather than integrating with an external payment provider.

This application is an academic banking transaction processing system and is not intended for production financial use.
