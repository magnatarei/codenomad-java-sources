/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  07. Calculadora de orçamento mensal
 * ------------------------------------------------------------
 * O que faz: recebe receitas e despesas por categoria, pelos argumentos ou
 *            por um arquivo de texto simples, e mostra o total de cada lado,
 *            o saldo, o percentual de cada categoria sobre a receita, barras
 *            de texto e um alerta para a despesa que mais pesa.
 * Como usar: java CalculadoraOrcamento.java receita:Salário:4500 despesa:Aluguel:1500 despesa:Mercado:900
 *            java CalculadoraOrcamento.java --arquivo orcamento.txt
 *            java CalculadoraOrcamento.java --demo
 *            (no arquivo, uma linha por lançamento: tipo:categoria:valor,
 *            linhas vazias e as que começam com # são ignoradas)
 * O que você aprende: enum com fábrica a partir de texto, records, Map.merge
 *            para somar por categoria, Files.readAllLines, String.split com
 *            limite, streams com mapToDouble e max, String.repeat para barras
 *            e arquivos temporários com Files.createTempFile.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CalculadoraOrcamento {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    enum Tipo {
        RECEITA, DESPESA;

        /** Aceita o nome inteiro ou só a inicial, sem diferenciar maiúsculas. */
        static Tipo de(String texto) {
            return switch (texto.trim().toLowerCase()) {
                case "receita", "r", "entrada" -> RECEITA;
                case "despesa", "d", "gasto" -> DESPESA;
                default -> throw new IllegalArgumentException("Tipo desconhecido '" + texto + "': use receita ou despesa.");
            };
        }
    }

    record Lancamento(Tipo tipo, String categoria, double valor) {

        Lancamento {
            if (categoria == null || categoria.isBlank()) throw new IllegalArgumentException("A categoria não pode ficar vazia.");
            if (valor <= 0) throw new IllegalArgumentException("O valor de '" + categoria + "' deve ser maior que zero.");
        }
    }

    /** Totais por categoria, já somados, separados em dois mapas. */
    record Relatorio(Map<String, Double> receitas, Map<String, Double> despesas) {

        static Relatorio de(List<Lancamento> lancamentos) {
            var receitas = new LinkedHashMap<String, Double>();
            var despesas = new LinkedHashMap<String, Double>();
            for (Lancamento l : lancamentos) {
                // merge soma ao valor que já existe ou cria a chave: substitui o "if (contém) ... else ...".
                Map<String, Double> destino = l.tipo() == Tipo.RECEITA ? receitas : despesas;
                destino.merge(l.categoria(), l.valor(), Double::sum);
            }
            return new Relatorio(receitas, despesas);
        }

        double totalReceitas() {
            return receitas.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        double totalDespesas() {
            return despesas.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        Optional<Map.Entry<String, Double>> despesaMaisPesada() {
            return despesas.entrySet().stream().max(Map.Entry.comparingByValue());
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) {
            uso();
            return;
        }
        try {
            if (args[0].equals("--demo")) {
                demo();
            } else if (args[0].equals("--arquivo")) {
                if (args.length != 2) throw new IllegalArgumentException("Depois de --arquivo informe o caminho do arquivo.");
                imprimir(Relatorio.de(lerArquivo(Path.of(args[1]))));
            } else {
                var lancamentos = new ArrayList<Lancamento>();
                for (String arg : args) lancamentos.add(lerLancamento(arg));
                imprimir(Relatorio.de(lancamentos));
            }
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println();
            uso();
            System.exit(1);
        }
    }

    static void demo() throws IOException {
        System.out.println("=== Demonstração: orçamento de exemplo de um mês ===");
        System.out.println();
        String exemplo = """
            # Orçamento de exemplo (tipo:categoria:valor)
            receita:Salário:4500
            receita:Freelance:1000
            despesa:Aluguel:1500
            despesa:Mercado:900
            despesa:Transporte:350
            despesa:Lazer:400
            despesa:Mercado:150
            despesa:Internet:120
            """;
        // Gravamos o exemplo em um arquivo temporário para exercitar o mesmo caminho do --arquivo.
        Path temporario = Files.createTempFile("orcamento-demo", ".txt");
        try {
            Files.writeString(temporario, exemplo, StandardCharsets.UTF_8);
            List<Lancamento> lancamentos = lerArquivo(temporario);
            System.out.println("Lidos " + lancamentos.size() + " lançamentos do arquivo temporário (Mercado aparece duas vezes e será somado).");
            System.out.println();
            imprimir(Relatorio.de(lancamentos));
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    /** Lê "despesa:Aluguel:1500". O limite 3 no split preserva ":" dentro do valor, se houver. */
    static Lancamento lerLancamento(String texto) {
        String[] partes = texto.split("[:;]", 3);
        if (partes.length != 3) throw new IllegalArgumentException("Lançamento inválido '" + texto + "': use tipo:categoria:valor.");
        return new Lancamento(Tipo.de(partes[0]), partes[1].trim(), numero(partes[2].trim(), partes[1].trim()));
    }

    static List<Lancamento> lerArquivo(Path caminho) throws IOException {
        if (!Files.isRegularFile(caminho)) throw new IllegalArgumentException("Não encontrei o arquivo '" + caminho + "'.");
        var lista = new ArrayList<Lancamento>();
        int numeroDaLinha = 0;
        for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
            numeroDaLinha++;
            String limpa = linha.strip();
            if (limpa.isEmpty() || limpa.startsWith("#")) continue;
            try {
                lista.add(lerLancamento(limpa));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Linha " + numeroDaLinha + ": " + e.getMessage());
            }
        }
        if (lista.isEmpty()) throw new IllegalArgumentException("O arquivo não tem nenhum lançamento.");
        return lista;
    }

    static void imprimir(Relatorio r) {
        double receita = r.totalReceitas();
        double despesa = r.totalDespesas();

        System.out.println("Receitas");
        for (var e : r.receitas().entrySet()) linha(e.getKey(), e.getValue(), receita);
        System.out.printf("  %-18s %14s%n", "Total", moeda(receita));
        System.out.println();
        System.out.println("Despesas (percentual sobre a receita)");
        for (var e : r.despesas().entrySet()) linha(e.getKey(), e.getValue(), receita);
        System.out.printf("  %-18s %14s %6s%n", "Total", moeda(despesa), percentual(despesa, receita));
        System.out.println();

        double saldo = receita - despesa;
        if (saldo >= 0) {
            System.out.println("Saldo do mês: " + moeda(saldo) + " (sobrou " + percentual(saldo, receita) + " da receita)");
        } else {
            System.out.println("Atenção: as despesas passam a receita em " + moeda(-saldo) + ".");
        }
        r.despesaMaisPesada().ifPresent(e -> System.out.println(
                "Alerta: a categoria que mais pesa é \"" + e.getKey() + "\", com " + percentual(e.getValue(), receita) + " da receita."));
        if (receita == 0) System.out.println("Sem receita cadastrada: os percentuais não se aplicam.");
    }

    /** Uma linha da tabela, com a barra: um bloco a cada 5% da receita. */
    static void linha(String categoria, double valor, double receita) {
        double pct = receita == 0 ? 0 : valor / receita * 100;
        int blocos = (int) Math.min(40, Math.round(pct / 5));
        System.out.printf("  %-18s %14s %6s  %s%n", categoria, moeda(valor), percentual(valor, receita), "█".repeat(blocos));
    }

    static String percentual(double parte, double total) {
        return total == 0 ? "n/d" : String.format(PT_BR, "%.1f%%", parte / total * 100);
    }

    static String moeda(double valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    /** Aceita "1500.50" e "1500,50": troca a vírgula por ponto antes de converter. */
    static double numero(String texto, String categoria) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor de '" + categoria + "' precisa ser um número, recebi '" + texto + "'.");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java CalculadoraOrcamento.java tipo:categoria:valor [tipo:categoria:valor ...]
                 java CalculadoraOrcamento.java --arquivo <caminho>
              tipo é "receita" ou "despesa" (ou só r / d); o valor aceita vírgula ou ponto
            Exemplos:
              java CalculadoraOrcamento.java receita:Salário:4500 despesa:Aluguel:1500 despesa:Mercado:900
              java CalculadoraOrcamento.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Ordene as despesas da maior para a menor antes de imprimir (List.sort com Comparator).
 *  2. Aceite um limite por categoria (ex.: Lazer até 10%) e marque as que passaram.
 *  3. Compare dois meses lidos de dois arquivos e mostre a variação de cada categoria.
 */
