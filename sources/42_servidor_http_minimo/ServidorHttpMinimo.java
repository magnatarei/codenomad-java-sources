/**
 * ============================================================
 * CodeNomad Academy  |  Sources Java  |  42. Servidor HTTP mínimo
 * ------------------------------------------------------------
 * O que faz: sobe um servidor HTTP escrito à mão sobre ServerSocket. Serve os
 *            arquivos de uma pasta (com o content-type certo e proteção contra
 *            "../"), responde JSON em /api/hora e /api/eco, e registra cada
 *            pedido no terminal com hora, método, caminho e status.
 * Como usar: java ServidorHttpMinimo.java ./site 8080
 *            java ServidorHttpMinimo.java --demo
 * O que você aprende: como é um pedido HTTP por dentro (linha de pedido,
 *            cabeçalhos, linha vazia), ServerSocket e Socket, threads por
 *            conexão, Path.normalize contra path traversal, records,
 *            switch expressions e HttpURLConnection para testar a si mesmo.
 * Requisitos: Java 17 ou superior, só biblioteca padrão
 * Licença MIT. Um projeto da CodeNomad Academy (codenomad.org)
 * ============================================================
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServidorHttpMinimo {

    /** O que o servidor extrai de um pedido: só o necessário para responder. */
    record Pedido(String metodo, String caminho, Map<String, String> parametros) {}

    /** Uma resposta pronta para ser escrita no socket. */
    record Resposta(int status, String tipo, byte[] corpo) {
        static Resposta texto(int status, String tipo, String conteudo) {
            return new Resposta(status, tipo, conteudo.getBytes(StandardCharsets.UTF_8));
        }
    }

    static final Map<Integer, String> FRASES = Map.of(200, "OK", 400, "Bad Request", 403, "Forbidden",
            404, "Not Found", 405, "Method Not Allowed");
    static final Map<String, String> TIPOS = Map.of("html", "text/html; charset=utf-8", "css", "text/css",
            "js", "text/javascript", "json", "application/json", "txt", "text/plain; charset=utf-8",
            "png", "image/png", "jpg", "image/jpeg", "svg", "image/svg+xml", "ico", "image/x-icon");

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        if (args.length == 0) { uso(); return; }
        if (args[0].equals("--demo")) { demo(); return; }
        Path pasta = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(pasta)) {
            System.out.println("Erro: a pasta '" + args[0] + "' não existe ou não é uma pasta.");
            System.exit(1);
        }
        int porta = 8080;
        if (args.length > 1) {
            try { porta = Integer.parseInt(args[1]); } catch (NumberFormatException e) { porta = -1; }
            if (porta < 1 || porta > 65535) {
                System.out.println("Erro: a porta precisa ser um número entre 1 e 65535, recebi '" + args[1] + "'.");
                System.exit(1);
            }
        }
        // Só escutamos no loopback: o servidor fica visível apenas para este computador.
        try (var servidor = new ServerSocket(porta, 50, InetAddress.getLoopbackAddress())) {
            System.out.println("Servindo " + pasta + " em http://localhost:" + servidor.getLocalPort() + " (Ctrl+C encerra)");
            atenderParaSempre(servidor, pasta);
        } catch (java.net.BindException e) {
            System.out.println("Erro: a porta " + porta + " já está em uso. Escolha outra.");
            System.exit(1);
        }
    }

    /** Laço principal: cada conexão ganha uma thread; o laço acaba quando o ServerSocket é fechado. */
    static void atenderParaSempre(ServerSocket servidor, Path pasta) {
        while (!servidor.isClosed()) {
            try {
                Socket cliente = servidor.accept();         // bloqueia até alguém conectar
                new Thread(() -> atender(cliente, pasta)).start();
            } catch (IOException e) {
                if (!servidor.isClosed()) System.out.println("Falha ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    static void atender(Socket cliente, Path pasta) {
        try (cliente) {                                     // try-with-resources fecha o socket no fim
            var entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.ISO_8859_1));
            Pedido pedido = lerPedido(entrada);
            Resposta resposta = pedido == null ? Resposta.texto(400, TIPOS.get("txt"), "Pedido malformado")
                    : responder(pedido, pasta);
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.printf("[%s] %s %s -> %d (%d bytes)%n", hora, pedido == null ? "?" : pedido.metodo(),
                    pedido == null ? "?" : pedido.caminho(), resposta.status(), resposta.corpo().length);
            escrever(cliente.getOutputStream(), resposta);
        } catch (IOException e) {
            System.out.println("Conexão perdida: " + e.getMessage());
        }
    }

    /** Lê "GET /caminho?a=1 HTTP/1.1" e os cabeçalhos até a linha vazia. Devolve null se vier lixo. */
    static Pedido lerPedido(BufferedReader entrada) throws IOException {
        String primeira = entrada.readLine();
        if (primeira == null) return null;
        String[] partes = primeira.split(" ");
        if (partes.length != 3 || !partes[2].startsWith("HTTP/")) return null;
        // Os cabeçalhos terminam numa linha vazia; aqui só os consumimos, sem usar.
        for (String linha = entrada.readLine(); linha != null && !linha.isEmpty(); linha = entrada.readLine()) { }
        String alvo = partes[1];
        int interrogacao = alvo.indexOf('?');
        String caminho = interrogacao < 0 ? alvo : alvo.substring(0, interrogacao);
        String consulta = interrogacao < 0 ? "" : alvo.substring(interrogacao + 1);
        return new Pedido(partes[0], caminho, lerParametros(consulta));
    }

    /** "msg=ol%C3%A1&n=2" vira {msg=olá, n=2}. URLDecoder desfaz o %XX e troca + por espaço. */
    static Map<String, String> lerParametros(String consulta) {
        var mapa = new LinkedHashMap<String, String>();
        for (String par : consulta.split("&")) {
            if (par.isEmpty()) continue;
            int igual = par.indexOf('=');
            String chave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            mapa.put(URLDecoder.decode(chave, StandardCharsets.UTF_8), URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return mapa;
    }

    /** Roteamento: as rotas /api primeiro, o resto é arquivo. */
    static Resposta responder(Pedido p, Path pasta) {
        if (!p.metodo().equals("GET")) return Resposta.texto(405, TIPOS.get("txt"), "Só aceito GET");
        return switch (p.caminho()) {
            case "/api/hora" -> Resposta.texto(200, TIPOS.get("json"), json(Map.of("hora",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "fuso", java.time.ZoneId.systemDefault().getId())));
            case "/api/eco" -> {
                var eco = new LinkedHashMap<String, String>(p.parametros());
                eco.put("caminho", p.caminho());
                eco.put("quantidade", String.valueOf(p.parametros().size()));
                yield Resposta.texto(200, TIPOS.get("json"), json(eco));
            }
            default -> arquivo(pasta, p.caminho());
        };
    }

    static Resposta arquivo(Path pasta, String caminho) {
        String relativo = caminho.equals("/") ? "index.html" : URLDecoder.decode(caminho.substring(1), StandardCharsets.UTF_8);
        // normalize resolve "../"; se o resultado sair da pasta, é tentativa de ler o que não deve.
        Path alvo = pasta.resolve(relativo).normalize();
        if (!alvo.startsWith(pasta)) return Resposta.texto(403, TIPOS.get("txt"), "Caminho fora da pasta servida");
        if (!Files.isRegularFile(alvo)) return Resposta.texto(404, TIPOS.get("txt"), "Não encontrei " + caminho);
        String nome = alvo.getFileName().toString();
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";
        try {
            return new Resposta(200, TIPOS.getOrDefault(extensao, "application/octet-stream"), Files.readAllBytes(alvo));
        } catch (IOException e) {
            return Resposta.texto(404, TIPOS.get("txt"), "Não consegui ler " + caminho);
        }
    }

    /** Escreve a resposta no formato HTTP: linha de status, cabeçalhos, linha vazia e corpo. */
    static void escrever(OutputStream saida, Resposta r) throws IOException {
        String cabecalho = "HTTP/1.1 " + r.status() + " " + FRASES.getOrDefault(r.status(), "OK") + "\r\n"
                + "Content-Type: " + r.tipo() + "\r\n"
                + "Content-Length: " + r.corpo().length + "\r\n"
                + "Connection: close\r\n\r\n";             // \r\n é obrigatório no HTTP, não só \n
        saida.write(cabecalho.getBytes(StandardCharsets.ISO_8859_1));
        saida.write(r.corpo());
        saida.flush();
    }

    /** Monta um objeto JSON plano a partir de um mapa, escapando aspas e barras. */
    static String json(Map<String, String> dados) {
        var sb = new StringBuilder("{");
        dados.forEach((chave, valor) -> sb.append(sb.length() > 1 ? ", " : "")
                .append('"').append(escapar(chave)).append("\": \"").append(escapar(valor)).append('"'));
        return sb.append("}").toString();
    }

    static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /** Sobe numa porta livre, faz pedidos a si mesmo com HttpURLConnection e desliga. */
    static void demo() throws IOException {
        System.out.println("=== Demonstração: servidor HTTP mínimo ===");
        Path pasta = Files.createTempDirectory("codenomad-site");
        Files.writeString(pasta.resolve("index.html"), "<h1>Olá, CodeNomad!</h1>\n<p>Servido por Java puro.</p>\n");
        Files.writeString(pasta.resolve("estilo.css"), "h1 { color: teal; }\n");
        // Porta 0 pede ao sistema uma porta livre qualquer; evita conflito com outros programas.
        var servidor = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread laco = new Thread(() -> atenderParaSempre(servidor, pasta));
        laco.setDaemon(true);                               // uma daemon thread não segura o programa aberto
        laco.start();
        String base = "http://localhost:" + servidor.getLocalPort();
        System.out.println("Servindo " + pasta + " em " + base);
        for (String rota : new String[] {"/api/hora", "/", "/estilo.css", "/api/eco?msg=ol%C3%A1+mundo&n=2",
                "/nao-existe.txt", "/../segredo.txt"}) {
            var conexao = (HttpURLConnection) URI.create(base + rota).toURL().openConnection();
            conexao.setReadTimeout(5_000);
            int status = conexao.getResponseCode();
            var fluxo = status >= 400 ? conexao.getErrorStream() : conexao.getInputStream();
            String corpo = new String(fluxo.readAllBytes(), StandardCharsets.UTF_8).strip();
            System.out.println("  cliente: " + status + " " + conexao.getContentType() + " | " + corpo.replace("\n", " "));
        }
        servidor.close();                                   // acorda o accept() e encerra o laço
        for (String nome : new String[] {"index.html", "estilo.css"}) Files.deleteIfExists(pasta.resolve(nome));
        Files.deleteIfExists(pasta);
        System.out.println("Servidor desligado e pasta temporária removida.");
    }

    static void uso() {
        System.out.println("""
            Uso: java ServidorHttpMinimo.java <pasta> [porta]
              pasta   a pasta cujos arquivos serão servidos (index.html responde em /)
              porta   padrão 8080; só aceita conexões deste computador (localhost)
            Rotas especiais: /api/hora (JSON com a hora) e /api/eco?chave=valor (devolve os parâmetros)
            Demonstração: java ServidorHttpMinimo.java --demo""");
    }
}

/*
 * Para ir além:
 *  1. Leia o cabeçalho Content-Length e aceite POST em /api/eco devolvendo o corpo recebido.
 *  2. Liste o conteúdo da pasta em HTML quando o caminho for um diretório sem index.html.
 *  3. Troque a thread por conexão por um ExecutorService com um número fixo de threads.
 */
