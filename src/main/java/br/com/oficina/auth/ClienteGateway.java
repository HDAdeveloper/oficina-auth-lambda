package br.com.oficina.auth;

import java.util.Optional;

/** Consulta o cadastro de clientes (OS Service). Trocável nos testes. */
public interface ClienteGateway {

    /** Cliente com esse CPF (só dígitos); vazio se não existir. Lança {@link IllegalStateException} se o serviço falhar. */
    Optional<Cliente> buscarPorCpf(String cpf);

    record Cliente(long id, String nome, boolean ativo) {
    }
}
