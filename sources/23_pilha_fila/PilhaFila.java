/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  23. Pilha e fila na prática
 * ------------------------------------------------------------
 * O que faz: implementa pilha (LIFO) e fila (FIFO) do zero, com as operações
 *            clássicas, e usa as duas em três problemas reais: parênteses
 *            balanceados, desfazer/refazer de um editor e fila de atendimento
 *            com tempo de espera.
 * Como usar: java PilhaFila.java parenteses "(a + b) * [c - {d}]"
 *            java PilhaFila.java editor digitar:Olá digitar:mundo desfazer refazer
 *            java PilhaFila.java fila Ana:4 Bruno:6 Carla:2 Davi:5
 *            java PilhaFila.java --demo
 * O que você aprende: classes genéricas (<T>), arrays que crescem com
 *            Arrays.copyOf, fila circular com o operador %, records, switch
 *            com texto, e a diferença entre LIFO e FIFO na prática.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PilhaFila {

    /** Pilha: o último que entra é o primeiro que sai (LIFO). Guarda os itens num array que dobra quando enche. */
    static class Pilha<T> {
        private Object[] itens = new Object[4];   // Java não cria array genérico: usamos Object[] e fazemos o cast na saída
        private int tamanho;

        void empilhar(T item) {
            if (tamanho == itens.length) itens = Arrays.copyOf(itens, tamanho * 2);
            itens[tamanho++] = item;
        }

        @SuppressWarnings("unchecked")
        T desempilhar() {
            if (vazia()) throw new IllegalStateException("A pilha está vazia: nada para desempilhar.");
            T item = (T) itens[--tamanho];
            itens[tamanho] = null;   // solta a referência para o coletor de lixo poder limpar
            return item;
        }

        @SuppressWarnings("unchecked")
        T topo() { if (vazia()) throw new IllegalStateException("A pilha está vazia: não há topo."); return (T) itens[tamanho - 1]; }

        boolean vazia() { return tamanho == 0; }
        int tamanho() { return tamanho; }
        void limpar() { Arrays.fill(itens, 0, tamanho, null); tamanho = 0; }
        @Override public String toString() { return "fundo " + Arrays.toString(Arrays.copyOf(itens, tamanho)) + " topo"; }
    }

    /** Fila: o primeiro que entra é o primeiro que sai (FIFO). Array circular: o início anda em vez de deslocar todo mundo. */
    static class Fila<T> {
        private Object[] itens = new Object[4];
        private int inicio, tamanho;

        void enfileirar(T item) {
            if (tamanho == itens.length) crescer();
            itens[(inicio + tamanho) % itens.length] = item;   // o % faz o índice "dar a volta" quando chega ao fim do array
            tamanho++;
        }

        @SuppressWarnings("unchecked")
        T desenfileirar() {
            if (vazia()) throw new IllegalStateException("A fila está vazia: ninguém para atender.");
            T item = (T) itens[inicio];
            itens[inicio] = null;
            inicio = (inicio + 1) % itens.length;
            tamanho--;
            return item;
        }

        @SuppressWarnings("unchecked")
        T primeiro() { if (vazia()) throw new IllegalStateException("A fila está vazia: não há primeiro."); return (T) itens[inicio]; }

        boolean vazia() { return tamanho == 0; }
        int tamanho() { return tamanho; }

        /** Copia os itens, já na ordem da fila, para um array com o dobro do espaço. */
        private void crescer() {
            Object[] novo = new Object[itens.length * 2];
            for (int i = 0; i < tamanho; i++) novo[i] = itens[(inicio + i) % itens.length];
            itens = novo;
            inicio = 0;
        }

        @Override public String toString() {
            Object[] copia = new Object[tamanho];
            for (int i = 0; i < tamanho; i++) copia[i] = itens[(inicio + i) % itens.length];
            return "início " + Arrays.toString(copia) + " fim";
        }
    }

    record Abertura(char simbolo, int posicao) {}

    /** Problema 1: cada abertura vai para a pilha; cada fechamento precisa casar com o topo. */
    static String conferirParenteses(String texto) {
        String abre = "([{", fecha = ")]}";
        Pilha<Abertura> abertos = new Pilha<>();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (abre.indexOf(c) >= 0) {
                abertos.empilhar(new Abertura(c, i));
            } else if (fecha.indexOf(c) >= 0) {
                if (abertos.vazia()) return "ERRADO: '" + c + "' na posição " + i + " fecha sem ter aberto";
                char esperado = fecha.charAt(abre.indexOf(abertos.desempilhar().simbolo()));
                if (c != esperado) return "ERRADO: '" + c + "' na posição " + i + " deveria ser '" + esperado + "'";
            }
        }
        if (!abertos.vazia()) return "ERRADO: '" + abertos.topo().simbolo() + "' aberto na posição " + abertos.topo().posicao() + " nunca fecha";
        return "OK: balanceado";
    }

    /** Problema 2: duas pilhas. Cada ação guarda o texto anterior em "desfazer"; desfazer move o atual para "refazer". */
    static class Editor {
        String texto = "";
        final Pilha<String> desfazer = new Pilha<>();
        final Pilha<String> refazer = new Pilha<>();

        void executar(String comando) {
            String[] partes = comando.split(":", 2);
            switch (partes[0]) {
                case "digitar" -> alterar(texto.isEmpty() ? argumento(partes) : texto + " " + argumento(partes));
                case "apagar" -> alterar(texto.substring(0, Math.max(0, texto.length() - inteiro(argumento(partes)))));
                case "desfazer" -> {
                    if (desfazer.vazia()) { System.out.println("   desfazer: nada para desfazer"); return; }
                    refazer.empilhar(texto);
                    texto = desfazer.desempilhar();
                }
                case "refazer" -> {
                    if (refazer.vazia()) { System.out.println("   refazer: nada para refazer"); return; }
                    desfazer.empilhar(texto);
                    texto = refazer.desempilhar();
                }
                default -> throw new IllegalArgumentException("Comando desconhecido '" + comando + "'. Use digitar:texto, apagar:N, desfazer ou refazer.");
            }
            System.out.printf("   %-16s texto: \"%s\"   (desfazer: %d, refazer: %d)%n", comando, texto, desfazer.tamanho(), refazer.tamanho());
        }

        void alterar(String novo) {
            desfazer.empilhar(texto);
            refazer.limpar();   // uma ação nova apaga o "futuro": o que foi desfeito antes não pode mais ser refeito
            texto = novo;
        }

        static String argumento(String[] partes) {
            if (partes.length < 2 || partes[1].isEmpty()) throw new IllegalArgumentException("O comando '" + partes[0] + "' precisa de um valor depois dos dois pontos.");
            return partes[1];
        }
    }

    record Cliente(String nome, int minutos) {
        @Override public String toString() { return nome + " (" + minutos + " min)"; }   // sem isto o record imprime Cliente[nome=..., minutos=...]
    }

    /** Problema 3: um guichê só. Cada cliente espera a soma dos atendimentos de quem chegou antes dele. */
    static void atender(List<Cliente> chegada) {
        Fila<Cliente> fila = new Fila<>();
        for (Cliente c : chegada) fila.enfileirar(c);
        System.out.println("   Fila na chegada: " + fila);
        System.out.printf("   %-10s %12s %10s %10s%n", "Cliente", "Atendimento", "Esperou", "Saiu aos");
        int relogio = 0, somaEspera = 0, maiorEspera = 0;
        while (!fila.vazia()) {
            Cliente c = fila.desenfileirar();
            somaEspera += relogio;
            maiorEspera = Math.max(maiorEspera, relogio);
            System.out.printf("   %-10s %8d min %6d min %6d min%n", c.nome(), c.minutos(), relogio, relogio + c.minutos());
            relogio += c.minutos();
        }
        System.out.printf("   Espera média: %.1f min, maior espera: %d min, guichê livre aos %d min.%n", (double) somaEspera / chegada.size(), maiorEspera, relogio);
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            String[] resto = Arrays.copyOfRange(args, 1, args.length);
            if (resto.length == 0) throw new IllegalArgumentException("O modo '" + args[0] + "' precisa de argumentos depois dele.");
            switch (args[0]) {
                case "parenteses" -> { for (String t : resto) System.out.println("\"" + t + "\" -> " + conferirParenteses(t)); }
                case "editor" -> { Editor e = new Editor(); for (String comando : resto) e.executar(comando); }
                case "fila" -> atender(clientes(resto));
                default -> throw new IllegalArgumentException("Modo desconhecido '" + args[0] + "'. Use parenteses, editor ou fila.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração 0: a diferença entre pilha e fila ===");
        Pilha<Integer> pilha = new Pilha<>();
        Fila<Integer> fila = new Fila<>();
        for (int n = 1; n <= 5; n++) { pilha.empilhar(n); fila.enfileirar(n); }   // 5 itens em array de 4: as duas crescem
        System.out.println("   Entraram 1, 2, 3, 4, 5 nas duas.  Pilha: " + pilha + "  |  Fila: " + fila);
        StringBuilder saiuPilha = new StringBuilder(), saiuFila = new StringBuilder();
        while (!pilha.vazia()) saiuPilha.append(pilha.desempilhar()).append(' ');
        while (!fila.vazia()) saiuFila.append(fila.desenfileirar()).append(' ');
        System.out.println("   Saem da pilha: " + saiuPilha + "(LIFO)   |   Saem da fila: " + saiuFila + "(FIFO)");

        System.out.println("\n=== Demonstração 1: parênteses balanceados (pilha) ===");
        for (String t : List.of("(a + b) * [c - {d}]", "((1 + 2)", "{[}]", "x) + 1", "")) {
            System.out.printf("   %-24s -> %s%n", "\"" + t + "\"", conferirParenteses(t));
        }

        System.out.println("\n=== Demonstração 2: desfazer e refazer (duas pilhas) ===");
        Editor editor = new Editor();
        for (String c : List.of("digitar:Olá", "digitar:mundo", "digitar:cruel", "apagar:6", "desfazer", "desfazer", "refazer", "digitar:!", "refazer")) editor.executar(c);

        System.out.println("\n=== Demonstração 3: fila de atendimento com um guichê (fila) ===");
        atender(clientes(new String[]{"Ana:4", "Bruno:6", "Carla:2", "Davi:5", "Elisa:3"}));
    }

    /** Converte "Nome:minutos" em Cliente, com erro claro quando o formato está errado. */
    static List<Cliente> clientes(String[] textos) {
        List<Cliente> lista = new ArrayList<>();
        for (String t : textos) {
            String[] partes = t.split(":");
            if (partes.length != 2 || partes[0].isBlank()) throw new IllegalArgumentException("Cliente no formato Nome:minutos, recebi '" + t + "'.");
            int minutos = inteiro(partes[1]);
            if (minutos <= 0) throw new IllegalArgumentException("Os minutos de atendimento devem ser positivos, recebi " + minutos + " para " + partes[0] + ".");
            lista.add(new Cliente(partes[0], minutos));
        }
        return lista;
    }

    static int inteiro(String texto) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Esperava um número inteiro, recebi '" + texto + "'."); }
    }

    static void uso() {
        System.out.println("""
            Uso: java PilhaFila.java parenteses <texto...>        confere ( ) [ ] { } em cada texto
                 java PilhaFila.java editor <comandos...>         digitar:texto, apagar:N, desfazer, refazer
                 java PilhaFila.java fila <Nome:minutos...>       simula o atendimento na ordem de chegada
            Exemplos:
              java PilhaFila.java parenteses "(a + b) * [c - {d}]" "((1 + 2)"
              java PilhaFila.java editor digitar:Olá digitar:mundo desfazer refazer
              java PilhaFila.java fila Ana:4 Bruno:6 Carla:2
              java PilhaFila.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Dê dois guichês à fila de atendimento: cada cliente vai para o guichê que desocupa primeiro.
 *  2. Implemente a pilha com uma lista ligada em vez de array e compare o código das duas versões.
 *  3. Use a pilha para converter um número inteiro para binário: empilhe os restos da divisão por 2 e desempilhe.
 */
