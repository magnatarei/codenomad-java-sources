/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  09. Calculadora de datas
 * ------------------------------------------------------------
 * O que faz: dias entre duas datas, dia da semana de uma data, idade exata
 *            em anos, meses e dias, somar ou subtrair dias e quantos dias
 *            faltam para uma data. Aceita datas como 25/12/2026 ou 2026-12-25
 *            e recusa as que não existem, como 31/02/2026.
 * Como usar: java CalculadoraDatas.java entre 01/01/2026 25/12/2026
 *            java CalculadoraDatas.java dia-semana 15/11/1889
 *            java CalculadoraDatas.java idade 20/05/1990
 *            java CalculadoraDatas.java somar 31/01/2026 30
 *            java CalculadoraDatas.java subtrair 01/03/2024 1
 *            java CalculadoraDatas.java faltam 25/12/2026
 *            java CalculadoraDatas.java --demo
 * O que você aprende: java.time (LocalDate, Period, ChronoUnit, DayOfWeek),
 *            DateTimeFormatter com ResolverStyle.STRICT, nomes em português
 *            com Locale, switch expression com yield e vários rótulos por
 *            caso, e tratamento de DateTimeParseException.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class CalculadoraDatas {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    // "uuuu" é o ano sem sinal de era; com STRICT, 31/02 é recusado em vez de virar 03/03.
    static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

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
            System.out.println(executar(args, LocalDate.now()));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() {
        // Uma data fixa como "hoje" deixa a demonstração igual em qualquer dia que você rodar.
        LocalDate hoje = LocalDate.of(2026, 10, 27);
        System.out.println("=== Demonstração (considerando hoje = " + br(hoje) + ") ===");
        System.out.println();
        String[][] comandos = {
                {"entre", "01/01/2026", "25/12/2026"},
                {"entre", "2024-02-28", "2024-03-01"},
                {"dia-semana", "15/11/1889"},
                {"idade", "20/05/1990", br(hoje)},
                {"idade", "29/02/2000", br(hoje)},
                {"somar", "31/01/2026", "30"},
                {"subtrair", "01/03/2024", "1"},
                {"faltam", "25/12/2026", br(hoje)},
                {"faltam", "01/01/2026", br(hoje)},
        };
        for (String[] c : comandos) {
            System.out.println("> " + String.join(" ", c));
            System.out.println("  " + executar(c, hoje));
        }
        System.out.println("> entre 31/02/2026 01/03/2026");
        try {
            executar(new String[]{"entre", "31/02/2026", "01/03/2026"}, hoje);
        } catch (IllegalArgumentException e) {
            System.out.println("  Erro tratado: " + e.getMessage());
        }
    }

    /** Escolhe a operação pelo primeiro argumento. O yield devolve o valor de um bloco no switch. */
    static String executar(String[] args, LocalDate hoje) {
        return switch (args[0].toLowerCase()) {
            case "entre" -> {
                exigir(args, 3, "entre <data1> <data2>");
                yield entre(data(args[1]), data(args[2]));
            }
            case "dia-semana", "dia" -> {
                exigir(args, 2, "dia-semana <data>");
                yield "Essa data cai em " + porExtenso(data(args[1])) + ".";
            }
            case "idade" -> {
                exigir(args, 2, 3, "idade <nascimento> [hoje]");
                yield idade(data(args[1]), args.length == 3 ? data(args[2]) : hoje);
            }
            case "somar" -> {
                exigir(args, 3, "somar <data> <dias>");
                yield somar(data(args[1]), inteiro(args[2]));
            }
            case "subtrair" -> {
                exigir(args, 3, "subtrair <data> <dias>");
                yield somar(data(args[1]), -inteiro(args[2]));
            }
            case "faltam" -> {
                exigir(args, 2, 3, "faltam <data> [hoje]");
                yield faltam(data(args[1]), args.length == 3 ? data(args[2]) : hoje);
            }
            default -> throw new IllegalArgumentException("Comando desconhecido '" + args[0] + "'.");
        };
    }

    static String entre(LocalDate a, LocalDate b) {
        // Garantimos a ordem para que dias e Period saiam sempre positivos.
        LocalDate inicio = a.isBefore(b) ? a : b;
        LocalDate fim = a.isBefore(b) ? b : a;
        long dias = ChronoUnit.DAYS.between(inicio, fim);
        Period p = Period.between(inicio, fim);
        return String.format("De %s a %s: %s (%s e %s), ou %s, %s e %s.",
                br(inicio), br(fim), plural(dias, "dia"), plural(dias / 7, "semana"), plural(dias % 7, "dia"),
                plural(p.getYears(), "ano"), plural(p.getMonths(), "mês", "meses"), plural(p.getDays(), "dia"));
    }

    static String idade(LocalDate nascimento, LocalDate hoje) {
        if (nascimento.isAfter(hoje)) throw new IllegalArgumentException("A data de nascimento " + br(nascimento) + " é depois de hoje (" + br(hoje) + ").");
        Period p = Period.between(nascimento, hoje);
        // withYear ajusta 29/02 para 28/02 nos anos que não são bissextos.
        LocalDate aniversario = nascimento.withYear(hoje.getYear());
        if (aniversario.isBefore(hoje)) aniversario = aniversario.plusYears(1);
        long faltam = ChronoUnit.DAYS.between(hoje, aniversario);
        String proximo = faltam == 0 ? "O aniversário é hoje!" : faltamTexto(faltam) + " para o próximo aniversário (" + br(aniversario) + ").";
        return String.format("Nascido em %s: %s, %s e %s. %s", porExtenso(nascimento),
                plural(p.getYears(), "ano"), plural(p.getMonths(), "mês", "meses"), plural(p.getDays(), "dia"), proximo);
    }

    static String somar(LocalDate data, long dias) {
        LocalDate resultado = data.plusDays(dias);
        String sinal = dias < 0 ? "- " + plural(-dias, "dia") : "+ " + plural(dias, "dia");
        return String.format("%s %s = %s.", br(data), sinal, porExtenso(resultado));
    }

    static String faltam(LocalDate alvo, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, alvo);
        if (dias == 0) return "É hoje: " + porExtenso(alvo) + ".";
        if (dias < 0) return String.format("%s já passou: foi há %s.", br(alvo), plural(-dias, "dia"));
        return String.format("%s para %s.", faltamTexto(dias), porExtenso(alvo));
    }

    /** "1 dia", "2 dias": o substantivo concorda com o número. */
    static String plural(long n, String singular) {
        return plural(n, singular, singular + "s");
    }

    static String plural(long n, String singular, String formaPlural) {
        return n + " " + (n == 1 ? singular : formaPlural);
    }

    static String faltamTexto(long n) {
        return (n == 1 ? "Falta " : "Faltam ") + plural(n, "dia");
    }

    /** Lê "25/12/2026" ou "2026-12-25". Qualquer outra coisa vira uma mensagem clara. */
    static LocalDate data(String texto) {
        try {
            return texto.contains("-") ? LocalDate.parse(texto) : LocalDate.parse(texto, BR);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida '" + texto + "': use dd/MM/aaaa ou aaaa-MM-dd com uma data que exista.");
        }
    }

    static String br(LocalDate d) {
        return d.format(BR);
    }

    /** "terça-feira, 27 de outubro de 2026": os nomes vêm do Locale, não de uma tabela nossa. */
    static String porExtenso(LocalDate d) {
        String diaSemana = d.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        String mes = d.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return String.format("%s, %d de %s de %d", diaSemana, d.getDayOfMonth(), mes, d.getYear());
    }

    static long inteiro(String texto) {
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A quantidade de dias precisa ser um número inteiro, recebi '" + texto + "'.");
        }
    }

    static void exigir(String[] args, int quantidade, String forma) {
        exigir(args, quantidade, quantidade, forma);
    }

    static void exigir(String[] args, int minimo, int maximo, String forma) {
        if (args.length < minimo || args.length > maximo) throw new IllegalArgumentException("Use: " + forma);
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraDatas.java <comando> ...
              entre <data1> <data2>        dias, semanas e anos/meses/dias entre as duas
              dia-semana <data>            em que dia da semana a data cai
              idade <nascimento> [hoje]    idade exata e dias até o próximo aniversário
              somar <data> <dias>          a data que vem <dias> depois
              subtrair <data> <dias>       a data que vem <dias> antes
              faltam <data> [hoje]         quantos dias faltam (ou há quantos passou)
            As datas aceitam dd/MM/aaaa ou aaaa-MM-dd.
            Exemplos:
              java CalculadoraDatas.java entre 01/01/2026 25/12/2026
              java CalculadoraDatas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Conte só os dias úteis entre duas datas, pulando sábados e domingos com DayOfWeek.
 *  2. Liste todas as sextas-feiras 13 de um ano usando um laço com plusMonths.
 *  3. Aceite horas também (LocalDateTime) e mostre a diferença em horas e minutos.
 */
