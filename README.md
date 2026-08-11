# Dulce — Backend

API do **Dulce** (Digitalização e Unificação da Logística de Confeitaria e Encomendas), sistema
de gestão de encomendas de uma confeitaria artesanal. Este repositório contém apenas o backend
(Spring Boot); o frontend (React) vive em um repositório separado.

## Pré-requisitos

- Java 25 (JDK)
- Maven 3.9.x
- PostgreSQL 18.x instalado localmente (sem Docker, conforme decidido para o ambiente de dev)
- (Opcional) um servidor SMTP de testes local, ou credenciais reais de um provedor de e-mail

## Subindo o ambiente local

1. Crie o banco e o usuário local (ajuste conforme preferir; esses são os valores padrão do
   perfil `dev`):

   ```sql
   CREATE DATABASE dulce_dev;
   CREATE USER dulce WITH PASSWORD 'dulce';
   GRANT ALL PRIVILEGES ON DATABASE dulce_dev TO dulce;
   ```

2. Defina as variáveis de ambiente necessárias (veja a tabela abaixo) ou use os defaults do
   perfil `dev`, que já apontam para `localhost`.

3. Rode a aplicação:

   ```bash
   mvn spring-boot:run
   ```

   O Flyway aplica a migração automaticamente na primeira subida. A API sobe em
   `http://localhost:8080` e o Swagger UI em `http://localhost:8080/docs`.

## Variáveis de ambiente

| Variável | Obrigatória | Default (dev) | Descrição |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | não | `dev` | Perfil ativo (`dev` ou `prod`) |
| `SERVER_PORT` | não | `8080` | Porta da API |
| `DB_HOST` | não (dev) / sim (prod) | `localhost` | Host do PostgreSQL |
| `DB_PORT` | não (dev) / sim (prod) | `5432` | Porta do PostgreSQL |
| `DB_NAME` | não (dev) / sim (prod) | `dulce_dev` | Nome do banco |
| `DB_USERNAME` | não (dev) / sim (prod) | `dulce` | Usuário do banco |
| `DB_PASSWORD` | não (dev) / sim (prod) | `dulce` | Senha do banco |
| `MAIL_HOST` | não (dev) / sim (prod) | `localhost` | Host SMTP |
| `MAIL_PORT` | não (dev) / sim (prod) | `1025` | Porta SMTP |
| `MAIL_USERNAME` | não (dev) / sim (prod) | vazio | Usuário SMTP |
| `MAIL_PASSWORD` | não (dev) / sim (prod) | vazio | Senha SMTP |
| `APP_JWT_SECRET` | **sim** | — | Segredo usado para assinar os tokens JWT |
| `APP_JWT_EXPIRATION_MINUTES` | não | `480` | Validade do token JWT, em minutos |
| `APP_CORS_ALLOWED_ORIGINS` | não | `http://localhost:5173` | Origem(ns) do frontend, separadas por vírgula |

Em produção, essas variáveis devem ser configuradas diretamente na plataforma de hospedagem
(ex.: Railway), nunca em arquivo versionado. `application-prod.yml` fica de fora do Git por isso
(veja `application-prod.yml.example` como referência do que ele contém).

## Estrutura de pastas

Pacotes organizados por domínio/funcionalidade, não por tipo técnico:

```
com.dulce.backend
├── auth            # login sem senha, JWT
├── customer        # clientes
├── order            # pedidos e itens de pedido
├── catalog         # recheios, tamanhos, matriz de preços
├── schedule        # bloqueios de agenda / calendário
├── payment         # cobrança e pagamentos
├── notification    # NotificationService e histórico de envios
├── audit           # auditoria
└── common
    ├── config      # segurança, CORS, OpenAPI
    └── exception   # tratamento global de erros
```

## Formatação de código

```bash
mvn spotless:apply   # formata o código automaticamente
mvn spotless:check   # verifica formatação (roda também no "mvn verify")
```

## Controle de versão

**Branches**
- `main`: sempre estável e implantável.
- `feature/nome-da-funcionalidade`: para cada nova funcionalidade.
- `fix/nome-do-bug`: correções.

Merge em `main` sempre via Pull Request.

**Commits** — [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: adiciona validação de prazo de 72h na criação de pedido
fix: corrige cálculo do valor total do pedido
docs: atualiza README com variáveis de ambiente
```

Tipos usados: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `style`.
