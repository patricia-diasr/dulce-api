-- ============================================================================
-- V4__customer_email_nullable.sql
-- E-mail passa a ser opcional: clientes cadastrados pelo admin por telefone, sem e-mail,
-- não conseguem logar (RF02 depende de e-mail) — mas o cadastro em si é válido.
-- ============================================================================

ALTER TABLE customer ALTER COLUMN email DROP NOT NULL;
