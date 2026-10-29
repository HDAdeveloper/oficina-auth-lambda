package br.com.oficina.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lê o cliente no OS Service por REST ({@code GET /api/clientes/documento/{cpf}}), com um token de serviço
 * (papel SERVICE, validade de 1 minuto). A Lambda nunca acessa o banco do OS Service.
 */
public class OsServiceClienteGateway implements ClienteGateway {

    private final String urlBase;
    private final JwtHs256 jwt;
    private final Clock relogio;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public OsServiceClienteGateway(String urlBase, JwtHs256 jwt, Clock relogio, HttpClient http) {
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.jwt = jwt;
        this.relogio = relogio;
        this.http = http;
    }

    @Override
    public Optional<Cliente> buscarPorCpf(String cpf) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(urlBase + "/api/clientes/documento/" + cpf))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + tokenDeServico())
                .header("Accept", "application/json")
                .GET().build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) {
                return Optional.empty();
            }
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("OS Service respondeu HTTP " + resp.statusCode());
            }
            JsonNode no = json.readTree(resp.body());
            return Optional.of(new Cliente(no.path("id").asText(""), no.path("nome").asText(""),
                    no.path("ativo").asBoolean(false)));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao consultar o OS Service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta ao OS Service interrompida", e);
        }
    }

    private String tokenDeServico() {
        Instant agora = Instant.now(relogio);
        return jwt.emitir(Map.of("iss", "oficina-auth-lambda", "sub", "auth-lambda",
                "iat", agora.getEpochSecond(), "exp", agora.plusSeconds(60).getEpochSecond(),
                "roles", List.of("SERVICE")));
    }
}
