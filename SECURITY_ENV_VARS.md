# Segurança de Variáveis de Ambiente

Este projeto foi atualizado para remover credenciais hardcoded dos arquivos `application.properties` e `docker-compose.yml`.

## O que foi removido
- Senha de banco de dados (`clinicfiapppass`)
- Credenciais SMTP (usuário e senha do Gmail)

## Como configurar

1. Copie o arquivo `.env.example` para `.env`:
```
cp .env.example .env
```
2. Edite os valores sensíveis:
```
SCHEDULER_DB_PASSWORD=senha-forte
NOTIFICATION_DB_PASSWORD=senha-forte
MAIL_USERNAME=seu-email@empresa.com
MAIL_PASSWORD=senha-ou-app-password
POSTGRES_PASSWORD=senha-forte
```
3. Suba os contêineres com o Docker (docker compose lê automaticamente variáveis do ambiente ou de export):
```
# Linux/macOS
export $(grep -v '^#' .env | xargs) && docker compose up -d

# Windows PowerShell
Get-Content .env | ForEach-Object { if ($_ -match '^[^#].*=') { $name,$value = $_.Split('='); [System.Environment]::SetEnvironmentVariable($name,$value) }}; docker compose up -d
```

## Boas práticas adicionais
- Nunca commitar `.env` real
- Usar secrets manager em produção (AWS Secrets Manager, Hashicorp Vault)
- Rotacionar senhas periodicamente
- Usar app password para SMTP (Gmail)

## Próximos passos sugeridos
- Adicionar suporte a `SPRING_CONFIG_IMPORT=optional:env[.env]` (Spring Boot 3.4+)
- Externalizar JWT secret
- Adicionar perfis (`application-prod.properties`) sem defaults fracos

## Referência
Variáveis utilizadas:
- Banco: `SCHEDULER_DB_URL`, `SCHEDULER_DB_USER`, `SCHEDULER_DB_PASSWORD`, etc.
- Kafka: `KAFKA_BOOTSTRAP_SERVERS`
- Email: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SSL_TRUST`

