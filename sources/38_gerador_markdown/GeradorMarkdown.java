/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  38. Gerador de relatório em Markdown
 * ------------------------------------------------------------
 * O que faz: monta um documento Markdown completo (títulos, parágrafo, tabela
 *            alinhada, listas e bloco de código) a partir de dados estruturados
 *            e também converte qualquer CSV numa tabela Markdown com as colunas
 *            alinhadas (números à direita, texto à esquerda).
 * Como usar: java GeradorMarkdown.java tabela vendas.csv
 *            java GeradorMarkdown.java relatorio vendas.csv --titulo "Vendas de março"
 *            java GeradorMarkdown.java relatorio vendas.csv --destino relatorio.md
 *            java GeradorMarkdown.java --demo
 * O que você aprende: uma classe construtora (builder) com métodos encadeados,
 *            StringBuilder, text blocks, cálculo de largura de colunas, leitura
 *            de CSV respeitando aspas, streams com mapToDouble e summaryStatistics
 *            e formatação de datas com DateTimeFormatter em pt-BR.
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GeradorMarkdown {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Uma tabela é só um cabeçalho e linhas de células; a conversão para Markdown fica no método toMarkdown. */
    record Tabela(List<String> cabecalho, List<List<String>> linhas) {
        String toMarkdown() {
            int colunas = cabecalho.size();
            int[] largura = new int[colunas];
            boolean[] numerica = new boolean[colunas];
            for (int c = 0; c < colunas; c++) {
                largura[c] = cabecalho.get(c).length();
                numerica[c] = !linhas.isEmpty();
                for (List<String> l : linhas) {
                    largura[c] = Math.max(largura[c], l.get(c).length());
                    numerica[c] &= l.get(c).matches("-?[\\d.]+(,\\d+)?%?"); // se uma célula não for número, a coluna vira texto
                }
            }
            StringBuilder sb = new StringBuilder(linha(cabecalho, largura, numerica));
            sb.append('|');
            for (int c = 0; c < colunas; c++) sb.append(numerica[c] ? " " + "-".repeat(largura[c]) + ":|" : ":" + "-".repeat(largura[c]) + " |");
            sb.append('\n');
            for (List<String> l : linhas) sb.append(linha(l, largura, numerica));
            return sb.toString();
        }
        static String linha(List<String> celulas, int[] largura, boolean[] numerica) {
            StringBuilder sb = new StringBuilder("|");
            for (int c = 0; c < celulas.size(); c++) {
                String texto = celulas.get(c).replace("|", "\\|"); // a barra vertical é o separador do Markdown
                sb.append(' ').append(String.format(numerica[c] ? "%" + largura[c] + "s" : "%-" + largura[c] + "s", texto)).append(" |");
            }
            return sb.append('\n').toString();
        }
    }

    /** Construtor de documento: cada método acrescenta um bloco e devolve o próprio objeto para encadear chamadas. */
    static final class Documento {
        private final StringBuilder sb = new StringBuilder();
        Documento titulo(int nivel, String texto) { sb.append("#".repeat(nivel)).append(' ').append(texto).append("\n\n"); return this; }
        Documento paragrafo(String texto) { sb.append(texto).append("\n\n"); return this; }
        Documento lista(List<String> itens) { itens.forEach(i -> sb.append("- ").append(i).append('\n')); sb.append('\n'); return this; }
        Documento listaNumerada(List<String> itens) {
            for (int i = 0; i < itens.size(); i++) sb.append(i + 1).append(". ").append(itens.get(i)).append('\n');
            sb.append('\n');
            return this;
        }
        Documento tabela(Tabela t) { sb.append(t.toMarkdown()).append('\n'); return this; }
        Documento codigo(String linguagem, String texto) { sb.append("```").append(linguagem).append('\n').append(texto.stripTrailing()).append("\n```\n\n"); return this; }
        Documento separador() { sb.append("---\n\n"); return this; }
        @Override public String toString() { return sb.toString().stripTrailing() + "\n"; }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            if (args.length < 2) throw new IllegalArgumentException("use: <tabela|relatorio> <arquivo.csv> [--titulo texto] [--destino arquivo.md]");
            Path csv = Path.of(args[1]);
            if (!Files.isRegularFile(csv)) throw new IllegalArgumentException("o arquivo '" + args[1] + "' não existe");
            String titulo = "Relatório de " + csv.getFileName(), destino = null;
            for (int i = 2; i < args.length; i += 2) {
                if (i + 1 >= args.length) throw new IllegalArgumentException("a opção " + args[i] + " precisa de um valor");
                switch (args[i]) {
                    case "--titulo" -> titulo = args[i + 1];
                    case "--destino" -> destino = args[i + 1];
                    default -> throw new IllegalArgumentException("opção desconhecida '" + args[i] + "'; use --titulo ou --destino");
                }
            }
            Tabela tabela = lerCsv(Files.readString(csv, StandardCharsets.UTF_8));
            String markdown = switch (args[0]) {
                case "tabela" -> tabela.toMarkdown();
                case "relatorio" -> relatorio(titulo, csv.getFileName().toString(), tabela).toString();
                default -> throw new IllegalArgumentException("comando desconhecido '" + args[0] + "'; use tabela ou relatorio");
            };
            if (destino == null) System.out.print(markdown);
            else { Files.writeString(Path.of(destino), markdown, StandardCharsets.UTF_8); System.out.println("Markdown gravado em " + destino); }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    static void demo() {
        System.out.println("=== Demonstração 1: documento montado a partir de dados em memória ===\n");
        record Aluno(String nome, double nota1, double nota2, int faltas) { double media() { return (nota1 + nota2) / 2; } }
        List<Aluno> turma = List.of(new Aluno("Ana Beatriz", 8.5, 9.0, 1), new Aluno("Bruno Carvalho", 5.0, 6.5, 4),
                new Aluno("Carla Mendes", 9.5, 10.0, 0), new Aluno("Diego Ramos", 6.0, 4.5, 7), new Aluno("Fernanda Lima", 7.0, 8.0, 2));
        List<List<String>> linhas = turma.stream().map(a -> List.of(a.nome(), nota(a.nota1()), nota(a.nota2()), nota(a.media()),
                String.valueOf(a.faltas()), a.media() >= 7 ? "aprovado" : "recuperação")).toList();
        Documento doc = new Documento()
                .titulo(1, "Turma de Java: fechamento do semestre")
                .paragrafo("Relatório gerado em " + LocalDate.of(2026, 6, 30).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR)) + ".")
                .titulo(2, "Notas")
                .tabela(new Tabela(List.of("Aluno", "Nota 1", "Nota 2", "Média", "Faltas", "Situação"), linhas))
                .titulo(2, "Aprovados")
                .lista(turma.stream().filter(a -> a.media() >= 7).map(a -> a.nome() + " (média " + nota(a.media()) + ")").toList())
                .titulo(2, "Próximos passos")
                .listaNumerada(List.of("Publicar as notas no mural", "Marcar a prova de recuperação", "Enviar o relatório à coordenação"))
                .titulo(2, "Como este relatório foi gerado")
                .codigo("bash", "java GeradorMarkdown.java relatorio notas.csv --titulo \"Turma de Java\"")
                .separador()
                .paragrafo("Alunos com mais de 5 faltas precisam justificar a ausência com atestado.");
        System.out.print(doc);
        System.out.println("\n=== Demonstração 2: CSV convertido em tabela alinhada ===\n");
        String csv = """
            produto,categoria,quantidade,preço
            Caderno 200 folhas,Papelaria,12,18.90
            "Fone, com fio",Eletrônicos,3,149.90
            Café 500g,Alimentos,40,24.50
            """;
        Tabela tabela = lerCsv(csv);
        System.out.print(tabela.toMarkdown());
        System.out.println("\n=== Demonstração 3: relatório automático do mesmo CSV ===\n");
        System.out.print(relatorio("Estoque da loja", "estoque.csv", tabela));
        System.out.println("\n=== Demonstração 4: CSV com linha quebrada ===\n");
        try { lerCsv("a,b,c\n1,2,3\n4,5\n"); }
        catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
    }

    /** Relatório genérico para qualquer CSV: resumo, tabela, estatísticas das colunas numéricas e o começo do arquivo cru. */
    static Documento relatorio(String titulo, String nomeArquivo, Tabela t) {
        List<String> estatisticas = new ArrayList<>();
        for (int c = 0; c < t.cabecalho().size(); c++) {
            int col = c;
            List<String> celulas = t.linhas().stream().map(l -> l.get(col)).toList();
            if (celulas.stream().anyMatch(v -> !v.matches("-?\\d+([.,]\\d+)?"))) continue; // basta uma célula sem número
            // summaryStatistics dá contagem, soma, mínimo, máximo e média numa passada só.
            DoubleSummaryStatistics st = celulas.stream().mapToDouble(v -> Double.parseDouble(v.replace(',', '.'))).summaryStatistics();
            estatisticas.add(String.format("**%s**: soma %s, média %s, mínimo %s, máximo %s", t.cabecalho().get(c),
                    numero(st.getSum()), numero(st.getAverage()), numero(st.getMin()), numero(st.getMax())));
        }
        String cru = t.linhas().stream().limit(3).map(GeradorMarkdown::linhaCsv).collect(Collectors.joining("\n", linhaCsv(t.cabecalho()) + "\n", ""));
        return new Documento()
                .titulo(1, titulo)
                .paragrafo("Fonte: `" + nomeArquivo + "`, com " + t.linhas().size() + " linha(s) e " + t.cabecalho().size() + " coluna(s).")
                .titulo(2, "Dados")
                .tabela(t)
                .titulo(2, "Colunas numéricas")
                .lista(estatisticas.isEmpty() ? List.of("Nenhuma coluna inteiramente numérica.") : estatisticas)
                .titulo(2, "Primeiras linhas em CSV")
                .codigo("csv", cru);
    }

    /** Lê o CSV inteiro: a primeira linha é o cabeçalho e cada linha seguinte precisa ter o mesmo número de campos. */
    static Tabela lerCsv(String texto) {
        List<String> brutas = texto.lines().filter(l -> !l.isBlank()).toList();
        if (brutas.isEmpty()) throw new IllegalArgumentException("o CSV está vazio");
        List<String> cabecalho = dividir(brutas.get(0));
        List<List<String>> linhas = new ArrayList<>();
        for (int n = 1; n < brutas.size(); n++) {
            List<String> campos = dividir(brutas.get(n));
            if (campos.size() != cabecalho.size()) throw new IllegalArgumentException("linha " + (n + 1) + " tem " + campos.size() + " campo(s), mas o cabeçalho tem " + cabecalho.size());
            linhas.add(campos);
        }
        return new Tabela(cabecalho, linhas);
    }

    /** O inverso de dividir: campo com vírgula volta entre aspas. */
    static String linhaCsv(List<String> campos) {
        return campos.stream().map(v -> v.contains(",") ? "\"" + v + "\"" : v).collect(Collectors.joining(","));
    }

    /** Divide uma linha CSV respeitando aspas: "Fone, com fio" é um campo só. */
    static List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (char c : linha.toCharArray()) {
            if (c == '"') entreAspas = !entreAspas;
            else if (c == ',' && !entreAspas) { campos.add(atual.toString().strip()); atual.setLength(0); }
            else atual.append(c);
        }
        campos.add(atual.toString().strip());
        return campos;
    }

    static String nota(double v) { return String.format(PT_BR, "%.1f", v); }

    /** Inteiros aparecem sem casas decimais; os demais com duas e vírgula, como no Brasil. */
    static String numero(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(PT_BR, "%.2f", v);
    }

    static void uso() {
        System.out.println("""
            Uso: java GeradorMarkdown.java tabela <arquivo.csv>
                 java GeradorMarkdown.java relatorio <arquivo.csv> [--titulo "texto"] [--destino arquivo.md]
              tabela      converte o CSV numa tabela Markdown alinhada (números à direita)
              relatorio   gera um documento com título, resumo, tabela, estatísticas e bloco de código
            Demonstração: java GeradorMarkdown.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente ao Documento o método imagem(alt, caminho) e o método link(texto, url).
 *  2. Aceite --ordenar <coluna> no comando tabela e ordene as linhas antes de montar o Markdown.
 *  3. Converta o Markdown gerado em HTML simples, trocando "# " por <h1>, "- " por <li> e assim por diante.
 */
