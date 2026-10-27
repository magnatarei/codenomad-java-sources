/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  12. Validador de e-mail e força de senha
 * ------------------------------------------------------------
 * O que faz: confere um e-mail com regras explícitas (um único @, parte local e
 *            domínio bem formados, domínio com ponto, sem espaços, tamanhos) e dá
 *            uma nota de 0 a 100 para a força de uma senha, com dicas de melhoria.
 * Como usar: java ValidadorEmailSenha.java email ana.souza@exemplo.com.br "joao silva@site.com"
 *            java ValidadorEmailSenha.java senha "Tr0ca-isso!" 123456
 *            java ValidadorEmailSenha.java --demo
 * O que você aprende: validação passo a passo sem expressão regular gigante,
 *            indexOf, split com limite, chars() com filter e anyMatch, Character
 *            para classificar letras e dígitos, record com método, Math.max e Math.min.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidadorEmailSenha {

    /** Senhas que aparecem em qualquer lista de vazamentos: nem a mais longa delas salva. */
    static final List<String> SENHAS_COMUNS = List.of(
            "123456", "12345678", "password", "senha", "senha123", "qwerty", "abc123",
            "111111", "123123", "admin", "letmein", "iloveyou", "brasil", "futebol");

    /** Fileiras do teclado: "asd" e "qwe" são tão previsíveis quanto "abc". */
    static final List<String> FILEIRAS_TECLADO = List.of("qwertyuiop", "asdfghjkl", "zxcvbnm");

    /** Nota de 0 a 100 mais a lista de dicas; o nível é derivado da nota. */
    record Forca(int pontos, List<String> dicas) {
        String nivel() {
            if (pontos < 20) return "muito fraca";
            if (pontos < 40) return "fraca";
            if (pontos < 60) return "razoável";
            if (pontos < 80) return "forte";
            return "muito forte";
        }

        /** Barra de 10 casas: cada # vale 10 pontos. */
        String barra() {
            int cheios = pontos / 10;
            return "[" + "#".repeat(cheios) + "-".repeat(10 - cheios) + "]";
        }
    }

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
        List<String> valores = Arrays.asList(args).subList(1, args.length);
        if (valores.isEmpty()) {
            System.out.println("Erro: informe pelo menos um valor depois de '" + args[0] + "'.");
            uso();
            System.exit(1);
        }
        switch (args[0]) {
            case "email" -> valores.forEach(ValidadorEmailSenha::imprimirEmail);
            case "senha" -> valores.forEach(ValidadorEmailSenha::imprimirSenha);
            default -> {
                System.out.println("Erro: comando desconhecido '" + args[0] + "'. Use email ou senha.");
                uso();
                System.exit(1);
            }
        }
    }

    /** Devolve a lista de problemas; lista vazia significa e-mail bem formado. */
    static List<String> problemasDoEmail(String email) {
        List<String> problemas = new ArrayList<>();
        if (email.isBlank()) {
            problemas.add("está vazio");
            return problemas;
        }
        if (email.chars().anyMatch(Character::isWhitespace)) problemas.add("contém espaço");
        if (email.length() > 254) problemas.add("passa de 254 caracteres");
        long arrobas = email.chars().filter(c -> c == '@').count();
        if (arrobas != 1) {
            problemas.add(arrobas == 0 ? "não tem @" : "tem " + arrobas + " arrobas, só pode ter uma");
            return problemas; // sem um único @ não dá para separar as duas partes
        }
        String local = email.substring(0, email.indexOf('@'));
        String dominio = email.substring(email.indexOf('@') + 1);
        if (local.isEmpty()) problemas.add("não tem nada antes do @");
        if (local.length() > 64) problemas.add("a parte antes do @ passa de 64 caracteres");
        if (local.startsWith(".") || local.endsWith(".")) problemas.add("a parte antes do @ não pode começar nem terminar com ponto");
        if (local.contains("..")) problemas.add("dois pontos seguidos não são permitidos");
        for (char c : local.toCharArray()) {
            if (!permitidoNaParteLocal(c)) {
                problemas.add("caractere não permitido antes do @: '" + c + "'");
                break;
            }
        }
        if (dominio.isEmpty()) {
            problemas.add("não tem nada depois do @");
            return problemas;
        }
        if (!dominio.contains(".")) problemas.add("o domínio precisa de um ponto, como em exemplo.com");
        // split com limite -1 mantém os pedaços vazios: "a..b" vira ["a", "", "b"] e o erro aparece.
        for (String rotulo : dominio.split("\\.", -1)) {
            if (rotulo.isEmpty()) {
                problemas.add("o domínio tem um pedaço vazio entre pontos");
                break;
            }
            if (rotulo.startsWith("-") || rotulo.endsWith("-")) {
                problemas.add("um pedaço do domínio começa ou termina com hífen");
                break;
            }
            if (rotulo.chars().anyMatch(c -> !(ehAsciiLetraOuDigito((char) c) || c == '-'))) {
                problemas.add("o domínio só aceita letras, dígitos e hífen: '" + rotulo + "'");
                break;
            }
        }
        String terminacao = dominio.substring(dominio.lastIndexOf('.') + 1);
        if (dominio.contains(".") && (terminacao.length() < 2 || terminacao.chars().anyMatch(c -> !Character.isLetter(c)))) {
            problemas.add("a terminação '" + terminacao + "' precisa ter ao menos duas letras");
        }
        return problemas;
    }

    static boolean ehAsciiLetraOuDigito(char c) {
        return c < 128 && Character.isLetterOrDigit(c); // 'ç' é letra para o Java, mas não para o e-mail
    }

    static boolean permitidoNaParteLocal(char c) {
        return ehAsciiLetraOuDigito(c) || "._%+-".indexOf(c) >= 0;
    }

    /**
     * Nota = comprimento (3 pontos por símbolo, até 16) + classes presentes (10 cada)
     * + bônus por senha longa e variada, menos as penalidades por padrões previsíveis.
     */
    static Forca avaliarSenha(String senha) {
        List<String> dicas = new ArrayList<>();
        int n = senha.length();
        boolean minuscula = senha.chars().anyMatch(Character::isLowerCase);
        boolean maiuscula = senha.chars().anyMatch(Character::isUpperCase);
        boolean digito = senha.chars().anyMatch(Character::isDigit);
        boolean simbolo = senha.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        int classes = (minuscula ? 1 : 0) + (maiuscula ? 1 : 0) + (digito ? 1 : 0) + (simbolo ? 1 : 0);

        int pontos = Math.min(n, 16) * 3 + classes * 10;
        if (n >= 14 && classes >= 3) pontos += 12;

        if (n < 12) dicas.add("use pelo menos 12 caracteres (tem " + n + ")");
        if (!minuscula) dicas.add("inclua letras minúsculas");
        if (!maiuscula) dicas.add("inclua letras maiúsculas");
        if (!digito) dicas.add("inclua números");
        if (!simbolo) dicas.add("inclua um símbolo como ! # ou -");

        int sequencias = contarSequencias(senha);
        if (sequencias > 0) {
            pontos -= 10 * Math.min(sequencias, 3);
            dicas.add("evite sequências como abc, 123 ou qwe (" + (sequencias == 1 ? "1 encontrada" : sequencias + " encontradas") + ")");
        }
        int repeticoes = contarRepeticoes(senha);
        if (repeticoes > 0) {
            pontos -= 10 * Math.min(repeticoes, 3);
            dicas.add("evite o mesmo símbolo três vezes seguidas (" + repeticoes + " vezes)");
        }
        String baixa = senha.toLowerCase();
        if (SENHAS_COMUNS.stream().anyMatch(baixa::contains)) {
            pontos -= 40;
            dicas.add("contém uma senha de lista de vazamentos; troque por completo");
        }
        return new Forca(Math.max(0, Math.min(100, pontos)), dicas);
    }

    /** Conta janelas de três símbolos em ordem crescente, decrescente ou de fileira do teclado. */
    static int contarSequencias(String senha) {
        String baixa = senha.toLowerCase();
        int total = 0;
        for (int i = 0; i + 2 < baixa.length(); i++) {
            char a = baixa.charAt(i), b = baixa.charAt(i + 1), c = baixa.charAt(i + 2);
            boolean crescente = b == a + 1 && c == b + 1;
            boolean decrescente = b == a - 1 && c == b - 1;
            String trecho = baixa.substring(i, i + 3);
            boolean teclado = FILEIRAS_TECLADO.stream().anyMatch(f -> f.contains(trecho));
            if (crescente || decrescente || teclado) total++;
        }
        return total;
    }

    /** Conta trechos como "aaa" ou "111": três vezes o mesmo símbolo em seguida. */
    static int contarRepeticoes(String senha) {
        int total = 0;
        for (int i = 0; i + 2 < senha.length(); i++) {
            if (senha.charAt(i) == senha.charAt(i + 1) && senha.charAt(i) == senha.charAt(i + 2)) total++;
        }
        return total;
    }

    static void imprimirEmail(String email) {
        List<String> problemas = problemasDoEmail(email);
        if (problemas.isEmpty()) {
            System.out.printf("%-32s -> aprovado%n", email);
        } else {
            System.out.printf("%-32s -> rejeitado: %s%n", email, String.join("; ", problemas));
        }
    }

    static void imprimirSenha(String senha) {
        Forca forca = avaliarSenha(senha);
        System.out.printf("%-24s %s %3d pontos, %s%n", senha, forca.barra(), forca.pontos(), forca.nivel());
        for (String dica : forca.dicas()) System.out.println("    dica: " + dica);
    }

    static void demo() {
        System.out.println("=== Demonstração: cinco e-mails ===");
        List.of("ana.souza@exemplo.com.br", "joao silva@site.com", "maria@@exemplo.com",
                "pedro@localhost", ".lucas@exemplo.com").forEach(ValidadorEmailSenha::imprimirEmail);
        System.out.println();
        System.out.println("=== Demonstração: cinco senhas ===");
        List.of("123456", "senha123", "Abc12345", "Tr0ca-isso!", "Gato#Verde$Pula2026")
                .forEach(ValidadorEmailSenha::imprimirSenha);
    }

    static void uso() {
        System.out.println("""
            Uso: java ValidadorEmailSenha.java email <endereço> [outro endereço ...]
                 java ValidadorEmailSenha.java senha <senha> [outra senha ...]
                 java ValidadorEmailSenha.java --demo
              Coloque entre aspas um valor que tenha espaço ou símbolo especial do terminal.
            Exemplos:
              java ValidadorEmailSenha.java email ana.souza@exemplo.com.br
              java ValidadorEmailSenha.java senha "Tr0ca-isso!" """);
    }
}

/*
 * Para ir além:
 *  1. Leia um arquivo com um e-mail por linha e separe os aprovados dos rejeitados em dois arquivos.
 *  2. Estime quanto tempo um ataque de força bruta levaria a 10 bilhões de tentativas por segundo.
 *  3. Aceite nomes de domínio com acento (como "ação.br") convertendo com java.net.IDN.toASCII.
 */
