-- ============================================================================
-- V7__audit_triggers.sql
-- Auditoria por gatilhos: toda inserção, alteração ou exclusão nas tabelas de negócio
-- é gravada em audit_log, na mesma transação da mudança.
-- O autor vem da variável de sessão app.current_actor, definida pela aplicação no início
-- de cada transação de escrita (sem ela, o autor é DATABASE = SQL manual).
-- ============================================================================

ALTER TABLE audit_log ADD COLUMN transaction_id BIGINT;
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

CREATE OR REPLACE FUNCTION audit_row_change() RETURNS trigger AS $$
DECLARE
    v_old jsonb;
    v_new jsonb;
    v_row jsonb;
    v_actor text;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_new := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        v_old := to_jsonb(OLD);
        v_new := to_jsonb(NEW);
        IF v_old = v_new THEN
            RETURN NULL; -- atualização que não mudou nada não gera registro
        END IF;
    ELSE
        v_old := to_jsonb(OLD);
    END IF;

    -- Nunca copiar o hash de senha para o log; só sinaliza quando ele foi trocado.
    IF TG_TABLE_NAME = 'administrator' THEN
        IF v_old IS NOT NULL AND v_new IS NOT NULL
           AND (v_old ->> 'password_hash') IS DISTINCT FROM (v_new ->> 'password_hash') THEN
            v_old := jsonb_set(v_old, '{password_hash}', '"***"');
            v_new := jsonb_set(v_new, '{password_hash}', '"*** (alterada)"');
        ELSE
            IF v_old IS NOT NULL THEN
                v_old := jsonb_set(v_old, '{password_hash}', '"***"');
            END IF;
            IF v_new IS NOT NULL THEN
                v_new := jsonb_set(v_new, '{password_hash}', '"***"');
            END IF;
        END IF;
    END IF;

    v_row := COALESCE(v_new, v_old);
    v_actor := COALESCE(NULLIF(current_setting('app.current_actor', true), ''), 'DATABASE');

    INSERT INTO audit_log (table_name, record_id, operation, old_data, new_data, changed_by, transaction_id)
    VALUES (TG_TABLE_NAME::text, (v_row ->> 'id')::bigint, TG_OP::audit_operation,
            v_old, v_new, v_actor, txid_current());

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_customer AFTER INSERT OR UPDATE OR DELETE ON customer
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_administrator AFTER INSERT OR UPDATE OR DELETE ON administrator
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_flavor AFTER INSERT OR UPDATE OR DELETE ON flavor
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_flavor_size AFTER INSERT OR UPDATE OR DELETE ON flavor_size
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_cake_order AFTER INSERT OR UPDATE OR DELETE ON cake_order
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_order_item AFTER INSERT OR UPDATE OR DELETE ON order_item
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_invoice AFTER INSERT OR UPDATE OR DELETE ON invoice
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_payment AFTER INSERT OR UPDATE OR DELETE ON payment
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_schedule_block AFTER INSERT OR UPDATE OR DELETE ON schedule_block
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();
CREATE TRIGGER audit_notification_type AFTER INSERT OR UPDATE OR DELETE ON notification_type
    FOR EACH ROW EXECUTE FUNCTION audit_row_change();

CREATE OR REPLACE FUNCTION audit_log_guard() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' AND COALESCE(current_setting('app.audit_purge', true), '') = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'audit_log é somente de acréscimo: % não permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_append_only BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_guard();
