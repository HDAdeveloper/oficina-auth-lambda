# Auth Lambda — documentação do serviço

## Responsabilidade

Função **AWS Lambda** (Java 21) que autentica o **cliente final pelo CPF** e devolve um JWT com papel `CLIENTE`. Fica atrás do API Gateway em `POST /auth/cpf`.
Subdomínio **genérico**: não tem regra de negócio própria; só valida o documento e confirma que o cliente existe. Decisão em [ADR-007](adr/007-autenticacao-cpf-lambda.md).

## Como funciona

1. Recebe `{"cpf": "..."}` e valida o CPF (dígitos verificadores).
2. Gera um token de serviço de 60 s (papel `SERVICE`) e chama o OS Service: `GET /api/clientes/documento/{cpf}`.
3. Se o cliente existe, assina e devolve um JWT (`roles: [CLIENTE]`) com o mesmo `JWT_SECRET` dos serviços. Se não existe, responde 404; se o CPF é inválido, 400.
4. O cliente usa o token em `GET /api/minhas-ordens` (única rota liberada para `CLIENTE`).

| Variável | Para que serve |
|---|---|
| `JWT_SECRET` | segredo HS256 compartilhado (vem do Terraform/Secret, nunca do Git) |
| `OS_SERVICE_URL` | URL base do OS Service |
| `TOKEN_MINUTOS` | validade do token do cliente (padrão 60) |

## Estrutura do código

`Cpf` (validação), `JwtHs256` (assinatura), `AutenticarPorCpf` (caso de uso), `ClienteGateway` / `OsServiceClienteGateway` (porta e adaptador HTTP),
`AuthHandler` (entrada da Lambda), `LocalServer` (servidor local na porta 8090 para testes).

## Testes e qualidade

- Testes unitários (JUnit 5, Mockito) do CPF, do JWT, do caso de uso e do handler.
- Cobertura mínima de **80% das linhas** aplicada no build (JaCoCo em `mvnw verify`) e em cada Pull Request.
- Análise estática no SonarQube Cloud pela pipeline. Evidências de cobertura: [`evidencias/`](evidencias/).
- Como rodar: `.\mvnw.cmd clean verify` (relatório em `target\site\jacoco\index.html`).

## Deploy

O `.github/workflows/ci.yml` constrói o `.jar` e, na `main` com `DEPLOY_ENABLED=true`, atualiza a função (`aws lambda update-function-code`). A função e o API Gateway são criados pelo Terraform no repositório `oficina-infra`.

## Documentação compartilhada do projeto

| Documento | Conteúdo |
|---|---|
| [`arquitetura.md`](arquitetura.md) | Visão geral, hexagonal, dados, Saga, mensageria, segurança, CI/CD |
| [`ddd.md`](ddd.md) | Contextos, agregados, estados e linguagem ubíqua (imagens em [`diagramas/ddd/`](diagramas/ddd/)) |
| [`eventos.md`](eventos.md) | Comandos, eventos, envelope, fluxo feliz e compensações da Saga |
| [`adr/`](adr/) | Decisões de arquitetura e justificativas |
