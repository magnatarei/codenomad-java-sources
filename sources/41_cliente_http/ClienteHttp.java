/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  41. Cliente HTTP sem dependências
 * ------------------------------------------------------------
 * O que faz: faz um GET em qualquer URL e mostra status, cabeçalhos e corpo.
 *            Se o corpo for JSON, interpreta com um leitor próprio e extrai
 *            os campos que você pedir (ex.: itens[0].nome). Timeout e falha
 *            de rede viram mensagens claras, sem pilha de erro.
 * Como usar: java ClienteHttp.java https://api.exemplo.test/loja.json loja.nome itens[0].preco
 *            java ClienteHttp.java --demo
 * O que você aprende: HttpURLConnection com timeout, exceções de rede, um
 *            parser recursivo de JSON, Map e List aninhados, switch
 *            expressions e pattern matching com instanceof.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClienteHttp {

    /** O que interessa de uma resposta, venha ela da rede ou do exemplo embutido. */
    record Resposta(int status, Map<String, String> cabecalhos, String corpo) {}

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            mostrar(buscar(args[0]), List.of(args).subList(1, args.length));
        } catch (IllegalArgumentException | MalformedURLException e) {
            falhar("URL inválida. Use o endereço completo, com http:// ou https://.", e);
        } catch (SocketTimeoutException e) {
            falhar("O servidor não respondeu dentro do limite de 10 segundos.", e);
        } catch (IOException e) {
            // Aqui caem ConnectException (porta fechada), UnknownHostException (nome errado) e afins.
            falhar("Não consegui falar com o servidor (" + e.getClass().getSimpleName() + ").", e);
        }
    }

    /** Algumas exceções de rede vêm sem mensagem; não vale imprimir "null". */
    static void falhar(String mensagem, Exception e) {
        System.out.println("Erro: " + mensagem + (e.getMessage() == null ? "" : " Detalhe: " + e.getMessage()));
        System.exit(1);                                 // código 1 avisa scripts que algo deu errado
    }

    /** Faz o GET de verdade. HttpURLConnection cuida de HTTPS e de redirecionamentos por nós. */
    static Resposta buscar(String url) throws IOException {
        // URI.create lança IllegalArgumentException se o texto não for uma URL; toURL exige o esquema.
        if (!(URI.create(url).toURL().openConnection() instanceof HttpURLConnection conexao)) {
            throw new IllegalArgumentException("só aceito http e https");
        }
        conexao.setConnectTimeout(5_000);               // milissegundos para conectar
        conexao.setReadTimeout(10_000);                 // e para o servidor começar a responder
        int status = conexao.getResponseCode();         // é aqui que a conexão acontece de fato
        var cabecalhos = new LinkedHashMap<String, String>();
        // A linha de status vem com chave null nesse mapa; não é um cabeçalho, pulamos.
        conexao.getHeaderFields().forEach((nome, v) -> { if (nome != null) cabecalhos.put(nome, String.join(", ", v)); });
        // Em 4xx e 5xx o corpo vem pelo fluxo de erro; em sucesso, pelo fluxo normal.
        InputStream fluxo = status >= 400 ? conexao.getErrorStream() : conexao.getInputStream();
        String corpo = fluxo == null ? "" : new String(fluxo.readAllBytes(), StandardCharsets.UTF_8);
        conexao.disconnect();
        return new Resposta(status, cabecalhos, corpo);
    }

    static void mostrar(Resposta r, List<String> campos) {
        System.out.println("Status: " + r.status() + (r.status() >= 400 ? " (erro)" : " (ok)"));
        System.out.println("Cabeçalhos (" + r.cabecalhos().size() + "):");
        r.cabecalhos().forEach((nome, valor) -> System.out.println("  " + nome + ": " + valor));
        String corpo = r.corpo().strip();
        System.out.println("Corpo (" + corpo.length() + " caracteres):");
        System.out.println(corpo.length() > 400 ? corpo.substring(0, 400) + "..." : corpo);
        Object json;
        try { json = Json.ler(corpo); }
        catch (IllegalArgumentException e) {
            System.out.println("O corpo não é JSON (" + e.getMessage() + "); nada para extrair.");
            return;
        }
        System.out.println();
        System.out.println("JSON interpretado. Chaves no topo: " + (json instanceof Map<?, ?> m ? m.keySet() : "nenhuma, é " + tipo(json)));
        for (String caminho : campos) {
            try {
                Object valor = extrair(json, caminho);
                // Textos saem entre aspas, para não confundir "3" com 3; o resto usa o toString do Java.
                System.out.println("  " + caminho + " = " + (valor instanceof String ? "\"" + valor + "\"" : valor));
            } catch (IllegalArgumentException e) {
                System.out.println("  " + caminho + " : " + e.getMessage());
            }
        }
    }

    /** Nome amigável do que o parser devolveu: objeto, lista, string, long, double, boolean ou null. */
    static String tipo(Object v) {
        if (v instanceof Map) return "objeto";
        return v instanceof List ? "lista" : v == null ? "null" : v.getClass().getSimpleName().toLowerCase();
    }

    /** Caminha pelo JSON seguindo "loja.nome" ou "itens[0].preco". */
    static Object extrair(Object raiz, String caminho) {
        Object atual = raiz;
        // "itens[0].nome" vira os passos: itens, 0, nome
        for (String passo : caminho.replace("[", ".").replace("]", "").split("\\.")) {
            if (atual instanceof List<?> lista) {
                if (!passo.matches("\\d+") || Integer.parseInt(passo) >= lista.size()) {
                    throw new IllegalArgumentException("'" + passo + "' não é uma posição válida; a lista tem " + lista.size() + " itens");
                }
                atual = lista.get(Integer.parseInt(passo));
            } else if (atual instanceof Map<?, ?> mapa && mapa.containsKey(passo)) {
                atual = mapa.get(passo);
            } else {
                String chaves = atual instanceof Map<?, ?> m ? " com chaves " + m.keySet() : "";
                throw new IllegalArgumentException("não achei '" + passo + "' em " + tipo(atual) + chaves);
            }
        }
        return atual;
    }

    /**
     * Leitor de JSON feito à mão: cada método lê um tipo e avança a posição i. Objetos viram
     * Map, listas viram List, textos viram String, números viram Long ou Double.
     */
    static final class Json {
        private final String s;
        private int i;
        Json(String s) { this.s = s; }

        static Object ler(String texto) {
            var j = new Json(texto);
            Object v = j.valor();
            j.espacos();
            if (j.i != j.s.length()) throw j.erro("sobrou texto depois do fim do JSON");
            return v;
        }
        Object valor() {
            espacos();
            return switch (atual()) {
                case '{' -> objeto();
                case '[' -> lista();
                case '"' -> texto();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numero();
            };
        }
        Map<String, Object> objeto() {
            var mapa = new LinkedHashMap<String, Object>();
            i++;                                            // pula o '{'
            if (proximo('}')) return mapa;
            do {
                String chave = texto();
                esperar(':');
                mapa.put(chave, valor());
            } while (proximo(','));
            esperar('}'); return mapa;
        }
        List<Object> lista() {
            var itens = new ArrayList<Object>();
            i++;                                            // pula o '['
            if (proximo(']')) return itens;
            do { itens.add(valor()); } while (proximo(','));
            esperar(']'); return itens;
        }
        /** Pula espaços e, se o próximo símbolo for c, consome e devolve true. */
        boolean proximo(char c) {
            espacos();
            if (atual() != c) return false;
            i++; return true;
        }
        void esperar(char c) {
            if (!proximo(c)) throw erro("esperava '" + c + "' e achei '" + atual() + "'");
        }
        String texto() {
            esperar('"');
            var sb = new StringBuilder();
            for (char c = atual(); c != '"'; c = atual()) {
                i++;
                sb.append(c == '\\' ? escape() : c);
            }
            i++;                                            // pula a aspa final
            return sb.toString();
        }
        /** Traduz o que vem depois da barra invertida; a letra u exige quatro dígitos hexadecimais. */
        char escape() {
            char e = atual(); i++;
            if (e == 'n') return '\n';
            if (e == 't') return '\t';
            if (e != 'u') return e;                         // cobre aspas, barra invertida e barra
            if (i + 4 > s.length()) throw erro("o escape u precisa de 4 dígitos hexadecimais");
            i += 4;
            return (char) Integer.parseInt(s.substring(i - 4, i), 16);
        }
        Object numero() {
            int inicio = i;      // lê tudo que possa fazer parte de um número e deixa o Java validar
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(inicio, i);
            if (n.isEmpty()) throw erro("símbolo inesperado '" + atual() + "'");
            // Cuidado: um ternário com Double e Long converte os dois para double. Por isso o if.
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.valueOf(n);
            return Long.valueOf(n);
        }
        Object literal(String palavra, Object valor) {
            if (!s.startsWith(palavra, i)) throw erro("esperava '" + palavra + "'");
            i += palavra.length(); return valor;
        }
        char atual() {
            if (i >= s.length()) throw erro("o JSON terminou antes da hora");
            return s.charAt(i);
        }
        void espacos() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        IllegalArgumentException erro(String motivo) { return new IllegalArgumentException(motivo + " na posição " + i); }
    }

    /** Uma resposta igual à que um servidor devolveria, guardada no próprio arquivo. */
    static final String JSON_EXEMPLO = """
            {
              "loja": {"nome": "Papelaria Girassol", "cidade": "Curitiba", "aberta": true},
              "itens": [{"nome": "Caderno 96 folhas", "preco": 18.9, "estoque": 42},
                        {"nome": "Caneta azul", "preco": 2.5, "estoque": 300},
                        {"nome": "Mochila escolar", "preco": 129.0, "estoque": 7}],
              "total_itens": 3, "atualizado_em": "2026-10-27T09:30:00Z", "observacao": null
            }
            """;

    static void demo() {
        System.out.println("=== Demonstração: resposta de exemplo, sem acessar a rede ===");
        System.out.println("GET https://api.exemplo.test/loja.json (simulado)");
        var cabecalhos = new LinkedHashMap<String, String>();
        cabecalhos.put("content-type", "application/json; charset=utf-8");
        cabecalhos.put("content-length", String.valueOf(JSON_EXEMPLO.getBytes(StandardCharsets.UTF_8).length));
        mostrar(new Resposta(200, cabecalhos, JSON_EXEMPLO), List.of("loja.nome", "loja", "itens[0].nome",
                "itens[2].preco", "total_itens", "loja.aberta", "observacao", "itens[5].nome", "loja.telefone"));
    }

    static void uso() {
        System.out.println("""
            Uso: java ClienteHttp.java <url> [campo ...]
              url     endereço completo, com http:// ou https://
              campo   caminho dentro do JSON da resposta, ex.: loja.nome ou itens[0].preco
            Exemplo: java ClienteHttp.java https://api.exemplo.test/loja.json loja.nome itens[0].preco
            Demonstração: java ClienteHttp.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Aceite um método (--post) e um corpo, usando HttpRequest.BodyPublishers.ofString.
 *  2. Salve o corpo em um arquivo quando o content-type for imagem ou binário.
 *  3. Faça o leitor de JSON acusar a linha e a coluna do erro, não só a posição.
 */
