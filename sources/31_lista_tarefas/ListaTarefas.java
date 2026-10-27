/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  31. Lista de tarefas em arquivo JSON
 * ------------------------------------------------------------
 * O que faz: guarda tarefas com prioridade e data num arquivo JSON legível e
 *            permite adicionar, listar (ordenadas por prioridade e data),
 *            concluir, remover e buscar. Sem --arquivo, usa tarefas.json.
 * Como usar: java ListaTarefas.java adicionar "Estudar Java" --prioridade alta --data 30/10/2026
 *            java ListaTarefas.java listar
 *            java ListaTarefas.java --arquivo minhas.json concluir 1
 *            java ListaTarefas.java --demo
 * O que você aprende: records imutáveis com método de cópia, leitor e escritor de JSON
 *            feitos à mão, Comparator encadeado, java.time, Files em UTF-8 e erros úteis.
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
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ListaTarefas {

    static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final List<String> PRIORIDADES = List.of("alta", "média", "baixa");

    /** Um record é imutável: para concluir uma tarefa criamos uma cópia com o campo trocado. */
    record Tarefa(int id, String titulo, String prioridade, LocalDate data, boolean concluida) {
        Tarefa concluir() { return new Tarefa(id, titulo, prioridade, data, true); }
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        Path arquivo = Path.of("tarefas.json");
        if (args[0].equals("--arquivo") && args.length >= 3) {
            arquivo = Path.of(args[1]);
            args = Arrays.copyOfRange(args, 2, args.length);
        }
        try { System.out.print(executar(arquivo, args)); }
        catch (IllegalArgumentException e) { System.out.println("Erro: " + e.getMessage()); System.exit(1); }
    }

    static void demo() throws IOException {
        Path arquivo = Files.createTempFile("codenomad_tarefas_", ".json");
        Files.delete(arquivo); // começamos sem arquivo, como um usuário novo
        System.out.println("=== Demonstração: lista de tarefas em " + arquivo + " ===");
        String[][] comandos = {
                {"adicionar", "Estudar records em Java", "--prioridade", "alta", "--data", "30/10/2026"},
                {"adicionar", "Entregar relatório do curso", "--prioridade", "media", "--data", "28/10/2026"},
                {"adicionar", "Marcar dentista", "--prioridade", "baixa", "--data", "15/11/2026"},
                {"adicionar", "Comprar pão"}, {"listar"}, {"concluir", "4"}, {"buscar", "relat"}, {"remover", "3"},
                {"listar"}, {"concluir", "99"}, {"adicionar", "Tarefa com data errada", "--data", "31/02/2026"},
        };
        for (String[] c : comandos) {
            System.out.println("\n> " + String.join(" ", c));
            try { System.out.print(executar(arquivo, c)); }
            catch (IllegalArgumentException e) { System.out.println("Erro tratado: " + e.getMessage()); }
        }
        System.out.println("\nConteúdo final do arquivo JSON:\n" + Files.readString(arquivo, StandardCharsets.UTF_8));
        Files.deleteIfExists(arquivo);
    }

    /** Carrega o arquivo, executa um comando e grava de volta quando algo mudou. */
    static String executar(Path arquivo, String[] args) throws IOException {
        List<Tarefa> tarefas = carregar(arquivo);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "adicionar" -> {
                Tarefa nova = montar(tarefas, args);
                tarefas.add(nova);
                salvar(arquivo, tarefas);
                return "Adicionada: " + linha(nova) + "\n";
            }
            case "listar" -> { return listar(tarefas); }
            case "concluir", "remover" -> {
                boolean concluir = args[0].equalsIgnoreCase("concluir");
                Tarefa alvo = porId(tarefas, args);
                if (concluir) tarefas.set(tarefas.indexOf(alvo), alvo = alvo.concluir());
                else tarefas.remove(alvo);
                salvar(arquivo, tarefas);
                return (concluir ? "Concluída: " : "Removida: ") + linha(alvo) + "\n";
            }
            case "buscar" -> {
                if (args.length != 2) throw new IllegalArgumentException("use: buscar <texto>");
                List<Tarefa> achadas = tarefas.stream().filter(t -> t.titulo().toLowerCase(Locale.ROOT).contains(args[1].toLowerCase(Locale.ROOT))).toList();
                return achadas.isEmpty() ? "Nenhuma tarefa contém \"" + args[1] + "\".\n" : listar(achadas);
            }
            default -> throw new IllegalArgumentException("comando desconhecido '" + args[0] + "'. Use adicionar, listar, concluir, remover ou buscar.");
        }
    }

    /** Lê "--prioridade" e "--data" depois do título; o que não for opção vira erro. */
    static Tarefa montar(List<Tarefa> tarefas, String[] args) {
        if (args.length < 2) throw new IllegalArgumentException("use: adicionar \"<título>\" [--prioridade alta|média|baixa] [--data dd/MM/aaaa]");
        String prioridade = "média";
        LocalDate data = null;
        for (int i = 2; i < args.length; i += 2) {
            if (i + 1 >= args.length) throw new IllegalArgumentException("a opção " + args[i] + " precisa de um valor");
            switch (args[i]) {
                case "--prioridade" -> {
                    prioridade = args[i + 1].toLowerCase(Locale.ROOT).replace("media", "média"); // aceita sem acento
                    if (!PRIORIDADES.contains(prioridade)) throw new IllegalArgumentException("prioridade '" + args[i + 1] + "' não existe; use alta, média ou baixa");
                }
                case "--data" -> {
                    try { data = LocalDate.parse(args[i + 1], BR); }
                    catch (DateTimeParseException e) { throw new IllegalArgumentException("data '" + args[i + 1] + "' inválida; use dd/MM/aaaa com uma data que exista"); }
                }
                default -> throw new IllegalArgumentException("opção desconhecida '" + args[i] + "'");
            }
        }
        int proximoId = tarefas.stream().mapToInt(Tarefa::id).max().orElse(0) + 1;
        return new Tarefa(proximoId, args[1].strip(), prioridade, data, false);
    }

    static Tarefa porId(List<Tarefa> tarefas, String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("use: " + args[0] + " <id>");
        int id;
        try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) { throw new IllegalArgumentException("o id precisa ser um número inteiro, recebi '" + args[1] + "'"); }
        return tarefas.stream().filter(t -> t.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("não existe tarefa com id " + id));
    }

    /** Pendentes primeiro, depois prioridade, depois data (sem data vai para o fim), depois id. */
    static String listar(List<Tarefa> tarefas) {
        if (tarefas.isEmpty()) return "Nenhuma tarefa cadastrada.\n";
        Comparator<Tarefa> ordem = Comparator.comparing(Tarefa::concluida).thenComparingInt(t -> PRIORIDADES.indexOf(t.prioridade()))
                .thenComparing(Tarefa::data, Comparator.nullsLast(Comparator.naturalOrder())).thenComparingInt(Tarefa::id);
        StringBuilder sb = new StringBuilder(String.format("%3s  %-3s  %-6s  %-10s  %s%n", "ID", "OK", "PRIOR.", "DATA", "TÍTULO"));
        tarefas.stream().sorted(ordem).forEach(t -> sb.append(linha(t)).append('\n'));
        long pendentes = tarefas.stream().filter(t -> !t.concluida()).count();
        return sb.append(String.format("%d pendente(s), %d concluída(s).%n", pendentes, tarefas.size() - pendentes)).toString();
    }

    static String linha(Tarefa t) {
        return String.format("%3d  %-3s  %-6s  %-10s  %s", t.id(), t.concluida() ? "[x]" : "[ ]",
                t.prioridade().toUpperCase(Locale.ROOT), t.data() == null ? "sem data" : BR.format(t.data()), t.titulo());
    }

    /** Escreve o JSON à mão, um objeto por linha; joining recebe o separador, o prefixo e o sufixo. */
    static void salvar(Path arquivo, List<Tarefa> tarefas) throws IOException {
        String json = tarefas.stream()
                .map(t -> String.format("  {\"id\": %d, \"titulo\": %s, \"prioridade\": %s, \"data\": %s, \"concluida\": %s}",
                        t.id(), aspas(t.titulo()), aspas(t.prioridade()), t.data() == null ? "null" : aspas(t.data().toString()), t.concluida()))
                .collect(Collectors.joining(",\n", "[\n", "\n]\n"));
        Files.writeString(arquivo, json, StandardCharsets.UTF_8);
    }

    /** Aspas e barras dentro do texto precisam de escape, senão o JSON quebra. */
    static String aspas(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    static List<Tarefa> carregar(Path arquivo) throws IOException {
        List<Tarefa> tarefas = new ArrayList<>();
        if (!Files.exists(arquivo)) return tarefas;
        try {
            for (Object item : (List<?>) new Json(Files.readString(arquivo, StandardCharsets.UTF_8)).valor()) {
                Map<?, ?> m = (Map<?, ?>) item;
                tarefas.add(new Tarefa(((Double) m.get("id")).intValue(), (String) m.get("titulo"), (String) m.get("prioridade"),
                        m.get("data") == null ? null : LocalDate.parse((String) m.get("data")), (Boolean) m.get("concluida")));
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("o arquivo " + arquivo + " não tem o formato esperado (" + e.getMessage() + ")");
        }
        return tarefas;
    }

    /** Leitor de JSON por descida recursiva: cada método lê um tipo de valor a partir da posição atual. */
    static final class Json {
        private final String s;
        private int i;
        Json(String s) { this.s = s; }

        Object valor() {
            char c = proximo();
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return texto();
            for (String p : List.of("true", "false", "null")) {
                if (s.startsWith(p, i)) { i += p.length(); return p.equals("null") ? null : Boolean.valueOf(p); }
            }
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (inicio == i) throw erro("valor inesperado");
            return Double.parseDouble(s.substring(inicio, i));
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            for (i++; proximo() != '}'; virgula()) {
                String chave = texto();
                if (proximo() != ':') throw erro("esperava ':'");
                i++;
                m.put(chave, valor());
            }
            i++;
            return m;
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<>();
            for (i++; proximo() != ']'; virgula()) l.add(valor());
            i++;
            return l;
        }

        /** Dentro de aspas, uma barra invertida anuncia um caractere especial. */
        String texto() {
            if (s.charAt(i++) != '"') throw erro("esperava aspas");
            StringBuilder sb = new StringBuilder();
            for (char c = s.charAt(i++); c != '"'; c = s.charAt(i++)) {
                if (c == '\\') { c = s.charAt(i++); c = c == 'n' ? '\n' : c == 't' ? '\t' : c; }
                sb.append(c);
            }
            return sb.toString();
        }

        /** Pula espaços e devolve o caractere atual sem consumir. */
        char proximo() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= s.length()) throw erro("o texto acabou antes da hora");
            return s.charAt(i);
        }
        void virgula() { if (proximo() == ',') i++; }
        IllegalArgumentException erro(String msg) { return new IllegalArgumentException(msg + " na posição " + i); }
    }

    static void uso() {
        System.out.println("""
            Uso: java ListaTarefas.java [--arquivo caminho.json] <comando> ...
              adicionar "<título>" [--prioridade alta|média|baixa] [--data dd/MM/aaaa]
              listar                     concluir <id>
              buscar <texto>             remover <id>
            Demonstração: java ListaTarefas.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Marque com "(atrasada)" as tarefas pendentes cuja data já passou, comparando com LocalDate.now().
 *  2. Adicione o comando "editar <id> --titulo ..." reaproveitando o método montar.
 *  3. Troque o Double do leitor por Long quando o número não tiver ponto nem expoente.
 */
