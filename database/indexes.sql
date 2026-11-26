
-- Banking System Performance Indexes
--
-- These indexes are included here for documentation.
-- Verify existing indexes before executing any statement.
-- They may already be included in schema.sql.

-- Account transaction history
-- CREATE INDEX idx_account_history
-- ON banking_transactions (
--     account_id,
--     created_at DESC,
--     transaction_id DESC
-- );

-- Filtered account transaction history
-- CREATE INDEX idx_account_type_history
-- ON banking_transactions (
--     account_id,
--     transaction_type,
--     created_at DESC,
--     transaction_id DESC
-- );

-- Successful operation reporting
-- CREATE INDEX idx_successful_operations
-- ON banking_transactions (
--     status,
--     transaction_type,
--     operation_uuid
-- );

-- Failed transaction reporting
-- CREATE INDEX idx_failed_transaction_type
-- ON failed_transactions (transaction_type);
