/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  08. Calculadora de notas e média ponderada
 * ------------------------------------------------------------
 * O que faz: recebe notas com pesos, calcula a média ponderada, informa a
 *            situação (aprovado, recuperação ou reprovado) e diz quanto o
 *            aluno precisa tirar na próxima prova para passar.
 * Como usar: java CalculadoraNotas.java 8,5:2 7:3 6,5:5
 *            java CalculadoraNotas.java 4:1 5,5:1 6:2 --aprovacao 7 --peso-prova 3 --nome Bruno
 *            java CalculadoraNotas.java --demo
 *            (cada nota vai como nota:peso; sem o peso, vale 1)
 * O que você aprende: leitura de opções nomeadas na linha de comando, records
 *            com validação, enum, streams com mapToDouble e Collectors.joining,
 *            uma conta de álgebra para "quanto falta" e formatação com Locale.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalculadoraNotas {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    enum Situacao {
        APROVADO("Aprovado"), RECUPERACAO("Recuperação"), REPROVADO("Reprovado");

        final String rotulo;

        Situacao(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    record Nota(double valor, double peso) {

        Nota {
            if (valor < 0 || valor > 10) throw new IllegalArgumentException("A nota deve ficar entre 0 e 10, recebi " + bonito(valor) + ".");
            if (peso <= 0) throw new IllegalArgumentException("O peso deve ser maior que zero, recebi " + bonito(peso) + ".");
        }
    }

    /** Tudo o que é preciso para avaliar um aluno: notas, critérios e o peso da próxima prova. */
    record Boletim(String nome, List<Nota> notas, double mediaMinima, double mediaRecuperacao, double pesoProximaProva) {

        Boletim {
            if (notas.isEmpty()) throw new IllegalArgumentException("Informe pelo menos uma nota.");
            if (mediaMinima <= 0 || mediaMinima > 10) throw new IllegalArgumentException("A média mínima deve ficar entre 0 e 10.");
            if (mediaRecuperacao < 0 || mediaRecuperacao >= mediaMinima) throw new IllegalArgumentException("A média de recuperação deve ser menor que a média mínima.");
            if (pesoProximaProva <= 0) throw new IllegalArgumentException("O peso da próxima prova deve ser maior que zero.");
        }

        double somaDosPesos() {
            return notas.stream().mapToDouble(Nota::peso).sum();
        }

        /** Cada nota multiplicada pelo próprio peso, tudo somado. */
        double somaPonderada() {
            return notas.stream().mapToDouble(n -> n.valor() * n.peso()).sum();
        }

        double mediaPonderada() {
            return somaPonderada() / somaDosPesos();
        }

        Situacao situacao() {
            double media = mediaPonderada();
            if (media >= mediaMinima) return Situacao.APROVADO;
            if (media >= mediaRecuperacao) return Situacao.RECUPERACAO;
            return Situacao.REPROVADO;
        }

        /**
         * Nota necessária na próxima prova. Vem de isolar x na desigualdade
         * (S + x * p) / (W + p) >= m, em que S é a soma ponderada, W a soma dos
         * pesos, p o peso da próxima prova e m a média mínima.
         */
        double notaNecessaria() {
            return (mediaMinima * (somaDosPesos() + pesoProximaProva) - somaPonderada()) / pesoProximaProva;
        }

        String conselho() {
            double x = notaNecessaria();
            String peso = String.format(PT_BR, "peso %s", bonito(pesoProximaProva));
            if (x <= 0) return "Já garantiu a aprovação: mesmo um zero na próxima prova (" + peso + ") mantém a média.";
            if (x > 10) return String.format(PT_BR, "Não dá para alcançar a média só com a próxima prova (%s): precisaria de %.2f.", peso, x);
            String situacao = situacao() == Situacao.APROVADO ? "Está aprovado por enquanto, mas" : "Para passar,";
            return String.format(PT_BR, "%s precisa de pelo menos %.2f na próxima prova (%s).", situacao, x, peso);
        }

        String notasPorExtenso() {
            return notas.stream()
                    .map(n -> String.format(PT_BR, "%s (peso %s)", bonito(n.valor()), bonito(n.peso())))
                    .collect(Collectors.joining(", "));
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
            imprimir(List.of(lerArgumentos(args)));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração: três alunos, pesos diferentes, média mínima 7 e recuperação a partir de 5 ===");
        System.out.println("Média ponderada = soma(nota x peso) / soma(pesos). O conselho considera o peso da próxima prova de cada aluno.");
        System.out.println();
        imprimir(List.of(
                new Boletim("Ana", List.of(new Nota(8.5, 2), new Nota(7, 3), new Nota(6.5, 5)), 7, 5, 2),
                new Boletim("Bruno", List.of(new Nota(4, 1), new Nota(5.5, 1), new Nota(6, 2)), 7, 5, 3),
                new Boletim("Carla", List.of(new Nota(3, 2), new Nota(4, 3)), 7, 5, 5)));
    }

    /** Percorre os argumentos: o que começa com "--" é uma opção com valor; o resto é nota:peso. */
    static Boletim lerArgumentos(String[] args) {
        var notas = new ArrayList<Nota>();
        String nome = "Aluno";
        double mediaMinima = 7, mediaRecuperacao = 5, pesoProxima = 1;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (i + 1 >= args.length) throw new IllegalArgumentException("A opção " + arg + " precisa de um valor.");
                String valor = args[++i];  // consome o próximo argumento como valor da opção
                switch (arg) {
                    case "--nome" -> nome = valor;
                    case "--aprovacao" -> mediaMinima = numero(valor, "média mínima");
                    case "--recuperacao" -> mediaRecuperacao = numero(valor, "média de recuperação");
                    case "--peso-prova" -> pesoProxima = numero(valor, "peso da próxima prova");
                    default -> throw new IllegalArgumentException("Opção desconhecida: " + arg);
                }
            } else {
                notas.add(lerNota(arg));
            }
        }
        return new Boletim(nome, notas, mediaMinima, mediaRecuperacao, pesoProxima);
    }

    /** "8,5:2" vira Nota(8.5, 2); "7" sozinho vira Nota(7, 1). */
    static Nota lerNota(String texto) {
        String[] partes = texto.split(":");
        if (partes.length > 2) throw new IllegalArgumentException("Nota inválida '" + texto + "': use nota:peso.");
        double valor = numero(partes[0], "nota");
        double peso = partes.length == 2 ? numero(partes[1], "peso") : 1;
        return new Nota(valor, peso);
    }

    static void imprimir(List<Boletim> boletins) {
        System.out.printf("%-8s | %-44s | %6s | %-12s%n", "Aluno", "Notas", "Média", "Situação");
        System.out.println("-".repeat(80));
        for (Boletim b : boletins) {
            System.out.printf(PT_BR, "%-8s | %-44s | %6.2f | %-12s%n", b.nome(), b.notasPorExtenso(), b.mediaPonderada(), b.situacao().rotulo);
        }
        System.out.println("-".repeat(80));
        System.out.println();
        for (Boletim b : boletins) {
            System.out.println(b.nome() + ": " + b.conselho());
        }
    }

    /** "8,5" para 8.5 e "7" para 7: inteiros saem sem vírgula. */
    static String bonito(double valor) {
        return valor == Math.rint(valor) ? String.format(PT_BR, "%.0f", valor) : String.format(PT_BR, "%.2f", valor).replaceAll("0$", "");
    }

    /** Aceita "8.5" e "8,5": troca a vírgula por ponto antes de converter. */
    static double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo '" + campo + "' precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraNotas.java nota:peso [nota:peso ...] [opções]
              --nome <texto>          nome do aluno (opcional)
              --aprovacao <n>         média mínima para aprovação (padrão 7)
              --recuperacao <n>       média mínima para recuperação (padrão 5)
              --peso-prova <n>        peso da próxima prova, para calcular quanto falta (padrão 1)
            Exemplos:
              java CalculadoraNotas.java 8,5:2 7:3 6,5:5
              java CalculadoraNotas.java 4:1 5,5:1 6:2 --aprovacao 7 --peso-prova 3 --nome Bruno
              java CalculadoraNotas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Leia uma turma inteira de um arquivo (nome;nota:peso;nota:peso...) e ordene pela média.
 *  2. Mostre a maior e a menor nota de cada aluno usando Collections.max e min com Comparator.
 *  3. Calcule também a média simples e mostre a diferença para a ponderada.
 */
