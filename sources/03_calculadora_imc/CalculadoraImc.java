/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  03. Calculadora de IMC
 * ------------------------------------------------------------
 * O que faz: recebe peso e altura, valida os dois, e devolve o IMC, a
 *            classificação oficial (de "abaixo do peso" a "obesidade grau III")
 *            e a faixa de peso considerada saudável para aquela altura.
 * Como usar: java CalculadoraImc.java 70 1.75
 *            java CalculadoraImc.java 82,5 168 Ana
 *            java CalculadoraImc.java --demo
 * O que você aprende: enum com limites e busca por faixa, record com métodos,
 *            normalização de entrada (altura em metros ou centímetros),
 *            printf com alinhamento de colunas e validação com exceções.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class CalculadoraImc {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Limites da faixa de peso saudável, segundo a tabela oficial de IMC. */
    static final double IMC_MINIMO_SAUDAVEL = 18.5;
    static final double IMC_MAXIMO_SAUDAVEL = 24.9;

    /** Cada faixa guarda o nome e o limite superior (exclusivo) do IMC. */
    enum Faixa {
        ABAIXO_DO_PESO("Abaixo do peso", 18.5),
        PESO_NORMAL("Peso normal", 25.0),
        SOBREPESO("Sobrepeso", 30.0),
        OBESIDADE_1("Obesidade grau I", 35.0),
        OBESIDADE_2("Obesidade grau II", 40.0),
        OBESIDADE_3("Obesidade grau III", Double.POSITIVE_INFINITY);

        final String nome;
        final double limiteSuperior;

        Faixa(String nome, double limiteSuperior) {
            this.nome = nome;
            this.limiteSuperior = limiteSuperior;
        }

        /** Percorre as faixas em ordem e devolve a primeira cujo limite ainda não foi passado. */
        static Faixa de(double imc) {
            for (Faixa f : values()) {
                if (imc < f.limiteSuperior) return f;
            }
            return OBESIDADE_3;
        }
    }

    /** Uma pessoa com os dados já validados e normalizados. */
    record Pessoa(String nome, double pesoKg, double alturaM) {

        Pessoa {
            if (pesoKg < 2 || pesoKg > 500) throw new IllegalArgumentException(String.format(PT_BR, "O peso deve ficar entre 2 e 500 kg, recebi %.1f.", pesoKg));
            if (alturaM < 0.5 || alturaM > 2.7) throw new IllegalArgumentException(String.format(PT_BR, "A altura deve ficar entre 0,50 m e 2,70 m, recebi %.2f.", alturaM));
            if (nome == null || nome.isBlank()) nome = "Pessoa";
        }

        double imc() {
            return pesoKg / (alturaM * alturaM);
        }

        Faixa faixa() {
            return Faixa.de(imc());
        }

        /** Invertendo a fórmula do IMC: peso = IMC x altura ao quadrado. */
        double pesoMinimoSaudavel() {
            return IMC_MINIMO_SAUDAVEL * alturaM * alturaM;
        }

        double pesoMaximoSaudavel() {
            return IMC_MAXIMO_SAUDAVEL * alturaM * alturaM;
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
            if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("Informe peso e altura (e, se quiser, um nome).");
            double peso = numero(args[0], "peso");
            double altura = normalizarAltura(numero(args[1], "altura"));
            String nome = args.length == 3 ? args[2] : "Pessoa";
            imprimirTabela(List.of(new Pessoa(nome, peso, altura)));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: IMC de três pessoas com perfis diferentes ===");
        System.out.println("Faixas: abaixo do peso até 18,4 | normal de 18,5 a 24,9 | sobrepeso de 25 a 29,9 | obesidade a partir de 30");
        System.out.println();
        imprimirTabela(List.of(
                new Pessoa("Ana", 52.0, 1.70),
                new Pessoa("Bruno", 95.5, 1.78),
                new Pessoa("Carla", 64.0, 1.62)));
    }

    /** Quem digita "175" quis dizer centímetros: acima de 3 convertemos para metros. */
    static double normalizarAltura(double altura) {
        return altura > 3 ? altura / 100.0 : altura;
    }

    static void imprimirTabela(List<Pessoa> pessoas) {
        System.out.printf("%-10s | %9s | %8s | %6s | %-20s | %s%n", "Nome", "Peso", "Altura", "IMC", "Classificação", "Faixa saudável");
        System.out.println("-".repeat(90));
        for (Pessoa p : pessoas) {
            System.out.printf(PT_BR, "%-10s | %6.1f kg | %6.2f m | %6.1f | %-20s | %.1f kg a %.1f kg%n",
                    p.nome(), p.pesoKg(), p.alturaM(), p.imc(), p.faixa().nome,
                    p.pesoMinimoSaudavel(), p.pesoMaximoSaudavel());
        }
        System.out.println("-".repeat(90));
        System.out.println();
        for (Pessoa p : pessoas) {
            System.out.println(conselho(p));
        }
    }

    /** Diz quantos quilos separam a pessoa da faixa saudável, ou confirma que ela está dentro. */
    static String conselho(Pessoa p) {
        double abaixo = p.pesoMinimoSaudavel() - p.pesoKg();
        double acima = p.pesoKg() - p.pesoMaximoSaudavel();
        if (abaixo > 0) {
            return String.format(PT_BR, "%s: faltam %.1f kg para chegar ao peso mínimo saudável.", p.nome(), abaixo);
        }
        if (acima > 0) {
            return String.format(PT_BR, "%s: está %.1f kg acima do peso máximo saudável.", p.nome(), acima);
        }
        return String.format(PT_BR, "%s: dentro da faixa saudável, com folga de %.1f kg até o limite.", p.nome(), -acima);
    }

    /** Aceita "1.75" e "1,75": troca a vírgula por ponto antes de converter. */
    static double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo '" + campo + "' precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraImc.java <peso em kg> <altura> [nome]
              peso      em quilos (ex.: 70 ou 82,5)
              altura    em metros (1.75) ou em centímetros (175): o programa entende os dois
              nome      opcional, só aparece na tabela
            Exemplos:
              java CalculadoraImc.java 70 1.75
              java CalculadoraImc.java 82,5 168 Ana
              java CalculadoraImc.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Leia várias pessoas de um arquivo (nome;peso;altura) e imprima a tabela inteira.
 *  2. Mostre quantos quilos faltam ou sobram para a pessoa mudar de faixa.
 *  3. Ordene a tabela pelo IMC usando List.sort com Comparator.comparingDouble.
 */
