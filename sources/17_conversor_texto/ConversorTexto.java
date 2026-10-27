/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  17. Conversor de formatos de texto
 * ------------------------------------------------------------
 * O que faz: converte um texto entre snake_case, camelCase, PascalCase,
 *            kebab-case e Title Case; gera slug para URL; remove acentos;
 *            inverte a ordem das letras e capitaliza a primeira letra de cada
 *            palavra. Entende a entrada em qualquer um desses formatos.
 * Como usar: java ConversorTexto.java snake "Relatório de Vendas Mensal"
 *            java ConversorTexto.java camel "relatorio_de_vendas"
 *            java ConversorTexto.java todos "Olá, Mundo da Programação!"
 *            java ConversorTexto.java --demo
 * O que você aprende: Normalizer para tirar acentos, regex com lookahead
 *            para separar camelCase, String.join com streams, switch
 *            expression devolvendo lambdas, enum com rótulo e Map ordenado
 *            de funções (Function<String, String>).
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConversorTexto {

    /** Cada formato conhece seu nome na linha de comando e a função que o produz. */
    enum Formato {
        SNAKE("snake", ConversorTexto::paraSnake),
        CAMEL("camel", ConversorTexto::paraCamel),
        PASCAL("pascal", ConversorTexto::paraPascal),
        KEBAB("kebab", ConversorTexto::paraKebab),
        TITLE("title", ConversorTexto::paraTitle),
        SLUG("slug", ConversorTexto::paraSlug),
        SEM_ACENTO("sem-acento", ConversorTexto::semAcento),
        INVERTER("inverter", ConversorTexto::inverter),
        CAPITALIZAR("capitalizar", ConversorTexto::capitalizar);

        final String nome;
        final Function<String, String> conversao;

        Formato(String nome, Function<String, String> conversao) {
            this.nome = nome;
            this.conversao = conversao;
        }

        static Formato porNome(String nome) {
            for (Formato f : values()) {
                if (f.nome.equals(nome)) return f;
            }
            throw new IllegalArgumentException("formato desconhecido '" + nome + "'. Use: " + nomes() + " ou todos.");
        }

        static String nomes() {
            return Arrays.stream(values()).map(f -> f.nome).collect(Collectors.joining(", "));
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        if (args[0].equals("--demo")) {
            demo();
            return;
        }
        try {
            if (args.length < 2) throw new IllegalArgumentException("faltou o texto depois do formato.");
            String texto = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (texto.isBlank()) throw new IllegalArgumentException("o texto está vazio.");
            if (args[0].equalsIgnoreCase("todos")) {
                imprimirTodos(texto);
            } else {
                System.out.println(Formato.porNome(args[0].toLowerCase(Locale.ROOT)).conversao.apply(texto));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
            System.exit(1);
        }
    }

    /** "São João" vira "Sao Joao": o acento é decomposto em uma marca separada e apagada. */
    static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Quebra qualquer entrada em palavras minúsculas. Funciona para frases com
     * espaço, snake_case, kebab-case e também camelCase, graças ao lookahead
     * "(?=[A-Z])", que insere um espaço antes de cada maiúscula sem consumi-la.
     */
    static List<String> palavras(String texto) {
        String separado = semAcento(texto)
                .replaceAll("([a-z0-9])(?=[A-Z])", "$1 ")   // "relatorioMensal" vira "relatorio Mensal"
                .replaceAll("[^A-Za-z0-9]+", " ");          // tudo que não é letra ou dígito vira espaço
        List<String> lista = new ArrayList<>();
        for (String p : separado.trim().split("\\s+")) {
            if (!p.isEmpty()) lista.add(p.toLowerCase(Locale.ROOT));
        }
        return lista;
    }

    static String primeiraMaiuscula(String palavra) {
        if (palavra.isEmpty()) return palavra;
        return Character.toUpperCase(palavra.charAt(0)) + palavra.substring(1);
    }

    static String paraSnake(String texto) {
        return String.join("_", palavras(texto));
    }

    static String paraKebab(String texto) {
        return String.join("-", palavras(texto));
    }

    static String paraPascal(String texto) {
        return palavras(texto).stream().map(ConversorTexto::primeiraMaiuscula).collect(Collectors.joining());
    }

    /** camelCase é PascalCase com a primeira letra minúscula. */
    static String paraCamel(String texto) {
        String pascal = paraPascal(texto);
        return pascal.isEmpty() ? pascal : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    static String paraTitle(String texto) {
        return palavras(texto).stream().map(ConversorTexto::primeiraMaiuscula).collect(Collectors.joining(" "));
    }

    /** Slug é o kebab-case que se vê em endereços de página: só minúsculas, dígitos e hífen. */
    static String paraSlug(String texto) {
        return paraKebab(texto);
    }

    /** StringBuilder já sabe inverter; os acentos ficam com suas letras porque o texto é NFC. */
    static String inverter(String texto) {
        return new StringBuilder(Normalizer.normalize(texto, Normalizer.Form.NFC)).reverse().toString();
    }

    /** Capitaliza cada palavra mantendo acentos e pontuação: "olá, mundo" vira "Olá, Mundo". */
    static String capitalizar(String texto) {
        StringBuilder saida = new StringBuilder();
        boolean inicioDePalavra = true;
        for (char c : texto.toLowerCase(Locale.ROOT).toCharArray()) {
            saida.append(inicioDePalavra && Character.isLetter(c) ? Character.toUpperCase(c) : c);
            inicioDePalavra = !Character.isLetterOrDigit(c);
        }
        return saida.toString();
    }

    static void imprimirTodos(String texto) {
        Map<String, String> resultados = new LinkedHashMap<>();
        for (Formato f : Formato.values()) resultados.put(f.nome, f.conversao.apply(texto));
        System.out.println("Entrada:      " + texto);
        for (Map.Entry<String, String> r : resultados.entrySet()) {
            System.out.printf("%-13s %s%n", r.getKey() + ":", r.getValue());
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: todas as conversões em três frases com acento ===");
        List<String> frases = List.of(
                "Relatório de Vendas do Mês",
                "Olá, mundo da programação!",
                "Ação e reação: 3ª lei de Newton");
        for (String frase : frases) {
            System.out.println();
            imprimirTodos(frase);
        }
        System.out.println();
        System.out.println("=== A entrada pode vir em qualquer formato: tudo vira as mesmas palavras ===");
        List<String> entradas = List.of("relatorio_de_vendas", "relatorioDeVendas", "RelatorioDeVendas", "relatorio-de-vendas");
        for (String e : entradas) {
            System.out.printf("%-22s -> palavras %s -> title \"%s\"%n", e, palavras(e), paraTitle(e));
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java ConversorTexto.java <formato> <texto>
                 java ConversorTexto.java todos <texto>
                 java ConversorTexto.java --demo
              Formatos: snake, camel, pascal, kebab, title, slug, sem-acento, inverter, capitalizar
              A entrada pode estar em frase normal, snake_case, kebab-case, camelCase ou PascalCase.
            Exemplos:
              java ConversorTexto.java slug "Olá, Mundo da Programação!"
              java ConversorTexto.java camel relatorio_de_vendas
              java ConversorTexto.java todos "Relatório de Vendas do Mês\"""");
    }
}

/*
 * Para ir além:
 *  1. Adicione o formato CONSTANT_CASE (snake em maiúsculas) e o Sentence case (só a primeira letra maiúscula).
 *  2. Leia várias linhas da entrada padrão e converta cada uma, para usar o programa em pipelines do terminal.
 *  3. Trate siglas no camelCase: "parserHTML" deve virar as palavras "parser" e "html", não "parser", "h", "t", "m", "l".
 */
