/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  24. Lista ligada do zero
 * ------------------------------------------------------------
 * O que faz: implementa uma lista simplesmente ligada genérica com inserir no
 *            início, no fim e numa posição, remover por valor e por posição,
 *            buscar, inverter, tamanho e impressão, e compara com o ArrayList
 *            da linguagem na operação em que cada um brilha.
 * Como usar: java ListaLigada.java inicio:uva fim:figo em:1:kiwi remover:figo inverter buscar:kiwi
 *            java ListaLigada.java --comparar 40000
 *            java ListaLigada.java --demo
 * O que você aprende: classes aninhadas e referências (o "ponteiro" do Java),
 *            genéricos, Iterable com classe anônima para ganhar o for-each,
 *            o cuidado com null, multi-catch, e por que inserir no início
 *            custa O(1) numa lista ligada e O(n) num array.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ListaLigada {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** A estrutura em si. Cada nó conhece só o próximo; a lista guarda o primeiro (cabeça) e o último (cauda). */
    static class Lista<T> implements Iterable<T> {

        private static class No<T> {
            T valor;
            No<T> proximo;   // null no último nó: é assim que sabemos onde a lista acaba
            No(T valor, No<T> proximo) { this.valor = valor; this.proximo = proximo; }
        }

        private No<T> cabeca;   // null quando a lista está vazia
        private No<T> cauda;    // guardar o último faz inserir no fim custar O(1) em vez de caminhar tudo
        private int tamanho;

        void inserirInicio(T valor) {
            cabeca = new No<>(valor, cabeca);   // o nó novo aponta para quem era o primeiro: ninguém se desloca
            if (cauda == null) cauda = cabeca;
            tamanho++;
        }

        void inserirFim(T valor) {
            No<T> novo = new No<>(valor, null);
            if (cauda == null) cabeca = novo; else cauda.proximo = novo;   // lista vazia: o novo é também o primeiro
            cauda = novo;
            tamanho++;
        }

        void inserirEm(int posicao, T valor) {
            if (posicao < 0 || posicao > tamanho) throw new IndexOutOfBoundsException("Posição " + posicao + " fora da lista de tamanho " + tamanho + " (vale 0 a " + tamanho + ").");
            if (posicao == 0) { inserirInicio(valor); return; }
            if (posicao == tamanho) { inserirFim(valor); return; }
            No<T> anterior = no(posicao - 1);   // caminha até o nó que ficará antes do novo
            anterior.proximo = new No<>(valor, anterior.proximo);
            tamanho++;
        }

        T removerEm(int posicao) {
            if (posicao < 0 || posicao >= tamanho) throw new IndexOutOfBoundsException("Posição " + posicao + " não existe na lista de tamanho " + tamanho + ".");
            No<T> removido;
            if (posicao == 0) {
                removido = cabeca;
                cabeca = cabeca.proximo;
                if (cabeca == null) cauda = null;
            } else {
                No<T> anterior = no(posicao - 1);
                removido = anterior.proximo;
                anterior.proximo = removido.proximo;   // "pula" o nó: sem ninguém apontando para ele, o coletor de lixo o recolhe
                if (removido == cauda) cauda = anterior;
            }
            tamanho--;
            return removido.valor;
        }

        /** Remove a primeira ocorrência do valor. Devolve false se ele não estava na lista. */
        boolean remover(T valor) {
            int i = indiceDe(valor);
            if (i < 0) return false;
            removerEm(i);
            return true;
        }

        /** Busca é sempre linear numa lista ligada: não existe "pular" para o meio. */
        int indiceDe(T valor) {
            int i = 0;
            for (No<T> n = cabeca; n != null; n = n.proximo, i++) if (Objects.equals(n.valor, valor)) return i;
            return -1;
        }

        T obter(int posicao) {
            if (posicao < 0 || posicao >= tamanho) throw new IndexOutOfBoundsException("Posição " + posicao + " não existe na lista de tamanho " + tamanho + ".");
            return no(posicao).valor;
        }

        /** Inverte no lugar: a cada passo vira a seta de um nó para trás. Três referências bastam. */
        void inverter() {
            No<T> anterior = null, atual = cabeca;
            cauda = cabeca;
            while (atual != null) {
                No<T> proximo = atual.proximo;   // guarda antes de virar a seta, senão o resto da lista se perde
                atual.proximo = anterior;
                anterior = atual;
                atual = proximo;
            }
            cabeca = anterior;
        }

        int tamanho() { return tamanho; }

        private No<T> no(int posicao) {
            No<T> n = cabeca;
            for (int i = 0; i < posicao; i++) n = n.proximo;
            return n;
        }

        /** Implementar Iterable é o que permite escrever for (String x : lista). */
        @Override public Iterator<T> iterator() {
            return new Iterator<>() {
                No<T> atual = cabeca;
                @Override public boolean hasNext() { return atual != null; }
                @Override public T next() {
                    if (atual == null) throw new NoSuchElementException("A lista acabou.");
                    T v = atual.valor; atual = atual.proximo; return v;
                }
            };
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (No<T> n = cabeca; n != null; n = n.proximo) sb.append(n.valor).append(n.proximo == null ? "" : " -> ");
            return sb.append("]").toString();
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args[0].equals("--comparar")) {
                if (args.length != 2) throw new IllegalArgumentException("--comparar precisa de exatamente um número, por exemplo: --comparar 40000.");
                comparar(inteiro(args[1]));
                return;
            }
            Lista<String> lista = new Lista<>();
            for (String comando : args) System.out.printf("%-20s %-42s %s%n", comando, executar(lista, comando), lista);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {   // multi-catch: dois tipos, o mesmo tratamento
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    /** Interpreta um comando de texto, aplica na lista e devolve a descrição do que aconteceu. */
    static String executar(Lista<String> lista, String comando) {
        String[] p = comando.split(":");
        return switch (p[0]) {
            case "inicio" -> { lista.inserirInicio(arg(p, 1)); yield "inseriu '" + p[1] + "' no início"; }
            case "fim" -> { lista.inserirFim(arg(p, 1)); yield "inseriu '" + p[1] + "' no fim"; }
            case "em" -> { lista.inserirEm(inteiro(arg(p, 1)), arg(p, 2)); yield "inseriu '" + p[2] + "' na posição " + p[1]; }
            case "remover" -> lista.remover(arg(p, 1)) ? "removeu '" + p[1] + "'" : "'" + p[1] + "' não estava na lista";
            case "removerem" -> "removeu '" + lista.removerEm(inteiro(arg(p, 1))) + "' da posição " + p[1];
            case "buscar" -> { int i = lista.indiceDe(arg(p, 1)); yield i < 0 ? "'" + p[1] + "' não está na lista" : "'" + p[1] + "' está na posição " + i; }
            case "obter" -> "a posição " + arg(p, 1) + " guarda '" + lista.obter(inteiro(p[1])) + "'";
            case "inverter" -> { lista.inverter(); yield "inverteu a lista"; }
            case "tamanho" -> "a lista tem " + lista.tamanho() + " elemento(s)";
            default -> throw new IllegalArgumentException("Comando desconhecido '" + comando + "'. Veja a lista de comandos no uso.");
        };
    }

    /** Onde cada estrutura brilha: inserir no início (ligada ganha) e acessar pelo meio (ArrayList ganha). */
    static void comparar(int n) {
        if (n < 1_000 || n > 200_000) throw new IllegalArgumentException("Use entre 1.000 e 200.000 elementos, recebi " + n + ".");
        long inicio = System.nanoTime();
        Lista<Integer> ligada = new Lista<>();
        for (int i = 0; i < n; i++) ligada.inserirInicio(i);
        long ligadaInsere = System.nanoTime() - inicio;

        inicio = System.nanoTime();
        List<Integer> array = new ArrayList<>();
        for (int i = 0; i < n; i++) array.add(0, i);   // cada add(0, x) empurra todos os anteriores uma casa para a direita
        long arrayInsere = System.nanoTime() - inicio;

        int meio = n / 2, leituras = 1_000;
        inicio = System.nanoTime();
        for (int i = 0; i < leituras; i++) ligada.obter(meio);   // caminha n/2 nós a cada leitura
        long ligadaLe = System.nanoTime() - inicio;
        inicio = System.nanoTime();
        for (int i = 0; i < leituras; i++) array.get(meio);   // pula direto para a posição: aritmética de endereço
        long arrayLe = System.nanoTime() - inicio;

        System.out.printf(PT_BR, "%-44s | %12s | %12s%n", "Operação (n = " + String.format(PT_BR, "%,d", n) + ")", "Lista ligada", "ArrayList");
        System.out.println("-".repeat(72));
        System.out.printf(PT_BR, "%-44s | %9.2f ms | %9.2f ms%n", n + " inserções no início", ligadaInsere / 1e6, arrayInsere / 1e6);
        System.out.printf(PT_BR, "%-44s | %9.2f ms | %9.2f ms%n", leituras + " leituras da posição " + meio, ligadaLe / 1e6, arrayLe / 1e6);
        System.out.println("Nenhuma estrutura é melhor em tudo: escolha pela operação que você mais faz.");
    }

    static void demo() {
        System.out.println("=== Demonstração 1: todas as operações, com a lista impressa a cada passo ===");
        Lista<String> lista = new Lista<>();
        for (String c : List.of("fim:uva", "inicio:figo", "fim:kiwi", "em:1:manga", "tamanho", "buscar:kiwi", "buscar:pera",
                "remover:figo", "removerem:1", "inverter", "obter:0", "inicio:ameixa")) {
            System.out.printf("%-16s %-36s %s%n", c, executar(lista, c), lista);
        }
        System.out.print("For-each pela lista (graças ao Iterable): ");
        for (String fruta : lista) System.out.print(fruta.toUpperCase() + " ");
        System.out.println();

        System.out.println("\n=== Demonstração 2: um erro tratado ===");
        try {
            executar(lista, "removerem:10");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("removerem:10 -> " + e.getMessage());
        }

        System.out.println("\n=== Demonstração 3: lista ligada x ArrayList com 40.000 elementos ===");
        comparar(40_000);
    }

    static String arg(String[] partes, int i) {
        if (partes.length <= i || partes[i].isEmpty()) throw new IllegalArgumentException("O comando '" + partes[0] + "' precisa de mais valores separados por dois pontos.");
        return partes[i];
    }

    static int inteiro(String texto) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Esperava um número inteiro, recebi '" + texto + "'."); }
    }

    static void uso() {
        System.out.println("""
            Uso: java ListaLigada.java <comandos...>      aplica os comandos em ordem numa lista vazia
                 java ListaLigada.java --comparar <n>     mede lista ligada x ArrayList com n elementos
            Comandos: inicio:valor  fim:valor  em:P:valor  remover:valor  removerem:P
                      buscar:valor  obter:P  inverter  tamanho
            Exemplos:
              java ListaLigada.java inicio:uva fim:figo em:1:kiwi remover:figo inverter buscar:kiwi
              java ListaLigada.java --comparar 40000
              java ListaLigada.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Transforme em lista duplamente ligada (cada nó conhece o anterior) e implemente removerFim em O(1).
 *  2. Escreva inverter() de forma recursiva e compare com a versão com while.
 *  3. Detecte um ciclo (um nó apontando para um anterior) com dois ponteiros, um andando 1 e outro 2 casas.
 */
