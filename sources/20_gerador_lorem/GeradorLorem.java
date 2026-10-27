/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  20. Gerador de texto de preenchimento
 * ------------------------------------------------------------
 * O que faz: gera parágrafos, frases ou palavras de texto de preenchimento
 *            (o clássico "lorem ipsum") em latim e também em português, com a
 *            quantidade que você escolher. Uma semente opcional faz o mesmo
 *            texto sair de novo, útil para testes e telas de exemplo.
 * Como usar: java GeradorLorem.java paragrafos 3
 *            java GeradorLorem.java frases 5 --idioma pt --semente 42
 *            java GeradorLorem.java palavras 20 --idioma latim
 *            java GeradorLorem.java --demo
 * O que você aprende: java.util.Random com semente, enum com dados (lista de
 *            palavras por idioma), StringBuilder, String.join, text block,
 *            quebra de linha a 80 colunas, análise de opções de linha de
 *            comando e validação de intervalo.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GeradorLorem {

    /** Cada idioma carrega seu vocabulário. Um enum pode ter campos e construtor como qualquer classe. */
    enum Idioma {
        LATIM("latim", List.of(
                "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do",
                "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua", "enim",
                "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco", "laboris", "nisi",
                "aliquip", "ex", "ea", "commodo", "consequat", "duis", "aute", "irure", "in", "reprehenderit",
                "voluptate", "velit", "esse", "cillum", "fugiat", "nulla", "pariatur", "excepteur", "sint",
                "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia", "deserunt",
                "mollit", "anim", "id", "est", "laborum")),
        PORTUGUES("pt", List.of(
                "a", "casa", "janela", "caminho", "rio", "montanha", "cidade", "aluno", "professor", "livro",
                "página", "código", "função", "número", "tela", "teclado", "memória", "programa", "ideia",
                "projeto", "manhã", "tarde", "noite", "chuva", "sol", "vento", "café", "pão", "mesa", "porta",
                "estrada", "ponte", "jardim", "música", "história", "lição", "exemplo", "prática", "teste",
                "resposta", "pergunta", "amigo", "família", "trabalho", "descanso", "viagem", "mapa", "chave",
                "porto", "barco", "estrela", "lua", "mar", "areia", "pedra", "árvore", "folha", "fruta", "sombra",
                "luz", "silêncio", "palavra", "frase", "parágrafo", "começo", "meio", "fim", "sempre", "nunca",
                "hoje", "amanhã", "depois", "antes", "perto", "longe", "grande", "pequeno", "novo", "antigo",
                "simples", "claro", "pronto", "aberto", "cheio", "vazio", "e", "ou", "de", "com", "sem", "para"));

        final String nome;
        final List<String> palavras;

        Idioma(String nome, List<String> palavras) {
            this.nome = nome;
            this.palavras = palavras;
        }

        static Idioma porNome(String nome) {
            for (Idioma i : values()) {
                if (i.nome.equalsIgnoreCase(nome)) return i;
            }
            throw new IllegalArgumentException("idioma desconhecido '" + nome + "', use latim ou pt.");
        }
    }

    /** O "lorem ipsum" sempre começa com a mesma frase clássica; é tradição. */
    static final String ABERTURA_CLASSICA = "Lorem ipsum dolor sit amet, consectetur adipiscing elit";

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
            executar(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
            System.exit(1);
        }
    }

    static void executar(String[] args) {
        if (args.length < 2) throw new IllegalArgumentException("informe o tipo (paragrafos, frases ou palavras) e a quantidade.");
        String tipo = args[0].toLowerCase(Locale.ROOT);
        int quantidade = lerInteiro(args[1], "a quantidade", 1, 500);
        Idioma idioma = Idioma.LATIM;
        Random sorteio = new Random(); // sem semente: cada execução sai diferente
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--idioma" -> idioma = Idioma.porNome(exigirValor(args, ++i, "--idioma"));
                case "--semente" -> sorteio = new Random(lerInteiro(exigirValor(args, ++i, "--semente"), "a semente", 0, Integer.MAX_VALUE));
                default -> throw new IllegalArgumentException("opção desconhecida '" + args[i] + "'.");
            }
        }
        String texto = switch (tipo) {
            case "paragrafos" -> paragrafos(quantidade, idioma, sorteio);
            case "frases" -> frases(quantidade, idioma, sorteio);
            case "palavras" -> String.join(" ", palavras(quantidade, idioma, sorteio));
            default -> throw new IllegalArgumentException("tipo desconhecido '" + tipo + "', use paragrafos, frases ou palavras.");
        };
        System.out.println(quebrarEm80(texto));
    }

    static String exigirValor(String[] args, int indice, String opcao) {
        if (indice >= args.length) throw new IllegalArgumentException(opcao + " precisa de um valor logo depois.");
        return args[indice];
    }

    static int lerInteiro(String valor, String rotulo, int minimo, int maximo) {
        try {
            int n = Integer.parseInt(valor);
            if (n < minimo || n > maximo) {
                throw new IllegalArgumentException(rotulo + " deve ficar entre " + minimo + " e " + maximo + ", recebi " + n + ".");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rotulo + " precisa ser um inteiro, recebi '" + valor + "'.");
        }
    }

    /** Sorteia palavras evitando repetir a mesma duas vezes seguidas, que soa artificial. */
    static List<String> palavras(int quantidade, Idioma idioma, Random sorteio) {
        List<String> lista = new ArrayList<>();
        String anterior = "";
        while (lista.size() < quantidade) {
            String palavra = idioma.palavras.get(sorteio.nextInt(idioma.palavras.size()));
            if (palavra.equals(anterior)) continue;
            lista.add(palavra);
            anterior = palavra;
        }
        return lista;
    }

    /** Uma frase tem de 6 a 14 palavras, começa com maiúscula, pode ter uma vírgula e termina com ponto. */
    static String frase(Idioma idioma, Random sorteio) {
        List<String> lista = palavras(6 + sorteio.nextInt(9), idioma, sorteio);
        if (lista.size() > 8 && sorteio.nextInt(3) > 0) {
            int posicao = 3 + sorteio.nextInt(lista.size() - 5); // a vírgula nunca cai no começo nem no fim
            lista.set(posicao, lista.get(posicao) + ",");
        }
        String texto = String.join(" ", lista);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1) + ".";
    }

    static String frases(int quantidade, Idioma idioma, Random sorteio) {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) lista.add(frase(idioma, sorteio));
        return String.join(" ", lista);
    }

    /** Um parágrafo tem de 3 a 6 frases; em latim, o primeiro abre com a frase clássica. */
    static String paragrafos(int quantidade, Idioma idioma, Random sorteio) {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            String paragrafo = frases(3 + sorteio.nextInt(4), idioma, sorteio);
            if (i == 0 && idioma == Idioma.LATIM) paragrafo = ABERTURA_CLASSICA + ". " + paragrafo;
            lista.add(paragrafo);
        }
        return String.join("\n\n", lista); // linha em branco entre parágrafos
    }

    /** Quebra cada parágrafo em linhas de até 80 colunas sem cortar palavra no meio. */
    static String quebrarEm80(String texto) {
        StringBuilder saida = new StringBuilder();
        for (String paragrafo : texto.split("\n\n")) {
            int coluna = 0;
            for (String palavra : paragrafo.split(" ")) {
                if (coluna > 0 && coluna + 1 + palavra.length() > 80) {
                    saida.append('\n');
                    coluna = 0;
                } else if (coluna > 0) {
                    saida.append(' ');
                    coluna++;
                }
                saida.append(palavra);
                coluna += palavra.length();
            }
            saida.append("\n\n");
        }
        return saida.toString().stripTrailing();
    }

    static void demo() {
        Random sorteio = new Random(2024); // semente fixa: a demonstração é sempre igual
        System.out.println("=== Dois parágrafos em latim ===");
        System.out.println(quebrarEm80(paragrafos(2, Idioma.LATIM, sorteio)));
        System.out.println();
        System.out.println("=== Dois parágrafos em português ===");
        System.out.println(quebrarEm80(paragrafos(2, Idioma.PORTUGUES, sorteio)));
        System.out.println();
        System.out.println("=== Doze palavras soltas, boas para rótulos e menus ===");
        List<String> soltas = palavras(12, Idioma.PORTUGUES, sorteio);
        for (int i = 0; i < soltas.size(); i++) {
            System.out.printf("%2d. %-12s%s", i + 1, soltas.get(i), (i % 4 == 3) ? "\n" : "");
        }
        System.out.println();
        System.out.println("=== A mesma semente produz o mesmo texto ===");
        String a = frases(2, Idioma.PORTUGUES, new Random(7));
        String b = frases(2, Idioma.PORTUGUES, new Random(7));
        System.out.println("Semente 7, 1ª vez: " + a);
        System.out.println("Semente 7, 2ª vez: " + b);
        System.out.println("Iguais? " + (a.equals(b) ? "sim" : "não"));
    }

    static void uso() {
        System.out.println("""
            Uso: java GeradorLorem.java <paragrafos|frases|palavras> <quantidade> [--idioma latim|pt] [--semente N]
                 java GeradorLorem.java --demo
              quantidade: de 1 a 500
              --idioma    latim (padrão) ou pt
              --semente   um inteiro; a mesma semente repete o mesmo texto
            Exemplos:
              java GeradorLorem.java paragrafos 3
              java GeradorLorem.java frases 5 --idioma pt --semente 42
              java GeradorLorem.java palavras 20""");
    }
}

/*
 * Para ir além:
 *  1. Adicione a opção --html para envolver cada parágrafo em <p> e </p>.
 *  2. Gere nomes de pessoas e endereços fictícios a partir de listas, para preencher formulários de teste.
 *  3. Grave o resultado em um arquivo com Files.writeString quando receber --arquivo <caminho>.
 */
