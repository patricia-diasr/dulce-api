package com.dulce.backend.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPurgeJob.class);

    private final JdbcClient jdbcClient;
    private final int retentionDays;

    public AuditLogPurgeJob(
            JdbcClient jdbcClient, @Value("${app.audit.retention-days}") int retentionDays) {
        this.jdbcClient = jdbcClient;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.audit.purge-cron}", zone = "America/Sao_Paulo")
    @Transactional
    public int purge() {
        if (retentionDays < 1) {
            log.warn("Expurgo de auditoria ignorado: retenção inválida ({} dias).", retentionDays);
            return 0;
        }

        jdbcClient
                .sql("SELECT set_config('app.audit_purge', 'on', true)")
                .query(String.class)
                .single();

        int deleted =
                jdbcClient
                        .sql(
                                "DELETE FROM audit_log WHERE created_at < now() - make_interval(days => :days)")
                        .param("days", retentionDays)
                        .update();

        log.info(
                "Expurgo de auditoria: {} registro(s) com mais de {} dias removido(s).",
                deleted,
                retentionDays);
        return deleted;
    }
}
