/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  46. Jogo da forca
 * ------------------------------------------------------------
 * O que faz: sorteia uma palavra de um banco com dicas, desenha a forca em
 *            texto a cada erro, mostra as letras já tentadas e decide vitória
 *            ou derrota. Aceita acentos (digitar "a" acerta o "á") e permite
 *            arriscar a palavra inteira. Você também pode fornecer a palavra.
 * Como usar: java JogoForca.java                       (interativo, palavra sorteada)
 *            java JogoForca.java --palavra compilador --dica "traduz código"
 *            java JogoForca.java --semente 7           (sorteio reproduzível)
 *            java JogoForca.java --demo
 * O que você aprende: Scanner no terminal, Set para letras tentadas, Random
 *            com semente, Normalizer para tirar acentos, text blocks para o
 *            desenho, records, enum como resultado de uma jogada e separação
 *            entre regras do jogo e entrada do usuário.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class JogoForca {

    record Palavra(String texto, String dica) {}

    enum Resultado { ACERTO, ERRO, REPETIDA, VENCEU, PERDEU }

    static final List<Palavra> BANCO = List.of(
            new Palavra("compilador", "traduz o código-fonte para a máquina"),
            new Palavra("recursão", "a função que chama a si mesma"),
            new Palavra("variável", "guarda um valor que pode mudar"),
            new Palavra("polimorfismo", "um mesmo método, vários comportamentos"),
            new Palavra("algoritmo", "receita de passos para resolver um problema"),
            new Palavra("depuração", "a caça aos erros do programa"),
            new Palavra("interface", "contrato que a classe promete cumprir"),
            new Palavra("exceção", "o que a JVM lança quando algo dá errado"),
            new Palavra("javascript", "a linguagem dos navegadores"),
            new Palavra("ponteiro", "em C, guarda um endereço de memória"));

    static final int MAX_ERROS = 6;

    /** As regras do jogo, sem nada de terminal: dá para testar e reaproveitar. */
    static final class Partida {
        final Palavra palavra;
        final String simples;                               // a palavra sem acentos, para comparar
        final Set<Character> tentadas = new LinkedHashSet<>();   // LinkedHashSet mantém a ordem de tentativa
        int erros;

        Partida(Palavra palavra) {
            this.palavra = palavra;
            this.simples = semAcento(palavra.texto());
        }

        Resultado tentar(char letra) {
            char l = semAcento(String.valueOf(letra)).charAt(0);
            if (!tentadas.add(l)) return Resultado.REPETIDA;
            if (simples.indexOf(l) < 0) {
                erros++;
                return erros >= MAX_ERROS ? Resultado.PERDEU : Resultado.ERRO;
            }
            return venceu() ? Resultado.VENCEU : Resultado.ACERTO;
        }

        /** Arriscar a palavra inteira: errar custa dois erros de uma vez. */
        Resultado arriscar(String palpite) {
            if (semAcento(palpite).equals(simples)) {
                for (char c : simples.toCharArray()) tentadas.add(c);
                return Resultado.VENCEU;
            }
            erros = Math.min(MAX_ERROS, erros + 2);
            return erros >= MAX_ERROS ? Resultado.PERDEU : Resultado.ERRO;
        }

        boolean venceu() {
            for (char c : simples.toCharArray()) if (!tentadas.contains(c)) return false;
            return true;
        }

        /** "c _ m p _ l a d _ r": mostra a letra original (com acento) quando a versão simples foi acertada. */
        String mascarada() {
            var sb = new StringBuilder();
            for (int i = 0; i < simples.length(); i++) {
                sb.append(tentadas.contains(simples.charAt(i)) ? palavra.texto().charAt(i) : '_').append(' ');
            }
            return sb.toString().strip();
        }

        String desenho() {
            String cabeca = erros >= 1 ? "O" : " ";
            String tronco = erros >= 2 ? "|" : " ";
            String esq = erros >= 3 ? "/" : " ", dir = erros >= 4 ? "\\" : " ";
            String pernaE = erros >= 5 ? "/" : " ", pernaD = erros >= 6 ? "\\" : " ";
            // Text block: o desenho fica legível no código e o formatted preenche cada parte.
            return """
                      +---+
                      |   %s
                      |  %s%s%s
                      |  %s %s
                    =====  erros: %d/%d""".formatted(cabeca, esq, tronco, dir, pernaE, pernaD, erros, MAX_ERROS);
        }
    }

    /** "variável" vira "variavel": o Normalizer separa a letra do acento e a regex apaga o acento. */
    static String semAcento(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Palavra escolhida;
        Random sorteio = new Random();
        String palavra = null, dica = "sem dica";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--demo" -> { demo(); return; }
                case "--semente" -> sorteio = new Random(inteiro(args, ++i, "--semente"));
                case "--palavra" -> palavra = i + 1 < args.length ? args[++i] : falhar("--palavra precisa de uma palavra");
                case "--dica" -> dica = i + 1 < args.length ? args[++i] : falhar("--dica precisa de um texto");
                default -> falhar("não conheço a opção '" + args[i] + "'");
            }
        }
        if (palavra != null) {
            if (!semAcento(palavra).matches("[a-z]{3,}")) falhar("a palavra precisa ter só letras e pelo menos 3 delas; recebi '" + palavra + "'");
            escolhida = new Palavra(palavra.toLowerCase(), dica);
        } else {
            escolhida = BANCO.get(sorteio.nextInt(BANCO.size()));
        }
        jogarInterativo(new Partida(escolhida));
    }

    static void jogarInterativo(Partida p) {
        var teclado = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("=== Jogo da forca ===  Dica: " + p.palavra.dica());
        System.out.println("Digite uma letra, ou a palavra inteira para arriscar. Você pode errar " + MAX_ERROS + " vezes.");
        while (true) {
            System.out.println(p.desenho());
            System.out.println("Palavra: " + p.mascarada() + "   tentadas: " + p.tentadas);
            System.out.print("Sua jogada: ");
            if (!teclado.hasNextLine()) { System.out.println("\nEntrada encerrada. Até a próxima!"); return; }
            String jogada = teclado.nextLine().strip().toLowerCase();
            if (jogada.isEmpty()) continue;
            Resultado r = jogada.length() == 1 ? p.tentar(jogada.charAt(0)) : p.arriscar(jogada);
            if (anunciar(p, r)) return;
        }
    }

    /** Imprime o resultado de uma jogada e devolve true quando a partida acabou. */
    static boolean anunciar(Partida p, Resultado r) {
        switch (r) {
            case ACERTO -> System.out.println("Boa! Essa letra está na palavra.");
            case ERRO -> System.out.println("Não tem. Mais uma parte do boneco...");
            case REPETIDA -> System.out.println("Você já tentou essa letra.");
            case VENCEU -> {
                System.out.println("Você venceu! A palavra era \"" + p.palavra.texto() + "\" com " + p.erros + " erro(s).");
                return true;
            }
            case PERDEU -> {
                System.out.println(p.desenho());
                System.out.println("Enforcado! A palavra era \"" + p.palavra.texto() + "\".");
                return true;
            }
        }
        return false;
    }

    /** Um jogador automático que tenta as letras na ordem de frequência do português. */
    static void demo() {
        System.out.println("=== Demonstração: jogador automático ===");
        var p = new Partida(BANCO.get(0));                  // compilador: dá para ver erros e a vitória
        System.out.println("Dica: " + p.palavra.dica() + "  (" + p.simples.length() + " letras)");
        String ordem = "aeiousrndmtclpgvfbhqjzxkwy";       // vogais primeiro, depois as consoantes mais comuns
        for (char letra : ordem.toCharArray()) {
            Resultado r = p.tentar(letra);
            System.out.println();
            System.out.println("Tentativa '" + letra + "': " + r + "  ->  " + p.mascarada());
            if (r == Resultado.ERRO) System.out.println(p.desenho());
            if (anunciar(p, r)) break;
        }
        System.out.println("Letras tentadas, na ordem: " + p.tentadas);
    }

    static int inteiro(String[] args, int i, String opcao) {
        if (i >= args.length) falhar(opcao + " precisa de um número");
        try {
            return Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            return Integer.parseInt(falhar(opcao + " precisa de um número inteiro; recebi '" + args[i] + "'"));
        }
    }

    /** Devolve String só para poder ser usado em expressões; o programa termina aqui. */
    static String falhar(String motivo) {
        System.out.println("Erro: " + motivo + ".");
        System.out.println("Uso: java JogoForca.java [--palavra X --dica Y] [--semente N] | --demo");
        System.exit(1);
        return "";
    }
}

/*
 * Para ir além:
 *  1. Leia o banco de palavras de um arquivo de texto (uma linha por palavra, separada da dica por ;).
 *  2. Adicione níveis: no fácil, revele as vogais de graça antes de começar.
 *  3. Faça o jogador automático da demo usar a frequência real das letras do próprio banco.
 */
