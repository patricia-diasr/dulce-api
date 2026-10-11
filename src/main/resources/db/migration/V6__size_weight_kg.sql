-- ============================================================================
-- V6__size_weight_kg.sql
-- Peso de cada tamanho em kg, usado nos relatórios de produção e vendas.
-- ============================================================================

ALTER TABLE size ADD COLUMN weight_kg NUMERIC(5, 2);

UPDATE size SET weight_kg = 1 WHERE name = '1 kg';
UPDATE size SET weight_kg = 2 WHERE name = '2 kg';
UPDATE size SET weight_kg = 4 WHERE name = '4 kg';

ALTER TABLE size ALTER COLUMN weight_kg SET NOT NULL;
