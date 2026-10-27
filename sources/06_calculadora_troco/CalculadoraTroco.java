/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  06. Calculadora de troco
 * ------------------------------------------------------------
 * O que faz: recebe o valor da compra e o valor pago, calcula o troco e o
 *            decompõe no menor número de cédulas e moedas do real, com o
 *            algoritmo guloso. Avisa quando o pagamento não cobre a compra.
 * Como usar: java CalculadoraTroco.java 37,45 50
 *            java CalculadoraTroco.java 12.90 20
 *            java CalculadoraTroco.java --demo
 * O que você aprende: por que dinheiro se conta em centavos inteiros e não
 *            em double, BigDecimal para ler valores digitados, algoritmo
 *            guloso, divisão inteira e resto, records e códigos de saída.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculadoraTroco {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Uma cédula ou moeda. O valor fica em centavos: 5000 é R$ 50,00. */
    record Dinheiro(int centavos, boolean cedula) {

        String nome() {
            return (cedula ? "cédula de " : "moeda de ") + reais(centavos);
        }
    }

    // Do maior para o menor: essa ordem é o que faz o algoritmo guloso dar certo.
    // Com os valores do real, o guloso sempre acha a menor quantidade de itens.
    static final List<Dinheiro> VALORES = List.of(
            new Dinheiro(20000, true), new Dinheiro(10000, true), new Dinheiro(5000, true),
            new Dinheiro(2000, true), new Dinheiro(1000, true), new Dinheiro(500, true),
            new Dinheiro(200, true),
            new Dinheiro(100, false), new Dinheiro(50, false), new Dinheiro(25, false),
            new Dinheiro(10, false), new Dinheiro(5, false), new Dinheiro(1, false));

    /** Quantas unidades de uma cédula ou moeda entram no troco. */
    record Parte(Dinheiro dinheiro, int quantidade) {}

    /** O resultado de uma compra: valores em centavos e a lista de partes do troco. */
    record Troco(int compraCentavos, int pagoCentavos, List<Parte> partes) {

        int trocoCentavos() {
            return pagoCentavos - compraCentavos;
        }

        boolean suficiente() {
            return trocoCentavos() >= 0;
        }

        int totalDeItens() {
            return partes.stream().mapToInt(Parte::quantidade).sum();
        }
    }

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
            if (args.length != 2) throw new IllegalArgumentException("Informe dois valores: o da compra e o pago.");
            Troco troco = calcular(centavos(args[0], "compra"), centavos(args[1], "pago"));
            imprimir(troco);
            // Código de saída 2 avisa um script que o pagamento não cobriu a compra;
            // 1 fica reservado para erro de uso. Assim quem automatiza sabe o que houve.
            if (!troco.suficiente()) System.exit(2);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: quatro compras e a decomposição de cada troco ===");
        System.out.println();
        System.out.println("Curiosidade: em double, 0.1 + 0.2 = " + (0.1 + 0.2) + ". Por isso contamos em centavos inteiros.");
        System.out.println();
        String[][] compras = {{"37,45", "50"}, {"199,99", "200"}, {"12,90", "100"}, {"80", "50"}};
        for (String[] c : compras) {
            imprimir(calcular(centavos(c[0], "compra"), centavos(c[1], "pago")));
            System.out.println();
        }
    }

    /** Decompõe o troco pegando sempre a maior cédula ou moeda que ainda cabe. */
    static Troco calcular(int compraCentavos, int pagoCentavos) {
        var partes = new ArrayList<Parte>();
        int resto = pagoCentavos - compraCentavos;
        for (Dinheiro d : VALORES) {
            if (resto <= 0) break;
            int quantidade = resto / d.centavos();  // divisão inteira: quantas cabem
            if (quantidade > 0) {
                partes.add(new Parte(d, quantidade));
                resto %= d.centavos();              // o resto segue para a próxima, menor
            }
        }
        return new Troco(compraCentavos, pagoCentavos, partes);
    }

    static void imprimir(Troco t) {
        System.out.println("Compra: " + reais(t.compraCentavos()) + " | Pago: " + reais(t.pagoCentavos()));
        if (!t.suficiente()) {
            System.out.println("  Pagamento insuficiente: faltam " + reais(-t.trocoCentavos()) + ".");
            return;
        }
        if (t.trocoCentavos() == 0) {
            System.out.println("  Pagamento exato, sem troco.");
            return;
        }
        System.out.println("  Troco: " + reais(t.trocoCentavos()));
        for (Parte p : t.partes()) {
            System.out.printf("  %3d x %s%n", p.quantidade(), p.dinheiro().nome());
        }
        System.out.println("  Total: " + t.totalDeItens() + " " + (t.totalDeItens() == 1 ? "item" : "itens"));
    }

    /**
     * Lê "37,45" ou "37.45" e devolve 3745 centavos sem passar por double.
     * BigDecimal guarda o texto exatamente; movePointRight(2) anda duas casas;
     * setScale(0) arredonda meio centavo para cima; intValueExact recusa estouro.
     */
    static int centavos(String texto, String campo) {
        try {
            BigDecimal valor = new BigDecimal(texto.trim().replace(',', '.'));
            if (valor.signum() < 0) throw new IllegalArgumentException("O campo '" + campo + "' não pode ser negativo.");
            return valor.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("O campo '" + campo + "' precisa ser um valor em reais (ex.: 37,45), recebi '" + texto + "'.");
        }
    }

    /** De centavos inteiros para "R$ 1.234,56". */
    static String reais(int centavos) {
        return String.format(PT_BR, "R$ %,d,%02d", centavos / 100, centavos % 100);
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraTroco.java <valor da compra> <valor pago>
              Os valores aceitam vírgula ou ponto: 37,45 ou 37.45
            Códigos de saída: 0 troco calculado, 1 erro de uso, 2 pagamento insuficiente
            Exemplos:
              java CalculadoraTroco.java 37,45 50
              java CalculadoraTroco.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Permita informar quais cédulas o caixa tem (ex.: sem notas de 200) e refaça a decomposição.
 *  2. Aceite vários pagamentos (duas notas de 20 e uma moeda de 1) e some antes de calcular o troco.
 *  3. Mostre um caso em que o guloso falha: use moedas fictícias de 1, 3 e 4 e um troco de 6.
 */
