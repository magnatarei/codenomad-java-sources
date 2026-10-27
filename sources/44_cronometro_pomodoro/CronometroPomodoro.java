/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  44. Cronômetro Pomodoro no terminal
 * ------------------------------------------------------------
 * O que faz: alterna ciclos de foco e pausa com contagem regressiva que se
 *            atualiza na mesma linha do terminal, avisa (com um bipe) ao fim de
 *            cada ciclo, dá uma pausa longa a cada quatro focos e fecha com um
 *            resumo do tempo concentrado.
 * Como usar: java CronometroPomodoro.java 25 5 4         (foco, pausa, ciclos em minutos)
 *            java CronometroPomodoro.java 10 2 2 --segundos   (os mesmos números em segundos)
 *            java CronometroPomodoro.java --demo
 * O que você aprende: Thread.sleep e InterruptedException, o retorno de
 *            carro \r para redesenhar a linha, records, enum com campo,
 *            Duration e Instant para medir tempo real e validação de argumentos.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CronometroPomodoro {

    /** Os três tipos de ciclo; cada um carrega o rótulo que aparece na tela. */
    enum Tipo {
        FOCO("Foco"), PAUSA("Pausa"), PAUSA_LONGA("Pausa longa");
        final String rotulo;
        Tipo(String rotulo) { this.rotulo = rotulo; }
    }

    record Ciclo(Tipo tipo, int segundos) {}

    /** Configuração completa de uma sessão. O plano é a lista de ciclos na ordem em que vão rodar. */
    record Sessao(int foco, int pausa, int ciclos, int tick) {
        List<Ciclo> plano() {
            var lista = new ArrayList<Ciclo>();
            for (int i = 1; i <= ciclos; i++) {
                lista.add(new Ciclo(Tipo.FOCO, foco));
                if (i == ciclos) break;                         // depois do último foco não há pausa
                // A cada quatro focos a pausa é longa (o triplo); é a regra clássica do método.
                lista.add(i % 4 == 0 ? new Ciclo(Tipo.PAUSA_LONGA, pausa * 3) : new Ciclo(Tipo.PAUSA, pausa));
            }
            return lista;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) {
            System.out.println("=== Demonstração: ciclos de 2 segundos ===");
            rodar(new Sessao(2, 2, 2, 1000));
            return;
        }
        boolean emSegundos = List.of(args).contains("--segundos");
        var numeros = new ArrayList<Integer>();
        for (String a : args) {
            if (a.equals("--segundos")) continue;
            numeros.add(inteiroPositivo(a));
        }
        if (numeros.size() != 3) falhar("preciso de três números: foco, pausa e quantidade de ciclos. Recebi " + numeros.size() + ".");
        int fator = emSegundos ? 1 : 60;
        if (numeros.get(2) > 50) falhar("mais de 50 ciclos não faz sentido numa sessão; recebi " + numeros.get(2) + ".");
        rodar(new Sessao(numeros.get(0) * fator, numeros.get(1) * fator, numeros.get(2), 1000));
    }

    static int inteiroPositivo(String texto) {
        try {
            int n = Integer.parseInt(texto);
            if (n <= 0) falhar("os valores precisam ser maiores que zero; recebi " + n + ".");
            return n;
        } catch (NumberFormatException e) {
            falhar("'" + texto + "' não é um número inteiro.");
            return 0;                                           // nunca chega aqui; o compilador exige um retorno
        }
    }

    static void rodar(Sessao sessao) throws InterruptedException {
        List<Ciclo> plano = sessao.plano();
        int totalFoco = plano.stream().filter(c -> c.tipo() == Tipo.FOCO).mapToInt(Ciclo::segundos).sum();
        System.out.printf("Plano: %d ciclos de foco de %s, pausas de %s, pausa longa a cada 4 focos.%n",
                sessao.ciclos(), tempo(sessao.foco()), tempo(sessao.pausa()));
        System.out.println("Tempo total de foco previsto: " + tempo(totalFoco) + ". Ctrl+C interrompe.");
        System.out.println();
        Instant inicio = Instant.now();
        int focosFeitos = 0;
        for (int i = 0; i < plano.size(); i++) {
            Ciclo ciclo = plano.get(i);
            if (ciclo.tipo() == Tipo.FOCO) focosFeitos++;
            String titulo = ciclo.tipo() == Tipo.FOCO ? "Foco " + focosFeitos + "/" + sessao.ciclos() : ciclo.tipo().rotulo;
            System.out.println("[" + hora() + "] " + titulo + " começou (" + tempo(ciclo.segundos()) + ")");
            contarRegressivo(ciclo, sessao.tick());
            // \u0007 é o caractere de campainha: muitos terminais tocam um bipe ao recebê-lo.
            System.out.println("[" + hora() + "] " + titulo + " terminou!\u0007");
        }
        Duration real = Duration.between(inicio, Instant.now());
        System.out.println();
        System.out.println("Sessão concluída: " + focosFeitos + " focos, " + tempo(totalFoco) + " concentrado, "
                + tempo((int) real.toSeconds()) + " de relógio.");
    }

    /**
     * Imprime "Foco      12:34 [#######...]" e volta ao começo da linha com \r, sem pular linha,
     * para a próxima impressão sobrescrever a anterior. O print (sem ln) exige flush manual.
     */
    static void contarRegressivo(Ciclo ciclo, int tickMs) throws InterruptedException {
        for (int restante = ciclo.segundos(); restante >= 0; restante--) {
            System.out.print("\r  " + String.format("%-12s", ciclo.tipo().rotulo) + tempo(restante) + "  " + barra(restante, ciclo.segundos()));
            System.out.flush();
            if (restante > 0) Thread.sleep(tickMs);            // dorme um tick por segundo contado
        }
        System.out.println();                                   // agora sim, fecha a linha
    }

    /** Barra de 20 posições que esvazia conforme o tempo passa. */
    static String barra(int restante, int total) {
        int cheios = total == 0 ? 0 : (int) Math.round(20.0 * restante / total);
        return "[" + "#".repeat(cheios) + ".".repeat(20 - cheios) + "]";
    }

    /** 125 segundos viram "02:05"; acima de uma hora, "1:02:05". */
    static String tempo(int segundos) {
        int h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    static String hora() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    static void falhar(String motivo) {
        System.out.println("Erro: " + motivo);
        System.exit(1);
    }

    static void uso() {
        System.out.println("""
            Uso: java CronometroPomodoro.java <foco> <pausa> <ciclos> [--segundos]
              foco     minutos de concentração (o clássico é 25)
              pausa    minutos de descanso entre focos (o clássico é 5; a cada 4 focos a pausa triplica)
              ciclos   quantos focos fazer antes de encerrar
              --segundos   interpreta foco e pausa em segundos, útil para testar
            Exemplo: java CronometroPomodoro.java 25 5 4
            Demonstração: java CronometroPomodoro.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Grave cada sessão concluída num arquivo de histórico (data, focos, minutos) e mostre o total da semana.
 *  2. Permita pausar e retomar a contagem lendo uma tecla numa thread separada.
 *  3. Aceite --sem-bipe e --barra 40 para configurar o aviso sonoro e a largura da barra.
 */
