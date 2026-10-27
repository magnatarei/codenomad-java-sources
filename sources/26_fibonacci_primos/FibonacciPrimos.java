/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  26. Fibonacci, fatorial e primos
 * ------------------------------------------------------------
 * O que faz: calcula Fibonacci de três jeitos (recursivo puro, recursivo com
 *            memória e iterativo) comparando chamadas e tempos, fatorial com
 *            long e com BigInteger, teste de primalidade e o crivo de
 *            Eratóstenes até N, explicando por que a recursão pura explode.
 * Como usar: java FibonacciPrimos.java fibonacci 30
 *            java FibonacciPrimos.java fatorial 25
 *            java FibonacciPrimos.java primo 7919
 *            java FibonacciPrimos.java primos 100
 *            java FibonacciPrimos.java --demo
 * O que você aprende: recursão e caso base, memoização com array, laços,
 *            estouro do long com Math.multiplyExact, BigInteger, boolean[]
 *            como tabela de marcação, parar na raiz quadrada e medir tempo
 *            com System.nanoTime.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FibonacciPrimos {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final int LIMITE_RECURSAO_PURA = 35;   // acima disto a versão pura leva minutos: fib(40) faz 330 milhões de chamadas
    static long chamadas;   // contador global: cada função recursiva soma 1 ao entrar

    /** Recursão pura: segue a definição ao pé da letra. Cada chamada gera duas, e o trabalho quase dobra a cada n. */
    static long fibRecursivo(int n) {
        chamadas++;
        if (n < 2) return n;   // caso base: fib(0) = 0 e fib(1) = 1; sem ele a recursão nunca para
        return fibRecursivo(n - 1) + fibRecursivo(n - 2);
    }

    /** Com memória (memoização): o resultado de cada n é guardado; na segunda vez que é pedido, sai da tabela. */
    static long fibMemoria(int n, long[] memoria) {
        chamadas++;
        if (n < 2) return n;
        if (memoria[n] != 0) return memoria[n];   // zero significa "ainda não calculado", pois fib(n) > 0 para n >= 1
        return memoria[n] = fibMemoria(n - 1, memoria) + fibMemoria(n - 2, memoria);
    }

    /** Iterativo: só precisa guardar os dois últimos valores. Sem pilha de chamadas, sem tabela. */
    static long fibIterativo(int n) {
        long anterior = 0, atual = 1;
        for (int i = 0; i < n; i++) {
            long proximo = anterior + atual;
            anterior = atual;
            atual = proximo;
        }
        return anterior;
    }

    static void fibonacci(int n) {
        if (n < 0 || n > 92) throw new IllegalArgumentException("Fibonacci aqui vai de 0 a 92 (fib(93) não cabe num long), recebi " + n + ".");
        System.out.println("fib(" + n + ") pelos três caminhos:");
        System.out.printf("   %-22s | %22s | %14s | %10s%n", "Método", "Resultado", "Chamadas", "Tempo");
        System.out.println("   " + "-".repeat(76));
        if (n <= LIMITE_RECURSAO_PURA) {
            chamadas = 0;
            long inicio = System.nanoTime();
            long r = fibRecursivo(n);
            linha("recursivo puro", r, chamadas, System.nanoTime() - inicio);
        } else {
            System.out.printf("   %-22s | %s%n", "recursivo puro", "pulado: acima de " + LIMITE_RECURSAO_PURA + " levaria minutos");
        }
        chamadas = 0;
        long inicio = System.nanoTime();
        long r = fibMemoria(n, new long[n + 2]);
        linha("recursivo com memória", r, chamadas, System.nanoTime() - inicio);
        inicio = System.nanoTime();
        r = fibIterativo(n);
        linha("iterativo", r, n, System.nanoTime() - inicio);
    }

    static void linha(String metodo, long resultado, long passos, long nanos) {
        System.out.printf(PT_BR, "   %-22s | %,22d | %,14d | %7.3f ms%n", metodo, resultado, passos, nanos / 1e6);
    }

    /** Fatorial com long estoura em 21!: multiplyExact avisa com exceção em vez de devolver lixo em silêncio. */
    static void fatorial(int n) {
        if (n < 0 || n > 5_000) throw new IllegalArgumentException("Fatorial aqui vai de 0 a 5.000, recebi " + n + ".");
        try {
            long r = 1;
            for (int i = 2; i <= n; i++) r = Math.multiplyExact(r, i);
            System.out.printf(PT_BR, "%d! com long = %,d%n", n, r);
        } catch (ArithmeticException e) {
            System.out.println(n + "! com long = estourou (o long vai só até 9.223.372.036.854.775.807)");
        }
        BigInteger grande = fatorialGrande(n);
        String digitos = grande.toString();
        System.out.println(n + "! com BigInteger = " + (digitos.length() <= 40 ? digitos : digitos.substring(0, 30) + "... (" + digitos.length() + " dígitos)"));
    }

    /** BigInteger não tem limite de tamanho: cresce o quanto for preciso, só mais devagar que o long. */
    static BigInteger fatorialGrande(int n) {
        BigInteger r = BigInteger.ONE;
        for (int i = 2; i <= n; i++) r = r.multiply(BigInteger.valueOf(i));
        return r;
    }

    /** Devolve o menor divisor de n maior que 1, ou n se for primo. Basta ir até a raiz: se n = a*b, um deles é <= sqrt(n). */
    static long menorDivisor(long n) {
        if (n % 2 == 0) return 2;
        for (long d = 3; d * d <= n; d += 2) if (n % d == 0) return d;
        return n;
    }

    static boolean ehPrimo(long n) { return n >= 2 && menorDivisor(n) == n; }

    static void primo(long n) {
        if (n < 0) throw new IllegalArgumentException("Teste de primo só para números não negativos, recebi " + n + ".");
        if (n < 2) System.out.println(n + " não é primo: por definição os primos começam em 2.");
        else if (ehPrimo(n)) System.out.printf(PT_BR, "%,d é primo (nenhum divisor até %,d, a raiz quadrada).%n", n, (long) Math.sqrt(n));
        else System.out.printf(PT_BR, "%,d não é primo: %,d x %,d.%n", n, menorDivisor(n), n / menorDivisor(n));
    }

    /** Crivo de Eratóstenes: risca os múltiplos de cada primo; quem sobra sem risco é primo. */
    static List<Integer> crivo(int limite) {
        boolean[] riscado = new boolean[limite + 1];
        List<Integer> primos = new ArrayList<>();
        for (int i = 2; i <= limite; i++) {
            if (riscado[i]) continue;
            primos.add(i);
            for (long m = (long) i * i; m <= limite; m += i) riscado[(int) m] = true;   // começa em i²: os múltiplos menores já foram riscados
        }
        return primos;
    }

    static void primos(int limite) {
        if (limite < 2 || limite > 10_000_000) throw new IllegalArgumentException("O limite do crivo vai de 2 a 10.000.000, recebi " + limite + ".");
        long inicio = System.nanoTime();
        List<Integer> lista = crivo(limite);
        System.out.printf(PT_BR, "Primos até %,d: %,d encontrados em %.2f ms%n", limite, lista.size(), (System.nanoTime() - inicio) / 1e6);
        if (lista.size() <= 200) {
            for (int i = 0; i < lista.size(); i++) System.out.print(String.format("%5d", lista.get(i)) + ((i + 1) % 10 == 0 || i == lista.size() - 1 ? "\n" : ""));
        } else {
            System.out.println("Os cinco últimos: " + lista.subList(lista.size() - 5, lista.size()));
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args.length != 2) throw new IllegalArgumentException("Use exatamente um comando e um número, por exemplo: fibonacci 30.");
            switch (args[0]) {
                case "fibonacci" -> fibonacci(inteiro(args[1]));
                case "fatorial" -> fatorial(inteiro(args[1]));
                case "primo" -> primo(Long.parseLong(args[1].trim()));
                case "primos" -> primos(inteiro(args[1]));
                default -> throw new IllegalArgumentException("Comando desconhecido '" + args[0] + "'. Use fibonacci, fatorial, primo ou primos.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Erro: esperava um número inteiro, recebi '" + args[1] + "'.\n");
            uso();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração 1: Fibonacci de 30 pelos três caminhos ===");
        fibonacci(30);
        System.out.println("   A recursão pura refaz o mesmo trabalho milhões de vezes: fib(28) é calculado 2 vezes, fib(27) 3 vezes,");
        System.out.println("   fib(26) 5 vezes... as repetições seguem a própria sequência de Fibonacci.");
        System.out.println("\n=== Demonstração 2: como as chamadas da recursão pura crescem ===");
        for (int n : new int[]{10, 15, 20, 25, 30}) {
            chamadas = 0;
            fibRecursivo(n);
            System.out.printf(PT_BR, "   fib(%2d): %,12d chamadas%n", n, chamadas);
        }
        System.out.println("   Somar 5 ao n multiplica as chamadas por cerca de 11. Em fib(50) seriam 40 bilhões.");
        System.out.println("\n=== Demonstração 3: fatorial de 20 cabe no long, o de 25 não ===");
        fatorial(20);
        fatorial(25);
        System.out.println("\n=== Demonstração 4: teste de primalidade ===");
        primo(7_919);
        primo(7_917);
        primo(1_000_000_007L);
        System.out.println("\n=== Demonstração 5: crivo de Eratóstenes até 100 ===");
        primos(100);
    }

    static int inteiro(String texto) {
        return Integer.parseInt(texto.trim());   // NumberFormatException é tratada no main, com mensagem amigável
    }

    static void uso() {
        System.out.println("""
            Uso: java FibonacciPrimos.java fibonacci <n>   os três Fibonacci com chamadas e tempos (0 a 92)
                 java FibonacciPrimos.java fatorial <n>    fatorial com long e com BigInteger (0 a 5000)
                 java FibonacciPrimos.java primo <n>       diz se n é primo ou mostra um divisor
                 java FibonacciPrimos.java primos <n>      todos os primos até n pelo crivo
            Exemplos:
              java FibonacciPrimos.java fibonacci 30
              java FibonacciPrimos.java primos 100
              java FibonacciPrimos.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Escreva o fatorial recursivo e descubra em que n a pilha de chamadas estoura (StackOverflowError).
 *  2. Troque o long[] da memoização por um Map<Integer, Long> e compare a legibilidade e o tempo.
 *  3. Use o crivo para achar os primos gêmeos (pares como 11 e 13) até 1.000.
 */
