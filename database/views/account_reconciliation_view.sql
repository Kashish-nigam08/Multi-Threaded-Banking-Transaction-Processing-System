CREATE OR REPLACE VIEW account_reconciliation_view AS
SELECT
    a.account_id,
    a.balance AS actual_balance,

    COALESCE(SUM(
        CASE
            WHEN t.transaction_type = 'DEPOSIT'
                THEN t.amount

            WHEN t.transaction_type = 'WITHDRAWAL'
                THEN -t.amount

            WHEN t.transaction_type = 'TRANSFER'
                AND t.description LIKE 'Transfer to account %'
                THEN -t.amount

            WHEN t.transaction_type = 'TRANSFER'
                AND t.description LIKE 'Transfer from account %'
                THEN t.amount

            ELSE 0
        END
    ), 0) AS expected_balance

FROM accounts a

LEFT JOIN banking_transactions t
    ON a.account_id = t.account_id
    AND t.status = 'SUCCESS'

GROUP BY a.account_id, a.balance;
