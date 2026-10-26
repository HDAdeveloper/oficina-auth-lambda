package br.com.oficina.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada da Lambda ({@code br.com.oficina.auth.AuthHandler::handleRequest}) atrás do API Gateway:
 * {@code POST /auth/cpf} com {@code {"cpf":"529.982.247-25"}}.
 * Variáveis: JWT_SECRET (o mesmo dos serviços), OS_SERVICE_URL e, opcional, TOKEN_MINUTOS (padrão 60).
 */
public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AutenticarPorCpf autenticar;

    /** Usado pela AWS: monta tudo a partir das variáveis de ambiente. */
    public AuthHandler() {
        this(System.getenv());
    }

    AuthHandler(Map<String, String> env) {
        String url = env.get("OS_SERVICE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Defina OS_SERVICE_URL");
        }
        JwtHs256 jwt = new JwtHs256(env.get("JWT_SECRET"));
        Clock relogio = Clock.systemUTC();
        long minutos = Long.parseLong(env.getOrDefault("TOKEN_MINUTOS", "60"));
        this.autenticar = new AutenticarPorCpf(
                new OsServiceClienteGateway(url, jwt, relogio,
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()),
                jwt, relogio, Duration.ofMinutes(minutos));
    }

    public AuthHandler(AutenticarPorCpf autenticar) {
        this.autenticar = autenticar;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent evento, Context contexto) {
        String cpf;
        try {
            JsonNode corpo = JSON.readTree(evento.getBody() == null ? "" : evento.getBody());
            cpf = corpo == null ? null : corpo.path("cpf").asText(null);
        } catch (Exception e) {
            return resposta(400, Map.of("mensagem", "Corpo inválido: envie {\"cpf\":\"...\"}"));
        }
        if (cpf == null || cpf.isBlank()) {
            return resposta(400, Map.of("mensagem", "Informe o cpf"));
        }
        try {
            return switch (autenticar.autenticar(cpf)) {
                case AutenticarPorCpf.Resultado.Autenticado a -> resposta(200, Map.of(
                        "token", a.token(), "tipo", "Bearer", "expiraEmSegundos", a.expiraEmSegundos()));
                case AutenticarPorCpf.Resultado.CpfInvalido i -> resposta(400, Map.of("mensagem", "CPF inválido"));
                case AutenticarPorCpf.Resultado.NaoAutorizado n -> resposta(401,
                        Map.of("mensagem", "Não foi possível autenticar com o CPF informado"));
            };
        } catch (RuntimeException e) {
            if (contexto != null && contexto.getLogger() != null) {
                contexto.getLogger().log("Erro ao autenticar: " + e);
            }
            return resposta(502, Map.of("mensagem", "Serviço de clientes indisponível, tente novamente"));
        }
    }

    private static APIGatewayProxyResponseEvent resposta(int status, Map<String, ?> corpo) {
        try {
            Map<String, String> cabecalhos = new LinkedHashMap<>();
            cabecalhos.put("Content-Type", "application/json");
            return new APIGatewayProxyResponseEvent().withStatusCode(status).withHeaders(cabecalhos)
                    .withBody(JSON.writeValueAsString(corpo));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
