/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  39. Diário de hábitos com sequência
 * ------------------------------------------------------------
 * O que faz: registra num arquivo de texto quais hábitos você cumpriu em cada
 *            dia, calcula a sequência atual e a maior sequência de cada hábito
 *            e desenha um resumo semanal em grade (uma linha por hábito, uma
 *            coluna por dia da semana).
 * Como usar: java DiarioHabitos.java marcar leitura
 *            java DiarioHabitos.java marcar corrida 2026-10-20
 *            java DiarioHabitos.java desmarcar leitura 2026-10-20
 *            java DiarioHabitos.java resumo --semanas 4
 *            java DiarioHabitos.java --arquivo meus.txt resumo
 *            java DiarioHabitos.java --demo
 * O que você aprende: java.time (LocalDate, DayOfWeek, TemporalAdjusters,
 *            ChronoUnit), TreeMap e TreeSet para dados sempre ordenados,
 *            leitura e escrita de arquivo linha a linha, e como tornar o
 *            programa testável fixando a data de "hoje" por parâmetro.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class DiarioHabitos {

    static final DateTimeFormatter CURTA = DateTimeFormatter.ofPattern("dd/MM");

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        Path arquivo = Path.of("habitos.txt");
        LocalDate hoje = LocalDate.now();
        // As duas opções globais vêm antes do comando; "--hoje" existe para testes e para registrar dias passados.
        while (args.length >= 3 && args[0].startsWith("--")) {
            if (args[0].equals("--arquivo")) arquivo = Path.of(args[1]);
            else if (args[0].equals("--hoje")) hoje = data(args[1]);
            else break;
            args = Arrays.copyOfRange(args, 2, args.length);
        }
        try { System.out.print(executar(arquivo, hoje, args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path arquivo = Files.createTempFile("codenomad_habitos_", ".txt");
        LocalDate hoje = LocalDate.of(2026, 10, 25); // um domingo: a grade fecha a semana nele
        // Três semanas de registros com padrões diferentes, para as sequências ficarem interessantes.
        List<String> linhas = new ArrayList<>();
        for (int d = 20; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            if (d != 9) linhas.add(dia + ";leitura");                               // falhou um dia só
            if (dia.getDayOfWeek().getValue() % 2 == 1) linhas.add(dia + ";exercício"); // segunda, quarta, sexta e domingo
            if (d >= 6) linhas.add(dia + ";água");                                  // parou há seis dias
        }
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
        System.out.println("=== Demonstração: diário em " + arquivo + " (hoje = " + hoje + ") ===");
        String[][] comandos = {
                {"habitos"}, {"marcar", "meditação"}, {"marcar", "meditação", "2026-10-24"}, {"marcar", "meditação"},
                {"desmarcar", "exercício", "2026-10-25"}, {"resumo", "--semanas", "3"}, {"marcar", "leitura", "2026-13-01"},
                {"marcar", "leitura", "2026-10-30"}, {"desmarcar", "yoga"},
        };
        for (String[] c : comandos) {
            System.out.println("\n> DiarioHabitos " + String.join(" ", c));
            try { System.out.print(executar(arquivo, hoje, c)); }
            catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        }
        Files.delete(arquivo);
    }

    static String executar(Path arquivo, LocalDate hoje, String[] args) throws IOException {
        Map<String, TreeSet<LocalDate>> diario = carregar(arquivo);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "marcar", "desmarcar" -> {
                if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("use: " + args[0] + " <hábito> [aaaa-mm-dd]");
                String habito = args[1].toLowerCase(Locale.ROOT);
                LocalDate dia = args.length == 3 ? data(args[2]) : hoje;
                if (dia.isAfter(hoje)) throw new IllegalArgumentException("não dá para registrar o futuro (" + dia + " é depois de " + hoje + ")");
                boolean marcar = args[0].equalsIgnoreCase("marcar");
                if (marcar && !diario.computeIfAbsent(habito, k -> new TreeSet<>()).add(dia)) return "'" + habito + "' já estava marcado em " + dia + ".\n";
                if (!marcar && (!diario.containsKey(habito) || !diario.get(habito).remove(dia))) throw new IllegalArgumentException("'" + habito + "' não estava marcado em " + dia);
                if (!marcar && diario.get(habito).isEmpty()) diario.remove(habito);
                salvar(arquivo, diario);
                return (marcar ? "Marcado: " : "Desmarcado: ") + habito + " em " + dia + ". Sequência atual: " + sequenciaAtual(diario.getOrDefault(habito, new TreeSet<>()), hoje) + " dia(s).\n";
            }
            case "habitos" -> {
                if (diario.isEmpty()) return "Nenhum hábito registrado ainda.\n";
                StringBuilder sb = new StringBuilder();
                diario.forEach((h, dias) -> sb.append(String.format("  %-12s %3d registro(s), desde %s%n", h, dias.size(), dias.first())));
                return sb.toString();
            }
            case "resumo" -> {
                int semanas = 3;
                if (args.length == 3 && args[1].equals("--semanas")) {
                    try { semanas = Integer.parseInt(args[2]); } catch (NumberFormatException e) { throw new IllegalArgumentException("--semanas precisa de um número inteiro, recebi '" + args[2] + "'"); }
                    if (semanas < 1 || semanas > 12) throw new IllegalArgumentException("--semanas precisa estar entre 1 e 12");
                } else if (args.length != 1) throw new IllegalArgumentException("use: resumo [--semanas N]");
                return resumo(diario, hoje, semanas);
            }
            default -> throw new IllegalArgumentException("comando desconhecido '" + args[0] + "'. Use marcar, desmarcar, habitos ou resumo.");
        }
    }

    /** Conta para trás a partir de hoje (ou de ontem, se hoje ainda não foi marcado). */
    static int sequenciaAtual(TreeSet<LocalDate> dias, LocalDate hoje) {
        LocalDate dia = dias.contains(hoje) ? hoje : hoje.minusDays(1);
        int n = 0;
        while (dias.contains(dia)) { n++; dia = dia.minusDays(1); }
        return n;
    }

    /** Percorre os dias em ordem; se a diferença para o anterior for 1 dia, a corrida continua, senão recomeça. */
    static int maiorSequencia(TreeSet<LocalDate> dias) {
        int maior = 0, atual = 0;
        LocalDate anterior = null;
        for (LocalDate dia : dias) {
            atual = anterior != null && ChronoUnit.DAYS.between(anterior, dia) == 1 ? atual + 1 : 1;
            maior = Math.max(maior, atual);
            anterior = dia;
        }
        return maior;
    }

    static String resumo(Map<String, TreeSet<LocalDate>> diario, LocalDate hoje, int semanas) {
        if (diario.isEmpty()) return "Nenhum hábito registrado ainda.\n";
        StringBuilder sb = new StringBuilder(String.format("%-12s %10s %10s %8s%n", "HÁBITO", "ATUAL", "RECORDE", "TOTAL"));
        diario.forEach((h, dias) -> sb.append(String.format("%-12s %7d dias %7d dias %8d%n", h, sequenciaAtual(dias, hoje), maiorSequencia(dias), dias.size())));
        // A grade: cada semana começa na segunda-feira anterior (ou igual) a hoje, voltando N-1 semanas.
        LocalDate inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(semanas - 1);
        for (int s = 0; s < semanas; s++) {
            LocalDate segunda = inicio.plusWeeks(s);
            sb.append(String.format("%nSemana de %s a %s   S T Q Q S S D%n", CURTA.format(segunda), CURTA.format(segunda.plusDays(6))));
            for (var e : diario.entrySet()) {
                sb.append(String.format("  %-22s", e.getKey()));
                for (int d = 0; d < 7; d++) {
                    LocalDate dia = segunda.plusDays(d);
                    sb.append(' ').append(e.getValue().contains(dia) ? 'X' : dia.isAfter(hoje) ? ' ' : '.');
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    static LocalDate data(String texto) {
        try { return LocalDate.parse(texto); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("data '" + texto + "' inválida; use aaaa-mm-dd, por exemplo 2026-10-20"); }
    }

    /** Cada linha do arquivo é "data;hábito". TreeMap e TreeSet mantêm hábitos e dias em ordem sem esforço. */
    static Map<String, TreeSet<LocalDate>> carregar(Path arquivo) throws IOException {
        Map<String, TreeSet<LocalDate>> diario = new TreeMap<>();
        if (!Files.exists(arquivo)) return diario;
        int n = 0;
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            n++;
            if (linha.isBlank()) continue;
            String[] partes = linha.split(";", 2);
            if (partes.length != 2 || partes[1].isBlank()) throw new IllegalArgumentException("linha " + n + " de " + arquivo + " não está no formato data;hábito: " + linha);
            diario.computeIfAbsent(partes[1].strip().toLowerCase(Locale.ROOT), k -> new TreeSet<>()).add(data(partes[0].strip()));
        }
        return diario;
    }

    static void salvar(Path arquivo, Map<String, TreeSet<LocalDate>> diario) throws IOException {
        List<String> linhas = new ArrayList<>();
        diario.forEach((h, dias) -> dias.forEach(d -> linhas.add(d + ";" + h)));
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }

    static void uso() {
        System.out.println("""
            Uso: java DiarioHabitos.java [--arquivo habitos.txt] [--hoje aaaa-mm-dd] <comando>
              marcar <hábito> [data]       registra o hábito no dia (padrão: hoje)
              desmarcar <hábito> [data]    apaga o registro
              habitos                      lista os hábitos e quantos registros cada um tem
              resumo [--semanas N]         sequência atual, recorde e grade das últimas N semanas (padrão 3)
            Demonstração: java DiarioHabitos.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Mostre a taxa de cumprimento (dias marcados / dias desde o primeiro registro) de cada hábito.
 *  2. Adicione o comando "renomear <antigo> <novo>" preservando todos os registros.
 *  3. Permita hábitos "por semana" (ex.: 3 vezes) e mude o cálculo de sequência para semanas cumpridas.
 */
