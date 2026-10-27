/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  05. Conversor de moedas com tabela de taxas
 * ------------------------------------------------------------
 * O que faz: converte um valor entre quaisquer duas moedas de uma tabela
 *            embutida (USD, EUR, GBP, JPY, ARS, BRL e outras) e lista as
 *            taxas com a data em que a tabela foi montada.
 *            As taxas são valores de referência para estudo. Para atualizar,
 *            troque os números na lista MOEDAS (quanto vale 1 dólar em cada
 *            moeda) e a constante DATA_TABELA, usando a cotação do dia de uma
 *            fonte confiável de câmbio. Nada mais precisa mudar.
 * Como usar: java ConversorMoedas.java 100 USD BRL
 *            java ConversorMoedas.java 250,50 EUR JPY
 *            java ConversorMoedas.java --taxas
 *            java ConversorMoedas.java --demo
 * O que você aprende: Map com LinkedHashMap (ordem de inserção), records,
 *            static initializer, moeda base para converter N moedas com N
 *            taxas, String.format com Locale e validação de entrada.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConversorMoedas {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Data em que os valores da tabela foram anotados. Atualize junto com as taxas. */
    static final String DATA_TABELA = "2026-10-01";

    /** Uma moeda: código ISO de três letras, nome, símbolo e quanto vale 1 USD nela. */
    record Moeda(String codigo, String nome, String simbolo, double porDolar) {}

    // LinkedHashMap mantém a ordem em que as moedas foram inseridas,
    // então a listagem sai sempre na mesma sequência. HashMap não garante isso.
    static final Map<String, Moeda> MOEDAS = new LinkedHashMap<>();

    // O bloco static roda uma vez, quando a classe é carregada: ideal para montar tabelas.
    static {
        List<Moeda> tabela = List.of(
                new Moeda("USD", "Dólar americano", "US$", 1.0),
                new Moeda("BRL", "Real brasileiro", "R$", 5.40),
                new Moeda("EUR", "Euro", "€", 0.92),
                new Moeda("GBP", "Libra esterlina", "£", 0.79),
                new Moeda("JPY", "Iene japonês", "¥", 150.0),
                new Moeda("ARS", "Peso argentino", "AR$", 1050.0),
                new Moeda("CAD", "Dólar canadense", "C$", 1.37),
                new Moeda("AUD", "Dólar australiano", "A$", 1.53),
                new Moeda("CHF", "Franco suíço", "CHF", 0.88),
                new Moeda("MXN", "Peso mexicano", "MX$", 18.90),
                new Moeda("CNY", "Yuan chinês", "CN¥", 7.25),
                new Moeda("CLP", "Peso chileno", "CLP$", 950.0),
                new Moeda("UYU", "Peso uruguaio", "$U", 42.0));
        for (Moeda m : tabela) {
            MOEDAS.put(m.codigo(), m);
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        switch (args[0]) {
            case "--demo" -> demo();
            case "--taxas" -> listarTaxas();
            default -> {
                try {
                    if (args.length != 3) throw new IllegalArgumentException("Informe valor, moeda de origem e moeda de destino.");
                    System.out.println(converterEDescrever(numero(args[0]), args[1], args[2]));
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro: " + e.getMessage());
                    System.out.println();
                    uso();
                    System.exit(1);
                }
            }
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: 100 unidades em seis pares de moedas ===");
        System.out.println();
        String[][] pares = {{"USD", "BRL"}, {"EUR", "USD"}, {"BRL", "ARS"}, {"GBP", "JPY"}, {"JPY", "BRL"}, {"CHF", "EUR"}};
        for (String[] par : pares) {
            System.out.println(converterEDescrever(100, par[0], par[1]));
        }
        System.out.println();
        listarTaxas();
    }

    /** Converte e monta a frase completa, com a taxa do par e a data da tabela. */
    static String converterEDescrever(double valor, String de, String para) {
        Moeda origem = moeda(de);
        Moeda destino = moeda(para);
        double convertido = converter(valor, origem, destino);
        double taxaDoPar = destino.porDolar() / origem.porDolar();
        return String.format(PT_BR, "%s %s = %s %s   (1 %s = %.4f %s, tabela de %s)",
                origem.simbolo(), valorFormatado(valor), destino.simbolo(), valorFormatado(convertido),
                origem.codigo(), taxaDoPar, destino.codigo(), DATA_TABELA);
    }

    /** Passa pelo dólar, que é a moeda base: valor / taxa de origem * taxa de destino. */
    static double converter(double valor, Moeda origem, Moeda destino) {
        if (valor < 0) throw new IllegalArgumentException("O valor não pode ser negativo.");
        return valor / origem.porDolar() * destino.porDolar();
    }

    /** Busca a moeda aceitando "brl" ou "BRL"; explica o erro quando não existe. */
    static Moeda moeda(String codigo) {
        Moeda m = MOEDAS.get(codigo.toUpperCase());
        if (m == null) {
            throw new IllegalArgumentException("Não conheço a moeda '" + codigo + "'. Disponíveis: " + String.join(", ", MOEDAS.keySet()));
        }
        return m;
    }

    static void listarTaxas() {
        System.out.println("Tabela de taxas (quanto vale 1 USD em cada moeda), anotada em " + DATA_TABELA + ":");
        System.out.printf("%-5s | %-20s | %-6s | %14s | %14s%n", "Cód.", "Moeda", "Símb.", "1 USD vale", "1 unidade em USD");
        System.out.println("-".repeat(70));
        for (Moeda m : MOEDAS.values()) {
            System.out.printf(PT_BR, "%-5s | %-20s | %-6s | %14s | %14.4f%n",
                    m.codigo(), m.nome(), m.simbolo(), valorFormatado(m.porDolar()), 1 / m.porDolar());
        }
        System.out.println("-".repeat(70));
        System.out.println("Valores de referência para estudo: confira a cotação do dia antes de usar de verdade.");
    }

    /** Duas casas e separador de milhar: 1.234,56. */
    static String valorFormatado(double valor) {
        return String.format(PT_BR, "%,.2f", valor);
    }

    /** Aceita "250.50" e "250,50": troca a vírgula por ponto antes de converter. */
    static double numero(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java ConversorMoedas.java <valor> <de> <para>
                 java ConversorMoedas.java --taxas     mostra a tabela de taxas e a data dela
            Exemplos:
              java ConversorMoedas.java 100 USD BRL
              java ConversorMoedas.java 250,50 EUR JPY
              java ConversorMoedas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Leia a tabela de um arquivo CSV (código;nome;símbolo;taxa) em vez de deixá-la no código.
 *  2. Aplique uma taxa de serviço opcional (ex.: 2%) e mostre quanto ela custou.
 *  3. Imprima uma matriz com todas as moedas contra todas, usando dois laços aninhados.
 */
