package service;

import domain.Capitulo;
import domain.Cliente;
import domain.Dinheiro;
import domain.EstadoOrcamento;
import domain.Empresa;
import domain.LinhaOrcamento;
import domain.Orcamento;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o PDF de um orçamento (US08) com a mesma disposição do modelo "Exemplar_NP":
 * cabeçalho (logótipo, nome da empresa, morada), caixa ORÇAMENTO, caixa do cliente,
 * tabela de artigos com os totais e condições lá dentro e, no rodapé,
 * contacto, e-mail e página do site. Pagina automaticamente.
 */
public class GeradorPdf {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** Gera o modelo em branco (mesmo layout, tabelas vazias) para preencher à mão. */
    public void gerarModeloEmBranco(Empresa empresa, Path destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            new Escritor(doc, null, empresa).escrever();
            doc.save(destino.toFile());
        }
    }

    /** Gera o ficheiro PDF em {@code destino}. */
    public void gerar(Orcamento orcamento, Empresa empresa, Path destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            new Escritor(doc, orcamento, empresa).escrever();
            doc.save(destino.toFile());
        }
    }

    /** Estado e desenho de um PDF; uma instância por geração. As posições "t" medem-se a partir do topo da página. */
    private static final class Escritor {
        private final PDFont NORMAL;
        private final PDFont NEGRITO;
        private static final float LARGURA = PDRectangle.A4.getWidth();
        private static final float ALTURA = PDRectangle.A4.getHeight();
        private static final float TAM = 9f;
        private static final float PEQ = 8f;
        private static final float ENTRELINHA = 12f;

        // Medidas (pontos, a partir do topo). Tudo centrado na página; tabelas com 480 pt de largura.
        private static final float TOPO = 28f;
        private static final float LARG_TAB = 552f;
        private static final float TAB_ESQ = 22f;
        private static final float TAB_DIR = TAB_ESQ + LARG_TAB;
        private static final float[] X = {22f, 57f, 360f, 397f, 471f, 574f};
        // Caixas da direita (ORÇAMENTO e cliente)
        private static final float BX = 250f;
        private static final float BL = 324f;
        private static final float HT = 30f;   // altura das barras pretas (ORÇAMENTO e CLIENTE)
        private static final float TAB_FUNDO = 770f;
        private static final float CABECALHO_TAB = 42f;

        private final PDDocument doc;
        private final Orcamento o;
        private final Empresa e;
        private PDPageContentStream cs;
        private PDImageXObject logotipo;
        private float t;          // posição atual (a partir do topo)
        private float tabTopo;
        private float fundoTab = TAB_FUNDO;    // topo da tabela na página atual

        Escritor(PDDocument doc, Orcamento o, Empresa e) {
            this.doc = doc;
            this.o = o;
            this.e = e;
            this.NORMAL = carregarFonte(doc, "/fonts/LiberationSans-Regular.ttf", PDType1Font.HELVETICA);
            this.NEGRITO = carregarFonte(doc, "/fonts/LiberationSans-Bold.ttf", PDType1Font.HELVETICA_BOLD);
        }

        /** Embute a fonte no PDF (igual em qualquer leitor); se falhar, usa a Helvetica padrão. */
        private static PDFont carregarFonte(PDDocument doc, String recurso, PDFont alternativa) {
            try (InputStream in = GeradorPdf.class.getResourceAsStream(recurso)) {
                if (in != null) {
                    return PDType0Font.load(doc, in);
                }
            } catch (IOException | RuntimeException ex) {
                // usa a alternativa
            }
            return alternativa;
        }

        private static float Y(float topo) {
            return ALTURA - topo;
        }

        void escrever() throws IOException {
            carregarLogotipo();
            novaPagina(true);
            if (o != null) {
                tabela();
                totaisECondicoes();
            }
            
            fecharTabela();
            cs.close();
            rodape();
        }

        // ---------------------------------------------------------------- páginas

        private void novaPagina(boolean primeira) throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            cs = new PDPageContentStream(doc, pagina);
            cs.setLineWidth(0.8f);
            cs.setStrokingColor(0f, 0f, 0f);
            cs.setNonStrokingColor(0f);
            if (primeira) {
                float esq = cabecalhoCompleto();
                float fim = caixaOrcamento(TOPO);
                fim = caixaCliente(fim + 18f);
                tabTopo = Math.max(esq, fim) + 28f;
            } else {
                tabTopo = TOPO;
            }
            cabecalhoTabela();
        }

        /** Fecha a página atual (borda e colunas da tabela) e abre a seguinte. */
        private void mudarPagina() throws IOException {
            fecharTabela();
            novaPagina(false);
        }

        private void garantir(float altura) throws IOException {
            if (t + altura > TAB_FUNDO) {
                mudarPagina();
            }
        }

        private void carregarLogotipo() {
            try {
                byte[] bytes = null;
                String caminho = e.getCaminhoLogotipo();
                if (caminho != null && !caminho.isBlank() && Files.exists(Path.of(caminho))) {
                    bytes = Files.readAllBytes(Path.of(caminho));
                } else {
                    try (InputStream in = GeradorPdf.class.getResourceAsStream("/logo.png")) {
                        if (in != null) {
                            bytes = in.readAllBytes();
                        }
                    }
                }
                if (bytes != null) {
                    logotipo = PDImageXObject.createFromByteArray(doc, bytes, "logotipo");
                }
            } catch (IOException | RuntimeException ex) {
                logotipo = null; // sem logótipo, usa-se o nome da empresa
            }
        }

        // ------------------------------------------------------------- cabeçalho

        // Só preto e branco: títulos das tabelas com fundo preto e letras brancas.
        private static final float[] AZUL = {0.09f, 0.25f, 0.45f};
        private static final float[] NAVY = {0f, 0f, 0f};
        private static final float[] CLARO = {1f, 1f, 1f};

        private void corTexto(float[] c) throws IOException {
            cs.setNonStrokingColor(c[0], c[1], c[2]);
        }

        private void pretoTexto() throws IOException {
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        private void preencher(float x, float topo, float w, float h, float[] c) throws IOException {
            cs.setNonStrokingColor(c[0], c[1], c[2]);
            cs.addRect(x, Y(topo + h), w, h);
            cs.fill();
            pretoTexto();
        }

        /** Texto centrado com cor. */
        private void textoCentradoCor(PDFont f, float tam, float x1, float x2, float yy, String t, float[] c) throws IOException {
            corTexto(c);
            textoCentrado(f, tam, x1, x2, yy, t);
            pretoTexto();
        }

        /** "517870444" -> "517 870 444". */
        private static String formatarNif(String nif) {
            String d = nif == null ? "" : nif.replaceAll("\\D", "");
            return d.length() == 9 ? d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6) : (nif == null ? "" : nif);
        }

        /** "933658520" -> "+351 933 658 520" (números com indicativo ficam como estão). */
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

        /** Texto centrado na página. */
        private void centro(PDFont f, float tam, float topo, String t) throws IOException {
            textoCentrado(f, tam, 0, LARGURA, Y(topo), t);
        }

        /** Coluna da esquerda: logótipo, nome, NIF, telefone, e-mail e site (negrito, destaque). Devolve o fim. */
        private float cabecalhoCompleto() throws IOException {
            float t0 = TOPO + 3f;
            float larguraCol = BX - X[0] - 14f;
            float meio = X[0] + larguraCol / 2f;
            if (logotipo != null) {
                float alt = 90f;
                float larg = alt * logotipo.getWidth() / logotipo.getHeight();
                if (larg > larguraCol) {
                    larg = larguraCol;
                    alt = larg * logotipo.getHeight() / logotipo.getWidth();
                }
                cs.drawImage(logotipo, meio - larg / 2f, Y(t0 + alt), larg, alt);
                t0 += alt;
            }
            t0 += 8f;
            for (String p : quebrar(e.getDesignacao() == null ? "" : e.getDesignacao(), NEGRITO, 23.5f, larguraCol + 12f)) {
                t0 += 26f;
                textoCentrado(NEGRITO, 23.5f, X[0] - 6f, X[0] + larguraCol + 6f, Y(t0), p);
            }
            t0 += 3f;
            String[] linhas = {
                    e.getNif() == null || e.getNif().isBlank() ? "" : "NIF: " + formatarNif(e.getNif()),
                    formatarTelefone(e.getTelefone()).isBlank() ? "" : "Tel: " + formatarTelefone(e.getTelefone()),
                    e.getEmail() == null ? "" : e.getEmail(),
                    e.getWebsite() == null ? "" : e.getWebsite()};
            for (int i = 0; i < linhas.length; i++) {
                String l = linhas[i];
                if (!l.isBlank()) {
                    boolean destaque = i < 2;           // só os números de NIF e telefone em negrito e maiores
                    t0 += destaque ? (i == 0 ? 17f : 19f) : 15f;
                    if (destaque) {
                        int k = l.indexOf(' ') + 1;     // "NIF: " / "Tel: " normal; número a negrito
                        String rot = l.substring(0, k), num = l.substring(k);
                        float tam = i == 0 ? 13f : 15.5f;      // NIF 13 pt, telefone maior
                        float w = largura(NORMAL, tam, rot) + largura(NEGRITO, tam, num);
                        float x = X[0] + (larguraCol - w) / 2f;
                        texto(NORMAL, tam, x, Y(t0), rot);
                        texto(NEGRITO, tam, x + largura(NORMAL, tam, rot), Y(t0), num);
                    } else {
                        textoCentrado(NORMAL, 12f, X[0], X[0] + larguraCol, Y(t0), l);
                    }
                }
            }
            return t0 + 4f;
        }

        /** Tabela ORÇAMENTO: barra preta com título branco + Nº Orçamento, Data, Código cliente. Devolve o fim. */
        private float caixaOrcamento(float topo) throws IOException {
            String titulo = "ORÇAMENTO";
            if (o != null && o.getEstado() == EstadoOrcamento.ANULADO) {
                titulo += " (" + o.getEstado().getDescricao().toUpperCase() + ")";
            }
            float h1 = HT, h2 = 24f, h3 = 34f;
            float[] cx = {BX, BX + 82f, BX + 196f, BX + BL};
            preencher(BX, topo, BL, h1, AZUL);
            retangulo(BX, Y(topo), BL, h1 + h2 + h3);
            textoEspacadoBranco(14f, BX, BX + BL, Y(topo + 20f), titulo);
            linha(BX, Y(topo + h1), BX + BL, Y(topo + h1));
            linha(BX, Y(topo + h1 + h2), BX + BL, Y(topo + h1 + h2));
            linha(cx[1], Y(topo + h1), cx[1], Y(topo + h1 + h2 + h3));
            linha(cx[2], Y(topo + h1), cx[2], Y(topo + h1 + h2 + h3));
            String[] cab = {"Nº Orçamento", "Data", "Código cliente"};
            String[] val = {"", "", ""};
            if (o != null) {
                Cliente cli = o.getCliente();
                val = new String[]{o.getNumero(), o.getData().format(DATA), cli.getCodigo() == null ? "" : cli.getCodigo()};
            }
            for (int i = 0; i < 3; i++) {
                textoCentrado(NEGRITO, 10.5f, cx[i], cx[i + 1], Y(topo + h1 + 15.5f), cab[i]);
                textoCentrado(NEGRITO, 14f, cx[i], cx[i + 1], Y(topo + h1 + h2 + 22f), val[i]);
            }
            return topo + h1 + h2 + h3;
        }

        /** Tabela CLIENTE: barra preta com título branco (mesmo tamanho), nome centrado e morada. Devolve o fim. */
        private float caixaCliente(float topo) throws IOException {
            float[] branco = {1f, 1f, 1f};
            float hCorpo = 80f;
            List<String> nome = new java.util.ArrayList<>();
            List<String> morada = new java.util.ArrayList<>();
            if (o != null) {
                Cliente c = o.getCliente();
                nome = quebrar(c.getNome(), NEGRITO, 13f, BL - 20f);
                String m = c.getMorada() == null ? "" : c.getMorada().replace("\n", ", ");
                morada = quebrar(m, NORMAL, 11f, BL - 20f);
                hCorpo = Math.max(hCorpo, 18f + nome.size() * 17f + 8f + morada.size() * 15f + 12f);
            }
            float h = HT + hCorpo;
            preencher(BX, topo, BL, HT, AZUL);
            retangulo(BX, Y(topo), BL, h);
            textoEspacadoBranco(14f, BX, BX + BL, Y(topo + 20f), "CLIENTE");
            float tt = topo + HT + 22f;
            for (String p : nome) {
                textoCentrado(NEGRITO, 13f, BX, BX + BL, Y(tt), p);
                tt += 17f;
            }
            tt += 8f;
            for (String p : morada) {
                texto(NORMAL, 11f, BX + 10f, Y(tt), p);
                tt += 15f;
            }
            return topo + h;
        }

        // ----------------------------------------------------------------- tabela

        private void cabecalhoTabela() throws IOException {
            t = tabTopo + CABECALHO_TAB;
            preencher(X[0], tabTopo, X[5] - X[0], CABECALHO_TAB, CLARO);
            String[] titulos = {"", "Descrição", "Un.", "Preço Unit.", "Preço Total"};
            for (int i = 0; i < 5; i++) {
                textoCentrado(NEGRITO, 11.5f, X[i], X[i + 1], Y(tabTopo + 25f), titulos[i]);
            }
            linha(X[0], Y(t), X[5], Y(t));
            t += 6f;
        }

        /** Desenha a moldura e as colunas da tabela na página atual. */
        private void fecharTabela() throws IOException {
            retangulo(X[0], Y(tabTopo), X[5] - X[0], fundoTab - tabTopo);
            for (int i = 1; i < 5; i++) {
                linha(X[i], Y(tabTopo), X[i], Y(fundoTab));
            }
        }

        private void tabela() throws IOException {
            boolean comCapitulos = o.temCapitulos();
            int numCap = 0;
            int numLinha = 0;
            for (Capitulo c : o.getCapitulos()) {
                if (c.temTitulo()) {
                    numCap++;
                    linhaCapitulo(numCap + ". " + c.getTitulo());
                }
                for (LinhaOrcamento l : c.getLinhas()) {
                    numLinha++;
                    linhaArtigo(numLinha + ")", l);
                }
                if (c.temTitulo() && comCapitulos) {
                    linhaSubtotalCapitulo("Subtotal " + numCap + ". " + c.getTitulo(), c.getSubtotal());
                }
            }
        }

        private float larguraDescricao() {
            return X[2] - X[1] - 16f;
        }

        private void linhaCapitulo(String titulo) throws IOException {
            List<String> q = quebrar(titulo, NEGRITO, TAM, larguraDescricao());
            garantir(q.size() * ENTRELINHA + 22f);
            for (int i = 0; i < q.size(); i++) {
                texto(NEGRITO, TAM, X[1] + 8f, Y(t + 9f + i * ENTRELINHA), q.get(i));
            }
            t += q.size() * ENTRELINHA + 8f;
        }

        private void linhaSubtotalCapitulo(String rotulo, BigDecimal valor) throws IOException {
            List<String> q = quebrar(rotulo, NEGRITO, TAM, larguraDescricao());
            garantir(q.size() * ENTRELINHA + 14f);
            for (int i = 0; i < q.size(); i++) {
                texto(NEGRITO, TAM, X[1] + 8f, Y(t + 9f + i * ENTRELINHA), q.get(i));
            }
            textoCentrado(NEGRITO, TAM, X[4], X[5], Y(t + 9f), Dinheiro.formatar(valor));
            t += q.size() * ENTRELINHA + 12f;
        }

        private void linhaArtigo(String indice, LinhaOrcamento l) throws IOException {
            List<String> desc = quebrar(l.getDescricao(), NORMAL, TAM, larguraDescricao());
            float h = desc.size() * ENTRELINHA + 10f;
            garantir(h + 4f);
            float base = Y(t + 9f);
            textoCentrado(NORMAL, TAM, X[0], X[1], base, indice);
            for (int i = 0; i < desc.size(); i++) {
                texto(NORMAL, TAM, X[1] + 8f, Y(t + 9f + i * ENTRELINHA), desc.get(i));
            }
            String un = quantidade(l.getQuantidade()) + " " + l.getUnidade().getSimbolo();
            float tamUn = largura(NORMAL, TAM, un) > X[3] - X[2] - 2f ? 7.5f : TAM;
            textoCentrado(NORMAL, tamUn, X[2], X[3], base, un);
            textoCentrado(NORMAL, TAM, X[3], X[4], base, Dinheiro.formatar(l.getPrecoUnitario()));
            textoCentrado(NORMAL, TAM, X[4], X[5], base, Dinheiro.formatar(l.getTotal()));
            t += h;
        }

        // ----------------------------------------------- totais e condições (dentro da tabela)

        private void totaisECondicoes() throws IOException {
            t += 6f;
            if (o.isIvaIncluido()) {
                totalLinha("Subtotal:", o.getSubtotal(), false);
                totalLinha("IVA " + semZeros(o.getTaxaIva()) + "%:", o.getValorIva(), false);
                totalLinha("Total:", o.getTotal(), true);
            } else {
                totalLinha("Preço Global:", o.getSubtotal(), true);
                paragrafoRotulo("IVA- ", "Não incluído.", true);
            }
            paragrafoRotulo("Prazo de execução: ", o.getPrazoExecucao(), false);
            paragrafoRotulo("Condições de pagamento: ", o.getCondicoesPagamento(), false);
            paragrafoRotulo("Exclusões: ", o.getExclusoes(), false);
            paragrafoRotulo("Inclusões: ", o.getInclusoes(), false);
            if (o.getValidoAte() != null) {
                paragrafoRotulo("Validade: ", "Orçamento válido até " + o.getValidoAte().format(DATA)
                        + ". Valores em euros.", false);
            }
        }

        private void totalLinha(String rotulo, BigDecimal valor, boolean destaque) throws IOException {
            garantir(24f);
            PDFont f = destaque || true ? NEGRITO : NORMAL;
            texto(f, TAM, X[1] + 8f, Y(t + 9f), rotulo);
            textoCentrado(f, TAM, X[4], X[5], Y(t + 9f), Dinheiro.formatar(valor));
            t += 22f;
        }

        private void paragrafoRotulo(String rotulo, String valor, boolean tudoNegrito) throws IOException {
            if (valor == null || valor.isBlank()) {
                return;
            }
            List<String> q = quebrar(rotulo + valor, NORMAL, TAM, larguraDescricao());
            for (int k = 0; k < q.size(); k++) {
                garantir(ENTRELINHA + 10f);
                String parte = q.get(k);
                float base = Y(t + 9f);
                float x = X[1] + 8f;
                if (k == 0 && parte.startsWith(rotulo)) {
                    texto(NEGRITO, TAM, x, base, rotulo);
                    texto(tudoNegrito ? NEGRITO : NORMAL, TAM, x + largura(NEGRITO, TAM, rotulo), base,
                            parte.substring(rotulo.length()));
                } else {
                    texto(NORMAL, TAM, x, base, parte);
                }
                t += ENTRELINHA;
            }
            t += 8f;
        }

        // ----------------------------------------------------------------- rodapé

        /** Rodapé em todas as páginas: morada completa, centrada e a negrito (+ numeração se houver várias páginas). */
        private void rodape() throws IOException {
            int total = doc.getNumberOfPages();
            String morada = e.getMorada() == null ? "" : e.getMorada().trim().replaceAll("\\s*\\R\\s*", ", ");
            for (int i = 0; i < total; i++) {
                PDPage pg = doc.getPage(i);
                cs = new PDPageContentStream(doc, pg, PDPageContentStream.AppendMode.APPEND, true, true);
                cs.setLineWidth(1.5f);
                cs.setStrokingColor(0f, 0f, 0f);
                cs.setNonStrokingColor(0f);
                linha(X[0], Y(784f), X[5], Y(784f));
                cs.setLineWidth(0.45f);                       // negrito mais carregado
                cs.setRenderingMode(org.apache.pdfbox.pdmodel.graphics.state.RenderingMode.FILL_STROKE);
                float tt = 802f;
                int v = morada.indexOf("Vizela");
                if (v >= 0 && largura(NEGRITO, 11.5f, morada) + 12f < LARG_TAB) {
                    // morada a negrito; só «Vizela» com a letra um pouco maior
                    String a = morada.substring(0, v), b = "Vizela", c = morada.substring(v + 6);
                    float w = largura(NEGRITO, 11.5f, a) + largura(NEGRITO, 16.5f, b) + largura(NEGRITO, 11.5f, c);
                    float x = (LARGURA - w) / 2f;
                    texto(NEGRITO, 11.5f, x, Y(tt), a);
                    texto(NEGRITO, 16.5f, x + largura(NEGRITO, 11.5f, a), Y(tt), b);
                    texto(NEGRITO, 11.5f, x + largura(NEGRITO, 11.5f, a) + largura(NEGRITO, 16.5f, b), Y(tt), c);
                    tt += 15f;
                } else {
                    for (String p : quebrar(morada, NEGRITO, 11.5f, LARG_TAB)) {
                        centro(NEGRITO, 11.5f, tt, p);
                        tt += 14f;
                    }
                }
                cs.setRenderingMode(org.apache.pdfbox.pdmodel.graphics.state.RenderingMode.FILL);
                if (total > 1) {
                    centro(NORMAL, 8f, tt + 2f, "Página " + (i + 1) + " de " + total);
                }
                cs.close();
            }
        }

        // -------------------------------------------------------------- primitivas

        private void texto(PDFont f, float tam, float x, float yy, String t) throws IOException {
            cs.beginText();
            cs.setFont(f, tam);
            cs.newLineAtOffset(x, yy);
            cs.showText(limpar(t));
            cs.endText();
        }

        /** Título das barras: negrito branco com as letras espaçadas (O R Ç A M E N T O). */
        private void textoEspacadoBranco(float tam, float x1, float x2, float yy, String t) throws IOException {
            float sp = 3.5f;
            String txt = limpar(t);
            float w = largura(NEGRITO, tam, t) + sp * (txt.length() - 1);
            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.beginText();
            cs.setFont(NEGRITO, tam);
            cs.setCharacterSpacing(sp);
            cs.newLineAtOffset(x1 + (x2 - x1 - w) / 2f, yy);
            cs.showText(txt);
            cs.setCharacterSpacing(0f);
            cs.endText();
            pretoTexto();
        }

        private void textoDireita(PDFont f, float tam, float xDireita, float yy, String t) throws IOException {
            texto(f, tam, xDireita - largura(f, tam, t), yy, t);
        }

        private void textoCentrado(PDFont f, float tam, float x1, float x2, float yy, String t)
                throws IOException {
            texto(f, tam, x1 + (x2 - x1 - largura(f, tam, t)) / 2f, yy, t);
        }

        private void linha(float x1, float y1, float x2, float y2) throws IOException {
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private void retangulo(float x, float yTopo, float w, float h) throws IOException {
            cs.addRect(x, yTopo - h, w, h);
            cs.stroke();
        }

        private void retanguloCinza(float x, float yTopo, float w, float h, float cinza) throws IOException {
            cs.setNonStrokingColor(cinza);
            cs.addRect(x, yTopo - h, w, h);
            cs.fill();
            cs.setNonStrokingColor(0f);
            retangulo(x, yTopo, w, h);
        }

        private static float largura(PDFont f, float tam, String t) {
            try {
                return f.getStringWidth(limpar(t)) / 1000f * tam;
            } catch (IOException | IllegalArgumentException ex) {
                return t.length() * tam * 0.5f;
            }
        }

        /** Troca caracteres que a fonte padrão (WinAnsi) não suporta. */
        private static String limpar(String t) {
            if (t == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(t.length());
            for (char c : t.toCharArray()) {
                if ((c >= 32 && c <= 126) || (c >= 160 && c <= 255)) {
                    sb.append(c);
                } else if (c == '€' || c == '–' || c == '—' || c == '•' || c == '…') {
                    sb.append(c);
                } else if (c == '‘' || c == '’') {
                    sb.append('\'');
                } else if (c == '“' || c == '”') {
                    sb.append('"');
                } else if (c == '\t') {
                    sb.append(' ');
                } else {
                    sb.append('?');
                }
            }
            return sb.toString();
        }

        /** Quebra o texto em linhas que cabem na largura dada. */
        private static List<String> quebrar(String texto, PDFont f, float tam, float larguraMax) {
            List<String> linhas = new ArrayList<>();
            String base = texto == null ? "" : texto;
            for (String par : base.split("\\r?\\n", -1)) {
                StringBuilder atual = new StringBuilder();
                for (String palavra : par.split(" ")) {
                    String tentativa = atual.length() == 0 ? palavra : atual + " " + palavra;
                    if (largura(f, tam, tentativa) <= larguraMax) {
                        atual = new StringBuilder(tentativa);
                        continue;
                    }
                    if (atual.length() > 0) {
                        linhas.add(atual.toString());
                        atual = new StringBuilder();
                    }
                    // palavra maior do que a linha: corta por caracteres
                    String resto = palavra;
                    while (largura(f, tam, resto) > larguraMax && resto.length() > 1) {
                        int n = resto.length() - 1;
                        while (n > 1 && largura(f, tam, resto.substring(0, n)) > larguraMax) {
                            n--;
                        }
                        linhas.add(resto.substring(0, n));
                        resto = resto.substring(n);
                    }
                    atual.append(resto);
                }
                linhas.add(atual.toString());
            }
            return linhas;
        }

        private static String quantidade(BigDecimal q) {
            return semZeros(q).replace('.', ',');
        }

        private static String semZeros(BigDecimal v) {
            return v.stripTrailingZeros().toPlainString();
        }
    }
}
