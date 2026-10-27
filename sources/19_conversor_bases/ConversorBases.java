/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  19. Conversor de bases numéricas
 * ------------------------------------------------------------
 * O que faz: converte números entre binário, octal, base 10 e hexadecimal, ou
 *            qualquer base de 2 a 36, explicando o cálculo passo a passo
 *            (divisões sucessivas e soma de potências). Também codifica texto
 *            em binário (um byte por caractere, UTF-8) e decodifica de volta.
 * Como usar: java ConversorBases.java 255 10 2
 *            java ConversorBases.java ff 16 10 --passos
 *            java ConversorBases.java --texto "Olá"
 *            java ConversorBases.java --bits "01001111 01101100 11000011 10100001"
 *            java ConversorBases.java --demo
 * O que você aprende: Long.parseLong e Long.toString com raiz, aritmética de
 *            divisão e resto, StringBuilder.insert, bytes em UTF-8,
 *            String.format com preenchimento de zeros, switch expression
 *            e validação de intervalo com mensagens claras.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConversorBases {

    /** Os dígitos de qualquer base até 36: depois do 9 entram as letras, como no hexadecimal. */
    static final String DIGITOS = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        try {
            switch (args[0]) {
                case "--demo" -> demo();
                case "--texto" -> {
                    if (args.length < 2) throw new IllegalArgumentException("--texto precisa do texto a codificar.");
                    System.out.println(textoParaBinario(args[1]));
                }
                case "--bits" -> {
                    if (args.length < 2) throw new IllegalArgumentException("--bits precisa dos bytes em binário, entre aspas.");
                    System.out.println(binarioParaTexto(args[1]));
                }
                default -> converterPorArgumentos(args);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
            System.exit(1);
        }
    }

    static void converterPorArgumentos(String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("informe <número> <base de origem> <base de destino>.");
        int origem = lerBase(args[1]);
        int destino = lerBase(args[2]);
        long valor = lerNumero(args[0], origem);
        boolean passos = args.length > 3 && args[3].equals("--passos");
        if (passos) {
            explicarParaBase10(args[0], origem);
            explicarDeBase10(valor, destino);
        }
        System.out.println(args[0].toLowerCase(Locale.ROOT) + " (base " + origem + ") = " + Long.toString(valor, destino) + " (base " + destino + ")");
    }

    static int lerBase(String texto) {
        int base;
        try {
            base = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("a base precisa ser um inteiro, recebi '" + texto + "'.");
        }
        if (base < 2 || base > 36) throw new IllegalArgumentException("a base deve ficar entre 2 e 36, recebi " + base + ".");
        return base;
    }

    /** Long.parseLong já entende qualquer base; só traduzimos o erro para algo legível. */
    static long lerNumero(String texto, int base) {
        try {
            long valor = Long.parseLong(texto.toLowerCase(Locale.ROOT), base);
            if (valor < 0) throw new IllegalArgumentException("só números não negativos, recebi " + texto + ".");
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + texto + "' não é um número válido na base " + base + ".");
        }
    }

    /** Conversão manual para base 10: cada dígito vale dígito x base elevado à sua posição. */
    static void explicarParaBase10(String numero, int base) {
        String texto = numero.toLowerCase(Locale.ROOT);
        System.out.println("Passo 1: de " + texto + " (base " + base + ") para base 10, somando potências");
        List<String> parcelas = new ArrayList<>();
        long total = 0;
        int n = texto.length();
        for (int i = 0; i < n; i++) {
            int digito = DIGITOS.indexOf(texto.charAt(i));
            int expoente = n - 1 - i; // o dígito mais à esquerda tem o maior expoente
            long parcela = digito * (long) Math.pow(base, expoente);
            parcelas.add(digito + "x" + base + "^" + expoente);
            total += parcela;
        }
        System.out.println("  " + String.join(" + ", parcelas) + " = " + total);
    }

    /** Conversão manual de base 10: divide pela base e guarda os restos, lidos de baixo para cima. */
    static void explicarDeBase10(long valor, int base) {
        System.out.println("Passo 2: de " + valor + " (base 10) para base " + base + ", por divisões sucessivas");
        if (valor == 0) {
            System.out.println("  0 é 0 em qualquer base.");
            return;
        }
        StringBuilder resultado = new StringBuilder();
        long resto = valor;
        while (resto > 0) {
            int digito = (int) (resto % base);
            System.out.printf("  %d / %d = %d, resto %d -> dígito '%c'%n", resto, base, resto / base, digito, DIGITOS.charAt(digito));
            resultado.insert(0, DIGITOS.charAt(digito)); // insert(0) monta o número da direita para a esquerda
            resto /= base;
        }
        System.out.println("  Lendo os restos do último para o primeiro: " + resultado);
    }

    /** Cada caractere vira seus bytes UTF-8, cada byte vira 8 bits com zeros à esquerda. */
    static String textoParaBinario(String texto) {
        List<String> bytes = new ArrayList<>();
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            // "& 0xFF" tira o sinal: em Java byte vai de -128 a 127, e queremos 0 a 255.
            String bits = Integer.toBinaryString(b & 0xFF);
            bytes.add(String.format("%8s", bits).replace(' ', '0'));
        }
        return String.join(" ", bytes);
    }

    static String binarioParaTexto(String binario) {
        String limpo = binario.replaceAll("\\s+", "");
        if (limpo.isEmpty() || !limpo.matches("[01]+")) throw new IllegalArgumentException("o binário só pode ter 0, 1 e espaços.");
        if (limpo.length() % 8 != 0) throw new IllegalArgumentException("o binário precisa ter grupos de 8 bits, tem " + limpo.length() + ".");
        byte[] bytes = new byte[limpo.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(limpo.substring(i * 8, i * 8 + 8), 2);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static void linhaDaTabela(long valor) {
        System.out.printf("  %-8d %-22s %-10s %-10s %s%n", valor, Long.toString(valor, 2), Long.toString(valor, 8),
                Long.toString(valor, 16), Long.toString(valor, 36));
    }

    static void demo() {
        System.out.println("=== Um mesmo número em várias bases ===");
        System.out.printf("  %-8s %-22s %-10s %-10s %s%n", "base 10", "binário", "octal", "hex", "base 36");
        for (long v : List.of(0L, 7L, 42L, 255L, 1024L, 65535L, 1_000_000L)) linhaDaTabela(v);
        System.out.println();
        System.out.println("=== Conversões entre bases diferentes de 10 ===");
        String[][] casos = {{"ff", "16", "2"}, {"777", "8", "16"}, {"101010", "2", "10"}, {"zz", "36", "10"}, {"1010", "2", "8"}};
        for (String[] c : casos) {
            int origem = Integer.parseInt(c[1]);
            int destino = Integer.parseInt(c[2]);
            long valor = lerNumero(c[0], origem);
            System.out.printf("  %-8s base %-2d -> %-10s base %d%n", c[0], origem, Long.toString(valor, destino), destino);
        }
        System.out.println();
        System.out.println("=== O cálculo passo a passo de 2f (hexadecimal) para binário ===");
        explicarParaBase10("2f", 16);
        explicarDeBase10(47, 2);
        System.out.println();
        System.out.println("=== Texto em binário e de volta ===");
        String palavra = "Olá";
        String bits = textoParaBinario(palavra);
        System.out.println("  \"" + palavra + "\" -> " + bits);
        System.out.println("  Note: o \"á\" ocupa dois bytes em UTF-8, por isso são quatro grupos para três letras.");
        System.out.println("  de volta -> \"" + binarioParaTexto(bits) + "\"");
        System.out.println();
        System.out.println("=== Uma entrada inválida ===");
        try {
            lerNumero("129", 8);
        } catch (IllegalArgumentException e) {
            System.out.println("  129 na base 8: " + e.getMessage());
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java ConversorBases.java <número> <base de origem> <base de destino> [--passos]
                 java ConversorBases.java --texto <texto>
                 java ConversorBases.java --bits "<bytes em binário separados por espaço>"
                 java ConversorBases.java --demo
              Bases de 2 a 36. Dígitos acima de 9 são letras (a=10 ... z=35), maiúsculas ou minúsculas.
            Exemplos:
              java ConversorBases.java 255 10 2
              java ConversorBases.java ff 16 10 --passos
              java ConversorBases.java --texto "Olá"
              java ConversorBases.java --bits "01001111 01101100 11000011 10100001\"""");
    }
}

/*
 * Para ir além:
 *  1. Aceite números negativos e mostre a representação em complemento de dois com 8, 16 e 32 bits.
 *  2. Converta números com parte fracionária (10,75 em base 10 é 1010,11 em binário).
 *  3. Use BigInteger no lugar de long para converter números com centenas de dígitos.
 */
