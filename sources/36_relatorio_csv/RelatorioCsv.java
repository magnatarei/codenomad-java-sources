/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  36. Relatório de vendas a partir de CSV
 * ------------------------------------------------------------
 * O que faz: lê um CSV de vendas (data, produto, categoria, quantidade, valor
 *            unitário), ignora linhas quebradas avisando qual é o problema,
 *            e imprime um relatório alinhado: faturamento, ticket médio,
 *            totais por categoria e por mês, ranking de produtos e o mais
 *            vendido. Também gera um CSV de exemplo para você experimentar.
 * Como usar: java RelatorioCsv.java vendas.csv
 *            java RelatorioCsv.java vendas.csv --top 3
 *            java RelatorioCsv.java --exemplo vendas.csv 100
 *            java RelatorioCsv.java --demo
 * O que você aprende: leitura de CSV com validação linha a linha, records,
 *            streams com groupingBy e summingDouble, TreeMap para ordenar por
 *            chave, YearMonth, formatação com Locale pt-BR (R$ 1.234,56) e
 *            Random com semente fixa para dados reproduzíveis.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RelatorioCsv {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final String CABECALHO = "data,produto,categoria,quantidade,valor";

    record Venda(LocalDate data, String produto, String categoria, int quantidade, double valor) {
        double total() { return quantidade * valor; }
    }

    /** Produtos fictícios usados pelo gerador de exemplo: nome, categoria e preço unitário. */
    record Produto(String nome, String categoria, double preco) {}
    static final List<Produto> CATALOGO = List.of(
            new Produto("Caneta azul", "Papelaria", 2.50), new Produto("Caderno 200 folhas", "Papelaria", 18.90),
            new Produto("Fone Bluetooth", "Eletrônicos", 149.90), new Produto("Mouse sem fio", "Eletrônicos", 79.90),
            new Produto("Camiseta básica", "Vestuário", 39.90), new Produto("Meia (par)", "Vestuário", 12.00),
            new Produto("Café 500g", "Alimentos", 24.50), new Produto("Chocolate 70%", "Alimentos", 9.90));

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args[0].equals("--exemplo")) {
                if (args.length < 2) throw new IllegalArgumentException("use: --exemplo <destino.csv> [quantidade]");
                int quantidade = args.length > 2 ? inteiro(args[2], "quantidade") : 60;
                Files.writeString(Path.of(args[1]), gerarExemplo(quantidade), StandardCharsets.UTF_8);
                System.out.println("CSV de exemplo com " + quantidade + " vendas gravado em " + args[1]);
                return;
            }
            int top = args.length == 3 && args[1].equals("--top") ? inteiro(args[2], "--top") : 5;
            System.out.print(relatorio(Path.of(args[0]), top));
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    static void demo() throws IOException {
        Path csv = Files.createTempFile("codenomad_vendas_", ".csv");
        // Duas linhas propositalmente erradas no fim, para mostrar como o leitor reage.
        Files.writeString(csv, gerarExemplo(60) + "2026-02-30,Caneta azul,Papelaria,2,2.50\n2026-03-10,Café 500g,Alimentos,muitos,24.50\n", StandardCharsets.UTF_8);
        System.out.println("=== Demonstração: CSV de exemplo com 60 vendas em " + csv + " ===");
        System.out.println("Primeiras linhas do arquivo:");
        Files.readAllLines(csv, StandardCharsets.UTF_8).stream().limit(4).forEach(l -> System.out.println("  | " + l));
        System.out.println();
        System.out.print(relatorio(csv, 5));
        Files.delete(csv);
    }

    /** Random com semente fixa: os mesmos 60 registros toda vez, o que facilita conferir o relatório. */
    static String gerarExemplo(int quantidade) {
        Random sorteio = new Random(42);
        List<Venda> vendas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            Produto p = CATALOGO.get(sorteio.nextInt(CATALOGO.size()));
            vendas.add(new Venda(LocalDate.of(2026, 1, 1).plusDays(sorteio.nextInt(90)), p.nome(), p.categoria(), 1 + sorteio.nextInt(5), p.preco()));
        }
        vendas.sort(Comparator.comparing(Venda::data));
        return vendas.stream()
                .map(v -> String.format(Locale.ROOT, "%s,%s,%s,%d,%.2f", v.data(), v.produto(), v.categoria(), v.quantidade(), v.valor()))
                .collect(Collectors.joining("\n", CABECALHO + "\n", "\n"));
    }

    static List<Venda> ler(Path arquivo, List<String> problemas) throws IOException {
        if (!Files.exists(arquivo)) throw new IllegalArgumentException("o arquivo '" + arquivo + "' não existe");
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.get(0).strip().equalsIgnoreCase(CABECALHO)) throw new IllegalArgumentException("a primeira linha precisa ser: " + CABECALHO);
        List<Venda> vendas = new ArrayList<>();
        for (int n = 1; n < linhas.size(); n++) {
            String linha = linhas.get(n);
            if (linha.isBlank()) continue;
            String[] c = linha.split(",", -1); // -1 mantém campos vazios no fim, para o erro ser "campo vazio" e não "faltou campo"
            try {
                if (c.length != 5) throw new IllegalArgumentException("esperava 5 campos, achei " + c.length);
                vendas.add(new Venda(LocalDate.parse(c[0].strip()), c[1].strip(), c[2].strip(), inteiro(c[3].strip(), "quantidade"),
                        Double.parseDouble(c[4].strip().replace(',', '.'))));
            } catch (DateTimeParseException e) {
                problemas.add("linha " + (n + 1) + ": data inválida '" + c[0] + "'");
            } catch (NumberFormatException e) {
                problemas.add("linha " + (n + 1) + ": valor inválido '" + c[4] + "'");
            } catch (IllegalArgumentException e) {
                problemas.add("linha " + (n + 1) + ": " + e.getMessage());
            }
        }
        return vendas;
    }

    static String relatorio(Path arquivo, int top) throws IOException {
        List<String> problemas = new ArrayList<>();
        List<Venda> vendas = ler(arquivo, problemas);
        if (vendas.isEmpty()) throw new IllegalArgumentException("nenhuma venda válida em " + arquivo + (problemas.isEmpty() ? "" : " (" + problemas.get(0) + ")"));
        double faturamento = vendas.stream().mapToDouble(Venda::total).sum();
        int itens = vendas.stream().mapToInt(Venda::quantidade).sum();
        LocalDate inicio = vendas.stream().map(Venda::data).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate fim = vendas.stream().map(Venda::data).max(Comparator.naturalOrder()).orElseThrow();
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO DE VENDAS: ").append(arquivo.getFileName()).append('\n');
        sb.append(String.format("Período: %s a %s   |   %d vendas   |   %d itens%n", BR.format(inicio), BR.format(fim), vendas.size(), itens));
        sb.append(String.format("Faturamento: %s   |   Ticket médio: %s%n", reais(faturamento), reais(faturamento / vendas.size())));

        // groupingBy + summingDouble resume o "some por chave"; o TreeMap deixa as chaves em ordem.
        Map<String, Double> porCategoria = vendas.stream().collect(Collectors.groupingBy(Venda::categoria, TreeMap::new, Collectors.summingDouble(Venda::total)));
        sb.append("\nPor categoria\n");
        porCategoria.forEach((cat, total) -> {
            double pct = 100 * total / faturamento;
            sb.append(String.format("  %-14s %14s  %5.1f%%  %s%n", cat, reais(total), pct, "█".repeat((int) Math.round(pct / 4))));
        });

        Map<YearMonth, List<Venda>> porMes = vendas.stream().collect(Collectors.groupingBy(v -> YearMonth.from(v.data()), TreeMap::new, Collectors.toList()));
        sb.append("\nPor mês\n");
        porMes.forEach((mes, lista) -> sb.append(String.format("  %-14s %14s  %3d vendas%n",
                mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + "/" + mes.getYear(), reais(lista.stream().mapToDouble(Venda::total).sum()), lista.size())));

        Map<String, Double> porProduto = vendas.stream().collect(Collectors.groupingBy(Venda::produto, Collectors.summingDouble(Venda::total)));
        sb.append("\nTop ").append(top).append(" produtos por faturamento\n");
        int[] posicao = {1};
        porProduto.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(top)
                .forEach(e -> sb.append(String.format("  %d. %-20s %14s%n", posicao[0]++, e.getKey(), reais(e.getValue()))));

        Map<String, Integer> unidades = vendas.stream().collect(Collectors.groupingBy(Venda::produto, Collectors.summingInt(Venda::quantidade)));
        Map.Entry<String, Integer> campeao = unidades.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        sb.append(String.format("%nProduto mais vendido em unidades: %s (%d unidades)%n", campeao.getKey(), campeao.getValue()));
        if (!problemas.isEmpty()) {
            sb.append("\nLinhas ignoradas: ").append(problemas.size()).append('\n');
            problemas.forEach(p -> sb.append("  ").append(p).append('\n'));
        }
        return sb.toString();
    }

    /** %,.2f com Locale pt-BR usa ponto nos milhares e vírgula nos centavos: R$ 1.234,56. */
    static String reais(double valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    static int inteiro(String texto, String campo) {
        try {
            int n = Integer.parseInt(texto);
            if (n <= 0) throw new IllegalArgumentException(campo + " precisa ser maior que zero, recebi " + n);
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " precisa ser um número inteiro, recebi '" + texto + "'");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java RelatorioCsv.java <vendas.csv> [--top N]
                 java RelatorioCsv.java --exemplo <destino.csv> [quantidade]
            O CSV precisa ter o cabeçalho: data,produto,categoria,quantidade,valor
            (data como 2026-03-15 e valor unitário como 19.90 ou 19,90)
            Demonstração: java RelatorioCsv.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Aceite também datas no formato 15/03/2026, detectando pelo conteúdo do campo.
 *  2. Adicione --categoria X para restringir o relatório a uma categoria.
 *  3. Grave o relatório em um arquivo .txt com --destino e compare o resultado com a tela.
 */
