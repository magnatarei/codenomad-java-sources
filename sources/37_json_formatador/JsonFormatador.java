/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  37. Validador e formatador de JSON
 * ------------------------------------------------------------
 * O que faz: lê um arquivo JSON e diz se ele é válido, apontando linha e coluna
 *            do erro. Também formata com recuo, compacta numa linha, extrai um
 *            valor por caminho (cliente.pedidos[0].total) e lista todas as chaves.
 * Como usar: java JsonFormatador.java validar pedido.json
 *            java JsonFormatador.java formatar pedido.json --recuo 4
 *            java JsonFormatador.java compactar pedido.json
 *            java JsonFormatador.java extrair pedido.json cliente.pedidos[0].total
 *            java JsonFormatador.java chaves pedido.json
 *            java JsonFormatador.java --demo
 * O que você aprende: leitor de JSON por descida recursiva que sabe em que linha
 *            está, pattern matching com instanceof (Java 16+), Matcher com região
 *            para reconhecer números, LinkedHashMap e escrita de texto com escapes.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonFormatador {

    /** O número fica guardado como texto: assim 1.50 volta como 1.50, e não como 1.5. */
    record Numero(String texto) {}

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        if (args.length < 2) { uso(); System.exit(1); }
        Path arquivo = Path.of(args[1]);
        if (!Files.isRegularFile(arquivo)) { System.out.println("Erro: o arquivo '" + args[1] + "' não existe"); System.exit(1); }
        try { System.out.println(executar(args[0], Files.readString(arquivo, StandardCharsets.UTF_8), Arrays.copyOfRange(args, 2, args.length))); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
        catch (MalformedInputException e) { System.out.println("Erro: o arquivo não está em UTF-8; salve-o de novo com essa codificação"); System.exit(1); }
    }

    static void demo() {
        String pedido = """
            {"loja": "Papelaria Central",
             "cliente": {"nome": "Mariana Costa", "vip": true, "telefone": null,
                         "pedidos": [{"numero": 1041, "total": 87.50, "itens": ["caderno", "caneta"]},
                                     {"numero": 1077, "total": 12.00, "itens": ["borracha"]}]},
             "observação": "entregar \\"com cuidado\\"\\n2º andar"}
            """;
        System.out.println("=== Demonstração: JSON de um pedido fictício ===");
        String[][] comandos = {
                {"validar"}, {"formatar"}, {"compactar"}, {"extrair", "cliente.pedidos[0].total"},
                {"extrair", "cliente.pedidos[1].itens[0]"}, {"extrair", "cliente.endereco"},
                {"extrair", "cliente.pedidos[7]"}, {"chaves"}, {"formatar", "--recuo", "muito"},
        };
        for (String[] c : comandos) rodar(c, pedido);
        System.out.println("\n--- Agora três arquivos com defeito ---");
        rodar(new String[]{"validar"}, "{\n  \"nome\": \"Ana\",\n  \"idade\": 30,\n}\n");
        rodar(new String[]{"validar"}, "{\"itens\": [1, 2 3]}");
        rodar(new String[]{"validar"}, "{\"texto\": \"sem fechar}");
    }

    static void rodar(String[] c, String json) {
        System.out.println("\n> JsonFormatador " + c[0] + " pedido.json " + String.join(" ", Arrays.copyOfRange(c, 1, c.length)));
        try { System.out.println(executar(c[0], json, Arrays.copyOfRange(c, 1, c.length))); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
    }

    static String executar(String comando, String json, String[] extras) {
        Object valor = new Leitor(json).ler(); // se o JSON for inválido, a exceção sai daqui com linha e coluna
        return switch (comando) {
            case "validar" -> "JSON válido: " + descrever(valor);
            case "formatar" -> {
                int recuo = 2;
                if (extras.length == 2 && extras[0].equals("--recuo")) {
                    try { recuo = Integer.parseInt(extras[1]); } catch (NumberFormatException e) { throw new IllegalArgumentException("--recuo precisa de um número inteiro, recebi '" + extras[1] + "'"); }
                    if (recuo < 1 || recuo > 8) throw new IllegalArgumentException("--recuo precisa estar entre 1 e 8");
                }
                yield escrever(valor, recuo, 0);
            }
            case "compactar" -> escrever(valor, 0, 0);
            case "extrair" -> {
                if (extras.length != 1) throw new IllegalArgumentException("use: extrair <arquivo> <caminho>, por exemplo cliente.pedidos[0].total");
                yield escrever(extrair(valor, extras[0]), 2, 0);
            }
            case "chaves" -> { List<String> linhas = new ArrayList<>(); chaves(valor, "", linhas); yield String.join("\n", linhas); }
            default -> throw new IllegalArgumentException("comando desconhecido '" + comando + "'; use validar, formatar, compactar, extrair ou chaves");
        };
    }

    /** Um caminho é uma sequência de chaves e índices: a expressão regular captura um de cada vez. */
    static final Pattern PASSO = Pattern.compile("\\[(\\d+)]|\\.?([^.\\[\\]]+)");
    static Object extrair(Object atual, String caminho) {
        Matcher m = PASSO.matcher(caminho);
        String percorrido = "";
        while (m.find()) {
            if (m.group(1) != null) {
                if (!(atual instanceof List<?> lista)) throw new IllegalArgumentException("'" + percorrido + "' não é uma lista, não dá para aplicar " + m.group());
                int indice = Integer.parseInt(m.group(1));
                if (indice >= lista.size()) throw new IllegalArgumentException("índice " + indice + " fora de '" + percorrido + "', que tem " + lista.size() + " item(ns)");
                atual = lista.get(indice);
            } else {
                if (!(atual instanceof Map<?, ?> mapa)) throw new IllegalArgumentException("'" + percorrido + "' não é um objeto, não tem a chave '" + m.group(2) + "'");
                if (!mapa.containsKey(m.group(2))) throw new IllegalArgumentException("a chave '" + m.group(2) + "' não existe; chaves disponíveis: " + mapa.keySet());
                atual = mapa.get(m.group(2));
            }
            percorrido += m.group();
        }
        return atual;
    }

    static void chaves(Object valor, String caminho, List<String> saida) {
        if (valor instanceof Map<?, ?> mapa) mapa.forEach((k, v) -> chaves(v, caminho.isEmpty() ? k.toString() : caminho + "." + k, saida));
        else if (valor instanceof List<?> lista) for (int i = 0; i < lista.size(); i++) chaves(lista.get(i), caminho + "[" + i + "]", saida);
        else saida.add(String.format("%-30s %s", caminho, descrever(valor)));
    }

    static String descrever(Object v) {
        if (v instanceof Map<?, ?> m) return "objeto com " + m.size() + " chave(s)";
        if (v instanceof List<?> l) return "lista com " + l.size() + " item(ns)";
        if (v instanceof String s) return "texto " + texto(s);
        if (v instanceof Numero n) return "número " + n.texto();
        return v == null ? "nulo" : "booleano " + v;
    }

    /** recuo 0 compacta tudo numa linha; qualquer outro valor quebra linhas e recua pelo nível. */
    static String escrever(Object v, int recuo, int nivel) {
        String quebra = recuo == 0 ? "" : "\n" + " ".repeat(recuo * (nivel + 1));
        String fecho = recuo == 0 ? "" : "\n" + " ".repeat(recuo * nivel);
        if (v instanceof Map<?, ?> mapa) {
            if (mapa.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{");
            for (var e : mapa.entrySet()) sb.append(quebra).append(texto(e.getKey().toString())).append(recuo == 0 ? ":" : ": ").append(escrever(e.getValue(), recuo, nivel + 1)).append(',');
            return sb.deleteCharAt(sb.length() - 1).append(fecho).append('}').toString(); // tira a vírgula sobrando no fim
        }
        if (v instanceof List<?> lista) {
            if (lista.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (Object item : lista) sb.append(quebra).append(escrever(item, recuo, nivel + 1)).append(',');
            return sb.deleteCharAt(sb.length() - 1).append(fecho).append(']').toString();
        }
        return v instanceof String s ? texto(s) : v instanceof Numero n ? n.texto() : String.valueOf(v);
    }

    /** Aspas, barra, quebras de linha e outros caracteres de controle precisam de escape dentro do texto. */
    static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            int k = "\"\\\n\r\t".indexOf(c);
            sb.append(k >= 0 ? "\\" + "\"\\nrt".charAt(k) : c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
        }
        return sb.append('"').toString();
    }

    /** Leitor por descida recursiva: cada método consome um tipo de valor a partir da posição i. */
    static final class Leitor {
        static final Pattern NUMERO = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");
        private final String s;
        private int i;
        Leitor(String s) { this.s = s; }
        Object ler() {
            Object v = valor();
            if (pular() < s.length()) throw erro("sobrou conteúdo depois do fim do JSON");
            return v;
        }
        Object valor() {
            if (pular() >= s.length()) throw erro("o arquivo acabou antes do valor terminar");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return texto();
            for (String p : List.of("true", "false", "null")) {
                if (s.startsWith(p, i)) { i += p.length(); return p.equals("null") ? null : Boolean.valueOf(p); }
            }
            Matcher m = NUMERO.matcher(s).region(i, s.length()); // lookingAt casa a partir de i, sem exigir que vá até o fim
            if (!m.lookingAt()) throw erro("valor inesperado '" + c + "'");
            i = m.end();
            return new Numero(m.group());
        }
        Map<String, Object> objeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            i++;
            if (pular() < s.length() && s.charAt(i) == '}') { i++; return mapa; }
            while (true) {
                if (pular() >= s.length() || s.charAt(i) != '"') throw erro("esperava uma chave entre aspas");
                String chave = texto();
                if (pular() >= s.length() || s.charAt(i) != ':') throw erro("esperava ':' depois da chave \"" + chave + "\"");
                i++;
                mapa.put(chave, valor());
                char c = pular() < s.length() ? s.charAt(i++) : '\0';
                if (c == '}') return mapa;
                if (c != ',') throw erro("esperava ',' ou '}' depois do valor de \"" + chave + "\"");
            }
        }
        List<Object> lista() {
            List<Object> itens = new ArrayList<>();
            i++;
            if (pular() < s.length() && s.charAt(i) == ']') { i++; return itens; }
            while (true) {
                itens.add(valor());
                char c = pular() < s.length() ? s.charAt(i++) : '\0';
                if (c == ']') return itens;
                if (c != ',') throw erro("esperava ',' ou ']' depois do item " + (itens.size() - 1));
            }
        }
        String texto() {
            StringBuilder sb = new StringBuilder();
            for (i++; i < s.length() && s.charAt(i) != '"'; i++) {
                char c = s.charAt(i);
                if (c < 0x20) throw erro("quebra de linha dentro de um texto; use \\n");
                if (c != '\\') { sb.append(c); continue; }
                if (++i >= s.length()) throw erro("escape incompleto");
                int k = "\"\\/ntrbf".indexOf(s.charAt(i)); // cada letra de escape vira o caractere na mesma posição
                if (k >= 0) { sb.append("\"\\/\n\t\r\b\f".charAt(k)); continue; }
                if (s.charAt(i) != 'u' || i + 4 >= s.length() || !s.substring(i + 1, i + 5).matches("[0-9a-fA-F]{4}")) throw erro("escape desconhecido \\" + s.charAt(i));
                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); // ç vale 4 dígitos hexadecimais
                i += 4;
            }
            if (i >= s.length()) throw erro("texto sem aspas de fechamento");
            i++;
            return sb.toString();
        }
        /** Pula espaços em branco e devolve a posição atual, o que encurta as condições acima. */
        int pular() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            return i;
        }
        /** Conta as quebras de linha até a posição do erro para informar linha e coluna. */
        IllegalArgumentException erro(String msg) {
            int pos = Math.min(i, s.length());
            int linha = 1 + (int) s.substring(0, pos).chars().filter(c -> c == '\n').count();
            return new IllegalArgumentException("JSON inválido na linha " + linha + ", coluna " + (pos - s.lastIndexOf('\n', pos - 1)) + ": " + msg);
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java JsonFormatador.java <comando> <arquivo.json> [...]
              validar <arquivo>                diz se é válido ou aponta linha e coluna do erro
              formatar <arquivo> [--recuo N]   reescreve com recuo (padrão 2 espaços)
              compactar <arquivo>              tudo numa linha só, sem espaços
              extrair <arquivo> <caminho>      ex.: cliente.pedidos[0].total
              chaves <arquivo>                 lista todos os caminhos e o tipo de cada valor
            Demonstração: java JsonFormatador.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Adicione o comando "ordenar", que formata com as chaves de cada objeto em ordem alfabética (TreeMap).
 *  2. Mostre a linha com defeito e um "^" embaixo da coluna do erro, como os compiladores fazem.
 *  3. Aceite "-" como nome de arquivo e leia o JSON da entrada padrão com System.in.readAllBytes().
 */
