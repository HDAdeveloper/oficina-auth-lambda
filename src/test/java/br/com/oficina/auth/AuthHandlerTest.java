package br.com.oficina.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthHandlerTest {

    private static AuthHandler handler(ClienteGateway gateway) {
        return new AuthHandler(new AutenticarPorCpf(gateway, new JwtHs256(JwtHs256Test.SEGREDO),
                Clock.systemUTC(), Duration.ofMinutes(60)));
    }

    private static APIGatewayProxyResponseEvent chamar(AuthHandler h, String corpo) {
        return h.handleRequest(new APIGatewayProxyRequestEvent().withBody(corpo), null);
    }

    private static final AuthHandler ATIVO = handler(cpf -> Optional.of(new ClienteGateway.Cliente("52998224725", "Ana", true)));

    @Test
    void cpfCadastradoDevolve200ComToken() {
        var r = chamar(ATIVO, "{\"cpf\":\"529.982.247-25\"}");

        assertThat(r.getStatusCode()).isEqualTo(200);
        assertThat(r.getHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(r.getBody()).contains("\"token\":\"").contains("\"tipo\":\"Bearer\"")
                .contains("\"expiraEmSegundos\":3600");
    }

    @Test
    void cpfInvalidoDevolve400() {
        var r = chamar(ATIVO, "{\"cpf\":\"123\"}");

        assertThat(r.getStatusCode()).isEqualTo(400);
        assertThat(r.getBody()).contains("CPF inválido");
    }

    @Test
    void corpoAusenteOuMalFormadoDevolve400() {
        assertThat(chamar(ATIVO, null).getStatusCode()).isEqualTo(400);
        assertThat(chamar(ATIVO, "{}").getStatusCode()).isEqualTo(400);
        assertThat(chamar(ATIVO, "{\"cpf\":\"  \"}").getStatusCode()).isEqualTo(400);
        assertThat(chamar(ATIVO, "isto não é json").getStatusCode()).isEqualTo(400);
    }

    @Test
    void clienteNaoCadastradoDevolve401() {
        var r = chamar(handler(cpf -> Optional.empty()), "{\"cpf\":\"52998224725\"}");

        assertThat(r.getStatusCode()).isEqualTo(401);
        assertThat(r.getBody()).doesNotContain("token");
    }

    @Test
    void falhaNoOsServiceDevolve502() {
        var r = chamar(handler(cpf -> {
            throw new IllegalStateException("fora do ar");
        }), "{\"cpf\":\"52998224725\"}");

        assertThat(r.getStatusCode()).isEqualTo(502);
    }

    @Test
    void configuracaoVemDasVariaveisDeAmbiente() {
        var h = new AuthHandler(Map.of("OS_SERVICE_URL", "http://localhost:1", "JWT_SECRET", JwtHs256Test.SEGREDO,
                "TOKEN_MINUTOS", "15"));

        // CPF inválido responde sem precisar do OS Service
        assertThat(chamar(h, "{\"cpf\":\"1\"}").getStatusCode()).isEqualTo(400);
        assertThatThrownBy(() -> new AuthHandler(Map.of("JWT_SECRET", JwtHs256Test.SEGREDO)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("OS_SERVICE_URL");
        assertThatThrownBy(() -> new AuthHandler(Map.of("OS_SERVICE_URL", "http://x", "JWT_SECRET", "curto")))
                .isInstanceOf(IllegalStateException.class);
    }
}
