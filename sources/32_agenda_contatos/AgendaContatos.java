/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  32. Agenda de contatos em CSV
 * ------------------------------------------------------------
 * O que faz: guarda contatos (nome, telefone, e-mail, etiqueta) num arquivo
 *            CSV e permite adicionar, listar ordenado por qualquer campo,
 *            buscar em todos os campos, exportar um filtro para outro CSV e
 *            apontar duplicados por telefone, e-mail ou nome.
 * Como usar: java AgendaContatos.java adicionar "Ana Souza" "(11) 98765-4321" ana@exemplo.com trabalho
 *            java AgendaContatos.java listar --por etiqueta
 *            java AgendaContatos.java buscar souza
 *            java AgendaContatos.java exportar etiqueta=trabalho equipe.csv
 *            java AgendaContatos.java --arquivo outra.csv duplicados
 *            java AgendaContatos.java --demo
 * O que você aprende: leitura e escrita de CSV com aspas (sem split cego),
 *            records com métodos, normalização de texto com Normalizer,
 *            groupingBy para achar duplicados, Comparator dinâmico e
 *            validação com expressões regulares.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AgendaContatos {

    static final List<String> CAMPOS = List.of("nome", "telefone", "email", "etiqueta");
    static final String CABECALHO = String.join(",", CAMPOS);

    record Contato(String nome, String telefone, String email, String etiqueta) {
        /** Devolve o valor de um campo pelo nome, o que permite ordenar e filtrar por qualquer um. */
        String campo(String qual) {
            return switch (qual) {
                case "nome" -> nome;
                case "telefone" -> telefone;
                case "email" -> email;
                case "etiqueta" -> etiqueta;
                default -> throw new IllegalArgumentException("campo '" + qual + "' não existe; use " + CAMPOS);
            };
        }
        boolean contem(String termo) {
            return CAMPOS.stream().anyMatch(c -> simplificar(campo(c)).contains(simplificar(termo)));
        }
        String csv() {
            return CAMPOS.stream().map(c -> escapar(campo(c))).collect(Collectors.joining(","));
        }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        Path arquivo = Path.of("contatos.csv");
        if (args[0].equals("--arquivo") && args.length >= 3) {
            arquivo = Path.of(args[1]);
            args = Arrays.copyOfRange(args, 2, args.length);
        }
        try { System.out.print(executar(arquivo, args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path pasta = Files.createTempDirectory("codenomad_agenda_");
        Path arquivo = pasta.resolve("contatos.csv");
        Path exportado = pasta.resolve("amigos.csv");
        salvar(arquivo, List.of(
                new Contato("Ana Beatriz Souza", "(11) 98765-4321", "ana.souza@exemplo.com", "trabalho"),
                new Contato("Bruno Carvalho", "21 99876-1234", "bruno.carvalho@exemplo.com", "amigos"),
                new Contato("Carla Mendes", "11987654321", "carla.mendes@exemplo.com", "trabalho"),
                new Contato("Diego Ramos", "(31) 3222-1010", "diego@exemplo.com", "família"),
                new Contato("Ana B. Souza", "(11) 91111-2222", "Ana.Souza@exemplo.com", "família, amigos"),
                new Contato("Fernanda Lima", "(41) 99999-0001", "fernanda.lima@exemplo.com", "trabalho")));
        System.out.println("=== Demonstração: agenda em " + arquivo + " ===");
        String[][] comandos = {
                {"adicionar", "Mariana Costa", "(51) 98888-7777", "mariana.costa@exemplo.com", "faculdade"},
                {"adicionar", "Diego Ramos", "(31) 98888-0000", "diego.ramos@exemplo.com", "trabalho"},
                {"listar", "--por", "nome"}, {"buscar", "souza"}, {"buscar", "familia"},
                {"exportar", "etiqueta=amigos", exportado.toString()}, {"duplicados"},
                {"adicionar", "Zé", "123", "ze@exemplo.com"}, {"adicionar", "Zé", "(11) 90000-0000", "ze.exemplo.com"},
                {"listar", "--por", "cor"},
        };
        for (String[] c : comandos) {
            System.out.println("\n> " + String.join(" ", c).replace(exportado.toString(), "amigos.csv"));
            try { System.out.print(executar(arquivo, c)); }
            catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        }
        System.out.println("\nConteúdo de amigos.csv (repare nas aspas do campo com vírgula):");
        System.out.print(Files.readString(exportado, StandardCharsets.UTF_8));
        Files.delete(exportado);
        Files.delete(arquivo);
        Files.delete(pasta);
    }

    static String executar(Path arquivo, String[] args) throws IOException {
        List<Contato> contatos = carregar(arquivo);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "adicionar" -> {
                if (args.length < 4 || args.length > 5) throw new IllegalArgumentException("use: adicionar <nome> <telefone> <email> [etiqueta]");
                Contato novo = validar(new Contato(args[1].strip(), args[2].strip(), args[3].strip(), args.length == 5 ? args[4].strip() : ""));
                contatos.add(novo);
                salvar(arquivo, contatos);
                return "Adicionado: " + linha(novo) + "\n";
            }
            case "listar" -> {
                String por = args.length == 3 && args[1].equals("--por") ? args[2] : "nome";
                if (!CAMPOS.contains(por)) throw new IllegalArgumentException("não dá para ordenar por '" + por + "'; use " + CAMPOS);
                // O Comparator é montado em tempo de execução a partir do nome do campo.
                return tabela(contatos.stream().sorted(Comparator.comparing(c -> simplificar(c.campo(por)))).toList());
            }
            case "buscar" -> {
                if (args.length != 2) throw new IllegalArgumentException("use: buscar <texto>");
                List<Contato> achados = contatos.stream().filter(c -> c.contem(args[1])).toList();
                return achados.isEmpty() ? "Nenhum contato combina com \"" + args[1] + "\".\n" : tabela(achados);
            }
            case "exportar" -> {
                if (args.length != 3 || !args[1].contains("=")) throw new IllegalArgumentException("use: exportar <campo>=<valor> <destino.csv>");
                String campo = args[1].substring(0, args[1].indexOf('=')), valor = args[1].substring(args[1].indexOf('=') + 1);
                List<Contato> filtro = contatos.stream().filter(c -> simplificar(c.campo(campo)).contains(simplificar(valor))).toList();
                salvar(Path.of(args[2]), filtro);
                return filtro.size() + " contato(s) com " + campo + " contendo \"" + valor + "\" exportado(s) para " + args[2] + "\n";
            }
            case "duplicados" -> { return duplicados(contatos); }
            default -> throw new IllegalArgumentException("comando desconhecido '" + args[0] + "'. Use adicionar, listar, buscar, exportar ou duplicados.");
        }
    }

    static Contato validar(Contato c) {
        if (c.nome().isEmpty()) throw new IllegalArgumentException("o nome não pode ficar vazio");
        int digitos = c.telefone().replaceAll("\\D", "").length();
        if (digitos < 8 || digitos > 13) throw new IllegalArgumentException("telefone '" + c.telefone() + "' precisa ter de 8 a 13 dígitos");
        // Uma validação simples: algo@algo.algo, sem espaços. Suficiente para uma agenda.
        if (!c.email().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("e-mail '" + c.email() + "' não parece válido");
        return c;
    }

    /** Agrupa por três chaves diferentes; um grupo com mais de um contato é um duplicado provável. */
    static String duplicados(List<Contato> contatos) {
        record Criterio(String rotulo, Function<Contato, String> chave) {}
        List<Criterio> criterios = List.of(
                new Criterio("telefone", c -> c.telefone().replaceAll("\\D", "")),
                new Criterio("e-mail", c -> c.email().toLowerCase(Locale.ROOT)),
                new Criterio("nome", c -> simplificar(c.nome())));
        StringBuilder sb = new StringBuilder();
        for (Criterio cr : criterios) {
            Map<String, List<Contato>> grupos = contatos.stream().collect(Collectors.groupingBy(cr.chave(), LinkedHashMap::new, Collectors.toList()));
            grupos.forEach((chave, grupo) -> {
                if (grupo.size() > 1) sb.append(String.format("  mesmo %s (%s): %s%n", cr.rotulo(), chave,
                        grupo.stream().map(Contato::nome).collect(Collectors.joining(" | "))));
            });
        }
        return sb.length() == 0 ? "Nenhum duplicado encontrado.\n" : "Possíveis duplicados:\n" + sb;
    }

    static String tabela(List<Contato> contatos) {
        if (contatos.isEmpty()) return "Agenda vazia.\n";
        StringBuilder sb = new StringBuilder(String.format("%-20s %-16s %-28s %s%n", "NOME", "TELEFONE", "E-MAIL", "ETIQUETA"));
        contatos.forEach(c -> sb.append(linha(c)).append('\n'));
        return sb.append(contatos.size()).append(" contato(s).\n").toString();
    }

    static String linha(Contato c) {
        return String.format("%-20s %-16s %-28s %s", c.nome(), c.telefone(), c.email(), c.etiqueta());
    }

    /** Minúsculas e sem acentos: "Família" e "familia" passam a ser iguais na busca. */
    static String simplificar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    static List<Contato> carregar(Path arquivo) throws IOException {
        List<Contato> contatos = new ArrayList<>();
        if (!Files.exists(arquivo)) return contatos;
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.get(0).strip().equals(CABECALHO)) throw new IllegalArgumentException(arquivo + " não começa com o cabeçalho " + CABECALHO);
        for (int n = 1; n < linhas.size(); n++) {
            if (linhas.get(n).isBlank()) continue;
            List<String> campos = dividir(linhas.get(n));
            if (campos.size() != 4) throw new IllegalArgumentException("linha " + (n + 1) + " de " + arquivo + " tem " + campos.size() + " campos em vez de 4");
            contatos.add(new Contato(campos.get(0), campos.get(1), campos.get(2), campos.get(3)));
        }
        return contatos;
    }

    static void salvar(Path arquivo, List<Contato> contatos) throws IOException {
        List<String> linhas = new ArrayList<>(List.of(CABECALHO));
        contatos.forEach(c -> linhas.add(c.csv()));
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }

    /** Divide uma linha CSV respeitando aspas: "família, amigos" é um campo só, com a vírgula dentro. */
    static List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"' && entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                atual.append('"'); // duas aspas seguidas dentro de aspas são uma aspa literal
                i++;
            } else if (c == '"') {
                entreAspas = !entreAspas;
            } else if (c == ',' && !entreAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    /** O inverso de dividir: só põe aspas quando o campo precisa delas. */
    static String escapar(String campo) {
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n")) return "\"" + campo.replace("\"", "\"\"") + "\"";
        return campo;
    }

    static void uso() {
        System.out.println("""
            Uso: java AgendaContatos.java [--arquivo caminho.csv] <comando> ...
              adicionar <nome> <telefone> <email> [etiqueta]
              listar [--por nome|telefone|email|etiqueta]
              buscar <texto>                       procura em todos os campos, sem ligar para acentos
              exportar <campo>=<valor> <destino.csv>
              duplicados                           mesmo telefone, e-mail ou nome
            Demonstração: java AgendaContatos.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Crie o comando "remover <email>" e grave o arquivo sem o contato.
 *  2. Formate o telefone na listagem como (DD) 9XXXX-XXXX a partir só dos dígitos.
 *  3. Aceite também ponto e vírgula como separador, detectando pelo cabeçalho.
 */
