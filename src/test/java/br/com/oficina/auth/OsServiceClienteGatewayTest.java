package br.com.oficina.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** OS Service de mentira (servidor HTTP do JDK) para conferir a chamada REST e a leitura da resposta. */
class OsServiceClienteGatewayTest {

    private HttpServer servidor;
    private final AtomicReference<String> caminho = new AtomicReference<>();
    private final AtomicReference<String> autorizacao = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String corpo = "{}";

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            caminho.set(troca.getRequestURI().getPath());
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            byte[] saida = corpo.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, saida.length);
            troca.getResponseBody().write(saida);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private OsServiceClienteGateway gateway(String sufixo) {
        return new OsServiceClienteGateway("http://127.0.0.1:" + servidor.getAddress().getPort() + sufixo,
                new JwtHs256(JwtHs256Test.SEGREDO), Clock.systemUTC(), HttpClient.newHttpClient());
    }

    @Test
    void leOClienteEEnviaTokenDeServico() throws Exception {
        corpo = "{\"id\":\"52998224725\",\"nome\":\"Ana Souza\",\"documento\":\"529.982.247-25\",\"ativo\":true}";

        var cliente = gateway("/").buscarPorCpf("52998224725");

        assertThat(cliente).contains(new ClienteGateway.Cliente("52998224725", "Ana Souza", true));
        assertThat(caminho.get()).isEqualTo("/api/clientes/documento/52998224725");
        assertThat(autorizacao.get()).startsWith("Bearer ");
        String claims = JwtHs256Test.decodificar(autorizacao.get().substring(7).split("\\.")[1]);
        assertThat(claims).contains("\"roles\":[\"SERVICE\"]");
    }

    @Test
    void clienteNaoEncontradoVirouVazio() {
        status = 404;

        assertThat(gateway("").buscarPorCpf("52998224725")).isEmpty();
    }

    @Test
    void erroDoServicoViraExcecao() {
        status = 500;

        assertThatThrownBy(() -> gateway("").buscarPorCpf("52998224725"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("500");
    }

    @Test
    void servicoForaDoArViraExcecao() {
        int porta = servidor.getAddress().getPort();
        servidor.stop(0);
        var gw = new OsServiceClienteGateway("http://127.0.0.1:" + porta, new JwtHs256(JwtHs256Test.SEGREDO),
                Clock.systemUTC(), HttpClient.newHttpClient());

        assertThatThrownBy(() -> gw.buscarPorCpf("52998224725")).isInstanceOf(IllegalStateException.class);
    }
}
