# Auth Lambda (login por CPF)

AWS Lambda (Java 21) que autentica o cliente pelo CPF e devolve um JWT com papel `CLIENTE`. Chamada pelo API Gateway em
`POST /auth/cpf`. Para validar o CPF, gera um token curto (60 s) com papel `SERVICE` e consulta o OS Service
(`GET /api/clientes/documento/{cpf}`). Decisao registrada em `docs/adr/007-autenticacao-cpf-lambda.md`.

## Como testar

```powershell
.\mvnw.cmd clean verify     # testes + cobertura minima de 80%
```

Servidor local para testes manuais (porta 8090): veja `scripts/local/auth-cpf-e2e.ps1` no `oficina-workspace`.

## Variaveis

| Variavel | Para que serve |
|---|---|
| `JWT_SECRET` | mesmo segredo HS256 dos demais servicos |
| `OS_SERVICE_URL` | URL base do OS Service |

## Deploy

Lambda e API Gateway sao criados pelo Terraform (`oficina-infra`, arquivo `lambda_gateway.tf`). O workflow
`.github/workflows/ci.yml` roda build, testes e cobertura; o pacote `.jar` e o artefato publicado na Lambda.
