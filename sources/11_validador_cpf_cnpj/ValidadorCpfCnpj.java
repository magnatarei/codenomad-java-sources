/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  11. Validador de CPF e CNPJ
 * ------------------------------------------------------------
 * O que faz: valida os dígitos verificadores de CPF e CNPJ, aceitando o número
 *            com ou sem pontuação, rejeita sequências repetidas (111.111.111-11),
 *            formata no padrão oficial e gera números válidos para usar em testes.
 * Como usar: java ValidadorCpfCnpj.java 529.982.247-25 11444777000161
 *            java ValidadorCpfCnpj.java --gerar cpf 3
 *            java ValidadorCpfCnpj.java --demo
 * O que você aprende: aritmética modular (regra do módulo 11), arrays de pesos,
 *            record com método, switch expression, replaceAll com grupos de
 *            captura, streams simples (distinct, filter, count) e código de saída.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ValidadorCpfCnpj {

    /** Pesos de cada dígito na soma, na mesma ordem em que os dígitos aparecem. */
    static final int[] PESOS_CPF_1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    static final int[] PESOS_CPF_2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    enum Tipo { CPF, CNPJ }

    /** O resultado de uma análise: o que entrou, o que foi reconhecido e por que passou ou não. */
    record Resultado(String entrada, String tipo, boolean valido, String motivo) {
        String rotulo() {
            return valido ? "VÁLIDO" : "inválido";
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
        if (args[0].equals("--gerar")) {
            try {
                gerarPorLinhaDeComando(args);
            } catch (IllegalArgumentException e) {
                System.out.println("Erro: " + e.getMessage());
                uso();
                System.exit(1);
            }
            return;
        }
        List<Resultado> resultados = new ArrayList<>();
        for (String arg : args) resultados.add(analisar(arg));
        imprimirTabela(resultados);
        // Código de saída 1 quando algum número falhou: útil para scripts que chamam este programa.
        boolean todosValidos = resultados.stream().filter(Resultado::valido).count() == resultados.size();
        System.exit(todosValidos ? 0 : 1);
    }

    /** Mantém só os dígitos: "529.982.247-25" vira "52998224725". */
    static String somenteDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }

    /** Verdadeiro para "11111111111" e afins: passam na conta, mas não existem de verdade. */
    static boolean todosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    /**
     * Regra do módulo 11: multiplica cada dígito pelo seu peso, soma tudo e pega o
     * resto da divisão por 11. Resto 0 ou 1 vira dígito 0; os demais viram 11 menos o resto.
     */
    static int digitoVerificador(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i]; // '7' - '0' é 7: truque clássico com char
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Calcula os dois verificadores a partir da base (9 dígitos no CPF, 12 no CNPJ). */
    static String digitosVerificadores(String base, Tipo tipo) {
        int[] pesos1 = tipo == Tipo.CPF ? PESOS_CPF_1 : PESOS_CNPJ_1;
        int[] pesos2 = tipo == Tipo.CPF ? PESOS_CPF_2 : PESOS_CNPJ_2;
        int primeiro = digitoVerificador(base, pesos1);
        int segundo = digitoVerificador(base + primeiro, pesos2); // o segundo inclui o primeiro na conta
        return "" + primeiro + segundo;
    }

    static Resultado analisar(String entrada) {
        String digitos = somenteDigitos(entrada);
        Tipo tipo = switch (digitos.length()) {
            case 11 -> Tipo.CPF;
            case 14 -> Tipo.CNPJ;
            default -> null;
        };
        if (tipo == null) {
            return new Resultado(entrada, "?", false, "precisa ter 11 dígitos (CPF) ou 14 (CNPJ), tem " + digitos.length());
        }
        if (todosIguais(digitos)) {
            return new Resultado(entrada, tipo.name(), false, "sequência com todos os dígitos iguais");
        }
        int tamanhoBase = digitos.length() - 2;
        String esperado = digitosVerificadores(digitos.substring(0, tamanhoBase), tipo);
        String informado = digitos.substring(tamanhoBase);
        if (!esperado.equals(informado)) {
            return new Resultado(entrada, tipo.name(), false, "verificadores deveriam ser " + esperado + ", vieram " + informado);
        }
        return new Resultado(entrada, tipo.name(), true, "confere, formatado: " + formatar(digitos));
    }

    /** Aplica a máscara oficial: 000.000.000-00 para CPF e 00.000.000/0000-00 para CNPJ. */
    static String formatar(String digitos) {
        if (digitos.length() == 11) {
            return digitos.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return digitos.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    /** Sorteia uma base, calcula os verificadores e descarta a sequência repetida. Serve só para teste. */
    static String gerar(Tipo tipo, Random sorteio) {
        while (true) {
            StringBuilder base = new StringBuilder();
            int tamanho = tipo == Tipo.CPF ? 9 : 8;
            for (int i = 0; i < tamanho; i++) base.append(sorteio.nextInt(10));
            if (tipo == Tipo.CNPJ) base.append("0001"); // 0001 identifica a matriz; filiais seguem 0002, 0003...
            String completo = base + digitosVerificadores(base.toString(), tipo);
            if (!todosIguais(completo)) return formatar(completo);
        }
    }

    static void gerarPorLinhaDeComando(String[] args) {
        if (args.length < 2) throw new IllegalArgumentException("diga o que gerar: --gerar cpf ou --gerar cnpj.");
        Tipo tipo = switch (args[1].toLowerCase()) {
            case "cpf" -> Tipo.CPF;
            case "cnpj" -> Tipo.CNPJ;
            default -> throw new IllegalArgumentException("tipo desconhecido '" + args[1] + "', use cpf ou cnpj.");
        };
        int quantidade = 1;
        if (args.length >= 3) {
            try {
                quantidade = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("a quantidade precisa ser um inteiro, recebi '" + args[2] + "'.");
            }
            if (quantidade < 1 || quantidade > 100) throw new IllegalArgumentException("a quantidade deve ficar entre 1 e 100.");
        }
        Random sorteio = new Random();
        for (int i = 0; i < quantidade; i++) System.out.println(gerar(tipo, sorteio));
    }

    static void imprimirTabela(List<Resultado> resultados) {
        System.out.printf("%-20s | %-4s | %-8s | %s%n", "Entrada", "Tipo", "Situação", "Motivo");
        System.out.println("-".repeat(92));
        for (Resultado r : resultados) {
            System.out.printf("%-20s | %-4s | %-8s | %s%n", r.entrada(), r.tipo(), r.rotulo(), r.motivo());
        }
    }

    /** Mostra a soma ponderada do primeiro verificador de um CPF, para o aluno enxergar a regra. */
    static void explicar(String digitos) {
        StringBuilder conta = new StringBuilder();
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            int d = digitos.charAt(i) - '0';
            soma += d * PESOS_CPF_1[i];
            conta.append(d).append("x").append(PESOS_CPF_1[i]).append(i < 8 ? " + " : "");
        }
        int resto = soma % 11;
        System.out.println("  " + conta + " = " + soma);
        System.out.println("  " + soma + " mod 11 = " + resto + "  ->  primeiro dígito = " + (resto < 2 ? 0 : 11 - resto));
        System.out.println("  O segundo dígito repete a conta com o primeiro incluído e os pesos de 11 a 2.");
    }

    static void demo() {
        System.out.println("=== Demonstração: validando uma lista de CPFs e CNPJs ===");
        System.out.println();
        List<String> casos = List.of(
                "529.982.247-25", "52998224725", "123.456.789-09", "111.111.111-11", "529.982.247-26",
                "11.444.777/0001-61", "11444777000161", "11.444.777/0001-62", "00.000.000/0000-00", "12345", "abc");
        List<Resultado> resultados = new ArrayList<>();
        for (String caso : casos) resultados.add(analisar(caso));
        imprimirTabela(resultados);
        long validos = resultados.stream().filter(Resultado::valido).count();
        System.out.println();
        System.out.println("Resumo: " + validos + " válidos e " + (resultados.size() - validos) + " inválidos em " + resultados.size() + " casos.");
        System.out.println();
        System.out.println("=== Dois números gerados para teste (semente fixa, o resultado se repete) ===");
        Random sorteio = new Random(42);
        String cpf = gerar(Tipo.CPF, sorteio);
        String cnpj = gerar(Tipo.CNPJ, sorteio);
        System.out.println("CPF  gerado: " + cpf + "  -> " + analisar(cpf).rotulo());
        System.out.println("CNPJ gerado: " + cnpj + "  -> " + analisar(cnpj).rotulo());
        System.out.println();
        System.out.println("Como a conta funciona para o CPF 529.982.247-25:");
        explicar("52998224725");
    }

    static void uso() {
        System.out.println("""
            Uso: java ValidadorCpfCnpj.java <número> [outro número ...]
                 java ValidadorCpfCnpj.java --gerar cpf|cnpj [quantidade]
                 java ValidadorCpfCnpj.java --demo
              O número pode vir com ou sem pontuação: 529.982.247-25 ou 52998224725.
              Com 11 dígitos é tratado como CPF; com 14, como CNPJ.
            Exemplos:
              java ValidadorCpfCnpj.java 529.982.247-25 11444777000161
              java ValidadorCpfCnpj.java --gerar cnpj 5""");
    }
}

/*
 * Para ir além:
 *  1. Leia os números de um arquivo texto, um por linha, e imprima só os inválidos com o motivo.
 *  2. Descubra o estado de origem de um CPF pelo nono dígito (a tabela da Receita tem 10 regiões).
 *  3. Aceite também o RENAVAM ou o PIS, que usam variações da mesma regra do módulo 11.
 */
