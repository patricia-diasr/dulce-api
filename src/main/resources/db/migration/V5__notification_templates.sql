-- ============================================================================
-- V5__notification_templates.sql
-- Identificador fixo (code) e assunto editável (subject) para os templates de notificação,
-- e semeadura dos 8 tipos fixos (RF29/RF31 + um adicional de pagamento).
-- ============================================================================

ALTER TABLE notification_type ADD COLUMN code VARCHAR(50);
ALTER TABLE notification_type ADD COLUMN subject VARCHAR(200);

INSERT INTO notification_type (code, description, subject, template_content) VALUES
('ORDER_CREATED', 'Pedido criado', 'Pedido recebido, {{customerName}}! Confira os detalhes',
 '<p>Olá, {{customerName}}!</p><p>Seu pedido <strong>#{{orderId}}</strong> foi recebido e está em análise. Você será avisado assim que ele for avaliado.</p><p><strong>Retirada:</strong> {{pickupDate}} às {{pickupTime}}</p>{{orderItems}}<p><strong>Valor total:</strong> {{orderTotal}}</p>{{discountNotice}}{{orderNotesNotice}}<p>Guarde este e-mail como comprovante do seu pedido.</p>'),

('ORDER_UPDATED', 'Pedido editado (cliente ou admin)', 'Seu pedido #{{orderId}} foi atualizado',
 '<p>Olá, {{customerName}}!</p><p>Seu pedido <strong>#{{orderId}}</strong> foi atualizado. Confira abaixo como ele ficou:</p><p><strong>Status atual:</strong> {{orderStatus}}</p><p><strong>Retirada:</strong> {{pickupDate}} às {{pickupTime}}</p>{{orderItems}}<p><strong>Valor total:</strong> {{orderTotal}}</p>{{discountNotice}}{{orderNotesNotice}}<p>Guarde este e-mail como comprovante atualizado do seu pedido.</p>'),

('ORDER_ACCEPTED', 'Pedido aceito', 'Pedido #{{orderId}} aceito!',
 '<p>Boas notícias, {{customerName}}! Seu pedido <strong>#{{orderId}}</strong> foi aceito.</p><p><strong>Retirada:</strong> {{pickupDate}} às {{pickupTime}}</p>{{orderItems}}<p><strong>Valor total:</strong> {{orderTotal}}</p>{{discountNotice}}'),

('ORDER_REJECTED', 'Pedido recusado', 'Sobre seu pedido #{{orderId}}',
 '<p>Olá, {{customerName}}. Infelizmente não foi possível aceitar seu pedido <strong>#{{orderId}}</strong>.</p>{{orderItems}}<p>Entre em contato conosco para mais informações.</p>{{refundNotice}}'),

('ORDER_CANCELED', 'Pedido cancelado', 'Pedido #{{orderId}} cancelado',
 '<p>Olá, {{customerName}}. Seu pedido <strong>#{{orderId}}</strong> foi cancelado.</p>{{orderItems}}{{refundNotice}}'),

('ORDER_COMPLETED', 'Pedido concluído', 'Pedido #{{orderId}} concluído — obrigado!',
 '<p>Olá, {{customerName}}! Seu pedido <strong>#{{orderId}}</strong> foi retirado. Esperamos que aproveite!</p>{{orderItems}}<p><strong>Valor total:</strong> {{orderTotal}}</p>'),

('PICKUP_REMINDER', 'Lembrete 24h antes da retirada', 'Lembrete: retirada do seu pedido amanhã',
 '<p>Olá, {{customerName}}! Passando para lembrar que seu pedido <strong>#{{orderId}}</strong> está agendado para retirada amanhã, {{pickupDate}} às {{pickupTime}}.</p>{{orderItems}}<p><strong>Valor total:</strong> {{orderTotal}}</p>'),

('PAYMENT_UPDATED', 'Pagamento registrado, editado ou excluído', 'Pagamento do pedido #{{orderId}} atualizado',
 '<p>Olá, {{customerName}}!</p><p>O pagamento do seu pedido <strong>#{{orderId}}</strong> foi atualizado.</p><p><strong>Status financeiro:</strong> {{paymentStatus}}</p><p><strong>Valor total do pedido:</strong> {{orderTotal}}</p><p><strong>Valor pago até o momento:</strong> {{amountPaid}}</p>{{orderItems}}');

ALTER TABLE notification_type ALTER COLUMN code SET NOT NULL;
ALTER TABLE notification_type ADD CONSTRAINT uq_notification_type_code UNIQUE (code);
ALTER TABLE notification_type ALTER COLUMN subject SET NOT NULL;
