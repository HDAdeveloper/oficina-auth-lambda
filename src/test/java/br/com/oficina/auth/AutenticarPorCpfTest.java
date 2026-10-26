package br.com.oficina.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticarPorCpfTest {

    private static final Instant AGORA = Instant.parse("2026-10-24T12:00:00Z");

    private AutenticarPorCpf caso(ClienteGateway gateway) {
        return new AutenticarPorCpf(gateway, new JwtHs256(JwtHs256Test.SEGREDO),
                Clock.fixed(AGORA, ZoneOffset.UTC), Duration.ofMinutes(60));
    }

    @Test
    void clienteAtivoRecebeTokenComPapelClienteEDadosDoCadastro() throws Exception {
        var resultado = caso(cpf -> Optional.of(new ClienteGateway.Cliente(7L, "Ana Souza", true)))
                .autenticar("529.982.247-25");

        var ok = (AutenticarPorCpf.Resultado.Autenticado) resultado;
        assertThat(ok.expiraEmSegundos()).isEqualTo(3600);
        JsonNode claims = new ObjectMapper().readTree(JwtHs256Test.decodificar(ok.token().split("\\.")[1]));
        assertThat(claims.get("sub").asText()).isEqualTo("52998224725");
        assertThat(claims.get("roles").get(0).asText()).isEqualTo("CLIENTE");
        assertThat(claims.get("clienteId").asLong()).isEqualTo(7L);
        assertThat(claims.get("nome").asText()).isEqualTo("Ana Souza");
        assertThat(claims.get("iat").asLong()).isEqualTo(AGORA.getEpochSecond());
        assertThat(claims.get("exp").asLong()).isEqualTo(AGORA.plusSeconds(3600).getEpochSecond());
    }

    @Test
    void cpfInvalidoNemConsultaOCadastro() {
        var resultado = caso(cpf -> {
            throw new AssertionError("não deveria consultar");
        }).autenticar("123");

        assertThat(resultado).isInstanceOf(AutenticarPorCpf.Resultado.CpfInvalido.class);
    }

    @Test
    void clienteInexistenteOuInativoNaoAutoriza() {
        assertThat(caso(cpf -> Optional.empty()).autenticar("52998224725"))
                .isInstanceOf(AutenticarPorCpf.Resultado.NaoAutorizado.class);
        assertThat(caso(cpf -> Optional.of(new ClienteGateway.Cliente(7L, "Ana", false))).autenticar("52998224725"))
                .isInstanceOf(AutenticarPorCpf.Resultado.NaoAutorizado.class);
    }
}
