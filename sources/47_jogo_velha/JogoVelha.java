/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  47. Jogo da velha com computador
 * ------------------------------------------------------------
 * O que faz: jogo da velha no terminal para dois jogadores ou contra o
 *            computador, que usa o algoritmo minimax e por isso nunca perde.
 *            Mostra o tabuleiro numerado, valida cada jogada e detecta vitória
 *            e empate. Na demonstração o computador joga contra ele mesmo.
 * Como usar: java JogoVelha.java                 (menu interativo)
 *            java JogoVelha.java --cpu O         (você é X, o computador é O)
 *            java JogoVelha.java --2p            (duas pessoas no mesmo teclado)
 *            java JogoVelha.java --demo
 * O que você aprende: o minimax (busca em árvore com pontuação), recursão,
 *            arrays como estado de jogo, enum para o resultado, Scanner com
 *            validação e por que o jogo da velha perfeito sempre empata.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JogoVelha {

    static final char VAZIO = ' ';
    /** As oito linhas que dão vitória: três horizontais, três verticais e duas diagonais. */
    static final int[][] LINHAS = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    enum Situacao { EM_ANDAMENTO, VITORIA_X, VITORIA_O, EMPATE }

    /** O tabuleiro são 9 casas, numeradas de 1 a 9 para o jogador e de 0 a 8 no array. */
    static final class Tabuleiro {
        final char[] casas = new char[9];
        Tabuleiro() { java.util.Arrays.fill(casas, VAZIO); }

        boolean livre(int i) { return casas[i] == VAZIO; }

        Situacao situacao() {
            for (int[] l : LINHAS) {
                char a = casas[l[0]];
                if (a != VAZIO && a == casas[l[1]] && a == casas[l[2]]) return a == 'X' ? Situacao.VITORIA_X : Situacao.VITORIA_O;
            }
            for (char c : casas) if (c == VAZIO) return Situacao.EM_ANDAMENTO;
            return Situacao.EMPATE;
        }

        String desenho() {
            var sb = new StringBuilder();
            for (int linha = 0; linha < 3; linha++) {
                for (int coluna = 0; coluna < 3; coluna++) {
                    int i = linha * 3 + coluna;
                    // Casa vazia mostra o número para o jogador saber o que digitar.
                    sb.append(' ').append(livre(i) ? (char) ('1' + i) : casas[i]).append(coluna < 2 ? " |" : "");
                }
                sb.append(linha < 2 ? "\n---+---+---\n" : "");
            }
            return sb.toString();
        }
    }

    /**
     * Minimax: avalia uma posição supondo que os dois lados jogam o melhor possível.
     * Vitória do computador vale +10, derrota -10, empate 0. Descontar a profundidade
     * faz ele preferir ganhar logo e perder o mais tarde possível.
     */
    static int minimax(Tabuleiro t, char cpu, char humano, char vez, int profundidade) {
        Situacao s = t.situacao();
        if (s != Situacao.EM_ANDAMENTO) {
            if (s == Situacao.EMPATE) return 0;
            boolean cpuGanhou = (s == Situacao.VITORIA_X) == (cpu == 'X');
            return cpuGanhou ? 10 - profundidade : profundidade - 10;
        }
        boolean maximizando = vez == cpu;
        int melhor = maximizando ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            if (!t.livre(i)) continue;
            t.casas[i] = vez;                               // joga...
            int valor = minimax(t, cpu, humano, vez == 'X' ? 'O' : 'X', profundidade + 1);
            t.casas[i] = VAZIO;                             // ...e desfaz, para testar a próxima casa
            melhor = maximizando ? Math.max(melhor, valor) : Math.min(melhor, valor);
        }
        return melhor;
    }

    /** Escolhe a casa com a melhor pontuação do minimax. Devolve o índice de 0 a 8. */
    static int melhorJogada(Tabuleiro t, char cpu) {
        char humano = cpu == 'X' ? 'O' : 'X';
        int melhorCasa = -1, melhorValor = Integer.MIN_VALUE;
        for (int i = 0; i < 9; i++) {
            if (!t.livre(i)) continue;
            t.casas[i] = cpu;
            int valor = minimax(t, cpu, humano, humano, 1);
            t.casas[i] = VAZIO;
            if (valor > melhorValor) { melhorValor = valor; melhorCasa = i; }
        }
        return melhorCasa;
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        var teclado = new Scanner(System.in, StandardCharsets.UTF_8);
        if (args.length == 0) {
            System.out.println("=== Jogo da velha ===\n1) Dois jogadores\n2) Contra o computador");
            System.out.print("Escolha: ");
            String escolha = teclado.hasNextLine() ? teclado.nextLine().strip() : "2";
            if (escolha.equals("1")) jogar(teclado, VAZIO); else jogar(teclado, 'O');
            return;
        }
        switch (args[0]) {
            case "--demo" -> demo();
            case "--2p" -> jogar(teclado, VAZIO);
            case "--cpu" -> {
                String simbolo = args.length > 1 ? args[1].toUpperCase() : "O";
                if (!simbolo.equals("X") && !simbolo.equals("O")) falhar("o computador só pode ser X ou O; recebi '" + args[1] + "'");
                jogar(teclado, simbolo.charAt(0));
            }
            default -> falhar("não conheço a opção '" + args[0] + "'");
        }
    }

    /** Partida interativa. Se cpu for VAZIO, as duas pessoas se alternam no teclado. X sempre começa. */
    static void jogar(Scanner teclado, char cpu) {
        var t = new Tabuleiro();
        char vez = 'X';
        System.out.println(cpu == VAZIO ? "Duas pessoas: X começa." : "Você é " + (cpu == 'X' ? 'O' : 'X') + ", o computador é " + cpu + ". X começa.");
        while (t.situacao() == Situacao.EM_ANDAMENTO) {
            System.out.println(t.desenho());
            int casa;
            if (vez == cpu) {
                casa = melhorJogada(t, cpu);
                System.out.println("Computador (" + cpu + ") joga na casa " + (casa + 1) + ".");
            } else {
                casa = lerCasa(teclado, t, vez);
                if (casa < 0) { System.out.println("Entrada encerrada. Partida abandonada."); return; }
            }
            t.casas[casa] = vez;
            vez = vez == 'X' ? 'O' : 'X';
        }
        System.out.println(t.desenho());
        System.out.println(mensagemFinal(t.situacao()));
    }

    /** Insiste até receber um número de 1 a 9 de uma casa livre. Devolve -1 se a entrada acabar. */
    static int lerCasa(Scanner teclado, Tabuleiro t, char vez) {
        while (true) {
            System.out.print("Jogador " + vez + ", escolha a casa (1-9): ");
            if (!teclado.hasNextLine()) return -1;
            String texto = teclado.nextLine().strip();
            if (!texto.matches("[1-9]")) { System.out.println("Digite um número de 1 a 9."); continue; }
            int casa = texto.charAt(0) - '1';
            if (!t.livre(casa)) { System.out.println("A casa " + texto + " já está ocupada."); continue; }
            return casa;
        }
    }

    static String mensagemFinal(Situacao s) {
        return switch (s) {
            case VITORIA_X -> "X venceu!";
            case VITORIA_O -> "O venceu!";
            case EMPATE -> "Deu velha: empate. Contra o minimax, é o melhor resultado possível.";
            case EM_ANDAMENTO -> "A partida continua.";
        };
    }

    /** O computador joga contra si mesmo. Dois jogadores perfeitos sempre empatam. */
    static void demo() {
        System.out.println("=== Demonstração: computador contra computador ===");
        var t = new Tabuleiro();
        char vez = 'X';
        int jogada = 1;
        while (t.situacao() == Situacao.EM_ANDAMENTO) {
            int casa = melhorJogada(t, vez);
            t.casas[casa] = vez;
            System.out.println("Jogada " + jogada++ + ": " + vez + " na casa " + (casa + 1));
            System.out.println(t.desenho());
            System.out.println();
            vez = vez == 'X' ? 'O' : 'X';
        }
        System.out.println("Resultado: " + mensagemFinal(t.situacao()));
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo + ".");
        System.out.println("Uso: java JogoVelha.java [--2p | --cpu X|O | --demo]");
        System.exit(1);
    }
}

/*
 * Para ir além:
 *  1. Mostre quantas posições o minimax avaliou em cada jogada e veja o número cair conforme o tabuleiro enche.
 *  2. Adicione um nível fácil em que o computador joga numa casa aleatória metade das vezes.
 *  3. Generalize para tabuleiro 4x4 e descubra por que o minimax puro fica lento (dica: poda alfa-beta).
 */
