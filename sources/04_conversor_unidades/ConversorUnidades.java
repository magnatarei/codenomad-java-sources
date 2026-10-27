/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  04. Conversor de unidades
 * ------------------------------------------------------------
 * O que faz: converte comprimento, massa, temperatura, volume e velocidade
 *            entre quaisquer duas unidades da mesma categoria, usando uma
 *            tabela de fatores. Também lista todas as unidades disponíveis.
 * Como usar: java ConversorUnidades.java 10 km mi
 *            java ConversorUnidades.java 100 C F
 *            java ConversorUnidades.java --unidades
 *            java ConversorUnidades.java --demo
 * O que você aprende: records e List.of como tabela de dados, Optional,
 *            streams com filter e findFirst, switch expression, a ideia de
 *            "unidade base" para converter entre N unidades com N fatores.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ConversorUnidades {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Uma unidade e o fator que a leva para a unidade base da categoria. */
    record Unidade(String simbolo, String nome, double paraBase) {}

    /** Uma categoria agrupa unidades que se convertem entre si. */
    record Categoria(String nome, String base, List<Unidade> unidades) {

        Optional<Unidade> achar(String simbolo) {
            return unidades.stream()
                    .filter(u -> u.simbolo().equalsIgnoreCase(simbolo))
                    .findFirst();
        }
    }

    // Para converter de A para B basta ir até a base e voltar: valor * fatorA / fatorB.
    // Assim a tabela precisa de um fator por unidade, e não de um fator por par.
    static final List<Categoria> CATEGORIAS = List.of(
            new Categoria("Comprimento", "m", List.of(
                    new Unidade("mm", "milímetro", 0.001),
                    new Unidade("cm", "centímetro", 0.01),
                    new Unidade("m", "metro", 1.0),
                    new Unidade("km", "quilômetro", 1000.0),
                    new Unidade("in", "polegada", 0.0254),
                    new Unidade("ft", "pé", 0.3048),
                    new Unidade("yd", "jarda", 0.9144),
                    new Unidade("mi", "milha", 1609.344))),
            new Categoria("Massa", "kg", List.of(
                    new Unidade("mg", "miligrama", 0.000001),
                    new Unidade("g", "grama", 0.001),
                    new Unidade("kg", "quilograma", 1.0),
                    new Unidade("t", "tonelada", 1000.0),
                    new Unidade("oz", "onça", 0.028349523125),
                    new Unidade("lb", "libra", 0.45359237))),
            // Temperatura não usa fator: a conversão tem soma e subtração (veja paraCelsius).
            new Categoria("Temperatura", "C", List.of(
                    new Unidade("C", "grau Celsius", 1.0),
                    new Unidade("F", "grau Fahrenheit", 1.0),
                    new Unidade("K", "kelvin", 1.0))),
            new Categoria("Volume", "l", List.of(
                    new Unidade("ml", "mililitro", 0.001),
                    new Unidade("l", "litro", 1.0),
                    new Unidade("m3", "metro cúbico", 1000.0),
                    new Unidade("gal", "galão americano", 3.785411784),
                    new Unidade("floz", "onça líquida", 0.0295735295625))),
            new Categoria("Velocidade", "m/s", List.of(
                    new Unidade("m/s", "metro por segundo", 1.0),
                    new Unidade("km/h", "quilômetro por hora", 1000.0 / 3600.0),
                    new Unidade("mph", "milha por hora", 1609.344 / 3600.0),
                    new Unidade("kn", "nó", 1852.0 / 3600.0))));

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        switch (args[0]) {
            case "--demo" -> demo();
            case "--unidades" -> listarUnidades();
            default -> {
                try {
                    if (args.length != 3) throw new IllegalArgumentException("Informe valor, unidade de origem e unidade de destino.");
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
        System.out.println("=== Demonstração: doze conversões, uma de cada tipo de uso ===");
        System.out.println();
        double[] valores = {10, 1, 180, 2.5, 16, 1, 100, 32, 2, 1, 100, 30};
        String[] de = {"km", "mi", "cm", "kg", "oz", "t", "C", "F", "l", "m3", "km/h", "kn"};
        String[] para = {"mi", "m", "ft", "lb", "g", "kg", "F", "K", "gal", "l", "mph", "km/h"};
        for (int i = 0; i < valores.length; i++) {
            System.out.println(converterEDescrever(valores[i], de[i], para[i]));
        }
        System.out.println();
        listarUnidades();
    }

    /** Converte e monta a frase "10 km = 6,2137 mi   [Comprimento]". */
    static String converterEDescrever(double valor, String de, String para) {
        Categoria categoria = categoriaDe(de, para);
        double resultado = converter(categoria, valor, de, para);
        return String.format("%s %s = %s %s   [%s]", bonito(valor), de, bonito(resultado), para, categoria.nome());
    }

    /** Inteiros saem sem casas ("10"); os demais com até quatro casas, sem zeros à direita. */
    static String bonito(double valor) {
        if (valor == Math.rint(valor) && Math.abs(valor) < 1e15) return String.format(PT_BR, "%,.0f", valor);
        // As duas trocas tiram os zeros do fim ("1,2500" vira "1,25") e uma vírgula que sobre sozinha.
        return String.format(PT_BR, "%,.4f", valor).replaceAll("0+$", "").replaceAll(",$", "");
    }

    /** Acha a categoria que contém as duas unidades, com mensagens claras quando não acha. */
    static Categoria categoriaDe(String de, String para) {
        Optional<Categoria> origem = CATEGORIAS.stream().filter(c -> c.achar(de).isPresent()).findFirst();
        Optional<Categoria> destino = CATEGORIAS.stream().filter(c -> c.achar(para).isPresent()).findFirst();
        if (origem.isEmpty()) throw new IllegalArgumentException("Não conheço a unidade '" + de + "'. Use --unidades para ver a lista.");
        if (destino.isEmpty()) throw new IllegalArgumentException("Não conheço a unidade '" + para + "'. Use --unidades para ver a lista.");
        if (origem.get() != destino.get()) {
            throw new IllegalArgumentException("Não dá para converter " + de + " em " + para + ": "
                    + origem.get().nome() + " e " + destino.get().nome() + " são categorias diferentes.");
        }
        return origem.get();
    }

    static double converter(Categoria categoria, double valor, String de, String para) {
        if (categoria.nome().equals("Temperatura")) {
            return deCelsius(paraCelsius(valor, de), para);
        }
        // Optional.orElseThrow é seguro aqui: categoriaDe já garantiu que as unidades existem.
        Unidade origem = categoria.achar(de).orElseThrow();
        Unidade destino = categoria.achar(para).orElseThrow();
        return valor * origem.paraBase() / destino.paraBase();
    }

    /** Temperatura passa sempre por Celsius, que faz o papel de unidade base. */
    static double paraCelsius(double valor, String de) {
        return switch (de.toUpperCase()) {
            case "C" -> valor;
            case "F" -> (valor - 32) * 5 / 9;
            case "K" -> valor - 273.15;
            default -> throw new IllegalArgumentException("Temperatura desconhecida: " + de);
        };
    }

    static double deCelsius(double celsius, String para) {
        return switch (para.toUpperCase()) {
            case "C" -> celsius;
            case "F" -> celsius * 9 / 5 + 32;
            case "K" -> celsius + 273.15;
            default -> throw new IllegalArgumentException("Temperatura desconhecida: " + para);
        };
    }

    static void listarUnidades() {
        System.out.println("Unidades disponíveis (o símbolo não diferencia maiúsculas de minúsculas):");
        for (Categoria c : CATEGORIAS) {
            System.out.println();
            System.out.println(c.nome() + " (base: " + c.base() + ")");
            for (Unidade u : c.unidades()) {
                System.out.printf("  %-6s %s%n", u.simbolo(), u.nome());
            }
        }
    }

    /** Aceita "2.5" e "2,5": troca a vírgula por ponto antes de converter. */
    static double numero(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java ConversorUnidades.java <valor> <de> <para>
                 java ConversorUnidades.java --unidades     lista todas as unidades
            Exemplos:
              java ConversorUnidades.java 10 km mi
              java ConversorUnidades.java 2,5 kg lb
              java ConversorUnidades.java 100 C F
              java ConversorUnidades.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente a categoria "Área" (m2, km2, ha, acre) só adicionando linhas na tabela.
 *  2. Aceite o nome por extenso ("quilômetro") além do símbolo, comparando sem acento.
 *  3. Imprima uma tabela cruzada: 1 unidade de origem em todas as outras da categoria.
 */
