/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  33. Renomeador de arquivos em lote
 * ------------------------------------------------------------
 * O que faz: renomeia todos os arquivos de uma pasta seguindo uma regra
 *            (prefixo, sufixo, numeração, troca de texto ou minúsculas).
 *            Por padrão só mostra a prévia; com --aplicar renomeia de fato,
 *            e nunca sobrescreve um arquivo que já exista.
 * Como usar: java RenomeadorLote.java ./fotos prefixo ferias_
 *            java RenomeadorLote.java ./fotos trocar " " _ --aplicar
 *            java RenomeadorLote.java ./fotos numerar 1 --ext jpg --aplicar
 *            java RenomeadorLote.java ./docs minusculas --aplicar
 *            java RenomeadorLote.java --demo
 * O que você aprende: Files.list com try-with-resources, Path.resolveSibling,
 *            Files.move sem sobrescrever, separar nome e extensão, records
 *            para planejar antes de executar (dry-run) e detectar conflitos
 *            entre os próprios nomes novos.
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class RenomeadorLote {

    /** Um plano diz de onde para onde vai cada arquivo e por que ele seria pulado. */
    record Plano(Path origem, String novoNome, String motivo) {
        boolean ok() { return motivo.isEmpty(); }
        Path destino() { return origem.resolveSibling(novoNome); }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        try { System.out.print(executar(args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path pasta = Files.createTempDirectory("codenomad_renomear_");
        for (String nome : List.of("Foto De Ferias 1.JPG", "Foto De Ferias 2.JPG", "foto2.jpg", "foto3.jpg", "Relatorio Final.PDF", "LEIA-ME.TXT")) {
            Files.writeString(pasta.resolve(nome), "conteúdo de " + nome, StandardCharsets.UTF_8);
        }
        System.out.println("=== Demonstração em " + pasta + " ===");
        String p = pasta.toString();
        String[][] comandos = {
                {p, "minusculas"},
                {p, "trocar", " ", "_", "--aplicar"},
                {p, "minusculas", "--aplicar"},
                {p, "prefixo", "2026_", "--ext", "jpg", "--aplicar"},
                {p, "trocar", "3", "2"},
                {p, "numerar", "1", "--aplicar"},
                {p, "sufixo"},
        };
        for (String[] c : comandos) {
            StringBuilder linha = new StringBuilder(">");
            for (String arg : c) linha.append(' ').append(arg.equals(p) ? "<pasta>" : arg.contains(" ") ? "\"" + arg + "\"" : arg);
            System.out.println("\n" + linha);
            try { System.out.print(executar(c)); }
            catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        }
        System.out.println("\nArquivos no fim da demonstração:");
        for (Path arquivo : listar(pasta, null)) {
            System.out.println("  " + arquivo.getFileName());
            Files.delete(arquivo);
        }
        Files.delete(pasta);
    }

    static String executar(String[] args) throws IOException {
        if (args.length < 2) throw new IllegalArgumentException("use: <pasta> <operação> [valores] [--ext xxx] [--aplicar]");
        Path pasta = Path.of(args[0]);
        if (!Files.isDirectory(pasta)) throw new IllegalArgumentException("a pasta '" + args[0] + "' não existe");
        boolean aplicar = false;
        String ext = null;
        List<String> valores = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--aplicar")) aplicar = true;
            else if (args[i].equals("--ext") && i + 1 < args.length) ext = args[++i].toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
            else valores.add(args[i]);
        }
        List<Path> arquivos = listar(pasta, ext);
        if (arquivos.isEmpty()) return "Nenhum arquivo" + (ext == null ? "" : " ." + ext) + " em " + pasta + "\n";
        List<Plano> planos = planejar(arquivos, args[1].toLowerCase(Locale.ROOT), valores);
        StringBuilder sb = new StringBuilder(aplicar ? "Aplicando:\n" : "Prévia (use --aplicar para renomear de verdade):\n");
        int feitos = 0;
        for (Plano plano : planos) {
            String status = plano.ok() ? "" : "  [pulado: " + plano.motivo() + "]";
            sb.append(String.format("  %-28s -> %-30s%s%n", plano.origem().getFileName(), plano.novoNome(), status));
            if (aplicar && plano.ok()) {
                renomear(plano.origem(), plano.destino());
                feitos++;
            }
        }
        long pulados = planos.stream().filter(pl -> !pl.ok()).count();
        return sb.append(aplicar ? feitos + " renomeado(s), " + pulados + " pulado(s).\n" : pulados + " seria(m) pulado(s).\n").toString();
    }

    /**
     * No Windows e no macOS o sistema de arquivos ignora maiúsculas: "Foto.JPG" e "foto.jpg" são o
     * mesmo arquivo, e Files.move entre os dois não faz nada. A saída é passar por um nome intermediário.
     * Sem REPLACE_EXISTING, o move lança exceção em vez de apagar um arquivo que exista no destino.
     */
    static void renomear(Path origem, Path destino) throws IOException {
        if (Files.exists(destino) && Files.isSameFile(origem, destino)) {
            Path intermediario = origem.resolveSibling(destino.getFileName() + ".renomeando-" + System.nanoTime());
            Files.move(origem, intermediario);
            Files.move(intermediario, destino);
        } else {
            Files.move(origem, destino);
        }
    }

    /** Só arquivos comuns (sem subpastas), em ordem alfabética ignorando maiúsculas. */
    static List<Path> listar(Path pasta, String ext) throws IOException {
        try (Stream<Path> fluxo = Files.list(pasta)) {
            return fluxo.filter(Files::isRegularFile)
                    .filter(p -> ext == null || extensao(p.getFileName().toString()).equalsIgnoreCase(ext))
                    .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                    .toList();
        }
    }

    /** Calcula o nome novo de cada arquivo e marca os que não podem ser renomeados. */
    static List<Plano> planejar(List<Path> arquivos, String operacao, List<String> valores) throws IOException {
        int inicio = operacao.equals("numerar") ? inteiro(valores.isEmpty() ? "1" : valores.get(0)) : 0;
        Set<String> usados = new HashSet<>();
        List<Plano> planos = new ArrayList<>();
        for (int i = 0; i < arquivos.size(); i++) {
            Path origem = arquivos.get(i);
            String nome = origem.getFileName().toString();
            String base = nome.substring(0, nome.length() - extensao(nome).length()).replaceFirst("\\.$", "");
            String ext = extensao(nome).isEmpty() ? "" : "." + extensao(nome);
            String novo = switch (operacao) {
                case "prefixo" -> exigir(valores, 1, "prefixo <texto>").get(0) + nome;
                case "sufixo" -> base + exigir(valores, 1, "sufixo <texto>").get(0) + ext;
                case "numerar" -> String.format("%03d_%s", inicio + i, nome);
                case "trocar" -> nome.replace(exigir(valores, 2, "trocar <de> <para>").get(0), valores.get(1));
                case "minusculas" -> nome.toLowerCase(Locale.ROOT);
                default -> throw new IllegalArgumentException("operação desconhecida '" + operacao + "'; use prefixo, sufixo, numerar, trocar ou minusculas");
            };
            String motivo = "";
            Path destino = origem.resolveSibling(novo);
            if (novo.equals(nome)) motivo = "nome não muda";
            else if (novo.isBlank() || novo.contains("/") || novo.contains("\\")) motivo = "nome inválido";
            else if (!usados.add(novo.toLowerCase(Locale.ROOT))) motivo = "outro arquivo do lote já vai usar esse nome";
            // isSameFile cobre o caso "Foto.JPG -> foto.jpg" no Windows, onde o destino "existe" mas é o próprio arquivo.
            else if (Files.exists(destino) && !Files.isSameFile(origem, destino)) motivo = "já existe um arquivo com esse nome";
            planos.add(new Plano(origem, novo, motivo));
        }
        return planos;
    }

    /** A extensão é o que vem depois do último ponto, sem o ponto; ".bashrc" e "nome" não têm extensão. */
    static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto <= 0 ? "" : nome.substring(ponto + 1);
    }

    static List<String> exigir(List<String> valores, int quantos, String forma) {
        if (valores.size() < quantos) throw new IllegalArgumentException("faltam valores; use: " + forma);
        return valores;
    }

    static int inteiro(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("o início da numeração precisa ser um número inteiro, recebi '" + texto + "'");
        }
    }

    static void uso() {
        System.out.println("""
            Uso: java RenomeadorLote.java <pasta> <operação> [valores] [--ext jpg] [--aplicar]
              prefixo <texto>       ferias_foto.jpg
              sufixo <texto>        foto_2026.jpg (antes da extensão)
              numerar [início]      001_foto.jpg, 002_foto.jpg, ...
              trocar <de> <para>    troca um trecho do nome (use "" para apagar)
              minusculas            tudo em minúsculas, inclusive a extensão
            Sem --aplicar só mostra a prévia. Nunca sobrescreve arquivo existente.
            Demonstração: java RenomeadorLote.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Adicione a operação "data", que põe a data de modificação (Files.getLastModifiedTime) na frente do nome.
 *  2. Grave um arquivo de log com "antes -> depois" para permitir desfazer o lote.
 *  3. Aceite --recursivo e renomeie também dentro das subpastas com Files.walk.
 */
