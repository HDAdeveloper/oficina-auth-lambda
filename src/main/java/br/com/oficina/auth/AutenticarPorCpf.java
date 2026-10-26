package br.com.oficina.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Caso de uso: CPF válido e cliente ativo cadastrado => JWT de cliente (papel CLIENTE). */
public class AutenticarPorCpf {

    public sealed interface Resultado {
        record Autenticado(String token, long expiraEmSegundos) implements Resultado {
        }

        /** CPF mal formado ou com dígitos verificadores errados. */
        record CpfInvalido() implements Resultado {
        }

        /** CPF válido, mas sem cliente ativo: a resposta ao chamador não diferencia os dois casos. */
        record NaoAutorizado() implements Resultado {
        }
    }

    private final ClienteGateway clientes;
    private final JwtHs256 jwt;
    private final Clock relogio;
    private final Duration validade;

    public AutenticarPorCpf(ClienteGateway clientes, JwtHs256 jwt, Clock relogio, Duration validade) {
        this.clientes = clientes;
        this.jwt = jwt;
        this.relogio = relogio;
        this.validade = validade;
    }

    public Resultado autenticar(String cpfInformado) {
        if (!Cpf.valido(cpfInformado)) {
            return new Resultado.CpfInvalido();
        }
        String cpf = Cpf.normalizar(cpfInformado);
        var cliente = clientes.buscarPorCpf(cpf);
        if (cliente.isEmpty() || !cliente.get().ativo()) {
            return new Resultado.NaoAutorizado();
        }
        Instant agora = Instant.now(relogio);
        String token = jwt.emitir(Map.of(
                "iss", "oficina-auth-lambda",
                "sub", cpf,
                "iat", agora.getEpochSecond(),
                "exp", agora.plus(validade).getEpochSecond(),
                "roles", List.of("CLIENTE"),
                "clienteId", cliente.get().id(),
                "nome", cliente.get().nome()));
        return new Resultado.Autenticado(token, validade.toSeconds());
    }
}
