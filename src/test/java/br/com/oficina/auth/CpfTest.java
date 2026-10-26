package br.com.oficina.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfTest {

    @Test
    void aceitaCpfValidoComOuSemPontuacao() {
        assertThat(Cpf.valido("529.982.247-25")).isTrue();
        assertThat(Cpf.valido("52998224725")).isTrue();
        assertThat(Cpf.valido("111.444.777-35")).isTrue();
    }

    @Test
    void recusaCpfInvalido() {
        assertThat(Cpf.valido(null)).isFalse();
        assertThat(Cpf.valido("")).isFalse();
        assertThat(Cpf.valido("123")).isFalse();
        assertThat(Cpf.valido("11111111111")).isFalse();
        assertThat(Cpf.valido("52998224726")).isFalse();
        assertThat(Cpf.valido("529982247250")).isFalse();
        assertThat(Cpf.valido("abc")).isFalse();
    }

    @Test
    void normalizaParaDigitos() {
        assertThat(Cpf.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Cpf.normalizar(null)).isNull();
    }
}
