/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  21. Algoritmos de ordenação com passos
 * ------------------------------------------------------------
 * O que faz: implementa bubble sort, insertion sort, merge sort e quick sort do
 *            zero, contando comparações e trocas, com opção de mostrar o vetor
 *            a cada passo e uma tabela final comparando o desempenho dos quatro.
 * Como usar: java OrdenacaoVisual.java bubble --passos 5 3 8 1 9 2
 *            java OrdenacaoVisual.java todos 5 3 8 1 9 2
 *            java OrdenacaoVisual.java todos --aleatorio 3000
 *            java OrdenacaoVisual.java --demo
 * O que você aprende: arrays e índices, laços aninhados, recursão com divisão e
 *            conquista, enum com switch expression, records para resultados,
 *            medição de tempo com System.nanoTime e por que n² e n log n
 *            fazem tanta diferença na prática.
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
import java.util.Random;

public class OrdenacaoVisual {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    enum Algoritmo {
        BUBBLE("bubble sort"), INSERTION("insertion sort"), MERGE("merge sort"), QUICK("quick sort");
        final String titulo;
        Algoritmo(String titulo) { this.titulo = titulo; }
    }

    /** O que uma ordenação custou. O record guarda os dados e ganha construtor, getters e toString de graça. */
    record Resultado(Algoritmo algoritmo, long comparacoes, long trocas, long nanos, int[] ordenado) {}

    /** Um Ordenador roda um algoritmo sobre uma cópia do vetor e conta cada comparação e cada troca. */
    static class Ordenador {
        final Algoritmo algoritmo;
        final boolean mostrarPassos;
        long comparacoes, trocas, nanosImprimindo;
        int passo;

        Ordenador(Algoritmo algoritmo, boolean mostrarPassos) { this.algoritmo = algoritmo; this.mostrarPassos = mostrarPassos; }

        Resultado executar(int[] original) {
            int[] v = original.clone();   // nunca mexa no vetor de quem chamou: cada algoritmo recebe a sua cópia
            comparacoes = trocas = nanosImprimindo = passo = 0;
            long inicio = System.nanoTime();
            switch (algoritmo) {
                case BUBBLE -> bubble(v);
                case INSERTION -> insertion(v);
                case MERGE -> mergeSort(v, 0, v.length - 1, new int[v.length]);
                case QUICK -> quickSort(v, 0, v.length - 1);
            }
            // Imprimir é lento: o tempo gasto mostrando os passos é descontado para não sujar a medição.
            return new Resultado(algoritmo, comparacoes, trocas, System.nanoTime() - inicio - nanosImprimindo, v);
        }

        /** Toda comparação passa por aqui, por isso dá para contar. Devolve negativo, zero ou positivo. */
        int comparar(int a, int b) { comparacoes++; return Integer.compare(a, b); }

        void trocar(int[] v, int i, int j) { int t = v[i]; v[i] = v[j]; v[j] = t; trocas++; }

        void registrar(int[] v, String descricao) {
            if (!mostrarPassos) return;
            long antes = System.nanoTime();
            System.out.printf("  passo %2d  %-34s %s%n", ++passo, descricao, Arrays.toString(v));
            nanosImprimindo += System.nanoTime() - antes;
        }

        /** Bubble sort: em cada passagem o maior elemento "borbulha" até o fim. Se uma passagem não trocou nada, acabou. */
        void bubble(int[] v) {
            for (int fim = v.length - 1; fim > 0; fim--) {
                boolean trocou = false;
                for (int i = 0; i < fim; i++) {
                    if (comparar(v[i], v[i + 1]) > 0) { trocar(v, i, i + 1); trocou = true; }
                }
                registrar(v, "passagem fixa a posição " + fim);
                if (!trocou) break;   // já está ordenado: nenhuma troca na passagem inteira
            }
        }

        /** Insertion sort: como organizar cartas na mão. Cada carta nova desliza para o lugar certo entre as já ordenadas. */
        void insertion(int[] v) {
            for (int i = 1; i < v.length; i++) {
                int atual = v[i];
                int j = i - 1;
                while (j >= 0 && comparar(v[j], atual) > 0) {
                    v[j + 1] = v[j];   // empurra o maior uma casa para a direita
                    j--;
                    trocas++;
                }
                v[j + 1] = atual;
                registrar(v, "insere " + atual + " na posição " + (j + 1));
            }
        }

        /** Merge sort: divide ao meio até sobrar um elemento, depois intercala metades que já estão ordenadas. */
        void mergeSort(int[] v, int ini, int fim, int[] apoio) {
            if (ini >= fim) return;   // um elemento só já está ordenado: é o caso base da recursão
            int meio = (ini + fim) >>> 1;   // >>> evita estouro do int quando ini + fim passa de 2 bilhões
            mergeSort(v, ini, meio, apoio);
            mergeSort(v, meio + 1, fim, apoio);
            intercalar(v, ini, meio, fim, apoio);
            registrar(v, "intercala [" + ini + ".." + meio + "] com [" + (meio + 1) + ".." + fim + "]");
        }

        void intercalar(int[] v, int ini, int meio, int fim, int[] apoio) {
            int i = ini, j = meio + 1, k = ini;
            while (i <= meio && j <= fim) {
                if (comparar(v[i], v[j]) <= 0) apoio[k++] = v[i++];   // <= mantém a ordem dos empates (estável)
                else apoio[k++] = v[j++];
            }
            while (i <= meio) apoio[k++] = v[i++];
            while (j <= fim) apoio[k++] = v[j++];
            for (k = ini; k <= fim; k++) { v[k] = apoio[k]; trocas++; }   // no merge, cada cópia de volta conta como movimentação
        }

        /** Quick sort: escolhe um pivô, põe os menores à esquerda e os maiores à direita, e repete em cada lado. */
        void quickSort(int[] v, int ini, int fim) {
            if (ini >= fim) return;
            int p = particionar(v, ini, fim);
            registrar(v, "pivô " + v[p] + " fixo na posição " + p);
            quickSort(v, ini, p - 1);
            quickSort(v, p + 1, fim);
        }

        int particionar(int[] v, int ini, int fim) {
            trocar(v, (ini + fim) >>> 1, fim);   // pivô do meio vai para o fim: evita o pior caso em vetor já ordenado
            int pivo = v[fim];
            int fronteira = ini;   // tudo antes da fronteira é menor ou igual ao pivô
            for (int i = ini; i < fim; i++) {
                if (comparar(v[i], pivo) <= 0) {
                    if (i != fronteira) trocar(v, i, fronteira);
                    fronteira++;
                }
            }
            trocar(v, fronteira, fim);
            return fronteira;
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            List<Algoritmo> escolhidos = escolher(args[0]);
            boolean passos = false;
            int[] vetor = null;
            List<Integer> numeros = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--passos" -> passos = true;
                    case "--aleatorio" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--aleatorio precisa da quantidade, por exemplo: --aleatorio 3000.");
                        vetor = aleatorio(inteiro(args[++i]), 42);
                    }
                    default -> numeros.add(inteiro(args[i]));
                }
            }
            if (vetor == null) vetor = numeros.stream().mapToInt(Integer::intValue).toArray();
            if (vetor.length == 0) throw new IllegalArgumentException("Informe os números a ordenar ou use --aleatorio <quantidade>.");
            if (passos && vetor.length > 20) throw new IllegalArgumentException("--passos só faz sentido com até 20 números; com " + vetor.length + " a tela não para em pé.");
            executar(escolhidos, vetor, passos);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        List<Algoritmo> todos = List.of(Algoritmo.values());
        int[] vetor = {29, 10, 14, 37, 13, 5, 42, 8};
        System.out.println("=== Demonstração 1: bubble sort passo a passo ===");
        executar(List.of(Algoritmo.BUBBLE), vetor, true);
        System.out.println("\n=== Demonstração 2: os quatro algoritmos no mesmo vetor de 8 números ===");
        executar(todos, vetor, false);
        System.out.println("Com 8 números o tempo é ruído de aquecimento da JVM: confie nas contagens.");
        System.out.println("\n=== Demonstração 3: 3.000 números aleatórios, onde a diferença aparece ===");
        executar(todos, aleatorio(3000, 42), false);
        System.out.println("\nLeitura: bubble e insertion crescem com n² (3.000² = 9.000.000); merge e quick");
        System.out.println("crescem com n log n (3.000 x 11,6 = cerca de 35.000). Dobre o n e veja o abismo.");
    }

    static void executar(List<Algoritmo> algoritmos, int[] vetor, boolean passos) {
        System.out.println("Vetor original (" + vetor.length + " números): " + resumo(vetor));
        List<Resultado> resultados = new ArrayList<>();
        for (Algoritmo a : algoritmos) {
            if (passos) System.out.println(a.titulo + ", passo a passo:");
            resultados.add(new Ordenador(a, passos).executar(vetor));
        }
        int[] esperado = vetor.clone();
        Arrays.sort(esperado);   // a biblioteca padrão serve de gabarito para conferir os quatro
        System.out.println("Vetor ordenado: " + resumo(resultados.get(0).ordenado()) + "\n");
        System.out.printf("%-16s | %14s | %22s | %10s | %s%n", "Algoritmo", "Comparações", "Trocas/movimentações", "Tempo", "Confere");
        System.out.println("-".repeat(80));
        for (Resultado r : resultados) {
            System.out.printf(PT_BR, "%-16s | %,14d | %,22d | %7.2f ms | %s%n", r.algoritmo().titulo, r.comparacoes(), r.trocas(),
                    r.nanos() / 1_000_000.0, Arrays.equals(r.ordenado(), esperado) ? "ok" : "ERRADO");
        }
    }

    /** "bubble", "merge"... ou "todos". A comparação ignora maiúsculas. */
    static List<Algoritmo> escolher(String nome) {
        if (nome.equalsIgnoreCase("todos")) return List.of(Algoritmo.values());
        for (Algoritmo a : Algoritmo.values()) if (a.name().equalsIgnoreCase(nome)) return List.of(a);
        throw new IllegalArgumentException("Algoritmo desconhecido '" + nome + "'. Use bubble, insertion, merge, quick ou todos.");
    }

    /** A semente fixa faz o "aleatório" ser sempre o mesmo: assim dá para comparar execuções. */
    static int[] aleatorio(int quantidade, long semente) {
        if (quantidade < 1 || quantidade > 200_000) throw new IllegalArgumentException("A quantidade deve ficar entre 1 e 200.000, recebi " + quantidade + ".");
        Random sorteio = new Random(semente);
        int[] v = new int[quantidade];
        for (int i = 0; i < v.length; i++) v[i] = sorteio.nextInt(quantidade * 10);
        return v;
    }

    /** Vetores grandes não cabem na tela: mostra o começo e o fim. */
    static String resumo(int[] v) {
        if (v.length <= 20) return Arrays.toString(v);
        String comeco = Arrays.toString(Arrays.copyOfRange(v, 0, 8));
        String fim = Arrays.toString(Arrays.copyOfRange(v, v.length - 3, v.length));
        return comeco.replace("]", ", ..., ") + fim.substring(1);
    }

    static int inteiro(String texto) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Esperava um número inteiro, recebi '" + texto + "'."); }
    }

    static void uso() {
        System.out.println("""
            Uso: java OrdenacaoVisual.java <algoritmo> [--passos] [--aleatorio N] <números...>
              algoritmo: bubble, insertion, merge, quick ou todos
              --passos     mostra o vetor a cada passo (até 20 números)
              --aleatorio  gera N números em vez de ler da linha de comando
            Exemplos:
              java OrdenacaoVisual.java bubble --passos 5 3 8 1 9 2
              java OrdenacaoVisual.java todos --aleatorio 3000
              java OrdenacaoVisual.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente o selection sort: ache o menor da parte não ordenada e traga-o para a frente.
 *  2. Rode "todos --aleatorio" com 1.000, 2.000 e 4.000 e anote como cada tempo cresce ao dobrar o n.
 *  3. Troque o pivô do quick sort pelo primeiro elemento e meça o que acontece com um vetor já ordenado.
 */
