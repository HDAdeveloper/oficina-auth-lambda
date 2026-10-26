package br.com.oficina.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtHs256Test {

    static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    static String decodificar(String parte) {
        return new String(Base64.getUrlDecoder().decode(parte), StandardCharsets.UTF_8);
    }

    @Test
    void emiteTokenComTresPartesEAssinaturaHs256Correta() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "52998224725");
        claims.put("exp", 1790000000L);
        claims.put("roles", List.of("CLIENTE"));
        claims.put("ativo", true);

        String token = new JwtHs256(SEGREDO).emitir(claims);

        String[] partes = token.split("\\.");
        assertThat(partes).hasSize(3);
        assertThat(decodificar(partes[0])).isEqualTo("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        assertThat(decodificar(partes[1]))
                .isEqualTo("{\"sub\":\"52998224725\",\"exp\":1790000000,\"roles\":[\"CLIENTE\"],\"ativo\":true}");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String esperada = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal((partes[0] + "." + partes[1]).getBytes(StandardCharsets.UTF_8)));
        assertThat(partes[2]).isEqualTo(esperada);
    }

    @Test
    void escapaCaracteresEspeciaisDosTextos() {
        String token = new JwtHs256(SEGREDO).emitir(Map.of("nome", "Ana \"A\" \\ Souza\n\t\r\u0001"));

        assertThat(decodificar(token.split("\\.")[1]))
                .isEqualTo("{\"nome\":\"Ana \\\"A\\\" \\\\ Souza\\n\\t\\r\\u0001\"}");
    }

    @Test
    void segredoCurtoOuNuloEhRecusado() {
        assertThatThrownBy(() -> new JwtHs256("curto")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtHs256(null)).isInstanceOf(IllegalStateException.class);
    }
}
