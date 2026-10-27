/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  15. Cifra de César e de Vigenère
 * ------------------------------------------------------------
 * O que faz: codifica e decodifica texto com a cifra de César (um deslocamento
 *            fixo) e com a de Vigenère (uma palavra-chave que muda o deslocamento
 *            a cada letra). Maiúsculas, espaços e pontuação são preservados.
 *            Inclui um quebrador de César que descobre a chave pela frequência
 *            das letras do português, sem conhecer a chave.
 * Como usar: java CifraCesarVigenere.java cesar codificar 3 "Ataque ao amanhecer"
 *            java CifraCesarVigenere.java vigenere decodificar LIMAO "Lbmqip ia aalvteqpz"
 *            java CifraCesarVigenere.java quebrar "Dwdtxh dr dpdqkhfhu"
 *            java CifraCesarVigenere.java --demo
 * O que você aprende: aritmética de char (letra - 'A'), módulo que nunca fica
 *            negativo, StringBuilder, enum com switch, arrays de double como
 *            tabela de frequência, Normalizer para tirar acentos e argumentos
 *            posicionais com validação.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public class CifraCesarVigenere {

    /** Frequência aproximada de cada letra no português (A..Z), em porcentagem. */
    static final double[] FREQUENCIA_PT = {
            14.63, 1.04, 3.88, 4.99, 12.57, 1.02, 1.30, 1.28, 6.18, 0.40, 0.02, 2.78, 4.74,
            5.05, 10.73, 2.52, 1.20, 6.53, 7.81, 4.34, 4.63, 1.67, 0.01, 0.21, 0.01, 0.47};

    enum Metodo { CESAR, VIGENERE }
    enum Operacao { CODIFICAR, DECODIFICAR }

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
        if (args[0].equalsIgnoreCase("quebrar")) {
            if (args.length < 2) throw new IllegalArgumentException("informe o texto cifrado para quebrar.");
            quebrarCesar(juntar(args, 1));
            return;
        }
        Metodo metodo = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "cesar" -> Metodo.CESAR;
            case "vigenere" -> Metodo.VIGENERE;
            default -> throw new IllegalArgumentException("método desconhecido '" + args[0] + "', use cesar, vigenere ou quebrar.");
        };
        if (args.length < 4) throw new IllegalArgumentException("faltou argumento: <método> <codificar|decodificar> <chave> <texto>.");
        Operacao operacao = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "codificar" -> Operacao.CODIFICAR;
            case "decodificar" -> Operacao.DECODIFICAR;
            default -> throw new IllegalArgumentException("operação desconhecida '" + args[1] + "', use codificar ou decodificar.");
        };
        String texto = juntar(args, 3);
        String resultado = metodo == Metodo.CESAR
                ? cesar(texto, lerDeslocamento(args[2]), operacao)
                : vigenere(texto, validarChave(args[2]), operacao);
        System.out.println(resultado);
    }

    static String juntar(String[] args, int desde) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, desde, args.length));
    }

    static int lerDeslocamento(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("a chave de César precisa ser um inteiro, recebi '" + valor + "'.");
        }
    }

    static String validarChave(String chave) {
        if (chave.isEmpty() || chave.chars().anyMatch(c -> !Character.isLetter(c))) {
            throw new IllegalArgumentException("a chave de Vigenère precisa ter só letras, recebi '" + chave + "'.");
        }
        return semAcento(chave).toUpperCase(Locale.ROOT);
    }

    /** "coração" vira "coracao": a cifra só conhece as 26 letras de A a Z. */
    static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Desloca uma letra dentro do alfabeto, preservando a caixa. O "+ 26" antes do
     * módulo evita resultado negativo quando o deslocamento é para trás.
     */
    static char deslocar(char letra, int deslocamento) {
        if (letra >= 'A' && letra <= 'Z') return (char) ('A' + ((letra - 'A' + deslocamento) % 26 + 26) % 26);
        if (letra >= 'a' && letra <= 'z') return (char) ('a' + ((letra - 'a' + deslocamento) % 26 + 26) % 26);
        return letra; // espaço, dígito e pontuação passam intactos
    }

    static String cesar(String texto, int chave, Operacao operacao) {
        int deslocamento = operacao == Operacao.CODIFICAR ? chave : -chave;
        StringBuilder saida = new StringBuilder();
        for (char c : semAcento(texto).toCharArray()) saida.append(deslocar(c, deslocamento));
        return saida.toString();
    }

    /** Cada letra do texto usa a letra seguinte da chave como deslocamento (A=0, B=1...). */
    static String vigenere(String texto, String chave, Operacao operacao) {
        StringBuilder saida = new StringBuilder();
        int posicaoChave = 0;
        for (char c : semAcento(texto).toCharArray()) {
            if (!Character.isLetter(c)) {
                saida.append(c); // a chave só avança nas letras, por isso espaços não a consomem
                continue;
            }
            int deslocamento = chave.charAt(posicaoChave % chave.length()) - 'A';
            if (operacao == Operacao.DECODIFICAR) deslocamento = -deslocamento;
            saida.append(deslocar(c, deslocamento));
            posicaoChave++;
        }
        return saida.toString();
    }

    /**
     * Pontua uma chave candidata: quanto mais a distribuição das letras decifradas
     * se parece com a do português, menor o erro (soma dos quadrados das diferenças).
     */
    static double erroDeFrequencia(String texto) {
        int[] contagem = new int[26];
        int total = 0;
        for (char c : texto.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                contagem[c - 'A']++;
                total++;
            }
        }
        if (total == 0) return Double.MAX_VALUE;
        double erro = 0;
        for (int i = 0; i < 26; i++) {
            double observada = 100.0 * contagem[i] / total;
            erro += Math.pow(observada - FREQUENCIA_PT[i], 2);
        }
        return erro;
    }

    /** Testa as 26 chaves possíveis e escolhe a que produz o texto mais parecido com português. */
    static void quebrarCesar(String cifrado) {
        int melhorChave = 0;
        double menorErro = Double.MAX_VALUE;
        for (int chave = 0; chave < 26; chave++) {
            double erro = erroDeFrequencia(cesar(cifrado, chave, Operacao.DECODIFICAR));
            if (erro < menorErro) {
                menorErro = erro;
                melhorChave = chave;
            }
        }
        System.out.println("Chave mais provável: " + melhorChave + " (erro de frequência " + String.format("%.1f", menorErro) + ")");
        System.out.println("Texto decifrado:     " + cesar(cifrado, melhorChave, Operacao.DECODIFICAR));
        System.out.println("Outras tentativas próximas:");
        int concorrentes = 0;
        for (int chave = 0; chave < 26; chave++) {
            if (chave == melhorChave) continue;
            double erro = erroDeFrequencia(cesar(cifrado, chave, Operacao.DECODIFICAR));
            if (erro < menorErro * 3) {
                System.out.printf("  chave %2d (erro %6.1f): %s%n", chave, erro, cesar(cifrado, chave, Operacao.DECODIFICAR));
                concorrentes++;
            }
        }
        if (concorrentes == 0) System.out.println("  (nenhuma: quanto mais longo o texto, mais isolada fica a chave certa)");
    }

    static void demo() {
        String frase = "A Academia abre às 8h: traga café, código e boa vontade!";
        System.out.println("=== Demonstração: cifrando e decifrando uma frase ===");
        System.out.println("Original:              " + frase);
        System.out.println();
        String cesarCifrado = cesar(frase, 3, Operacao.CODIFICAR);
        System.out.println("César, chave 3:        " + cesarCifrado);
        System.out.println("César, decifrado:      " + cesar(cesarCifrado, 3, Operacao.DECODIFICAR));
        System.out.println("César, chave -3:       " + cesar(frase, -3, Operacao.CODIFICAR) + "  (deslocar para trás também funciona)");
        System.out.println();
        String chave = "LIMAO";
        String vigenereCifrado = vigenere(frase, chave, Operacao.CODIFICAR);
        System.out.println("Vigenère, chave LIMAO: " + vigenereCifrado);
        System.out.println("Vigenère, decifrado:   " + vigenere(vigenereCifrado, chave, Operacao.DECODIFICAR));
        System.out.println("Note que os acentos somem: a cifra trabalha só com as 26 letras de A a Z.");
        System.out.println();
        System.out.println("=== Quebrando um texto de César sem saber a chave ===");
        String segredo = "Quem estuda um pouco todo dia aprende mais do que quem estuda muito de vez em quando. "
                + "A constância vence o talento quando o talento não pratica.";
        String cifrado = cesar(segredo, 17, Operacao.CODIFICAR);
        System.out.println("Cifrado (chave secreta): " + cifrado);
        System.out.println();
        quebrarCesar(cifrado);
        System.out.println();
        System.out.println("=== Tabela de Vigenère para a palavra CASA com a chave LIMAO ===");
        System.out.println("  letra  chave  deslocamento  resultado");
        for (int i = 0; i < 4; i++) {
            char letra = "CASA".charAt(i);
            char k = chave.charAt(i);
            System.out.printf("  %-6c %-6c %-13d %c%n", letra, k, k - 'A', deslocar(letra, k - 'A'));
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CifraCesarVigenere.java cesar <codificar|decodificar> <deslocamento> <texto>
                 java CifraCesarVigenere.java vigenere <codificar|decodificar> <palavra-chave> <texto>
                 java CifraCesarVigenere.java quebrar <texto cifrado com César>
                 java CifraCesarVigenere.java --demo
            Exemplos:
              java CifraCesarVigenere.java cesar codificar 3 "Ataque ao amanhecer"
              java CifraCesarVigenere.java vigenere decodificar LIMAO "Lbmqip ia aalvteqpz"
              java CifraCesarVigenere.java quebrar "Dwdtxh dr dpdqkhfhu"
            O quebrador funciona melhor com textos de pelo menos 50 letras.""");
    }
}

/*
 * Para ir além:
 *  1. Implemente a cifra ROT13 como um caso especial e mostre que codificar duas vezes devolve o original.
 *  2. Quebre Vigenère: descubra o tamanho da chave pelo índice de coincidência e aplique o quebrador de César em cada coluna.
 *  3. Leia o texto de um arquivo e grave o resultado em outro, preservando as quebras de linha.
 */
