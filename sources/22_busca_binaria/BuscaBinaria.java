/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  22. Busca binária e busca linear
 * ------------------------------------------------------------
 * O que faz: implementa busca linear e busca binária do zero, contando os
 *            passos de cada uma em vetores de tamanhos diferentes, e mostra
 *            por que a binária exige vetor ordenado e como os passos crescem.
 * Como usar: java BuscaBinaria.java 23 2 5 8 12 16 23 38 56 72 91
 *            java BuscaBinaria.java 777 --tamanho 100000
 *            java BuscaBinaria.java --demo
 * O que você aprende: laços while com dois índices que se aproximam, o cuidado
 *            com (baixo + alto) >>> 1, logaritmo na prática com Math.log,
 *            records com métodos, List para registrar a trilha da busca e
 *            Arrays.sort / Arrays.binarySearch da biblioteca padrão.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BuscaBinaria {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final int LIMITE_TRILHA = 16;   // só vetores pequenos ganham a explicação passo a passo

    /** Resultado de uma busca: onde achou (-1 se não achou), quantos passos custou e a trilha para explicar. */
    record Busca(int indice, int passos, List<String> trilha) {
        boolean achou() { return indice >= 0; }
        String resumo() { return (achou() ? "achou na posição " + indice : "não achou") + " em " + passos + " passo(s)"; }
    }

    /** Busca linear: olha cada posição, da primeira à última. Funciona em qualquer vetor, ordenado ou não. */
    static Busca linear(int[] v, int alvo) {
        List<String> trilha = new ArrayList<>();
        for (int i = 0; i < v.length; i++) {
            if (v.length <= LIMITE_TRILHA) trilha.add("passo " + (i + 1) + ": posição " + i + " vale " + v[i] + (v[i] == alvo ? ", achou!" : ", segue"));
            if (v[i] == alvo) return new Busca(i, i + 1, trilha);
        }
        return new Busca(-1, v.length, trilha);   // percorreu tudo e não achou: pior caso, n passos
    }

    /** Busca binária: olha o meio da faixa; se o alvo é menor, descarta a metade da direita; se é maior, a da esquerda. */
    static Busca binaria(int[] v, int alvo) {
        List<String> trilha = new ArrayList<>();
        int baixo = 0, alto = v.length - 1, passos = 0;
        while (baixo <= alto) {   // enquanto a faixa [baixo..alto] ainda tem alguma posição
            int meio = (baixo + alto) >>> 1;   // não escreva (baixo + alto) / 2: a soma pode estourar o int em vetores enormes
            passos++;
            String decisao = v[meio] == alvo ? "achou!" : alvo > v[meio] ? alvo + " é maior, segue à direita" : alvo + " é menor, segue à esquerda";
            if (v.length <= LIMITE_TRILHA) trilha.add("passo " + passos + ": faixa [" + baixo + ".." + alto + "], meio " + meio + " vale " + v[meio] + "; " + decisao);
            if (v[meio] == alvo) return new Busca(meio, passos, trilha);
            if (v[meio] < alvo) baixo = meio + 1;
            else alto = meio - 1;
        }
        return new Busca(-1, passos, trilha);
    }

    /** Quantos passos a binária gasta no pior caso: cada passo corta a faixa pela metade. */
    static int piorCasoBinaria(int n) {
        return (int) Math.floor(Math.log(n) / Math.log(2)) + 1;   // Math.log é natural; log2(n) = ln(n) / ln(2)
    }

    static boolean ordenado(int[] v) {
        for (int i = 1; i < v.length; i++) if (v[i - 1] > v[i]) return false;
        return true;
    }

    /** Vetor com os números de 1 até n: serve para testar tamanhos grandes sem digitar nada. */
    static int[] sequencia(int n) {
        if (n < 1 || n > 50_000_000) throw new IllegalArgumentException("O tamanho deve ficar entre 1 e 50.000.000, recebi " + n + ".");
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = i + 1;
        return v;
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            int alvo = inteiro(args[0]);
            int[] vetor;
            if (args.length >= 2 && args[1].equals("--tamanho")) {
                if (args.length != 3) throw new IllegalArgumentException("--tamanho precisa de exatamente um número depois dele.");
                vetor = sequencia(inteiro(args[2]));
            } else {
                if (args.length == 1) throw new IllegalArgumentException("Informe os números do vetor ou use --tamanho N.");
                vetor = new int[args.length - 1];
                for (int i = 1; i < args.length; i++) vetor[i - 1] = inteiro(args[i]);
            }
            comparar(vetor, alvo);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    /** Roda as duas buscas no mesmo vetor e explica o que aconteceu. */
    static void comparar(int[] vetor, int alvo) {
        System.out.println("Vetor com " + vetor.length + " posições" + (vetor.length <= LIMITE_TRILHA ? ": " + Arrays.toString(vetor) : "") + ". Procurando " + alvo + ".");
        Busca lin = linear(vetor, alvo);
        System.out.println("Busca linear: " + lin.resumo());
        lin.trilha().forEach(l -> System.out.println("   " + l));
        if (!ordenado(vetor)) {
            System.out.println("Atenção: o vetor NÃO está ordenado. A binária daria resposta errada, então ordenei uma cópia antes:");
            vetor = vetor.clone();
            Arrays.sort(vetor);
            System.out.println("   " + Arrays.toString(vetor));
        }
        Busca bin = binaria(vetor, alvo);
        System.out.println("Busca binária: " + bin.resumo() + " (pior caso para " + vetor.length + " posições: " + piorCasoBinaria(vetor.length) + ")");
        bin.trilha().forEach(l -> System.out.println("   " + l));
        int padrao = Arrays.binarySearch(vetor, alvo);   // a biblioteca padrão faz o mesmo; negativo significa "não achou"
        System.out.println("Arrays.binarySearch da biblioteca padrão concorda: " + (padrao >= 0 ? "posição " + padrao : "não achou"));
    }

    static void demo() {
        System.out.println("=== Demonstração 1: as duas buscas explicadas passo a passo ===");
        int[] pequeno = {2, 5, 8, 12, 16, 23, 38, 56, 72, 91};
        comparar(pequeno, 23);
        System.out.println();
        comparar(pequeno, 40);

        System.out.println("\n=== Demonstração 2: como os passos crescem com o tamanho (pior caso: procurar o último) ===");
        System.out.printf("%12s | %14s | %14s | %s%n", "Tamanho", "Linear", "Binária", "Teto da binária");
        System.out.println("-".repeat(64));
        for (int n : new int[]{10, 1_000, 100_000}) {
            int[] v = sequencia(n);
            System.out.printf(PT_BR, "%,12d | %,14d | %,14d | %d%n", n, linear(v, n).passos(), binaria(v, n).passos(), piorCasoBinaria(n));
        }
        System.out.println("Dobrar o vetor dobra os passos da linear e soma apenas 1 passo à binária.");
        System.out.println("Com 1 bilhão de posições a binária ainda resolve em 31 passos.");

        System.out.println("\n=== Demonstração 3: por que a binária exige vetor ordenado ===");
        int[] bagunca = {8, 3, 5, 1, 9};
        System.out.println("Vetor " + Arrays.toString(bagunca) + ", procurando 1 sem ordenar:");
        Busca errada = binaria(bagunca, 1);
        errada.trilha().forEach(l -> System.out.println("   " + l));
        System.out.println("Resultado: " + errada.resumo() + ". O 1 está lá, na posição 3, mas a binária descartou a metade errada.");
        System.out.println("A binária só funciona porque, em vetor ordenado, 'o meio é maior' garante que o alvo está à esquerda.");
    }

    static int inteiro(String texto) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Esperava um número inteiro, recebi '" + texto + "'."); }
    }

    static void uso() {
        System.out.println("""
            Uso: java BuscaBinaria.java <valor procurado> <números do vetor...>
                 java BuscaBinaria.java <valor procurado> --tamanho <N>   (vetor com os números de 1 a N)
            Exemplos:
              java BuscaBinaria.java 23 2 5 8 12 16 23 38 56 72 91
              java BuscaBinaria.java 777 --tamanho 100000
              java BuscaBinaria.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Faça a binária devolver a posição onde o valor DEVERIA ser inserido quando não acha (como Arrays.binarySearch).
 *  2. Escreva a busca binária recursiva e compare o código com a versão com while.
 *  3. Procure a primeira ocorrência em um vetor com repetidos: [1, 3, 3, 3, 7] e alvo 3 deve devolver 1.
 */
