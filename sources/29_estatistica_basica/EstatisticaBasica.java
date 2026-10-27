/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  29. Estatística descritiva
 * ------------------------------------------------------------
 * O que faz: calcula média, mediana, moda, variância, desvio padrão, mínimo,
 *            máximo, amplitude e quartis de uma lista de números vinda dos
 *            argumentos ou de um arquivo de texto, e desenha um histograma.
 * Como usar: java EstatisticaBasica.java 7,5 8 6,5 9 10 4 7 8,5 7,5
 *            java EstatisticaBasica.java --arquivo notas.txt --faixas 6
 *            java EstatisticaBasica.java --demo
 * O que você aprende: arrays de double e Arrays.sort, a diferença entre média e
 *            mediana, variância amostral (n - 1), TreeMap para contar
 *            frequências, record com método, leitura de arquivo com
 *            Files.readAllLines, formatação com Locale e String.repeat.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class EstatisticaBasica {

    // Locale fixo para a saída usar sempre vírgula nas casas fracionárias, seja qual for a máquina.
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Tudo o que o resumo calcula, num só lugar. Um record é imutável: calculou, não muda mais. */
    record Resumo(int n, double media, double mediana, List<Double> modas, int frequenciaModa,
                  double variancia, double desvio, double minimo, double maximo,
                  double q1, double q3) {
        double amplitude() { return maximo - minimo; }
        double intervaloInterquartil() { return q3 - q1; }
    }

    static Resumo resumir(double[] dados) {
        if (dados.length < 2) throw new IllegalArgumentException("Preciso de pelo menos 2 números, recebi " + dados.length + ".");
        double[] ord = dados.clone();   // ordenamos uma cópia: quem chamou continua com a ordem original
        Arrays.sort(ord);
        int n = ord.length;

        double soma = 0;
        for (double x : ord) soma += x;
        double media = soma / n;

        // Variância amostral: divide por (n - 1), não por n, porque a média usada
        // veio da própria amostra. É a fórmula que planilhas chamam de VAR.A.
        double somaQuadrados = 0;
        for (double x : ord) somaQuadrados += (x - media) * (x - media);
        double variancia = somaQuadrados / (n - 1);

        // Moda: o valor que mais se repete. Pode haver mais de uma, ou nenhuma (todos aparecem uma vez).
        TreeMap<Double, Integer> frequencia = new TreeMap<>();
        for (double x : ord) frequencia.merge(x, 1, Integer::sum);
        int maiorFrequencia = Collections.max(frequencia.values());
        List<Double> modas = new ArrayList<>();
        if (maiorFrequencia > 1) {
            for (var e : frequencia.entrySet()) if (e.getValue() == maiorFrequencia) modas.add(e.getKey());
        }

        // Quartis pelo método das metades: Q1 é a mediana da metade de baixo e Q3 da metade
        // de cima. Quando n é ímpar, o valor central fica de fora das duas metades.
        double q1 = mediana(ord, 0, n / 2);
        double q3 = mediana(ord, (n + 1) / 2, n);

        return new Resumo(n, media, mediana(ord, 0, n), modas, maiorFrequencia,
                variancia, Math.sqrt(variancia), ord[0], ord[n - 1], q1, q3);
    }

    /** Mediana da fatia ord[de..ate): o valor do meio ou, com quantidade par, a média dos dois do meio. */
    static double mediana(double[] ord, int de, int ate) {
        int tamanho = ate - de;
        int meio = de + tamanho / 2;
        return tamanho % 2 == 1 ? ord[meio] : (ord[meio - 1] + ord[meio]) / 2;
    }

    /** Histograma em texto: divide [mínimo, máximo] em faixas iguais e desenha uma barra por faixa. */
    static List<String> histograma(double[] dados, int faixas) {
        double minimo = Arrays.stream(dados).min().getAsDouble();
        double maximo = Arrays.stream(dados).max().getAsDouble();
        if (maximo == minimo) return List.of(String.format(PT_BR, "Todos os %d valores são iguais a %.2f.", dados.length, minimo));
        double largura = (maximo - minimo) / faixas;
        int[] contagem = new int[faixas];
        for (double x : dados) {
            int i = (int) ((x - minimo) / largura);
            if (i == faixas) i = faixas - 1;   // o próprio máximo cairia numa faixa a mais; vai para a última
            contagem[i]++;
        }
        int maiorContagem = Arrays.stream(contagem).max().getAsInt();
        List<String> linhas = new ArrayList<>();
        for (int i = 0; i < faixas; i++) {
            double inicio = minimo + i * largura, fim = inicio + largura;
            String fecha = i == faixas - 1 ? "]" : ")";   // só a última faixa inclui o limite de cima
            int barra = (int) Math.round(contagem[i] * 40.0 / maiorContagem);
            linhas.add(String.format(PT_BR, "  [%8.2f, %8.2f%s %4d  %s", inicio, fim, fecha, contagem[i], "#".repeat(barra)));
        }
        return linhas;
    }

    static double lerNumero(String texto) {
        String limpo = texto.trim();
        if (limpo.contains(",") && limpo.contains(".")) throw new IllegalArgumentException("Número ambíguo '" + texto + "': use só vírgula ou só ponto como separador decimal.");
        try {
            return Double.parseDouble(limpo.replace(',', '.'));   // aceita 7,5 e 7.5
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Esperava um número, recebi '" + texto + "'.");
        }
    }

    /** Lê um arquivo com números separados por espaço, ponto e vírgula ou quebra de linha. Linhas com # são comentários. */
    static double[] lerArquivo(Path caminho) {
        List<String> linhas;
        try {
            linhas = Files.readAllLines(caminho, StandardCharsets.UTF_8);
        } catch (IOException e) {
            String motivo = e instanceof java.nio.file.NoSuchFileException ? "ele não existe" : e.getMessage();
            throw new IllegalArgumentException("Não consegui ler o arquivo '" + caminho + "': " + motivo + ".");
        }
        List<Double> valores = new ArrayList<>();
        for (String linha : linhas) {
            String util = linha.strip();
            if (util.isEmpty() || util.startsWith("#")) continue;
            for (String pedaco : util.split("[\\s;]+")) valores.add(lerNumero(pedaco));
        }
        if (valores.isEmpty()) throw new IllegalArgumentException("O arquivo '" + caminho + "' não tem nenhum número.");
        return valores.stream().mapToDouble(Double::doubleValue).toArray();
    }

    static void relatorio(String titulo, double[] dados, int faixas) {
        Resumo r = resumir(dados);
        System.out.println("=== " + titulo + " ===");
        System.out.println("Quantidade de valores: " + r.n());
        System.out.println(String.format(PT_BR, "Média ........... %10.3f", r.media()));
        System.out.println(String.format(PT_BR, "Mediana ......... %10.3f", r.mediana()));
        String moda = r.modas().isEmpty() ? "não há (nenhum valor se repete)"
                : r.modas().stream().map(m -> String.format(PT_BR, "%.2f", m)).toList() + " (aparece " + r.frequenciaModa() + " vezes)";
        System.out.println("Moda ............ " + moda);
        System.out.println(String.format(PT_BR, "Variância ....... %10.3f  (amostral, divide por n - 1)", r.variancia()));
        System.out.println(String.format(PT_BR, "Desvio padrão ... %10.3f", r.desvio()));
        System.out.println(String.format(PT_BR, "Mínimo / Máximo . %10.3f / %.3f  (amplitude %.3f)", r.minimo(), r.maximo(), r.amplitude()));
        System.out.println(String.format(PT_BR, "Quartis ......... Q1 %.3f | Q2 %.3f | Q3 %.3f  (intervalo interquartil %.3f)",
                r.q1(), r.mediana(), r.q3(), r.intervaloInterquartil()));
        long dentro = Arrays.stream(dados).filter(x -> Math.abs(x - r.media()) <= r.desvio()).count();
        System.out.println(String.format(PT_BR, "%d de %d valores (%.0f%%) ficam a menos de um desvio padrão da média.",
                dentro, r.n(), 100.0 * dentro / r.n()));
        System.out.println("Histograma com " + faixas + " faixas (cada # vale uma fração da faixa mais cheia):");
        histograma(dados, faixas).forEach(System.out::println);
        System.out.println();
    }

    static void demo() throws IOException {
        System.out.println("CodeNomad Academy :: Estatística descritiva, modo demonstração\n");
        // Notas de 40 alunos numa prova de 0 a 10. Dados fictícios, com uma moda clara em 7,5.
        double[] notas = {
            7.5, 8.0, 6.5, 9.0, 10.0, 4.0, 7.0, 8.5, 7.5, 6.0,
            5.5, 7.5, 8.0, 9.5, 3.0, 7.0, 6.5, 8.0, 7.5, 5.0,
            9.0, 7.5, 6.0, 8.5, 7.0, 2.5, 8.0, 7.5, 6.5, 9.0,
            4.5, 7.0, 8.5, 7.5, 5.5, 8.0, 6.0, 9.5, 7.0, 8.0
        };
        System.out.println("Amostra: " + Arrays.toString(notas).replace(".", ","));
        relatorio("Notas de 40 alunos (vindas do código)", notas, 8);

        // O mesmo relatório lendo de um arquivo, criado na pasta temporária só para a demonstração.
        Path arquivo = Files.createTempFile("codenomad-estatistica-", ".txt");
        try {
            Files.writeString(arquivo, """
                    # tempo, em minutos, que cada cliente esperou na fila da loja
                    12 8 15; 9 11
                    7,5 14 10 22 9,5
                    6 13 11 8
                    """, StandardCharsets.UTF_8);
            double[] tempos = lerArquivo(arquivo);
            relatorio("Tempo de espera em minutos (lido de " + arquivo.getFileName() + ")", tempos, 5);
        } finally {
            Files.deleteIfExists(arquivo);   // não deixamos lixo na pasta temporária
        }
        System.out.println("Repare: na primeira amostra a média (" + String.format(PT_BR, "%.2f", resumir(notas).media())
                + ") fica abaixo da mediana porque as notas baixas puxam a média; a mediana ignora isso.");
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try {
            List<Double> valores = new ArrayList<>();
            Path arquivo = null;
            int faixas = 8;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--arquivo" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--arquivo precisa do caminho logo depois.");
                        arquivo = Path.of(args[++i]);
                    }
                    case "--faixas" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--faixas precisa de um número inteiro logo depois.");
                        try { faixas = Integer.parseInt(args[++i]); }
                        catch (NumberFormatException e) { throw new IllegalArgumentException("--faixas precisa de um número inteiro, recebi '" + args[i] + "'."); }
                        if (faixas < 1 || faixas > 50) throw new IllegalArgumentException("O número de faixas deve ir de 1 a 50, recebi " + faixas + ".");
                    }
                    default -> valores.add(lerNumero(args[i]));
                }
            }
            double[] dados = arquivo != null ? lerArquivo(arquivo) : valores.stream().mapToDouble(Double::doubleValue).toArray();
            relatorio(arquivo != null ? "Dados de " + arquivo : "Dados da linha de comando", dados, faixas);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            uso();
            System.exit(1);
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java EstatisticaBasica.java <números...> [--faixas N]
                 java EstatisticaBasica.java --arquivo <caminho> [--faixas N]
              Os números aceitam vírgula ou ponto decimal. No arquivo, separe por espaço, ";" ou quebra de linha; linhas com # são ignoradas.
            Exemplos:
              java EstatisticaBasica.java 7,5 8 6,5 9 10 4 7 8,5 7,5
              java EstatisticaBasica.java --arquivo notas.txt --faixas 6
              java EstatisticaBasica.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Acrescente a variância populacional (divide por n) e mostre as duas lado a lado para o aluno ver a diferença.
 *  2. Detecte valores atípicos pela regra de Tukey: abaixo de Q1 - 1,5 x IQR ou acima de Q3 + 1,5 x IQR, e liste-os.
 *  3. Aceite um arquivo CSV com várias colunas e um argumento --coluna N para escolher qual analisar.
 */
