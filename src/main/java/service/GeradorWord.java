package service;

import domain.Capitulo;
import domain.Cliente;
import domain.Dinheiro;
import domain.Empresa;
import domain.EstadoOrcamento;
import domain.LinhaOrcamento;
import domain.Orcamento;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gera o orçamento em Word (.docx), no mesmo modelo do PDF, para que possa ser alterado à mão.
 * O ficheiro é escrito diretamente (OOXML), sem bibliotecas externas. Com {@code o == null}
 * gera o modelo em branco (duas páginas com as tabelas vazias).
 */
public class GeradorWord {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String FONTE = "Arial";
    private static final String AZUL = "174073";
    private static final int W = 11040;                       // largura total (dxa)
    private static final int ESQ = 4560;                      // coluna da esquerda do cabeçalho
    private static final int DIR = W - ESQ;                   // coluna da direita
    private static final int[] COLS = {700, 6060, 740, 1480, 2060};
    private static final int LINHA = 500;                     // altura mínima de uma linha da tabela
    private static final int LINHAS_PAG1 = 16;                // linhas livres da 1.ª página (modelo em branco)
    private static final int LINHAS_PAG2 = 27;

    private Orcamento o;
    private Empresa e;
    private byte[] logo;
    private int logoLarg = 141;
    private int logoAlt = 120;

    public void gerar(Orcamento orcamento, Empresa empresa, Path destino) throws IOException {
        this.o = orcamento;
        this.e = empresa;
        carregarLogotipo();

        String documento = documento();
        String rodape = rodape();
        Path pasta = destino.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        try (OutputStream out = Files.newOutputStream(destino);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            entrada(zip, "[Content_Types].xml", CONTENT_TYPES);
            entrada(zip, "_rels/.rels", RELS);
            entrada(zip, "word/document.xml", documento);
            entrada(zip, "word/footer1.xml", rodape);
            entrada(zip, "word/styles.xml", ESTILOS);
            entrada(zip, "word/_rels/document.xml.rels", DOC_RELS);
            if (logo != null) {
                zip.putNextEntry(new ZipEntry("word/media/logo.png"));
                zip.write(logo);
                zip.closeEntry();
            }
        }
    }

    private static void entrada(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ------------------------------------------------------------------ logótipo

    private void carregarLogotipo() {
        try {
            String caminho = e.getCaminhoLogotipo();
            if (caminho != null && !caminho.isBlank() && Files.exists(Path.of(caminho))) {
                logo = Files.readAllBytes(Path.of(caminho));
            } else {
                try (InputStream in = GeradorWord.class.getResourceAsStream("/logo.png")) {
                    logo = in == null ? null : in.readAllBytes();
                }
            }
            if (logo != null) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(logo));
                if (img != null) {
                    logoAlt = 120;
                    logoLarg = Math.round(120f * img.getWidth() / img.getHeight());
                } else {
                    logo = null;
                }
            }
        } catch (IOException | RuntimeException ex) {
            logo = null;
        }
    }

    // ------------------------------------------------------------------ XML base

    private static String esc(String t) {
        if (t == null) {
            return "";
        }
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Um "run" de texto. tam em meios-pontos; espaco em vigésimos de ponto. */
    private static String run(String texto, int tam, boolean negrito, String cor, String fonte, int espaco) {
        StringBuilder sb = new StringBuilder("<w:r><w:rPr>");
        String f = fonte == null ? FONTE : fonte;
        sb.append("<w:rFonts w:ascii=\"").append(f).append("\" w:hAnsi=\"").append(f).append("\" w:cs=\"")
                .append(f).append("\"/>");
        if (negrito) {
            sb.append("<w:b/><w:bCs/>");
        }
        if (cor != null) {
            sb.append("<w:color w:val=\"").append(cor).append("\"/>");
        }
        if (espaco != 0) {
            sb.append("<w:spacing w:val=\"").append(espaco).append("\"/>");
        }
        sb.append("<w:sz w:val=\"").append(tam).append("\"/><w:szCs w:val=\"").append(tam).append("\"/></w:rPr>");
        sb.append("<w:t xml:space=\"preserve\">").append(esc(texto)).append("</w:t></w:r>");
        return sb.toString();
    }

    private static String run(String texto, int tam, boolean negrito) {
        return run(texto, tam, negrito, null, null, 0);
    }

    private static String par(String runs, String alinhamento, int antes, int depois) {
        return "<w:p><w:pPr><w:spacing w:before=\"" + antes + "\" w:after=\"" + depois + "\"/><w:jc w:val=\""
                + alinhamento + "\"/></w:pPr>" + runs + "</w:p>";
    }

    private static String parVazio(int alturaExata) {
        return "<w:p><w:pPr><w:spacing w:before=\"0\" w:after=\"0\" w:line=\"" + alturaExata
                + "\" w:lineRule=\"exact\"/></w:pPr></w:p>";
    }

    /** Bordas: t, b, l, r (true = linha preta). */
    private static String bordas(boolean t, boolean b, boolean l, boolean r) {
        return "<w:tcBorders>" + borda("top", t) + borda("left", l) + borda("bottom", b) + borda("right", r)
                + "</w:tcBorders>";
    }

    private static String borda(String lado, boolean ligada) {
        return ligada
                ? "<w:" + lado + " w:val=\"single\" w:sz=\"6\" w:space=\"0\" w:color=\"000000\"/>"
                : "<w:" + lado + " w:val=\"nil\"/>";
    }

    private static String celula(int largura, int span, String bordas, String fundo, String vAlign,
                                 int margem, String conteudo) {
        StringBuilder sb = new StringBuilder("<w:tc><w:tcPr><w:tcW w:w=\"").append(largura)
                .append("\" w:type=\"dxa\"/>");
        if (span > 1) {
            sb.append("<w:gridSpan w:val=\"").append(span).append("\"/>");
        }
        sb.append(bordas);
        if (fundo != null) {
            sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(fundo).append("\"/>");
        }
        sb.append("<w:tcMar><w:top w:w=\"40\" w:type=\"dxa\"/><w:left w:w=\"").append(margem)
                .append("\" w:type=\"dxa\"/><w:bottom w:w=\"40\" w:type=\"dxa\"/><w:right w:w=\"").append(margem)
                .append("\" w:type=\"dxa\"/></w:tcMar>");
        sb.append("<w:vAlign w:val=\"").append(vAlign).append("\"/></w:tcPr>");
        sb.append(conteudo.isEmpty() ? "<w:p/>" : conteudo).append("</w:tc>");
        return sb.toString();
    }

    private static String linha(String celulas, int altura, boolean exata, boolean cabecalho) {
        return "<w:tr><w:trPr><w:cantSplit/><w:trHeight w:val=\"" + altura + "\" w:hRule=\""
                + (exata ? "exact" : "atLeast") + "\"/>" + (cabecalho ? "<w:tblHeader/>" : "")
                + "</w:trPr>" + celulas + "</w:tr>";
    }

    private static String tabela(int largura, int[] grelha, String linhas) {
        StringBuilder sb = new StringBuilder("<w:tbl><w:tblPr><w:tblW w:w=\"").append(largura)
                .append("\" w:type=\"dxa\"/><w:tblLayout w:type=\"fixed\"/>")
                .append("<w:tblCellMar><w:left w:w=\"0\" w:type=\"dxa\"/><w:right w:w=\"0\" w:type=\"dxa\"/>")
                .append("</w:tblCellMar></w:tblPr><w:tblGrid>");
        for (int g : grelha) {
            sb.append("<w:gridCol w:w=\"").append(g).append("\"/>");
        }
        return sb.append("</w:tblGrid>").append(linhas).append("</w:tbl>").toString();
    }

    /** «O R Ç A M E N T O»: letras espaçadas, palavras separadas por dois espaços. */
    private static String espacado(String t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == ' ') {
                sb.append("  ");
            } else {
                sb.append(ch);
                if (i < t.length() - 1 && t.charAt(i + 1) != ' ') {
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ documento

    private String documento() {
        StringBuilder corpo = new StringBuilder();
        corpo.append(cabecalho());
        corpo.append(parVazio(560));
        if (o == null) {
            corpo.append(tabelaDetalhesBranco(LINHAS_PAG1));
            corpo.append("<w:p><w:pPr><w:spacing w:before=\"0\" w:after=\"0\" w:line=\"20\" w:lineRule=\"exact\"/></w:pPr>"
                    + "<w:r><w:br w:type=\"page\"/></w:r></w:p>");
            corpo.append(tabelaDetalhesBranco(LINHAS_PAG2));
        } else {
            corpo.append(tabelaDetalhes());
        }
        corpo.append(parVazio(20));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document " + NAMESPACES + "><w:body>" + corpo
                + "<w:sectPr><w:footerReference w:type=\"default\" r:id=\"rIdFooter\"/>"
                + "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
                + "<w:pgMar w:top=\"560\" w:right=\"433\" w:bottom=\"1300\" w:left=\"433\" w:header=\"400\" "
                + "w:footer=\"500\" w:gutter=\"0\"/></w:sectPr></w:body></w:document>";
    }

    private String cabecalho() {
        String semBordas = bordas(false, false, false, false);
        String esq = colunaEsquerda();
        String dir = tabelaOrcamento() + parVazio(360) + tabelaCliente() + parVazio(20);
        String linha = "<w:tr>" + celula(ESQ, 1, semBordas, null, "top", 0, esq)
                + celula(DIR, 1, semBordas, null, "top", 0, dir) + "</w:tr>";
        return tabela(W, new int[]{ESQ, DIR}, linha);
    }

    private String colunaEsquerda() {
        StringBuilder sb = new StringBuilder();
        if (logo != null) {
            long cx = Math.round(logoLarg * 9525.0);
            long cy = Math.round(logoAlt * 9525.0);
            sb.append("<w:p><w:pPr><w:spacing w:before=\"60\" w:after=\"120\"/><w:jc w:val=\"center\"/></w:pPr><w:r>"
                    + "<w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
                    + "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/>"
                    + "<wp:docPr id=\"1\" name=\"Logótipo\" descr=\"Logótipo\"/>"
                    + "<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>"
                    + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
                    + "<pic:pic><pic:nvPicPr><pic:cNvPr id=\"0\" name=\"logo.png\"/><pic:cNvPicPr/></pic:nvPicPr>"
                    + "<pic:blipFill><a:blip r:embed=\"rIdLogo\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>"
                    + "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
                    + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>"
                    + "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>");
        }
        sb.append(par(run(nz(e.getDesignacao()), 46, true), "center", 0, 100));
        String nif = nz(e.getNif()).isBlank() ? "" : formatarNif(e.getNif());
        String tel = formatarTelefone(e.getTelefone());
        if (!nif.isBlank()) {
            sb.append(par(run("NIF: ", 26, false) + run(nif, 26, true), "center", 0, 40));
        }
        if (!tel.isBlank()) {
            sb.append(par(run("Tel: ", 31, false) + run(tel, 31, true), "center", 0, 40));
        }
        if (!nz(e.getEmail()).isBlank()) {
            sb.append(par(run(e.getEmail(), 24, false), "center", 0, 40));
        }
        if (!nz(e.getWebsite()).isBlank()) {
            sb.append(par(run(e.getWebsite(), 24, false), "center", 0, 0));
        }
        return sb.toString();
    }

    private String tabelaOrcamento() {
        int c1 = 1640, c2 = 2280, c3 = 2560;
        String titulo = "ORÇAMENTO";
        if (o != null && o.getEstado() == EstadoOrcamento.ANULADO) {
            titulo += " (ANULADO)";
        }
        String todas = bordas(true, true, true, true);
        StringBuilder rows = new StringBuilder();
        rows.append(linha(celula(DIR, 3, todas, AZUL, "center", 100,
                par(run(espacado(titulo), 28, true, "FFFFFF", null, 70), "center", 0, 0)), 600, false, false));
        String[] cab = {"Nº Orçamento", "Data", "Código cliente"};
        int[] lg = {c1, c2, c3};
        StringBuilder r2 = new StringBuilder();
        StringBuilder r3 = new StringBuilder();
        String[] val = {"", "", ""};
        if (o != null) {
            Cliente cli = o.getCliente();
            val = new String[]{o.getNumero(), o.getData().format(DATA), nz(cli.getCodigo())};
        }
        for (int i = 0; i < 3; i++) {
            r2.append(celula(lg[i], 1, todas, null, "center", 100, par(run(cab[i], 21, true), "center", 0, 0)));
            r3.append(celula(lg[i], 1, todas, null, "center", 100,
                    val[i].isEmpty() ? "" : par(run(val[i], 28, true), "center", 0, 0)));
        }
        rows.append(linha(r2.toString(), 480, false, false));
        rows.append(linha(r3.toString(), 680, false, false));
        return tabela(DIR, new int[]{c1, c2, c3}, rows.toString());
    }

    private String tabelaCliente() {
        String todas = bordas(true, true, true, true);
        StringBuilder rows = new StringBuilder();
        rows.append(linha(celula(DIR, 1, todas, AZUL, "center", 100,
                par(run(espacado("CLIENTE"), 28, true, "FFFFFF", null, 70), "center", 0, 0)), 600, false, false));
        StringBuilder conteudo = new StringBuilder();
        if (o != null) {
            Cliente c = o.getCliente();
            if (!nz(c.getNome()).isBlank()) {
                conteudo.append(par(run(c.getNome(), 26, true), "center", 120, 120));
            }
            String m = nz(c.getMorada()).replace("\n", ", ");
            if (!m.isBlank()) {
                conteudo.append(par(run(m, 22, false), "left", 60, 0));
            }
        }
        rows.append(linha(celula(DIR, 1, todas, null, "top", 140, conteudo.toString()), 1600, false, false));
        return tabela(DIR, new int[]{DIR}, rows.toString());
    }

    // ------------------------------------------------------------------ tabela de detalhes

    private String cabecalhoDetalhes() {
        String todas = bordas(true, true, true, true);
        String[] t = {"", "Descrição", "Un.", "Preço Unit.", "Preço Total"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(celula(COLS[i], 1, todas, null, "center", 100,
                    t[i].isEmpty() ? "" : par(run(t[i], 23, true), "center", 0, 0)));
        }
        return linha(sb.toString(), 840, false, true);
    }

    private static String linhaVazia(boolean ultima) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(celula(COLS[i], 1, bordas(false, ultima, true, true), null, "top", 100, ""));
        }
        return linha(sb.toString(), LINHA, false, false);     // «pelo menos»: cresce se se escrever mais
    }

    private String tabelaDetalhesBranco(int n) {
        StringBuilder rows = new StringBuilder(cabecalhoDetalhes());
        for (int i = 0; i < n; i++) {
            rows.append(linhaVazia(i == n - 1));
        }
        return tabela(W, COLS, rows.toString());
    }

    /** Linha de conteúdo: índice, descrição (texto já formatado), un., preço e total. */
    private String linhaConteudo(String indice, String descricao, String un, String preco, String total,
                                 boolean negrito, boolean ultima) {
        String b = bordas(false, ultima, true, true);
        StringBuilder sb = new StringBuilder();
        sb.append(celula(COLS[0], 1, b, null, "top", 60,
                indice.isEmpty() ? "" : par(run(indice, 18, false), "center", 20, 0)));
        sb.append(celula(COLS[1], 1, b, null, "top", 120, paragrafos(descricao, negrito)));
        sb.append(celula(COLS[2], 1, b, null, "top", 40,
                un.isEmpty() ? "" : par(run(un, 18, false), "center", 20, 0)));
        sb.append(celula(COLS[3], 1, b, null, "top", 60,
                preco.isEmpty() ? "" : par(run(preco, 18, false), "center", 20, 0)));
        sb.append(celula(COLS[4], 1, b, null, "top", 60,
                total.isEmpty() ? "" : par(run(total, 18, negrito), "center", 20, 0)));
        return linha(sb.toString(), LINHA, false, false);
    }

    private static String paragrafos(String texto, boolean negrito) {
        if (texto.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : texto.split("\\r?\\n")) {
            sb.append(par(run(p, 18, negrito), "left", 20, 0));
        }
        return sb.toString();
    }

    /** Linha em que o rótulo vai a negrito e o resto em normal (ex.: «Prazo de execução: …»). */
    private String linhaRotulo(String rotulo, String valor) {
        String b = bordas(false, false, true, true);
        StringBuilder sb = new StringBuilder();
        sb.append(celula(COLS[0], 1, b, null, "top", 60, ""));
        sb.append(celula(COLS[1], 1, b, null, "top", 120,
                par(run(rotulo, 18, true) + run(valor, 18, false), "left", 20, 0)));
        for (int i = 2; i < 5; i++) {
            sb.append(celula(COLS[i], 1, b, null, "top", 60, ""));
        }
        return linha(sb.toString(), 400, false, false);
    }

    private String tabelaDetalhes() {
        List<String> linhas = new ArrayList<>();
        int estimativa = 0;                                     // altura aproximada ocupada (dxa)
        boolean comCap = o.temCapitulos();
        int numCap = 0;
        int numLinha = 0;
        for (Capitulo c : o.getCapitulos()) {
            if (c.temTitulo()) {
                numCap++;
                linhas.add(linhaConteudo("", numCap + ". " + c.getTitulo(), "", "", "", true, false));
                estimativa += LINHA;
            }
            for (LinhaOrcamento l : c.getLinhas()) {
                numLinha++;
                linhas.add(linhaConteudo(numLinha + ")", l.getDescricao(),
                        quantidade(l.getQuantidade()) + " " + l.getUnidade().getSimbolo(),
                        Dinheiro.formatar(l.getPrecoUnitario()), Dinheiro.formatar(l.getTotal()), false, false));
                estimativa += Math.max(LINHA, linhasDeTexto(l.getDescricao()) * 215 + 140);
            }
            if (c.temTitulo() && comCap) {
                linhas.add(linhaConteudo("", "Subtotal " + numCap + ". " + c.getTitulo(), "", "",
                        Dinheiro.formatar(c.getSubtotal()), true, false));
                estimativa += LINHA;
            }
        }
        // totais
        if (o.isIvaIncluido()) {
            linhas.add(linhaConteudo("", "Subtotal:", "", "", Dinheiro.formatar(o.getSubtotal()), true, false));
            linhas.add(linhaConteudo("", "IVA " + semZeros(o.getTaxaIva()) + "%:", "", "",
                    Dinheiro.formatar(o.getValorIva()), true, false));
            linhas.add(linhaConteudo("", "Total:", "", "", Dinheiro.formatar(o.getTotal()), true, false));
            estimativa += 3 * LINHA;
        } else {
            linhas.add(linhaConteudo("", "Preço Global:", "", "", Dinheiro.formatar(o.getSubtotal()), true, false));
            linhas.add(linhaRotulo("IVA- ", "Não incluído."));
            estimativa += LINHA + 400;
        }
        String[][] extras = {{"Prazo de execução: ", o.getPrazoExecucao()},
                {"Condições de pagamento: ", o.getCondicoesPagamento()},
                {"Exclusões: ", o.getExclusoes()}, {"Inclusões: ", o.getInclusoes()}};
        for (String[] x : extras) {
            if (x[1] != null && !x[1].isBlank()) {
                linhas.add(linhaRotulo(x[0], x[1]));
                estimativa += Math.max(400, linhasDeTexto(x[0] + x[1]) * 215 + 140);
            }
        }
        if (o.getValidoAte() != null) {
            linhas.add(linhaRotulo("Validade: ", "Orçamento válido até " + o.getValidoAte().format(DATA)
                    + ". Valores em euros."));
            estimativa += 400;
        }
        // linhas livres até ao fundo da 1.ª página (só se o conteúdo ocupar menos de uma página)
        int livre = (LINHAS_PAG1 * LINHA - estimativa - 300) / LINHA;
        for (int i = 0; i < livre; i++) {
            linhas.add(linhaVazia(false));
        }
        StringBuilder rows = new StringBuilder(cabecalhoDetalhes());
        for (int i = 0; i < linhas.size(); i++) {
            rows.append(linhas.get(i));
        }
        rows.append(linhaFecho());
        return tabela(W, COLS, rows.toString());
    }

    /** Última linha, fina, só para fechar a tabela por baixo. */
    private static String linhaFecho() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(celula(COLS[i], 1, bordas(false, true, true, true), null, "top", 0, ""));
        }
        return linha(sb.toString(), 40, true, false);
    }

    private static int linhasDeTexto(String t) {
        int n = 0;
        for (String p : t.split("\\r?\\n", -1)) {
            n += Math.max(1, (int) Math.ceil(p.length() / 62.0));
        }
        return n;
    }

    // ------------------------------------------------------------------ rodapé

    private String rodape() {
        String morada = nz(e.getMorada()).trim().replaceAll("\\s*\\R\\s*", ", ");
        int v = morada.indexOf("Vizela");
        String runs;
        String fb = "Arial Black";
        if (v >= 0) {
            runs = run(morada.substring(0, v), 22, true, null, fb, 0) + run("Vizela", 33, true, null, fb, 0)
                    + run(morada.substring(v + 6), 22, true, null, fb, 0);
        } else {
            runs = run(morada, 22, true, null, fb, 0);
        }
        String linhaMorada = "<w:p><w:pPr><w:pBdr><w:top w:val=\"single\" w:sz=\"12\" w:space=\"4\" w:color=\"000000\"/></w:pBdr>"
                + "<w:spacing w:before=\"0\" w:after=\"40\"/><w:jc w:val=\"center\"/></w:pPr>" + runs + "</w:p>";
        String pagina = "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr>" + run("Página ", 16, false)
                + campo("PAGE") + run(" de ", 16, false) + campo("NUMPAGES") + "</w:p>";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:ftr " + NAMESPACES + ">" + linhaMorada + pagina + "</w:ftr>";
    }

    private static String campo(String nome) {
        String rpr = "<w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/><w:sz w:val=\"16\"/></w:rPr>";
        return "<w:r>" + rpr + "<w:fldChar w:fldCharType=\"begin\"/></w:r><w:r>" + rpr
                + "<w:instrText xml:space=\"preserve\"> " + nome + " </w:instrText></w:r><w:r>" + rpr
                + "<w:fldChar w:fldCharType=\"separate\"/></w:r><w:r>" + rpr + "<w:t>1</w:t></w:r><w:r>" + rpr
                + "<w:fldChar w:fldCharType=\"end\"/></w:r>";
    }

    // ------------------------------------------------------------------ formatação

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String quantidade(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String semZeros(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String formatarNif(String nif) {
        String d = nif.replaceAll("\\D", "");
        return d.length() == 9 ? d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6) : nif;
    }

    private static String formatarTelefone(String tel) {
        if (tel == null || tel.isBlank()) {
            return "";
        }
        String t = tel.trim();
        String d = t.replaceAll("\\D", "");
        if (!t.startsWith("+") && !t.startsWith("00") && d.length() == 9) {
            return "+351 " + d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6);
        }
        return t;
    }

    // ------------------------------------------------------------------ partes fixas do pacote

    private static final String NAMESPACES =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                    + "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
                    + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                    + "xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"";

    private static final String CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Default Extension=\"png\" ContentType=\"image/png\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            + "<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>"
            + "<Override PartName=\"/word/footer1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml\"/>"
            + "</Types>";

    private static final String RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
            + "</Relationships>";

    private static final String DOC_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rIdStyles\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "<Relationship Id=\"rIdFooter\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer\" Target=\"footer1.xml\"/>"
            + "<Relationship Id=\"rIdLogo\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/logo.png\"/>"
            + "</Relationships>";

    private static final String ESTILOS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
            + "<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Arial\"/>"
            + "<w:sz w:val=\"20\"/><w:szCs w:val=\"20\"/><w:lang w:val=\"pt-PT\"/></w:rPr></w:rPrDefault>"
            + "<w:pPrDefault><w:pPr><w:spacing w:after=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault>"
            + "</w:docDefaults>"
            + "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/></w:style>"
            + "</w:styles>";
}
