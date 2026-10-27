/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  02. Simulador de financiamento (Price e SAC)
 * ------------------------------------------------------------
 * O que faz: recebe o valor financiado, a taxa mensal e o prazo. Gera a tabela
 *            de parcelas nos dois sistemas mais usados, Price (parcela fixa) e
 *            SAC (amortização fixa), e compara o total pago em cada um.
 * Como usar: java SimuladorFinanciamento.java 50000 1 12
 *            java SimuladorFinanciamento.java 250000 0,75 240
 *            java SimuladorFinanciamento.java --demo
 * O que você aprende: enum com campos e construtor, switch expression,
 *            records, List, Math.pow, printf com Locale e como separar a
 *            regra de cálculo da impressão da tabela.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimuladorFinanciamento {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Os dois sistemas de amortização. Cada constante carrega a própria descrição. */
    enum Sistema {
        PRICE("Tabela Price: parcelas iguais do início ao fim"),
        SAC("SAC: amortização constante, parcelas que diminuem");

        final String descricao;

        Sistema(String descricao) {
            this.descricao = descricao;
        }
    }

    /** Uma parcela: o que se paga, quanto é juro, quanto abate a dívida e o que ainda falta. */
    record Parcela(int numero, double valor, double juros, double amortizacao, double saldoDevedor) {}

    /** O contrato, validado no construtor compacto. */
    record Financiamento(double valor, double taxaMensal, int meses) {

        Financiamento {
            if (valor <= 0) throw new IllegalArgumentException("O valor financiado deve ser maior que zero.");
            if (taxaMensal < 0 || taxaMensal > 50) throw new IllegalArgumentException("A taxa mensal deve ficar entre 0 e 50 (em %).");
            if (meses < 1 || meses > 600) throw new IllegalArgumentException("O prazo deve ter entre 1 e 600 meses.");
        }

        /** Gera todas as parcelas no sistema pedido. */
        List<Parcela> gerar(Sistema sistema) {
            double i = taxaMensal / 100.0;
            // Na Price a parcela é fixa (fórmula do valor presente de uma anuidade).
            // Com taxa zero a fórmula divide por zero, então basta dividir o valor pelo prazo.
            double parcelaFixa = i == 0 ? valor / meses : valor * i / (1 - Math.pow(1 + i, -meses));
            // No SAC o que é fixo é a amortização: a dívida cai em partes iguais.
            double amortizacaoFixa = valor / meses;

            var parcelas = new ArrayList<Parcela>();
            double saldo = valor;
            for (int n = 1; n <= meses; n++) {
                double juros = saldo * i;
                double amortizacao = switch (sistema) {
                    case PRICE -> parcelaFixa - juros;
                    case SAC -> amortizacaoFixa;
                };
                // Math.max evita um "-0,00" na última linha por causa de arredondamento.
                saldo = Math.max(0, saldo - amortizacao);
                parcelas.add(new Parcela(n, amortizacao + juros, juros, amortizacao, saldo));
            }
            return parcelas;
        }
    }

    /** Totais de um sistema, prontos para a comparação final. */
    record Resumo(Sistema sistema, double totalPago, double totalJuros, double primeira, double ultima) {

        static Resumo de(Sistema sistema, List<Parcela> parcelas) {
            double totalPago = parcelas.stream().mapToDouble(Parcela::valor).sum();
            double totalJuros = parcelas.stream().mapToDouble(Parcela::juros).sum();
            double primeira = parcelas.get(0).valor();
            double ultima = parcelas.get(parcelas.size() - 1).valor();
            return new Resumo(sistema, totalPago, totalJuros, primeira, ultima);
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        if (args[0].equals("--demo")) {
            System.out.println("=== Demonstração: R$ 50.000,00 a 1% ao mês em 12 meses ===");
            System.out.println();
            simular(new Financiamento(50_000, 1.0, 12));
            return;
        }
        try {
            if (args.length != 3) throw new IllegalArgumentException("Informe exatamente três valores: valor, taxa mensal e meses.");
            simular(new Financiamento(numero(args[0], "valor"), numero(args[1], "taxa mensal"), inteiro(args[2], "meses")));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void simular(Financiamento f) {
        System.out.printf(PT_BR, "Valor financiado: %s | Taxa: %.2f%% ao mês | Prazo: %d meses%n%n",
                moeda(f.valor()), f.taxaMensal(), f.meses());

        var resumos = new ArrayList<Resumo>();
        for (Sistema sistema : Sistema.values()) {
            List<Parcela> parcelas = f.gerar(sistema);
            imprimirTabela(sistema, parcelas);
            resumos.add(Resumo.de(sistema, parcelas));
        }
        imprimirComparacao(resumos.get(0), resumos.get(1));
    }

    static void imprimirTabela(Sistema sistema, List<Parcela> parcelas) {
        System.out.println(sistema.descricao);
        System.out.printf("%4s | %14s | %12s | %14s | %16s%n", "N", "Parcela", "Juros", "Amortização", "Saldo devedor");
        System.out.println("-".repeat(72));
        for (Parcela p : parcelas) {
            System.out.printf("%4d | %14s | %12s | %14s | %16s%n",
                    p.numero(), moeda(p.valor()), moeda(p.juros()), moeda(p.amortizacao()), moeda(p.saldoDevedor()));
        }
        System.out.println("-".repeat(72));
        System.out.println();
    }

    static void imprimirComparacao(Resumo price, Resumo sac) {
        System.out.println("Comparação dos dois sistemas");
        System.out.printf("%-22s | %16s | %16s%n", "", "Price", "SAC");
        System.out.printf("%-22s | %16s | %16s%n", "Primeira parcela", moeda(price.primeira()), moeda(sac.primeira()));
        System.out.printf("%-22s | %16s | %16s%n", "Última parcela", moeda(price.ultima()), moeda(sac.ultima()));
        System.out.printf("%-22s | %16s | %16s%n", "Total de juros", moeda(price.totalJuros()), moeda(sac.totalJuros()));
        System.out.printf("%-22s | %16s | %16s%n", "Total pago", moeda(price.totalPago()), moeda(sac.totalPago()));
        System.out.println();
        double diferenca = price.totalPago() - sac.totalPago();
        if (diferenca > 0.005) {
            System.out.println("No SAC você paga " + moeda(diferenca) + " a menos, mas as primeiras parcelas são mais altas.");
            System.out.println("Na Price a parcela cabe melhor no início, e o custo total é maior.");
        } else {
            System.out.println("Com essa taxa os dois sistemas custam o mesmo: sem juros, só existe a amortização.");
        }
    }

    /** Aceita "0.75" e "0,75": troca a vírgula por ponto antes de converter. */
    static double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo '" + campo + "' precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static int inteiro(String texto, String campo) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo '" + campo + "' precisa ser um número inteiro, recebi '" + texto + "'.");
        }
    }

    static String moeda(double valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    static void uso() {
        System.out.println("""
            Uso: java SimuladorFinanciamento.java <valor> <taxa mensal em %> <meses>
              valor         quanto foi financiado, em reais (ex.: 50000)
              taxa mensal   juros por mês, em porcentagem (ex.: 1 ou 0,75)
              meses         prazo do contrato, de 1 a 600
            Exemplos:
              java SimuladorFinanciamento.java 50000 1 12
              java SimuladorFinanciamento.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente uma entrada (valor pago à vista) e financie só a diferença.
 *  2. Receba a taxa anual e converta para mensal antes de simular.
 *  3. Permita um pagamento extra em um mês escolhido e mostre quantas parcelas ele elimina.
 */
