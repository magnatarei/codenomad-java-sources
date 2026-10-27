/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  01. Calculadora de juros compostos
 * ------------------------------------------------------------
 * O que faz: recebe um capital inicial, uma taxa mensal, um prazo em meses
 *            e um aporte mensal opcional. Mostra o montante final, os juros
 *            ganhos e uma tabela mês a mês com saldo inicial, juros, aporte
 *            e saldo final.
 * Como usar: java CalculadoraJurosCompostos.java 1000 0.8 24 200
 *            java CalculadoraJurosCompostos.java 5000 1,2 36
 *            java CalculadoraJurosCompostos.java --demo
 * O que você aprende: records com construtor compacto, listas e for-each,
 *            Math.pow, String.format com Locale, text blocks, validação de
 *            entrada com exceções e separação entre cálculo e apresentação.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculadoraJurosCompostos {

    /** Formato brasileiro: ponto nos milhares e vírgula nos centavos. */
    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Uma linha da tabela: o que aconteceu com o dinheiro em um mês. */
    record Linha(int mes, double saldoInicial, double juros, double aporte, double saldoFinal) {}

    /** Os dados de entrada, já validados no construtor compacto. */
    record Simulacao(double capital, double taxaMensal, int meses, double aporte) {

        // O construtor compacto roda antes de guardar os campos: é o lugar
        // certo para rejeitar dados impossíveis, uma vez só, para todo o programa.
        Simulacao {
            if (capital < 0) throw new IllegalArgumentException("O capital não pode ser negativo.");
            if (taxaMensal < 0 || taxaMensal > 100) throw new IllegalArgumentException("A taxa mensal deve ficar entre 0 e 100 (em %).");
            if (meses < 1 || meses > 1200) throw new IllegalArgumentException("O prazo deve ter entre 1 e 1200 meses.");
            if (aporte < 0) throw new IllegalArgumentException("O aporte não pode ser negativo.");
        }

        /** Gera a tabela mês a mês. O aporte entra no fim do mês, depois dos juros. */
        List<Linha> calcular() {
            var linhas = new ArrayList<Linha>();
            double saldo = capital;
            for (int m = 1; m <= meses; m++) {
                double juros = saldo * taxaMensal / 100.0;
                double saldoFinal = saldo + juros + aporte;
                linhas.add(new Linha(m, saldo, juros, aporte, saldoFinal));
                saldo = saldoFinal;
            }
            return linhas;
        }

        /** Tudo o que saiu do bolso: o capital mais todos os aportes. */
        double totalInvestido() {
            return capital + aporte * meses;
        }

        /**
         * Confere o resultado pela fórmula fechada, sem laço:
         * M = C * (1 + i)^n + A * ((1 + i)^n - 1) / i
         * Quando a taxa é zero não existe divisão, só a soma dos aportes.
         */
        double montantePelaFormula() {
            double i = taxaMensal / 100.0;
            double fator = Math.pow(1 + i, meses);
            if (i == 0) return capital + aporte * meses;
            return capital * fator + aporte * (fator - 1) / i;
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
            imprimirRelatorio(lerArgumentos(args));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: R$ 1.000,00 a 0,8% ao mês por 24 meses, com aporte de R$ 200,00 ===");
        System.out.println();
        imprimirRelatorio(new Simulacao(1000, 0.8, 24, 200));
    }

    /** Converte os argumentos da linha de comando em uma Simulacao validada. */
    static Simulacao lerArgumentos(String[] args) {
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Informe capital, taxa mensal e meses (o aporte é opcional).");
        }
        double capital = numero(args[0], "capital");
        double taxa = numero(args[1], "taxa mensal");
        int meses = inteiro(args[2], "meses");
        double aporte = args.length == 4 ? numero(args[3], "aporte") : 0;
        return new Simulacao(capital, taxa, meses, aporte);
    }

    /** Aceita "0.8" e "0,8": troca a vírgula por ponto antes de converter. */
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

    static void imprimirRelatorio(Simulacao sim) {
        List<Linha> tabela = sim.calcular();
        double montante = tabela.get(tabela.size() - 1).saldoFinal();
        double jurosGanhos = montante - sim.totalInvestido();

        System.out.printf(PT_BR, "Capital inicial: %s | Taxa: %.2f%% ao mês | Prazo: %d meses | Aporte mensal: %s%n",
                moeda(sim.capital()), sim.taxaMensal(), sim.meses(), moeda(sim.aporte()));
        System.out.println();
        System.out.printf("%4s | %15s | %12s | %12s | %15s%n", "Mês", "Saldo inicial", "Juros", "Aporte", "Saldo final");
        System.out.println("-".repeat(70));
        for (Linha l : tabela) {
            System.out.printf("%4d | %15s | %12s | %12s | %15s%n",
                    l.mes(), moeda(l.saldoInicial()), moeda(l.juros()), moeda(l.aporte()), moeda(l.saldoFinal()));
        }
        System.out.println("-".repeat(70));
        System.out.println("Total investido:   " + moeda(sim.totalInvestido()));
        System.out.println("Juros ganhos:      " + moeda(jurosGanhos));
        System.out.println("Montante final:    " + moeda(montante));
        System.out.printf(PT_BR, "Rendimento total:  %.2f%% sobre o que foi investido%n",
                sim.totalInvestido() == 0 ? 0 : jurosGanhos / sim.totalInvestido() * 100);
        // A diferença entre o laço e a fórmula fica na casa dos centavos, por arredondamento.
        System.out.println("Conferência pela fórmula fechada: " + moeda(sim.montantePelaFormula()));
    }

    /** Dinheiro sempre com duas casas e separador de milhar: R$ 1.234,56. */
    static String moeda(double valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraJurosCompostos.java <capital> <taxa mensal em %> <meses> [aporte mensal]
              capital       valor inicial investido, em reais (ex.: 1000)
              taxa mensal   juros por mês, em porcentagem (ex.: 0.8 ou 0,8)
              meses         prazo da aplicação, de 1 a 1200
              aporte        depósito feito todo mês, opcional (ex.: 200)
            Exemplos:
              java CalculadoraJurosCompostos.java 1000 0.8 24 200
              java CalculadoraJurosCompostos.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Aceite a taxa anual e converta para mensal com a fórmula (1 + a)^(1/12) - 1.
 *  2. Mostre em que mês os juros do período passam a ser maiores que o aporte.
 *  3. Gere o relatório em CSV (uma linha por mês) para abrir na planilha.
 */
