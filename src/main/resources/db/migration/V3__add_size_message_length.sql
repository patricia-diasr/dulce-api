-- ============================================================================
-- V3__add_size_message_length.sql
-- Limite de caracteres da mensagem no bolo, por tamanho (RF06: 1kg=12, 2kg=20, 4kg=30).
-- ============================================================================

ALTER TABLE size ADD COLUMN max_message_length SMALLINT NOT NULL DEFAULT 0;

UPDATE size SET max_message_length = 12 WHERE name = '1 kg';
UPDATE size SET max_message_length = 20 WHERE name = '2 kg';
UPDATE size SET max_message_length = 30 WHERE name = '4 kg';

ALTER TABLE size ALTER COLUMN max_message_length DROP DEFAULT;
