/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  16. Palíndromos e anagramas
 * ------------------------------------------------------------
 * O que faz: verifica se uma frase é palíndromo ignorando acentos, espaços,
 *            maiúsculas e pontuação; verifica se duas frases são anagramas uma
 *            da outra; e agrupa uma lista de palavras pelos anagramas que
 *            compartilham.
 * Como usar: java PalindromoAnagrama.java palindromo "Socorram-me, subi no ônibus em Marrocos"
 *            java PalindromoAnagrama.java anagrama "amor" "roma"
 *            java PalindromoAnagrama.java agrupar amor roma mora ramo porta tropa
 *            java PalindromoAnagrama.java --demo
 * O que você aprende: Normalizer para tirar acentos, dois ponteiros numa
 *            String, char[] com Arrays.sort como "assinatura" de anagrama,
 *            Map de listas com computeIfAbsent, LinkedHashMap para manter a
 *            ordem de chegada e record como valor de retorno.
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

public class PalindromoAnagrama {

    /** O resultado de um teste de palíndromo: a frase limpa e o veredito. */
    record Veredito(String original, String limpa, boolean positivo) {
        String rotulo() {
            return positivo ? "SIM" : "não";
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
            executar(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
            System.exit(1);
        }
    }

    static void executar(String[] args) {
        String comando = args[0].toLowerCase(Locale.ROOT);
        String[] resto = Arrays.copyOfRange(args, 1, args.length);
        switch (comando) {
            case "palindromo" -> {
                if (resto.length == 0) throw new IllegalArgumentException("informe a frase a testar.");
                Veredito v = testarPalindromo(String.join(" ", resto));
                System.out.println("Frase: " + v.original());
                System.out.println("Limpa: " + v.limpa());
                System.out.println("Palíndromo? " + v.rotulo());
            }
            case "anagrama" -> {
                if (resto.length != 2) throw new IllegalArgumentException("anagrama precisa de exatamente duas frases (use aspas se tiver espaço).");
                boolean sao = saoAnagramas(resto[0], resto[1]);
                System.out.printf("\"%s\" e \"%s\": %s%n", resto[0], resto[1], sao ? "SÃO anagramas" : "não são anagramas");
                System.out.println("Assinaturas: " + assinatura(resto[0]) + " / " + assinatura(resto[1]));
            }
            case "agrupar" -> {
                if (resto.length < 2) throw new IllegalArgumentException("agrupar precisa de pelo menos duas palavras.");
                imprimirGrupos(agrupar(List.of(resto)));
            }
            default -> throw new IllegalArgumentException("comando desconhecido '" + args[0] + "', use palindromo, anagrama ou agrupar.");
        }
    }

    /** Minúsculas, sem acento e só letras e dígitos: "Ônibus!" vira "onibus". */
    static String limpar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Dois índices caminham das pontas para o centro; basta um par diferente para falhar. */
    static Veredito testarPalindromo(String frase) {
        String limpa = limpar(frase);
        if (limpa.isEmpty()) throw new IllegalArgumentException("a frase não tem letras nem dígitos.");
        int inicio = 0;
        int fim = limpa.length() - 1;
        while (inicio < fim) {
            if (limpa.charAt(inicio) != limpa.charAt(fim)) return new Veredito(frase, limpa, false);
            inicio++;
            fim--;
        }
        return new Veredito(frase, limpa, true);
    }

    /** A assinatura de uma palavra são suas letras em ordem: "roma" e "amor" viram "amor". */
    static String assinatura(String texto) {
        char[] letras = limpar(texto).toCharArray();
        Arrays.sort(letras);
        return new String(letras);
    }

    static boolean saoAnagramas(String a, String b) {
        String assinaturaA = assinatura(a);
        // Duas frases vazias não contam como anagramas; e a mesma palavra também não.
        if (assinaturaA.isEmpty() || limpar(a).equals(limpar(b))) return false;
        return assinaturaA.equals(assinatura(b));
    }

    /** Agrupa palavras com a mesma assinatura. LinkedHashMap preserva a ordem em que apareceram. */
    static Map<String, List<String>> agrupar(List<String> palavras) {
        Map<String, List<String>> grupos = new LinkedHashMap<>();
        for (String palavra : palavras) {
            String chave = assinatura(palavra);
            if (chave.isEmpty()) continue;
            // computeIfAbsent cria a lista na primeira vez e devolve a existente nas seguintes.
            List<String> grupo = grupos.computeIfAbsent(chave, k -> new ArrayList<>());
            if (!grupo.contains(palavra)) grupo.add(palavra);
        }
        return grupos;
    }

    static void imprimirGrupos(Map<String, List<String>> grupos) {
        int comAnagrama = 0;
        for (Map.Entry<String, List<String>> g : grupos.entrySet()) {
            List<String> palavras = g.getValue();
            String marca = palavras.size() > 1 ? "*" : " ";
            if (palavras.size() > 1) comAnagrama++;
            System.out.printf(" %s [%-8s] %s%n", marca, g.getKey(), String.join(", ", palavras));
        }
        System.out.println("Grupos com anagramas (marcados com *): " + comAnagrama + " de " + grupos.size());
    }

    static void demo() {
        System.out.println("=== Palíndromos: frases famosas e armadilhas ===");
        List<String> frases = List.of(
                "Socorram-me, subi no ônibus em Marrocos",
                "A base do teto desaba",
                "Anotaram a data da maratona",
                "Luz azul",
                "Ana",
                "12321",
                "Roma me tem amor",
                "Programar é bom",
                "A grama é amarga");
        for (String f : frases) {
            Veredito v = testarPalindromo(f);
            System.out.printf("  %-3s  %-42s -> %s%n", v.rotulo(), f, v.limpa());
        }
        System.out.println();
        System.out.println("=== Anagramas: pares de palavras e frases ===");
        String[][] pares = {
                {"amor", "roma"}, {"pedra", "perda"}, {"escola", "colesa"}, {"ator", "rota"},
                {"maçã", "cama"}, {"amor", "amor"}, {"Um amor", "Roma um"}, {"O ator", "A rota"}};
        for (String[] par : pares) {
            boolean sao = saoAnagramas(par[0], par[1]);
            System.out.printf("  %-8s x %-8s -> %s  (assinaturas %s / %s)%n",
                    par[0], par[1], sao ? "anagramas  " : "diferentes ", assinatura(par[0]), assinatura(par[1]));
        }
        System.out.println("  Note: \"maçã\" vira \"maca\" sem acentos e casa com \"cama\"; uma palavra não é anagrama dela mesma.");
        System.out.println();
        System.out.println("=== Agrupando uma lista de palavras por anagrama ===");
        List<String> lista = List.of("amor", "roma", "mora", "ramo", "porta", "tropa", "pedra", "perda",
                "lama", "mala", "café", "escola", "colesa", "ator", "rota", "tora", "java");
        imprimirGrupos(agrupar(lista));
    }

    static void uso() {
        System.out.println("""
            Uso: java PalindromoAnagrama.java palindromo <frase>
                 java PalindromoAnagrama.java anagrama <frase 1> <frase 2>
                 java PalindromoAnagrama.java agrupar <palavra> <palavra> [mais palavras ...]
                 java PalindromoAnagrama.java --demo
              Acentos, espaços, maiúsculas e pontuação são ignorados em todos os comandos.
            Exemplos:
              java PalindromoAnagrama.java palindromo "A base do teto desaba"
              java PalindromoAnagrama.java anagrama "Um amor" "Roma um"
              java PalindromoAnagrama.java agrupar amor roma mora porta tropa""");
    }
}

/*
 * Para ir além:
 *  1. Leia um dicionário de um arquivo (uma palavra por linha) e liste os maiores grupos de anagramas.
 *  2. Encontre o maior palíndromo contido dentro de uma frase (subcadeia contígua).
 *  3. Faça o teste de palíndromo com recursão em vez do laço de dois índices e compare a clareza.
 */
