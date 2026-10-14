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

        // Medidas do modelo (pontos, a partir do topo)
        private static final float TOPO = 28f;
        private static final float CX = 233.7f;               // caixa ORÇAMENTO
        private static final float CW = 574.5f - 233.7f;
        private static final float TAB_ESQ = 21.8f;
        private static final float TAB_DIR = 574.4f;
        private static final float[] X = {TAB_ESQ, 56.5f, 360.2f, 396.8f, 471.1f, TAB_DIR};
        private static final float TAB_FUNDO = 751.7f;
        private static final float CABECALHO_TAB = 42.4f;

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
            tabela();
            totaisECondicoes();
            
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
            cs.setLineWidth(0.7f);
            cs.setStrokingColor(0f);
            cs.setNonStrokingColor(0f);
            float fimCabecalho = cabecalho();
            float fimCliente = primeira ? caixaCliente() : fimCabecalho;
            tabTopo = primeira ? Math.max(fimCliente, 198f) + 27.6f : fimCabecalho + 18f;
            if (primeira) {
                tabTopo = Math.max(tabTopo, 253.3f);
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

        /** Cabeçalho: logótipo, nome da empresa e morada (esquerda) + caixa ORÇAMENTO (direita). Devolve o fim (topo). */
        private float cabecalho() throws IOException {
            float esq = 25f;
            float nomeX = esq;
            float baixoLogo = TOPO + 4;
            if (logotipo != null) {
                float alt = 70f;
                float larg = alt * logotipo.getWidth() / logotipo.getHeight();
                cs.drawImage(logotipo, esq, Y(TOPO + 4 + alt), larg, alt);
                nomeX = esq + larg + 10;
                baixoLogo = TOPO + 4 + alt;
            }
            float larguraNome = 217f - nomeX;
            List<String> nome = quebrar(e.getDesignacao(), NEGRITO, 13f, larguraNome);
            float tn = TOPO + 28;
            for (String parte : nome) {
                texto(NEGRITO, 13f, nomeX, Y(tn), parte);
                tn += 15f;
            }
            float tm = Math.max(baixoLogo + 14, tn + 4);
            tm = campo("Morada: ", e.getMorada(), esq, tm);
            tm = campo("Nº Contribuinte: ", e.getNif(), esq, tm);

            // Caixa ORÇAMENTO
            String titulo = "ORÇAMENTO";
            if (o.getEstado() == EstadoOrcamento.RASCUNHO || o.getEstado() == EstadoOrcamento.ANULADO) {
                titulo += " (" + o.getEstado().getDescricao().toUpperCase() + ")";
            }
            retangulo(CX, Y(TOPO), CW, 82.1f);
            textoCentrado(NEGRITO, 11f, CX, CX + CW, Y(TOPO + 27), titulo);
            linha(CX, Y(69.8f), CX + CW, Y(69.8f));
            linha(CX, Y(82.3f), CX + CW, Y(82.3f));
            linha(319.9f, Y(69.8f), 319.9f, Y(110.1f));
            linha(437.8f, Y(69.8f), 437.8f, Y(110.1f));
            float[] lim = {CX, 319.9f, 437.8f, CX + CW};
            Cliente c = o.getCliente();
            String codigo = c.getCodigo() == null ? "" : c.getCodigo();
            String[] cab = {"Nº Orçamento", "Data", "Código cliente"};
            String[] val = {o.getNumero(), o.getData().format(DATA), codigo};
            for (int i = 0; i < 3; i++) {
                textoCentrado(NEGRITO, 8.5f, lim[i], lim[i + 1], Y(79.5f), cab[i]);
                textoCentrado(NORMAL, TAM, lim[i], lim[i + 1], Y(98f), val[i]);
            }
            return Math.max(tm, 110.1f);
        }

        private float campo(String rotulo, String valor, float x, float tAtual) throws IOException {
            if (valor == null || valor.isBlank()) {
                return tAtual;
            }
            List<String> q = quebrar(rotulo + valor, NORMAL, PEQ, 217f - x);
            for (int k = 0; k < q.size(); k++) {
                String parte = q.get(k);
                if (k == 0 && parte.startsWith(rotulo)) {
                    texto(NEGRITO, PEQ, x, Y(tAtual), rotulo);
                    texto(NORMAL, PEQ, x + largura(NEGRITO, PEQ, rotulo), Y(tAtual), parte.substring(rotulo.length()));
                } else {
                    texto(NORMAL, PEQ, x, Y(tAtual), parte);
                }
                tAtual += 10.5f;
            }
            return tAtual;
        }

        /** Caixa do cliente (só na 1.ª página). Devolve o fim (topo). */
        private float caixaCliente() throws IOException {
            Cliente c = o.getCliente();
            float larguraTexto = CW - 2 * 31f;
            List<String[]> itens = new ArrayList<>();
            itens.add(new String[]{"", c.getNif() == null || c.getNif().isBlank() ? "" : ""}); // reservado
            itens.clear();
            itens.add(new String[]{"", c.getMorada()});
            if (c.getContacto() != null && !c.getContacto().isBlank()) {
                itens.add(new String[]{"A/C ", c.getContacto()});
            }
            itens.add(new String[]{"NIF: ", c.getNif()});
            if (c.getTelefone() != null && !c.getTelefone().isBlank()) {
                itens.add(new String[]{"Tel.: ", c.getTelefone()});
            }
            if (c.getEmail() != null && !c.getEmail().isBlank()) {
                itens.add(new String[]{"E-mail: ", c.getEmail()});
            }
            if (o.getAssunto() != null && !o.getAssunto().isBlank()) {
                itens.add(new String[]{"Assunto: ", o.getAssunto()});
            }
            List<List<String>> blocos = new ArrayList<>();
            float alturaTexto = 14f + 8f; // nome centrado
            for (String[] it : itens) {
                List<String> q = quebrar(it[0] + (it[1] == null ? "" : it[1]), NORMAL, TAM, larguraTexto);
                blocos.add(q);
                alturaTexto += q.size() * ENTRELINHA + 5f;
            }
            float topo = 130.8f;
            float altura = Math.max(94.9f, alturaTexto + 8f);
            retangulo(CX, Y(topo), CW, altura);
            float tt = topo + 20f;
            textoCentrado(NORMAL, TAM, CX, CX + CW, Y(tt), c.getNome());
            tt += 22f;
            for (List<String> q : blocos) {
                for (String parte : q) {
                    texto(NORMAL, TAM, CX + 8f, Y(tt), parte);
                    tt += ENTRELINHA;
                }
                tt += 5f;
            }
            return topo + altura;
        }

        // ----------------------------------------------------------------- tabela

        private void cabecalhoTabela() throws IOException {
            t = tabTopo + CABECALHO_TAB;
            String[] titulos = {"", "Descrição", "Un.", "Preço Unit.", "Preço Total"};
            for (int i = 0; i < 5; i++) {
                textoCentrado(NEGRITO, 10.5f, X[i], X[i + 1], Y(tabTopo + 26f), titulos[i]);
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

        /** Rodapé em todas as páginas: contacto, e-mail, página do site e numeração. */
        private void rodape() throws IOException {
            int total = doc.getNumberOfPages();
            List<String[]> partes = new ArrayList<>();
            if (e.getTelefone() != null && !e.getTelefone().isBlank()) {
                partes.add(new String[]{"Contacto: ", e.getTelefone()});
            }
            if (e.getEmail() != null && !e.getEmail().isBlank()) {
                partes.add(new String[]{"E-mail: ", e.getEmail()});
            }
            if (e.getWebsite() != null && !e.getWebsite().isBlank()) {
                partes.add(new String[]{"Site: ", e.getWebsite()});
            }
            float sep = 18f;
            float soma = 0;
            for (String[] p : partes) {
                soma += largura(NEGRITO, PEQ, p[0]) + largura(NORMAL, PEQ, p[1]);
            }
            soma += sep * Math.max(0, partes.size() - 1);
            for (int i = 0; i < total; i++) {
                PDPage pg = doc.getPage(i);
                cs = new PDPageContentStream(doc, pg, PDPageContentStream.AppendMode.APPEND, true, true);
                cs.setLineWidth(0.7f);
                cs.setStrokingColor(0f);
                cs.setNonStrokingColor(0f);
                linha(X[0], Y(778f), X[5], Y(778f));
                float x = (LARGURA - soma) / 2f;
                for (String[] p : partes) {
                    texto(NEGRITO, PEQ, x, Y(792f), p[0]);
                    x += largura(NEGRITO, PEQ, p[0]);
                    texto(NORMAL, PEQ, x, Y(792f), p[1]);
                    x += largura(NORMAL, PEQ, p[1]) + sep;
                }
                textoCentrado(NORMAL, PEQ, 0, LARGURA, Y(806f), "Página " + (i + 1) + " de " + total);
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
