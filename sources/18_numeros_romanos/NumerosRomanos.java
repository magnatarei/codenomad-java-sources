/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  18. Conversor de números romanos
 * ------------------------------------------------------------
 * O que faz: converte um inteiro de 1 a 3999 para algarismos romanos e faz o
 *            caminho de volta, rejeitando romanos mal formados (IIII, VX, IC,
 *            MMMM) com uma explicação do erro. Inclui uma tabela de referência
 *            com os símbolos e as subtrações permitidas.
 * Como usar: java NumerosRomanos.java 1994 2024 49
 *            java NumerosRomanos.java MCMXCIV xlii
 *            java NumerosRomanos.java --tabela
 *            java NumerosRomanos.java --demo
 * O que você aprende: algoritmo guloso com arrays paralelos, detecção de tipo
 *            de entrada com regex, validação por reconstrução (converte e
 *            compara), record, Map.of, switch expression e código de saída.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NumerosRomanos {

    /**
     * Os valores em ordem decrescente, já com as seis subtrações válidas (CM, CD,
     * XC, XL, IX, IV). Com essa lista o algoritmo guloso nunca erra.
     */
    static final int[] VALORES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    static final String[] SIMBOLOS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    static final Map<Character, Integer> VALOR_DO_SIMBOLO = Map.of(
            'I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);

    /** O resultado de uma conversão em qualquer sentido, com o motivo quando falha. */
    record Conversao(String entrada, String saida, boolean ok, String motivo) { }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        switch (args[0]) {
            case "--demo" -> demo();
            case "--tabela" -> tabela();
            default -> {
                boolean todosOk = true;
                for (String arg : args) {
                    Conversao c = converter(arg);
                    imprimir(c);
                    todosOk &= c.ok();
                }
                System.exit(todosOk ? 0 : 1); // scripts conseguem saber se alguma entrada falhou
            }
        }
    }

    /** Decide o sentido pela cara da entrada: só dígitos é inteiro, só letras romanas é romano. */
    static Conversao converter(String entrada) {
        String limpa = entrada.trim();
        if (limpa.matches("-?\\d+")) {
            try {
                int n = Integer.parseInt(limpa);
                return new Conversao(entrada, paraRomano(n), true, "");
            } catch (IllegalArgumentException e) {
                return new Conversao(entrada, "", false, e.getMessage());
            }
        }
        if (limpa.matches("(?i)[IVXLCDM]+")) {
            try {
                return new Conversao(entrada, String.valueOf(paraInteiro(limpa)), true, "");
            } catch (IllegalArgumentException e) {
                return new Conversao(entrada, "", false, e.getMessage());
            }
        }
        return new Conversao(entrada, "", false, "não é inteiro nem romano (só I, V, X, L, C, D, M).");
    }

    /**
     * Algoritmo guloso: enquanto o número couber no maior valor da tabela,
     * escreve o símbolo e subtrai. Como as subtrações já estão na tabela,
     * 4 vira IV e não IIII.
     */
    static String paraRomano(int numero) {
        if (numero < 1 || numero > 3999) {
            throw new IllegalArgumentException("o inteiro deve ficar entre 1 e 3999, recebi " + numero + ".");
        }
        StringBuilder romano = new StringBuilder();
        int resto = numero;
        for (int i = 0; i < VALORES.length; i++) {
            while (resto >= VALORES[i]) {
                romano.append(SIMBOLOS[i]);
                resto -= VALORES[i];
            }
        }
        return romano.toString();
    }

    /**
     * Soma os símbolos da esquerda para a direita; quando um símbolo vale menos
     * que o seguinte (I antes de V), ele é subtraído. Depois de somar, a forma
     * canônica é reconstruída e comparada: se for diferente, o romano era mal
     * formado (IIII soma 4, mas 4 se escreve IV).
     */
    static int paraInteiro(String romano) {
        String texto = romano.toUpperCase(Locale.ROOT);
        int total = 0;
        for (int i = 0; i < texto.length(); i++) {
            int atual = VALOR_DO_SIMBOLO.get(texto.charAt(i));
            int proximo = i + 1 < texto.length() ? VALOR_DO_SIMBOLO.get(texto.charAt(i + 1)) : 0;
            total += atual < proximo ? -atual : atual;
        }
        if (total < 1 || total > 3999) {
            throw new IllegalArgumentException("'" + romano + "' vale " + total + ", fora da faixa de 1 a 3999.");
        }
        String canonico = paraRomano(total);
        if (!canonico.equals(texto)) {
            throw new IllegalArgumentException("'" + romano + "' é mal formado: " + total + " se escreve " + canonico + ".");
        }
        return total;
    }

    static void imprimir(Conversao c) {
        if (c.ok()) {
            System.out.printf("%-10s -> %s%n", c.entrada(), c.saida());
        } else {
            System.out.printf("%-10s -> erro: %s%n", c.entrada(), c.motivo());
        }
    }

    static void tabela() {
        System.out.println("=== Símbolos ===");
        System.out.println("  I = 1    V = 5    X = 10    L = 50    C = 100    D = 500    M = 1000");
        System.out.println();
        System.out.println("=== Regras ===");
        System.out.println("  1. Símbolos em ordem decrescente se somam: VI = 6, XV = 15, MDC = 1600.");
        System.out.println("  2. Um símbolo menor antes de um maior é subtraído, mas só nestes seis pares:");
        System.out.println("     IV = 4    IX = 9    XL = 40    XC = 90    CD = 400    CM = 900");
        System.out.println("  3. I, X, C e M repetem no máximo três vezes seguidas; V, L e D nunca repetem.");
        System.out.println("  4. O maior número representável é MMMCMXCIX = 3999.");
        System.out.println();
        System.out.println("=== Referência rápida ===");
        int[] exemplos = {1, 4, 5, 9, 10, 40, 50, 90, 100, 400, 500, 900, 1000, 1500, 2000, 2500, 3000, 3999};
        for (int i = 0; i < exemplos.length; i++) {
            System.out.printf("  %4d = %-10s", exemplos[i], paraRomano(exemplos[i]));
            if (i % 3 == 2) System.out.println();
        }
        System.out.println();
    }

    static void demo() {
        System.out.println("=== Inteiro para romano ===");
        for (int n : List.of(1, 4, 9, 14, 40, 90, 400, 1994, 2024, 3999)) {
            imprimir(converter(String.valueOf(n)));
        }
        System.out.println();
        System.out.println("=== Romano para inteiro (maiúsculas ou minúsculas) ===");
        for (String r : List.of("III", "IV", "IX", "LVIII", "MCMXCIV", "mmxxiv", "MMMCMXCIX")) {
            imprimir(converter(r));
        }
        System.out.println();
        System.out.println("=== Entradas rejeitadas, cada uma com o motivo ===");
        for (String ruim : List.of("IIII", "VX", "IC", "MMMM", "XXXX", "0", "4000", "ABC")) {
            imprimir(converter(ruim));
        }
        System.out.println();
        System.out.println("=== Ida e volta: todo número de 1 a 3999 sobrevive à conversão dupla? ===");
        int falhas = 0;
        for (int n = 1; n <= 3999; n++) {
            if (paraInteiro(paraRomano(n)) != n) falhas++;
        }
        System.out.println("Testados 3999 números, falhas: " + falhas + (falhas == 0 ? " (tudo certo)" : ""));
        System.out.println();
        tabela();
    }

    static void uso() {
        System.out.println("""
            Uso: java NumerosRomanos.java <inteiro ou romano> [mais valores ...]
                 java NumerosRomanos.java --tabela
                 java NumerosRomanos.java --demo
              Inteiros de 1 a 3999 viram romanos; romanos (I V X L C D M) viram inteiros.
            Exemplos:
              java NumerosRomanos.java 1994 2024 49
              java NumerosRomanos.java MCMXCIV xlii""");
    }
}

/*
 * Para ir além:
 *  1. Aceite números acima de 3999 usando a barra superior (V com traço = 5000), representada por um prefixo.
 *  2. Mostre o passo a passo da soma: "MCMXCIV = 1000 + (1000 - 100) + (100 - 10) + (5 - 1)".
 *  3. Escreva a conversão para romano com Map.entry e um laço sobre uma lista de pares, sem os dois arrays paralelos.
 */
