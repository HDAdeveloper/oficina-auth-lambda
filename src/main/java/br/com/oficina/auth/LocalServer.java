package br.com.oficina.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Executa a mesma Lambda como servidor HTTP local (porta 8090) para demonstrar sem AWS: {@code POST /auth/cpf}.
 * Uso: {@code java -cp target/auth-lambda-1.0.0-SNAPSHOT.jar br.com.oficina.auth.LocalServer} (veja docs/PROGRESS.md, Etapa 6).
 */
public final class LocalServer {

    private LocalServer() {
    }

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8090"));
        AuthHandler handler = new AuthHandler();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/auth/cpf", troca -> {
            byte[] saida;
            int status;
            if (!"POST".equals(troca.getRequestMethod())) {
                status = 405;
                saida = "{\"mensagem\":\"Use POST\"}".getBytes(StandardCharsets.UTF_8);
            } else {
                String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                var resposta = handler.handleRequest(new APIGatewayProxyRequestEvent().withBody(corpo), null);
                status = resposta.getStatusCode();
                saida = resposta.getBody().getBytes(StandardCharsets.UTF_8);
            }
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, saida.length);
            troca.getResponseBody().write(saida);
            troca.close();
        });
        servidor.start();
        System.out.println("auth-lambda local em http://localhost:" + porta + "/auth/cpf");
    }
}
