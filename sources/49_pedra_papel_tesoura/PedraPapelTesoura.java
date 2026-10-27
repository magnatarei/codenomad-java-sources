/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  49. Pedra, papel e tesoura com placar
 * ------------------------------------------------------------
 * O que faz: série de rodadas contra o computador com placar. O computador
 *            começa jogando ao acaso, mas conta as suas jogadas e passa a
 *            escolher o que vence a sua jogada mais usada. No fim mostra o
 *            placar e o que ele aprendeu sobre você.
 * Como usar: java PedraPapelTesoura.java --rodadas 5          (melhor de 5, interativo)
 *            java PedraPapelTesoura.java --jogadas p,p,a,t,p   (série fixa, sem teclado)
 *            java PedraPapelTesoura.java --demo
 * O que você aprende: enum com métodos, EnumMap para contar, Random com
 *            semente, Scanner, Collectors e streams para achar o máximo,
 *            records e uma estratégia simples de "aprendizado" por frequência.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class PedraPapelTesoura {

    /** Cada jogada sabe quem ela vence. PEDRA.vence(TESOURA) é true. */
    enum Jogada {
        PEDRA('p'), PAPEL('a'), TESOURA('t');
        final char tecla;
        Jogada(char tecla) { this.tecla = tecla; }

        boolean vence(Jogada outra) {
            return (this == PEDRA && outra == TESOURA) || (this == PAPEL && outra == PEDRA) || (this == TESOURA && outra == PAPEL);
        }

        /** A jogada que ganha desta: o contra-ataque que o computador vai usar. */
        Jogada contra() {
            return switch (this) { case PEDRA -> PAPEL; case PAPEL -> TESOURA; case TESOURA -> PEDRA; };
        }

        static Jogada daTecla(String texto) {
            for (Jogada j : values()) {
                if (texto.equals(String.valueOf(j.tecla)) || texto.equals(j.name().toLowerCase())) return j;
            }
            return null;
        }
    }

    record Rodada(int numero, Jogada jogador, Jogada computador, String resultado) {}

    /** O computador: guarda a contagem das jogadas do adversário e decide a partir dela. */
    static final class Computador {
        final Map<Jogada, Integer> contagem = new EnumMap<>(Jogada.class);   // EnumMap: mapa rápido e ordenado por enum
        final Random sorteio;
        Computador(Random sorteio) { this.sorteio = sorteio; }

        Jogada decidir() {
            Jogada favorita = favoritaDoAdversario();
            // Sem histórico (ou com empate entre as mais usadas) não há o que aprender: joga ao acaso.
            if (favorita == null) return Jogada.values()[sorteio.nextInt(3)];
            return favorita.contra();
        }

        /** A jogada mais usada pelo adversário, ou null se não houver uma só líder. */
        Jogada favoritaDoAdversario() {
            Jogada lider = null;
            int maior = 0;
            boolean empate = false;
            for (var e : contagem.entrySet()) {
                if (e.getValue() > maior) { maior = e.getValue(); lider = e.getKey(); empate = false; }
                else if (e.getValue() == maior) empate = true;
            }
            return empate ? null : lider;
        }

        void aprender(Jogada doAdversario) {
            contagem.merge(doAdversario, 1, Integer::sum);  // merge: cria com 1 ou soma 1 ao que já existe
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        int rodadas = 3;
        List<Jogada> fixas = null;
        long semente = System.nanoTime();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--demo" -> { demo(); return; }
                case "--rodadas" -> rodadas = inteiro(args, ++i, "--rodadas", 1, 99);
                case "--semente" -> semente = inteiro(args, ++i, "--semente", Integer.MIN_VALUE, Integer.MAX_VALUE);
                case "--jogadas" -> fixas = lerJogadas(i + 1 < args.length ? args[++i] : "");
                default -> falhar("não conheço a opção '" + args[i] + "'");
            }
        }
        if (fixas != null) {
            serie(fixas, new Computador(new Random(semente)));
            return;
        }
        if (rodadas % 2 == 0) falhar("--rodadas precisa ser ímpar, para sempre haver um vencedor; recebi " + rodadas);
        melhorDe(rodadas, new Scanner(System.in, StandardCharsets.UTF_8), new Computador(new Random(semente)));
    }

    /** "p,p,a,t" vira a lista de jogadas; qualquer letra desconhecida é erro. */
    static List<Jogada> lerJogadas(String texto) {
        var lista = new ArrayList<Jogada>();
        for (String parte : texto.split(",")) {
            Jogada j = Jogada.daTecla(parte.strip().toLowerCase());
            if (j == null) falhar("jogada desconhecida '" + parte + "'; use p (pedra), a (papel) ou t (tesoura)");
            lista.add(j);
        }
        return lista;
    }

    /** Joga uma rodada: decide, compara, anuncia e deixa o computador aprender. */
    static Rodada jogarRodada(int numero, Jogada doJogador, Computador cpu, int[] placar) {
        Jogada daCpu = cpu.decidir();
        String resultado;
        if (doJogador == daCpu) { resultado = "empate"; placar[2]++; }
        else if (doJogador.vence(daCpu)) { resultado = "você venceu"; placar[0]++; }
        else { resultado = "computador venceu"; placar[1]++; }
        cpu.aprender(doJogador);
        var r = new Rodada(numero, doJogador, daCpu, resultado);
        System.out.printf("Rodada %d: você %-8s x %-8s computador  ->  %s   [%d x %d, %d empates]%n",
                r.numero(), r.jogador(), r.computador(), r.resultado(), placar[0], placar[1], placar[2]);
        return r;
    }

    /** Interativo: acaba quando alguém vence a maioria das rodadas (empates não contam). */
    static void melhorDe(int rodadas, Scanner teclado, Computador cpu) {
        int precisa = rodadas / 2 + 1;
        int[] placar = new int[3];                          // vitórias suas, do computador, empates
        System.out.println("=== Melhor de " + rodadas + ": quem fizer " + precisa + " vitórias leva ===");
        for (int n = 1; placar[0] < precisa && placar[1] < precisa; n++) {
            System.out.print("Sua jogada [p]edra, p[a]pel, [t]esoura: ");
            if (!teclado.hasNextLine()) { System.out.println("\nEntrada encerrada."); break; }
            Jogada j = Jogada.daTecla(teclado.nextLine().strip().toLowerCase());
            if (j == null) { System.out.println("Não entendi. Digite p, a ou t."); n--; continue; }
            jogarRodada(n, j, cpu, placar);
        }
        encerrar(placar, cpu);
    }

    /** Série fixa: joga todas as jogadas da lista, sem teclado. */
    static void serie(List<Jogada> jogadas, Computador cpu) {
        int[] placar = new int[3];
        System.out.println("=== Série de " + jogadas.size() + " rodadas ===");
        for (int i = 0; i < jogadas.size(); i++) jogarRodada(i + 1, jogadas.get(i), cpu, placar);
        encerrar(placar, cpu);
    }

    static void encerrar(int[] placar, Computador cpu) {
        System.out.println();
        System.out.printf("Placar final: você %d x %d computador (%d empates). %s%n", placar[0], placar[1], placar[2],
                placar[0] > placar[1] ? "Você venceu!" : placar[0] < placar[1] ? "O computador venceu." : "Empatou.");
        System.out.println("O que o computador aprendeu sobre você:");
        cpu.contagem.forEach((jogada, vezes) -> System.out.printf("  %-8s %d vez(es) %s%n", jogada, vezes, "#".repeat(vezes)));
        Jogada favorita = cpu.favoritaDoAdversario();
        System.out.println(favorita == null ? "  Nenhuma jogada favorita clara; ele continuaria jogando ao acaso."
                : "  Sua favorita é " + favorita + ", então ele responderia com " + favorita.contra() + ".");
    }

    static void demo() {
        System.out.println("=== Demonstração: 7 rodadas com jogadas pré-definidas ===");
        System.out.println("O jogador insiste em PEDRA; veja o computador perceber e passar a jogar PAPEL.");
        // Semente fixa: as jogadas aleatórias do começo saem iguais toda vez que a demo roda.
        serie(lerJogadas("p,p,a,p,t,p,p"), new Computador(new Random(42)));
    }

    static int inteiro(String[] args, int i, String opcao, int minimo, int maximo) {
        if (i >= args.length) falhar(opcao + " precisa de um número");
        try {
            int n = Integer.parseInt(args[i]);
            if (n < minimo || n > maximo) falhar(opcao + " precisa estar entre " + minimo + " e " + maximo + "; recebi " + n);
            return n;
        } catch (NumberFormatException e) {
            falhar(opcao + " precisa de um número inteiro; recebi '" + args[i] + "'");
            return 0;
        }
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo + ".");
        System.out.println("Uso: java PedraPapelTesoura.java [--rodadas N] [--semente N] [--jogadas p,a,t,...] | --demo");
        System.exit(1);
    }
}

/*
 * Para ir além:
 *  1. Faça o computador olhar só as últimas 5 jogadas, para reagir a quem muda de estratégia.
 *  2. Adicione Lagarto e Spock (cada jogada vence duas) e reescreva vence() com um mapa.
 *  3. Guarde o histórico de séries num arquivo e mostre a taxa de vitória acumulada do jogador.
 */
