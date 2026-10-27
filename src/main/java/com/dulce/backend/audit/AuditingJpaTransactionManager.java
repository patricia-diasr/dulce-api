package com.dulce.backend.audit;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;

public class AuditingJpaTransactionManager extends JpaTransactionManager {

    public AuditingJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        DataSource dataSource = getDataSource();
        if (definition.isReadOnly() || dataSource == null) {
            return;
        }

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT set_config('app.current_actor', ?, true)")) {
            statement.setString(1, AuditActor.current());
            statement.execute();
        } catch (SQLException e) {
            throw new CannotCreateTransactionException(
                    "Falha ao registrar o autor da auditoria na transação.", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
