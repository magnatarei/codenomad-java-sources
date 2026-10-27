/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  28. Operações com matrizes
 * ------------------------------------------------------------
 * O que faz: soma, subtração, multiplicação, transposta, determinante
 *            (recursivo, por expansão de Laplace) e matriz identidade, com
 *            validação de dimensões e impressão alinhada em colunas.
 * Como usar: java MatrizOperacoes.java somar "1,2;3,4" "5,6;7,8"
 *            java MatrizOperacoes.java multiplicar "1,2,3;4,5,6" "7,8;9,10;11,12"
 *            java MatrizOperacoes.java determinante "2,0,1;1,3,2;1,1,1"
 *            java MatrizOperacoes.java transposta "1,2,3;4,5,6"
 *            java MatrizOperacoes.java identidade 3
 *            java MatrizOperacoes.java --demo
 * O que você aprende: arrays bidimensionais (double[][]), laços aninhados em
 *            três níveis, recursão com caso base, record com validação no
 *            construtor compacto, interfaces funcionais (DoubleBinaryOperator),
 *            String.split em dois níveis e formatação alinhada com printf.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.DoubleBinaryOperator;

public class MatrizOperacoes {

    /** Um record com validação: o construtor compacto roda antes de guardar os dados. */
    record Matriz(double[][] dados) {
        Matriz {
            if (dados.length == 0 || dados[0].length == 0) throw new IllegalArgumentException("A matriz precisa de pelo menos uma linha e uma coluna.");
            for (double[] linha : dados) {
                if (linha.length != dados[0].length) throw new IllegalArgumentException("Todas as linhas precisam ter o mesmo número de colunas: a primeira tem " + dados[0].length + ", outra tem " + linha.length + ".");
            }
        }

        int linhas() { return dados.length; }
        int colunas() { return dados[0].length; }
        String dimensao() { return linhas() + "x" + colunas(); }

        Matriz somar(Matriz outra) { return combinar(outra, "somar", (a, b) -> a + b); }
        Matriz subtrair(Matriz outra) { return combinar(outra, "subtrair", (a, b) -> a - b); }

        /** Soma e subtração são elemento a elemento: as duas matrizes precisam ter exatamente a mesma dimensão. */
        private Matriz combinar(Matriz outra, String operacao, DoubleBinaryOperator operacaoCelula) {
            if (linhas() != outra.linhas() || colunas() != outra.colunas()) throw new IllegalArgumentException("Para " + operacao + ", as dimensões devem ser iguais: recebi " + dimensao() + " e " + outra.dimensao() + ".");
            double[][] r = new double[linhas()][colunas()];
            for (int i = 0; i < linhas(); i++) {
                for (int j = 0; j < colunas(); j++) r[i][j] = operacaoCelula.applyAsDouble(dados[i][j], outra.dados[i][j]);
            }
            return new Matriz(r);
        }

        /** Multiplicação: o elemento (i, j) é a linha i desta vezes a coluna j da outra, somando os produtos. */
        Matriz multiplicar(Matriz outra) {
            if (colunas() != outra.linhas()) throw new IllegalArgumentException("Para multiplicar, as colunas da primeira (" + colunas() + ") devem ser iguais às linhas da segunda (" + outra.linhas() + ").");
            double[][] r = new double[linhas()][outra.colunas()];
            for (int i = 0; i < linhas(); i++) {
                for (int j = 0; j < outra.colunas(); j++) {
                    for (int k = 0; k < colunas(); k++) r[i][j] += dados[i][k] * outra.dados[k][j];   // três laços: n³ operações
                }
            }
            return new Matriz(r);
        }

        /** Transposta: linhas viram colunas. Uma 2x3 vira 3x2. */
        Matriz transposta() {
            double[][] r = new double[colunas()][linhas()];
            for (int i = 0; i < linhas(); i++) {
                for (int j = 0; j < colunas(); j++) r[j][i] = dados[i][j];
            }
            return new Matriz(r);
        }

        /** Determinante por expansão de Laplace na primeira linha: recursivo, com casos base 1x1 e 2x2. */
        double determinante() {
            if (linhas() != colunas()) throw new IllegalArgumentException("Determinante só existe para matriz quadrada; esta é " + dimensao() + ".");
            int n = linhas();
            if (n > 9) throw new IllegalArgumentException("Determinante aqui vai até 9x9: a expansão de Laplace cresce com n! (fatorial).");
            if (n == 1) return dados[0][0];
            if (n == 2) return dados[0][0] * dados[1][1] - dados[0][1] * dados[1][0];
            double soma = 0;
            for (int j = 0; j < n; j++) {
                double sinal = j % 2 == 0 ? 1 : -1;   // os sinais alternam ao longo da linha: + - + - ...
                soma += sinal * dados[0][j] * menor(0, j).determinante();   // cada termo chama o determinante de uma matriz menor
            }
            return soma;
        }

        /** O "menor" é a matriz sem a linha e a coluna indicadas: (n-1)x(n-1). */
        Matriz menor(int linhaFora, int colunaFora) {
            double[][] r = new double[linhas() - 1][colunas() - 1];
            for (int i = 0, ri = 0; i < linhas(); i++) {
                if (i == linhaFora) continue;
                for (int j = 0, rj = 0; j < colunas(); j++) {
                    if (j != colunaFora) r[ri][rj++] = dados[i][j];
                }
                ri++;
            }
            return new Matriz(r);
        }

        /** Identidade: 1 na diagonal, 0 no resto. Multiplicar por ela não muda nada, como o 1 dos números. */
        static Matriz identidade(int n) {
            if (n < 1 || n > 20) throw new IllegalArgumentException("A identidade aqui vai de 1x1 a 20x20, recebi " + n + ".");
            double[][] r = new double[n][n];
            for (int i = 0; i < n; i++) r[i][i] = 1;
            return new Matriz(r);
        }

        /** Lê "1,2;3,4": ponto e vírgula separa linhas, vírgula (ou espaço) separa colunas, ponto marca casas decimais. */
        static Matriz de(String texto) {
            if (texto == null || texto.isBlank()) throw new IllegalArgumentException("A matriz veio vazia. Exemplo de formato: \"1,2;3,4\".");
            String[] linhas = texto.trim().split(";");
            double[][] r = new double[linhas.length][];
            for (int i = 0; i < linhas.length; i++) {
                String[] celulas = linhas[i].trim().split("[,\\s]+");
                r[i] = new double[celulas.length];
                for (int j = 0; j < celulas.length; j++) {
                    try {
                        r[i][j] = Double.parseDouble(celulas[j]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("O valor '" + celulas[j] + "' na linha " + (i + 1) + " não é um número.");
                    }
                }
            }
            return new Matriz(r);
        }

        /** Impressão alinhada: mede a célula mais larga e usa essa largura em todas as colunas. */
        @Override public String toString() {
            int largura = 1;
            for (double[] linha : dados) for (double v : linha) largura = Math.max(largura, formatar(v).length());
            StringBuilder sb = new StringBuilder();
            for (double[] linha : dados) {
                sb.append("   |");
                for (double v : linha) sb.append(String.format("%" + (largura + 1) + "s", formatar(v)));
                sb.append(" |\n");
            }
            return sb.toString();
        }
    }

    /** Inteiros saem sem casas, os demais com duas. O "-0" que aparece em contas com negativos vira "0". */
    static String formatar(double v) {
        if (v == 0) v = 0;   // -0.0 == 0.0 é verdadeiro, e a atribuição troca pelo zero positivo
        if (v == Math.rint(v) && Math.abs(v) < 1e12) return String.format(Locale.ROOT, "%.0f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    static void mostrar(String titulo, Matriz m) {
        System.out.println(titulo + " (" + m.dimensao() + "):");
        System.out.print(m);
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            String operacao = args[0].toLowerCase();
            int esperados = switch (operacao) {
                case "somar", "subtrair", "multiplicar" -> 2;
                case "transposta", "determinante", "identidade" -> 1;
                default -> throw new IllegalArgumentException("Operação desconhecida '" + args[0] + "'. Use somar, subtrair, multiplicar, transposta, determinante ou identidade.");
            };
            if (args.length - 1 != esperados) throw new IllegalArgumentException("A operação " + operacao + " espera " + esperados + " argumento(s), recebi " + (args.length - 1) + ".");
            if (operacao.equals("identidade")) {
                mostrar("Identidade", Matriz.identidade(Integer.parseInt(args[1].trim())));
                return;
            }
            Matriz a = Matriz.de(args[1]);
            mostrar("Matriz A", a);
            if (esperados == 1) {
                if (operacao.equals("transposta")) mostrar("Transposta de A", a.transposta());
                else System.out.println("Determinante de A = " + formatar(a.determinante()));
                return;
            }
            Matriz b = Matriz.de(args[2]);
            mostrar("Matriz B", b);
            switch (operacao) {
                case "somar" -> mostrar("A + B", a.somar(b));
                case "subtrair" -> mostrar("A - B", a.subtrair(b));
                default -> mostrar("A x B", a.multiplicar(b));
            }
        } catch (NumberFormatException e) {
            System.out.println("Erro: o tamanho da identidade precisa ser um número inteiro, recebi '" + args[1] + "'.\n");
            uso();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        Matriz a = Matriz.de("1,2;3,4"), b = Matriz.de("5,6;7,8");
        System.out.println("=== Demonstração 1: matrizes 2x2 ===");
        mostrar("A", a);
        mostrar("B", b);
        mostrar("A + B", a.somar(b));
        mostrar("A - B", a.subtrair(b));
        mostrar("A x B", a.multiplicar(b));
        mostrar("B x A (a ordem importa: multiplicação de matrizes não é comutativa)", b.multiplicar(a));
        mostrar("Transposta de A", a.transposta());
        System.out.println("Determinante de A = " + formatar(a.determinante()) + "  (1*4 - 2*3)");

        System.out.println("\n=== Demonstração 2: matrizes 3x3 e uma 2x3 ===");
        Matriz c = Matriz.de("2,0,1;1,3,2;1,1,1"), r = Matriz.de("1,2,3;4,5,6");
        mostrar("C", c);
        mostrar("C x Identidade (não muda nada)", c.multiplicar(Matriz.identidade(3)));
        System.out.println("Determinante de C = " + formatar(c.determinante()) + "  (expansão pela primeira linha: 2*(3-2) - 0 + 1*(1-3))");
        mostrar("R", r);
        mostrar("R x Transposta de R (2x3 vezes 3x2 dá 2x2)", r.multiplicar(r.transposta()));
        mostrar("Metade de R", r.combinar(r, "somar", (x, y) -> (x + y) / 2).subtrair(r.combinar(r, "somar", (x, y) -> y / 2)));

        System.out.println("\n=== Demonstração 3: erros tratados ===");
        for (String[] caso : new String[][]{{"somar", "1,2;3,4", "1,2,3;4,5,6"}, {"multiplicar", "1,2,3;4,5,6", "1,2,3;4,5,6"}, {"determinante", "1,2,3;4,5,6", ""}, {"somar", "1,2;3", ""}}) {
            try {
                Matriz x = Matriz.de(caso[1]);
                switch (caso[0]) {
                    case "somar" -> x.somar(Matriz.de(caso[2]));
                    case "multiplicar" -> x.multiplicar(Matriz.de(caso[2]));
                    default -> x.determinante();
                }
            } catch (IllegalArgumentException e) {
                System.out.println("   " + caso[0] + " " + caso[1] + (caso[2].isEmpty() ? "" : " " + caso[2]) + " -> " + e.getMessage());
            }
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java MatrizOperacoes.java <operação> <matriz> [matriz]
              operações: somar, subtrair, multiplicar (duas matrizes); transposta, determinante (uma); identidade <n>
              formato da matriz: linhas separadas por ";" e colunas por "," (ponto para casas decimais): "1,2;3,4"
            Exemplos:
              java MatrizOperacoes.java multiplicar "1,2,3;4,5,6" "7,8;9,10;11,12"
              java MatrizOperacoes.java determinante "2,0,1;1,3,2;1,1,1"
              java MatrizOperacoes.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Implemente a multiplicação por escalar (todos os elementos vezes um número) e a potência A^n por multiplicações repetidas.
 *  2. Calcule a inversa de uma 2x2 com a fórmula (1/det) * [d, -b; -c, a] e confira que A x inversa dá a identidade.
 *  3. Leia a matriz de um arquivo de texto com uma linha por linha da matriz, em vez da linha de comando.
 */
