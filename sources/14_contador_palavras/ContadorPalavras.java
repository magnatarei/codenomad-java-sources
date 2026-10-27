/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  14. Contador de palavras e frequência
 * ------------------------------------------------------------
 * O que faz: lê um texto (passado direto na linha de comando ou em um arquivo)
 *            e conta letras, palavras, linhas e frases. Depois lista as N
 *            palavras mais frequentes, ignorando acentos e as palavras vazias
 *            do português (de, a, que, para...).
 * Como usar: java ContadorPalavras.java "O rato roeu a roupa do rei de Roma"
 *            java ContadorPalavras.java --arquivo texto.txt --top 5
 *            java ContadorPalavras.java --demo
 * O que você aprende: text blocks, Normalizer para tirar acentos, regex com
 *            classes Unicode (\p{L}), Map com merge, ordenação com Comparator
 *            encadeado, streams com limit, Files.readString e printf alinhado.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContadorPalavras {

    /** Palavras muito comuns que só fariam volume no ranking. Já estão sem acento. */
    static final Set<String> PALAVRAS_VAZIAS = Set.of(
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "da", "do", "das", "dos", "em", "na", "no",
            "nas", "nos", "e", "ou", "que", "se", "por", "para", "com", "sem", "ao", "aos", "mas", "como",
            "mais", "muito", "ja", "nao", "sim", "ate", "sua", "seu", "suas", "seus", "ele", "ela", "eles",
            "elas", "isso", "isto", "este", "esta", "esse", "essa", "foi", "ser", "era", "sao", "tem", "ha");

    /** \p{L} casa qualquer letra de qualquer alfabeto; o apóstrofo cobre palavras como d'água. */
    static final Pattern PALAVRA = Pattern.compile("[\\p{L}\\p{N}]+(?:'[\\p{L}]+)?");
    /** Uma frase termina em ponto, exclamação, interrogação ou reticências. */
    static final Pattern FIM_DE_FRASE = Pattern.compile("[.!?…]+(\\s|$)");

    /** O resumo numérico do texto. Um record é imutável e já ganha construtor e acessores. */
    record Estatisticas(int caracteres, int letras, int palavras, int linhas, int frases) {
        double mediaPorFrase() {
            return frases == 0 ? 0 : (double) palavras / frases;
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
            String texto = null;
            int top = 10;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--arquivo" -> texto = lerArquivo(exigirValor(args, ++i, "--arquivo"));
                    case "--top" -> top = lerInteiro(exigirValor(args, ++i, "--top"));
                    default -> texto = texto == null ? args[i] : texto + " " + args[i];
                }
            }
            if (texto == null || texto.isBlank()) throw new IllegalArgumentException("o texto está vazio.");
            analisarEImprimir(texto, top);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
            System.exit(1);
        }
    }

    static String exigirValor(String[] args, int indice, String opcao) {
        if (indice >= args.length) throw new IllegalArgumentException(opcao + " precisa de um valor logo depois.");
        return args[indice];
    }

    static int lerInteiro(String valor) {
        try {
            int n = Integer.parseInt(valor);
            if (n < 1 || n > 100) throw new IllegalArgumentException("--top deve ficar entre 1 e 100.");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--top precisa de um inteiro, recebi '" + valor + "'.");
        }
    }

    static String lerArquivo(String caminho) {
        try {
            return Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Transformamos a exceção de E/S numa mensagem amigável; o main cuida do resto.
            throw new IllegalArgumentException("não consegui ler o arquivo '" + caminho + "': " + e.getMessage());
        }
    }

    /** "coração" vira "coracao": decompõe o acento em um símbolo separado e o apaga. */
    static String semAcento(String texto) {
        String decomposto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return decomposto.replaceAll("\\p{M}", "");
    }

    static Estatisticas calcular(String texto) {
        int letras = (int) texto.chars().filter(Character::isLetter).count();
        int linhas = texto.isEmpty() ? 0 : texto.split("\\R").length; // \R casa qualquer quebra de linha
        int palavras = 0;
        Matcher m = PALAVRA.matcher(texto);
        while (m.find()) palavras++;
        int frases = 0;
        Matcher f = FIM_DE_FRASE.matcher(texto);
        while (f.find()) frases++;
        // Um texto que termina sem pontuação ainda tem uma última frase a contar.
        String aparado = texto.strip();
        boolean terminaComPontuacao = !aparado.isEmpty() && ".!?…".indexOf(aparado.charAt(aparado.length() - 1)) >= 0;
        if (palavras > 0 && !terminaComPontuacao) frases++;
        return new Estatisticas(texto.length(), letras, palavras, linhas, frases);
    }

    /** Conta cada palavra normalizada (minúscula e sem acento), pulando as palavras vazias. */
    static Map<String, Integer> frequencias(String texto) {
        Map<String, Integer> contagem = new HashMap<>();
        Matcher m = PALAVRA.matcher(texto);
        while (m.find()) {
            String palavra = semAcento(m.group().toLowerCase());
            if (PALAVRAS_VAZIAS.contains(palavra) || palavra.length() < 2) continue;
            contagem.merge(palavra, 1, Integer::sum); // merge: insere 1 ou soma ao que já existe
        }
        return contagem;
    }

    /** Ordena por frequência decrescente e, no empate, por ordem alfabética. */
    static List<Map.Entry<String, Integer>> ranking(Map<String, Integer> contagem, int top) {
        Comparator<Map.Entry<String, Integer>> porFrequencia =
                Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());
        return contagem.entrySet().stream().sorted(porFrequencia).limit(top).toList();
    }

    static void analisarEImprimir(String texto, int top) {
        Estatisticas e = calcular(texto);
        System.out.println("=== Estatísticas do texto ===");
        System.out.printf("%-24s %6d%n", "Caracteres (com espaço)", e.caracteres());
        System.out.printf("%-24s %6d%n", "Letras", e.letras());
        System.out.printf("%-24s %6d%n", "Palavras", e.palavras());
        System.out.printf("%-24s %6d%n", "Linhas", e.linhas());
        System.out.printf("%-24s %6d%n", "Frases", e.frases());
        System.out.printf("%-24s %8.1f%n", "Palavras por frase", e.mediaPorFrase());
        Map<String, Integer> contagem = frequencias(texto);
        System.out.printf("%-24s %6d%n", "Palavras distintas", contagem.size());
        System.out.println();
        System.out.println("=== Top " + top + " (sem acento, sem palavras vazias) ===");
        List<Map.Entry<String, Integer>> lista = ranking(contagem, top);
        if (lista.isEmpty()) {
            System.out.println("  (nenhuma palavra relevante encontrada)");
            return;
        }
        int maior = lista.get(0).getValue();
        int posicao = 1;
        for (Map.Entry<String, Integer> item : lista) {
            // A barra é proporcional à palavra mais frequente, que ocupa 30 blocos.
            int blocos = (int) Math.round(30.0 * item.getValue() / maior);
            System.out.printf("%2d. %-14s %4d  %s%n", posicao++, item.getKey(), item.getValue(), "#".repeat(blocos));
        }
    }

    static void demo() {
        String texto = """
            A programação é uma conversa com a máquina. Você escreve, a máquina obedece,
            e quando ela obedece errado, o problema quase sempre está no que você escreveu.

            Aprender a programar é aprender a dizer exatamente o que se quer. Cada função
            tem um nome, cada variável tem um propósito, e cada teste confirma uma promessa.
            Não existe código bom sem teste, e não existe teste bom sem código claro!

            Quem pratica todo dia, mesmo que por vinte minutos, aprende mais do que quem
            estuda oito horas uma vez por mês. A prática constante vira hábito, o hábito
            vira habilidade, e a habilidade abre portas. Qual porta você quer abrir hoje?
            """;
        System.out.println("=== Demonstração: analisando um texto de três parágrafos ===");
        System.out.println();
        System.out.println(texto.strip().lines().map(l -> "  | " + l).reduce((a, b) -> a + "\n" + b).orElse(""));
        System.out.println();
        analisarEImprimir(texto, 10);
        System.out.println();
        System.out.println("Observe: \"Você\" e \"você\" contam juntos porque tudo vira minúscula sem acento.");
    }

    static void uso() {
        System.out.println("""
            Uso: java ContadorPalavras.java "<texto entre aspas>" [--top N]
                 java ContadorPalavras.java --arquivo <caminho.txt> [--top N]
                 java ContadorPalavras.java --demo
              --top N   quantas palavras listar no ranking (padrão 10, máximo 100)
            Exemplos:
              java ContadorPalavras.java "O rato roeu a roupa do rei de Roma" --top 3
              java ContadorPalavras.java --arquivo redacao.txt""");
    }
}

/*
 * Para ir além:
 *  1. Aceite vários arquivos de uma vez e mostre um ranking por arquivo e um geral.
 *  2. Conte bigramas (pares de palavras seguidas) e mostre os cinco mais comuns.
 *  3. Exporte o ranking em CSV com Files.write, uma linha por palavra.
 */
