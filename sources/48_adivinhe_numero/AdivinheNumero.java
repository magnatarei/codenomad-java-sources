/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  48. Adivinhe o número, nos dois sentidos
 * ------------------------------------------------------------
 * O que faz: modo 1: o computador sorteia um número e você adivinha com dicas
 *            de "maior" ou "menor". Modo 2: você pensa num número e o
 *            computador adivinha por busca binária, pedindo só "maior",
 *            "menor" ou "certo". Mostra quantas tentativas cada lado usou.
 * Como usar: java AdivinheNumero.java --modo 1 --max 100
 *            java AdivinheNumero.java --modo 2 --max 1000
 *            java AdivinheNumero.java --modo 2 --alvo 73   (sem teclado: o programa responde por você)
 *            java AdivinheNumero.java --demo
 * O que você aprende: busca binária na prática, logaritmo para prever o
 *            número de tentativas, Random, Scanner com validação, interfaces
 *            funcionais (IntUnaryOperator) para trocar o teclado por uma função
 *            e detecção de respostas contraditórias.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntUnaryOperator;

public class AdivinheNumero {

    /** Quantas tentativas a busca binária precisa, no pior caso, para um intervalo de n números. */
    static int tentativasNoPiorCaso(int n) {
        return (int) Math.ceil(Math.log(n) / Math.log(2)) + 1;
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        var teclado = new Scanner(System.in, StandardCharsets.UTF_8);
        int modo = 0, maximo = 100, alvo = -1;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--demo" -> { demo(); return; }
                case "--modo" -> modo = inteiro(args, ++i, "--modo");
                case "--max" -> maximo = inteiro(args, ++i, "--max");
                case "--alvo" -> alvo = inteiro(args, ++i, "--alvo");
                default -> falhar("não conheço a opção '" + args[i] + "'");
            }
        }
        if (maximo < 2) falhar("--max precisa ser pelo menos 2; recebi " + maximo);
        if (alvo != -1 && (alvo < 1 || alvo > maximo)) falhar("--alvo precisa estar entre 1 e " + maximo + "; recebi " + alvo);
        if (modo == 0) {
            System.out.println("=== Adivinhe o número ===\n1) Eu adivinho o número do computador\n2) O computador adivinha o meu");
            System.out.print("Escolha: ");
            modo = teclado.hasNextLine() && teclado.nextLine().strip().equals("2") ? 2 : 1;
        }
        if (modo == 1) voceAdivinha(teclado, maximo, new Random());
        else if (modo == 2 && alvo != -1) computadorAdivinha(1, maximo, oraculoFixo(alvo));
        else if (modo == 2) computadorAdivinha(1, maximo, oraculoDoTeclado(teclado));
        else falhar("--modo precisa ser 1 ou 2; recebi " + modo);
    }

    /** Modo 1: o jogador tenta e o programa diz se o número secreto é maior ou menor. */
    static void voceAdivinha(Scanner teclado, int maximo, Random sorteio) {
        int secreto = sorteio.nextInt(maximo) + 1;      // nextInt(n) vai de 0 a n-1; somamos 1
        int tentativas = 0;
        System.out.printf("Pensei num número entre 1 e %d. Com busca binária você acerta em até %d tentativas.%n",
                maximo, tentativasNoPiorCaso(maximo));
        while (true) {
            System.out.print("Seu palpite: ");
            if (!teclado.hasNextLine()) { System.out.println("\nEntrada encerrada. O número era " + secreto + "."); return; }
            String texto = teclado.nextLine().strip();
            int palpite;
            try {
                palpite = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("'" + texto + "' não é um número inteiro. Tente de novo.");
                continue;
            }
            if (palpite < 1 || palpite > maximo) { System.out.println("Só vale de 1 a " + maximo + "."); continue; }
            tentativas++;
            if (palpite < secreto) System.out.println("O número é MAIOR que " + palpite + ".");
            else if (palpite > secreto) System.out.println("O número é MENOR que " + palpite + ".");
            else {
                System.out.println("Acertou! Era " + secreto + ", em " + tentativas + " tentativa(s).");
                return;
            }
        }
    }

    /**
     * Modo 2: busca binária. O oráculo recebe o palpite e devolve -1 (o segredo é menor),
     * 0 (acertou) ou 1 (o segredo é maior). Pode ser o teclado ou uma função de teste.
     */
    static void computadorAdivinha(int minimo, int maximo, IntUnaryOperator oraculo) {
        int baixo = minimo, alto = maximo, tentativas = 0;
        System.out.printf("Pense num número entre %d e %d. Vou acertar em no máximo %d tentativas.%n",
                minimo, maximo, tentativasNoPiorCaso(maximo - minimo + 1));
        while (baixo <= alto) {
            int palpite = baixo + (alto - baixo) / 2;   // o meio, sem risco de estourar o int
            tentativas++;
            System.out.printf("Tentativa %d: é %d? (intervalo possível: %d a %d)%n", tentativas, palpite, baixo, alto);
            int resposta = oraculo.applyAsInt(palpite);
            if (resposta == 0) {
                System.out.println("Acertei! Era " + palpite + ", em " + tentativas + " tentativa(s).");
                return;
            }
            // A resposta corta o intervalo pela metade: é isso que faz a busca ser logarítmica.
            if (resposta > 0) baixo = palpite + 1; else alto = palpite - 1;
        }
        System.out.println("Suas respostas se contradizem: nenhum número sobrou. Você mudou o número no meio?");
    }

    /** Oráculo que lê "maior", "menor" ou "certo" (ou só a primeira letra) do teclado. */
    static IntUnaryOperator oraculoDoTeclado(Scanner teclado) {
        return palpite -> {
            while (true) {
                System.out.print("  Responda [m]aior, [n]menor ou [c]erto: ");
                if (!teclado.hasNextLine()) { System.out.println("\nEntrada encerrada."); System.exit(0); }
                String r = teclado.nextLine().strip().toLowerCase();
                if (r.startsWith("ma")) return 1;
                if (r.startsWith("me") || r.equals("n")) return -1;
                if (r.startsWith("c")) return 0;
                System.out.println("  Não entendi. Digite maior, menor ou certo.");
            }
        };
    }

    /** Oráculo automático: compara com um número fixo e mostra a resposta que daria. */
    static IntUnaryOperator oraculoFixo(int alvo) {
        return palpite -> {
            int r = Integer.compare(alvo, palpite);
            System.out.println("  resposta: " + (r > 0 ? "maior" : r < 0 ? "menor" : "certo"));
            return r;
        };
    }

    static void demo() {
        System.out.println("=== Demonstração: o computador adivinha 73 entre 1 e 100 ===");
        computadorAdivinha(1, 100, oraculoFixo(73));
        System.out.println();
        System.out.println("Por que no máximo " + tentativasNoPiorCaso(100) + "? Cada resposta divide o intervalo ao meio:");
        for (int n = 100, passo = 1; n > 1; n = n / 2, passo++) System.out.println("  depois da tentativa " + passo + ": sobram no máximo " + (n / 2) + " números");
        System.out.println();
        System.out.println("E se o oráculo mentir? Diremos 'maior' para tudo:");
        computadorAdivinha(1, 8, palpite -> 1);
    }

    static int inteiro(String[] args, int i, String opcao) {
        if (i >= args.length) falhar(opcao + " precisa de um número");
        try {
            return Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            falhar(opcao + " precisa de um número inteiro; recebi '" + args[i] + "'");
            return 0;
        }
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo + ".");
        System.out.println("Uso: java AdivinheNumero.java [--modo 1|2] [--max N] [--alvo N] | --demo");
        System.exit(1);
    }
}

/*
 * Para ir além:
 *  1. No modo 1, conte quantas tentativas a mais que o mínimo teórico o jogador usou e dê uma nota.
 *  2. No modo 2, troque a busca binária por palpites aleatórios e compare a média de tentativas em 1000 jogos.
 *  3. Permita intervalos negativos (ex.: -50 a 50) e confira que o cálculo do meio continua certo.
 */
