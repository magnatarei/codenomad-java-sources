/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  25. Fila de prioridade com heap
 * ------------------------------------------------------------
 * O que faz: implementa um heap binário mínimo do zero (inserir, extrair,
 *            espiar) guardado num array, e usa a estrutura numa simulação de
 *            pronto-socorro em que os pacientes são atendidos pela gravidade,
 *            não pela ordem de chegada.
 * Como usar: java FilaPrioridade.java Ana:3 Bruno:1 Carla:4 Davi:2 Elisa:1
 *            java FilaPrioridade.java --valores 9 4 7 1 8 3
 *            java FilaPrioridade.java --demo
 * O que você aprende: árvore binária guardada num array (filhos em 2i+1 e
 *            2i+2, pai em (i-1)/2), os movimentos subir e descer do heap,
 *            Comparable com desempate, genéricos com limite
 *            (<T extends Comparable<T>>) e por que inserir e extrair custam
 *            O(log n).
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class FilaPrioridade {

    /** Heap mínimo: o menor elemento fica sempre na raiz (posição 0) e todo pai é menor ou igual aos filhos. */
    static class HeapMinimo<T extends Comparable<T>> {
        private final List<T> itens = new ArrayList<>();   // a árvore mora num array: nada de nós e referências

        void inserir(T item) {
            itens.add(item);            // entra na última folha...
            subir(itens.size() - 1);    // ...e sobe enquanto for menor que o pai
        }

        T extrair() {
            if (vazio()) throw new NoSuchElementException("O heap está vazio: nada para extrair.");
            T menor = itens.get(0);
            T ultimo = itens.remove(itens.size() - 1);
            if (!itens.isEmpty()) {
                itens.set(0, ultimo);   // o último toma o lugar da raiz...
                descer(0);              // ...e desce até os dois filhos serem maiores que ele
            }
            return menor;
        }

        T espiar() {
            if (vazio()) throw new NoSuchElementException("O heap está vazio: nada para espiar.");
            return itens.get(0);
        }

        boolean vazio() { return itens.isEmpty(); }
        int tamanho() { return itens.size(); }

        private void subir(int i) {
            while (i > 0) {
                int pai = (i - 1) / 2;
                if (itens.get(i).compareTo(itens.get(pai)) >= 0) break;   // já respeita o pai: parou
                Collections.swap(itens, i, pai);
                i = pai;
            }
        }

        private void descer(int i) {
            int n = itens.size();
            while (true) {
                int esquerdo = 2 * i + 1, direito = 2 * i + 2, menor = i;
                if (esquerdo < n && itens.get(esquerdo).compareTo(itens.get(menor)) < 0) menor = esquerdo;
                if (direito < n && itens.get(direito).compareTo(itens.get(menor)) < 0) menor = direito;
                if (menor == i) break;   // nenhum filho é menor: a propriedade do heap voltou a valer
                Collections.swap(itens, i, menor);
                i = menor;
            }
        }

        /** Desenha a árvore por níveis: o nível k começa na posição 2^k - 1 e tem 2^k elementos. */
        String desenhar() {
            StringBuilder sb = new StringBuilder();
            for (int inicio = 0, largura = 1, nivel = 0; inicio < itens.size(); inicio += largura, largura *= 2, nivel++) {
                sb.append("   nível ").append(nivel).append(": ");
                for (int i = inicio; i < Math.min(inicio + largura, itens.size()); i++) sb.append(itens.get(i)).append("  ");
                sb.append('\n');
            }
            return sb.toString();
        }

        @Override public String toString() { return itens.toString(); }
    }

    /** Gravidade 1 é a mais urgente. Empate na gravidade: quem chegou primeiro é atendido primeiro. */
    record Paciente(String nome, int gravidade, int chegada) implements Comparable<Paciente> {
        static final String[] DESCRICAO = {"", "emergência", "muito urgente", "urgente", "pouco urgente", "não urgente"};

        Paciente {
            if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O paciente precisa de um nome.");
            if (gravidade < 1 || gravidade > 5) throw new IllegalArgumentException("A gravidade de " + nome + " deve ir de 1 (emergência) a 5 (não urgente), recebi " + gravidade + ".");
        }

        @Override public int compareTo(Paciente outro) {
            if (gravidade != outro.gravidade) return Integer.compare(gravidade, outro.gravidade);
            return Integer.compare(chegada, outro.chegada);   // o desempate é o que torna a fila justa entre iguais
        }

        @Override public String toString() { return nome + "(" + gravidade + ")"; }
    }

    /** Todos chegam, entram no heap, e a saída é sempre quem está em pior estado. */
    static void prontoSocorro(List<Paciente> chegada) {
        System.out.println("   Ordem de chegada: " + chegada);
        HeapMinimo<Paciente> fila = new HeapMinimo<>();
        for (Paciente p : chegada) fila.inserir(p);
        System.out.println("   Heap depois das inserções: " + fila);
        System.out.println("   Próximo a ser chamado (espiar): " + fila.espiar());
        System.out.printf("   %-5s %-10s %-16s %-10s %s%n", "Vez", "Paciente", "Gravidade", "Chegou", "Observação");
        for (int vez = 1; !fila.vazio(); vez++) {
            Paciente p = fila.extrair();
            int pulou = p.chegada() - vez;
            String obs = pulou > 0 ? "passou na frente de " + pulou : pulou < 0 ? "esperou " + (-pulou) + " mais grave(s)" : "na própria ordem";
            System.out.printf("   %-5s %-10s %-16s %-10s %s%n", vez + "º", p.nome(), p.gravidade() + " " + Paciente.DESCRICAO[p.gravidade()], p.chegada() + "º", obs);
        }
    }

    /** Mostra o heap crescendo número a número e depois esvaziando: extrair tudo devolve os números em ordem. */
    static void mostrarNumeros(int[] numeros) {
        HeapMinimo<Integer> heap = new HeapMinimo<>();
        for (int n : numeros) {
            heap.inserir(n);
            System.out.printf("   inserir %-4d -> %s%n", n, heap);
        }
        System.out.println("   Árvore final:");
        System.out.print(heap.desenhar());
        List<Integer> saida = new ArrayList<>();
        while (!heap.vazio()) saida.add(heap.extrair());
        System.out.println("   Extraindo tudo: " + saida + "  (isto é o heap sort: n extrações de O(log n) cada)");
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args[0].equals("--valores")) {
                if (args.length == 1) throw new IllegalArgumentException("--valores precisa de pelo menos um número.");
                mostrarNumeros(Arrays.stream(args).skip(1).mapToInt(FilaPrioridade::inteiro).toArray());
            } else {
                prontoSocorro(pacientes(args));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração 1: o heap de números, passo a passo ===");
        mostrarNumeros(new int[]{9, 4, 7, 1, 8, 3, 5});

        System.out.println("\n=== Demonstração 2: pronto-socorro, atendimento por gravidade ===");
        prontoSocorro(pacientes(new String[]{"Ana:3", "Bruno:1", "Carla:4", "Davi:2", "Elisa:1", "Fábio:5", "Gabi:3", "Hugo:2"}));

        System.out.println("\n=== Demonstração 3: dois erros tratados ===");
        for (String caso : new String[]{"Iara:7", "Joel"}) {
            try {
                pacientes(new String[]{caso});
            } catch (IllegalArgumentException e) {
                System.out.println("   " + caso + " -> " + e.getMessage());
            }
        }
    }

    /** "Nome:gravidade" vira Paciente; a ordem na linha de comando é a ordem de chegada. */
    static List<Paciente> pacientes(String[] textos) {
        List<Paciente> lista = new ArrayList<>();
        for (int i = 0; i < textos.length; i++) {
            String[] partes = textos[i].split(":");
            if (partes.length != 2) throw new IllegalArgumentException("Paciente no formato Nome:gravidade, recebi '" + textos[i] + "'.");
            lista.add(new Paciente(partes[0].trim(), inteiro(partes[1]), i + 1));
        }
        return lista;
    }

    static int inteiro(String texto) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Esperava um número inteiro, recebi '" + texto + "'."); }
    }

    static void uso() {
        System.out.println("""
            Uso: java FilaPrioridade.java <Nome:gravidade...>     simula o pronto-socorro (1 = emergência, 5 = não urgente)
                 java FilaPrioridade.java --valores <números...>  mostra o heap crescendo e esvaziando
            Exemplos:
              java FilaPrioridade.java Ana:3 Bruno:1 Carla:4 Davi:2 Elisa:1
              java FilaPrioridade.java --valores 9 4 7 1 8 3
              java FilaPrioridade.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Transforme em heap máximo trocando só as comparações, e use-o para achar os 3 maiores de uma lista.
 *  2. Aceite um Comparator no construtor, como faz a PriorityQueue da biblioteca padrão, em vez de exigir Comparable.
 *  3. Simule chegadas no meio do atendimento: insira um paciente de gravidade 1 depois de duas extrações e veja o que acontece.
 */
