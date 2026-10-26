package br.com.oficina.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Emissão de JWT HS256 com o mesmo segredo dos serviços (JWT_SECRET); eles só validam assinatura e validade.
 * Escrito só com o JDK para a Lambda ficar pequena (inicia mais rápido).
 */
public final class JwtHs256 {

    private final byte[] segredo;

    public JwtHs256(String segredo) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
    }

    /** Valores aceitos nas claims: String, Number, Boolean ou lista de textos. */
    public String emitir(Map<String, Object> claims) {
        String cabecalho = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String corpo = base64(json(claims));
        return cabecalho + "." + corpo + "." + assinar(cabecalho + "." + corpo);
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o token", e);
        }
    }

    private static String base64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String json(Map<String, Object> claims) {
        StringJoiner juntos = new StringJoiner(",", "{", "}");
        claims.forEach((k, v) -> juntos.add(texto(k) + ":" + valor(v)));
        return juntos.toString();
    }

    private static String valor(Object v) {
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        if (v instanceof Collection<?> itens) {
            StringJoiner juntos = new StringJoiner(",", "[", "]");
            itens.forEach(i -> juntos.add(texto(String.valueOf(i))));
            return juntos.toString();
        }
        return texto(String.valueOf(v));
    }

    private static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
