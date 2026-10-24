package view;

import domain.Capitulo;
import domain.Cliente;
import domain.Dinheiro;
import domain.EstadoOrcamento;
import domain.Empresa;
import domain.LinhaOrcamento;
import domain.Orcamento;
import domain.UnidadeMedida;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Editor do orçamento no estilo "cartão" (folha A4 com barra azul): clica-se em qualquer texto
 * para o editar. Cobre US05 (dados), US06 (linhas e totais), US07 (estado), US08 (PDF) e US10 (duplicar).
 * Tudo o que se escreve é gravado automaticamente.
 */
public class OrcamentoView extends BorderPane {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double LARG_DESC = 350;
    // idx | sep | descrição | sep | qtd | un | sep | preço | sep | total
    private static final double[] LARG = {36, 1, LARG_DESC, 1, 46, 66, 1, 96, 1, 108};
    private static final double LARG_TAB = 708;   // 706 de colunas + 2 de bordas
    private static final String AZUL_ESCURO = "#174073";
    private static final String[] IVAS = {"IVA não incluído", "IVA 23%", "IVA 13%", "IVA 6%"};

    private final Contexto ctx;
    private final Orcamento orc;
    private final Runnable voltar;
    private final Consumer<Orcamento> abrir;

    private final VBox folha = new VBox();
    private final ScrollPane rolagem = new ScrollPane();
    private final List<Runnable> pendentes = new ArrayList<>();
    private final Label lblSubtotal = new Label();
    private final Label lblIva = new Label();
    private final Label lblRotuloIva = new Label("IVA");
    private final Label lblRotuloTotal = new Label();
    private final Label lblTotal = new Label();
    private final Label lblRodape = new Label();
    private Label lblCodigo;
    private final VBox blocos = new VBox(0);
    private final List<Linha> linhas = new ArrayList<>();

    public OrcamentoView(Contexto ctx, Orcamento orc, Runnable voltar, Consumer<Orcamento> abrir) {
        this.ctx = ctx;
        this.orc = orc;
        this.voltar = voltar;
        this.abrir = abrir;

        folha.getStyleClass().add("folha");
        folha.setMaxWidth(794);
        folha.setMinWidth(794);
        folha.setPrefWidth(794);

        StackPane papel = new StackPane(folha);
        papel.setMaxWidth(794);
        papel.getStyleClass().add("folha-sombra");

        VBox mesa = new VBox(papel);
        mesa.setAlignment(Pos.TOP_CENTER);
        mesa.setPadding(new Insets(24, 16, 64, 16));
        rolagem.setContent(mesa);
        rolagem.setFitToWidth(true);
        rolagem.getStyleClass().add("mesa");

        setTop(barraFerramentas());
        setCenter(rolagem);
        construir();
    }

    // ================================================================= estrutura

    private boolean editavel() {
        return orc.getEstado() == EstadoOrcamento.RASCUNHO;
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void construir() {
        pendentes.clear();
        linhas.clear();
        blocos.getChildren().clear();
        folha.getChildren().setAll(cabecalho(), espaco(34), seccaoServicos(),
                espaco(18), seccaoTotais(), seccaoObservacoes(), lblRodape, rodapeMorada());
        lblRodape.getStyleClass().add("folha-rodape");
        atualizarTotais();
    }

    /** Linha ainda por completar (não está no orçamento): guarda-se para não desaparecer ao reconstruir. */
    private static final class Resto {
        int pos;
        String desc;
        String qtd;
        String preco;
        UnidadeMedida un;
    }

    private List<Object[]> restauro;   // por bloco: {nº de linhas, List<Resto>}
    private int blocoIdx;

    private void reconstruir() {
        reconstruir(true);
    }

    private void reconstruir(boolean preservarLinhas) {
        double posicao = rolagem.getVvalue();
        restauro = preservarLinhas ? capturarRascunhos() : null;
        blocoIdx = 0;
        setTop(barraFerramentas());
        construir();
        restauro = null;
        Platform.runLater(() -> rolagem.setVvalue(posicao));
    }

    /** Guarda as linhas escritas mas ainda sem preço (ou só com a descrição) e o nº de linhas de cada bloco. */
    private List<Object[]> capturarRascunhos() {
        List<Object[]> r = new ArrayList<>();
        for (Node b : blocos.getChildren()) {
            if (!(b.getUserData() instanceof Bloco)) {
                continue;
            }
            Bloco bl = (Bloco) b.getUserData();
            List<Resto> restos = new ArrayList<>();
            int pos = 0;
            for (Node f : bl.corpo.getChildren()) {
                if (f.getUserData() instanceof Linha) {
                    Linha l = (Linha) f.getUserData();
                    boolean escrita = !l.desc.getText().isBlank() || !l.qtd.getText().isBlank()
                            || !l.preco.getText().isBlank();
                    if (l.modelo == null && escrita) {
                        Resto x = new Resto();
                        x.pos = pos;
                        x.desc = l.desc.getText();
                        x.qtd = l.qtd.getText();
                        x.preco = l.preco.getText();
                        x.un = l.un.getValue();
                        restos.add(x);
                    }
                }
                pos++;
            }
            r.add(new Object[]{bl.corpo.getChildren().size(), restos});
        }
        return r;
    }

    private void gravarPendentes() {
        for (Runnable r : new ArrayList<>(pendentes)) {
            r.run();
        }
    }

    private static Region espaco(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        r.setMaxHeight(h);
        return r;
    }

    /** Rodapé como no PDF: linha preta e a morada completa, centrada e a negrito («Vizela» maior). */
    private javafx.scene.Node rodapeMorada() {
        Region linha = new Region();
        linha.setStyle("-fx-background-color: black;");
        linha.setMinHeight(2);
        linha.setPrefHeight(2);
        linha.setMaxHeight(2);
        VBox.setMargin(linha, new Insets(14, 0, 8, 0));
        String m = ctx.repo.getEmpresa().getMorada() == null ? ""
                : ctx.repo.getEmpresa().getMorada().trim().replaceAll("\\s*\\R\\s*", ", ");
        int v = m.indexOf("Vizela");
        javafx.scene.text.TextFlow fluxo = new javafx.scene.text.TextFlow();
        fluxo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        String est = "-fx-font-weight: bold; -fx-fill: black; -fx-font-size: ";
        if (v >= 0) {
            Text a = new Text(m.substring(0, v));
            a.setStyle(est + "15px;");
            Text b = new Text("Vizela");
            b.setStyle(est + "22px;");
            Text c = new Text(m.substring(v + 6));
            c.setStyle(est + "15px;");
            fluxo.getChildren().addAll(a, b, c);
        } else {
            Text a = new Text(m);
            a.setStyle(est + "15px;");
            fluxo.getChildren().add(a);
        }
        return new VBox(linha, fluxo);
    }

    private static Region regra() {
        Region r = new Region();
        r.getStyleClass().add("folha-regra");
        r.setMinHeight(2.5);
        r.setPrefHeight(2.5);
        r.setMaxHeight(2.5);
        VBox.setMargin(r, new Insets(22, 0, 22, 0));
        return r;
    }

    private static Label h2(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("folha-h2");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    // ============================================================ barra superior

    private HBox barraFerramentas() {
        Button bVoltar = new Button("← Orçamentos");
        bVoltar.getStyleClass().add("tb-botao");
        bVoltar.setOnAction(e -> {
            gravarPendentes();
            voltar.run();
        });

        Label dica = new Label(editavel()
                ? "Preencha os campos vazios: as contas fazem-se sozinhas."
                : "Este orçamento já não é um rascunho, por isso não pode ser editado.");
        dica.getStyleClass().add("tb-dica");

        Label estado = new Label(orc.getEstadoTexto());
        estado.setMinWidth(Region.USE_PREF_SIZE);
        estado.getStyleClass().addAll("badge", "estado-" + orc.getEstado().name().toLowerCase());

        MenuButton bEstado = new MenuButton("Alterar estado");
        bEstado.getStyleClass().add("tb-botao");
        for (EstadoOrcamento novo : orc.getEstado().transicoesPermitidas()) {
            MenuItem item = new MenuItem(rotuloTransicao(novo));
            item.setOnAction(e -> mudarEstado(novo));
            bEstado.getItems().add(item);
        }
        if (orc.podeEliminar()) {
            if (!bEstado.getItems().isEmpty()) {
                bEstado.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }
            MenuItem del = new MenuItem("Eliminar rascunho");
            del.setOnAction(e -> {
                if (Ui.confirmar(janela(), "Eliminar este rascunho? Esta ação não pode ser desfeita.")) {
                    try {
                        ctx.orcamentos.eliminar(orc);
                        voltar.run();
                    } catch (RuntimeException ex) {
                        Ui.erro(janela(), Ui.mensagem(ex));
                    }
                }
            });
            bEstado.getItems().add(del);
        }
        bEstado.setDisable(bEstado.getItems().isEmpty());

        Button bDuplicar = new Button("Duplicar");
        bDuplicar.getStyleClass().add("tb-botao");
        bDuplicar.setOnAction(e -> {
            gravarPendentes();
            try {
                Orcamento copia = ctx.orcamentos.duplicar(orc);
                abrir.accept(copia);
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });

        Button bNovo = new Button("Novo orçamento");
        bNovo.getStyleClass().add("tb-botao");
        bNovo.setOnAction(e -> {
            gravarPendentes();
            OrcamentosView.criarEAbrir(ctx, janela(), abrir);
        });

        Button bPdf = new Button("Guardar em Word");
        bPdf.getStyleClass().addAll("tb-botao", "tb-primario");
        bPdf.setOnAction(e -> {
            gravarPendentes();
            Ui.guardarWord(ctx, orc, janela());
        });

        dica.setMinWidth(0);
        dica.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox.setHgrow(dica, Priority.SOMETIMES);
        for (javafx.scene.control.ButtonBase x : new javafx.scene.control.ButtonBase[]{bVoltar, bEstado, bDuplicar, bNovo, bPdf}) {
            x.setMinWidth(Region.USE_PREF_SIZE);
        }
        HBox b = new HBox(12, bVoltar, estado, dica, Ui.espaco(), bEstado, bDuplicar, bNovo, bPdf);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().add("tb");
        return b;
    }

    private static String rotuloTransicao(EstadoOrcamento e) {
        switch (e) {
            case EMITIDO:
                return "Emitir orçamento";
            case ADJUDICADO:
                return "Marcar como adjudicado";
            case RECUSADO:
                return "Marcar como recusado";
            case ANULADO:
                return "Anular orçamento";
            default:
                return e.getDescricao();
        }
    }

    private void mudarEstado(EstadoOrcamento novo) {
        gravarPendentes();
        String aviso = novo == EstadoOrcamento.ADJUDICADO && orc.isExpirado()
                ? "A validade deste orçamento já terminou. Marcar mesmo assim como adjudicado?"
                : novo == EstadoOrcamento.EMITIDO
                ? "Ao emitir, o orçamento deixa de poder ser editado. Continuar?"
                : "Mudar o estado do orçamento para «" + novo.getDescricao() + "»?";
        if (!Ui.confirmar(janela(), aviso)) {
            return;
        }
        try {
            ctx.orcamentos.mudarEstado(orc, novo);
            reconstruir();
        } catch (RuntimeException ex) {
            Ui.erro(janela(), Ui.mensagem(ex));
        }
    }

    // ================================================================= cabeçalho

    private HBox cabecalho() {
        Empresa emp = ctx.repo.getEmpresa();

        VBox esq = new VBox(3);
        esq.setAlignment(Pos.TOP_CENTER);
        esq.setMinWidth(304);
        esq.setPrefWidth(304);
        esq.setMaxWidth(304);
        ImageView logo = Ui.logotipo(141);
        Label nome = new Label(emp.getDesignacao());
        nome.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: black;");
        nome.setMinWidth(Region.USE_PREF_SIZE);
        VBox.setMargin(nome, new Insets(8, 0, 2, 0));
        esq.getChildren().addAll(logo, nome);
        String nif = emp.getNif() == null || emp.getNif().isBlank() ? "" : formatarNif(emp.getNif());
        String tel = formatarTelefone(emp.getTelefone());
        if (!nif.isBlank()) {
            esq.getChildren().add(rotuloNumero("NIF: ", nif, 17));
        }
        if (!tel.isBlank()) {
            esq.getChildren().add(rotuloNumero("Tel: ", tel, 20.5));
        }
        for (String s : new String[]{emp.getEmail(), emp.getWebsite()}) {
            if (s != null && !s.isBlank()) {
                Label l = new Label(s);
                l.setStyle("-fx-font-size: 16px; -fx-text-fill: black;");
                esq.getChildren().add(l);
            }
        }

        VBox dir = new VBox(24, tabelaOrcamento(), caixaCliente());
        dir.setMinWidth(432);
        dir.setPrefWidth(432);
        dir.setMaxWidth(432);
        HBox h = new HBox(esq, dir);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
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

    /** «NIF: » em letra normal e o número a negrito. */
    private static HBox rotuloNumero(String rotulo, String numero, double px) {
        Label a = new Label(rotulo);
        a.setStyle("-fx-font-size: " + px + "px; -fx-text-fill: black;");
        Label b = new Label(numero);
        b.setStyle("-fx-font-size: " + px + "px; -fx-font-weight: bold; -fx-text-fill: black;");
        HBox h = new HBox(a, b);
        h.setAlignment(Pos.CENTER);
        return h;
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

    private static Label barraAzul(String texto) {
        Label l = new Label(espacado(texto));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setMinHeight(40);
        l.setPrefHeight(40);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color: " + AZUL_ESCURO + "; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-font-size: 18.5px;");
        return l;
    }

    private static Label celula(String t, double px, double altura, boolean borda) {
        Label l = new Label(t);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setMinHeight(altura);
        l.setPrefHeight(altura);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-size: " + px + "px; -fx-font-weight: bold; -fx-text-fill: black;"
                + " -fx-border-color: black; -fx-border-width: 1 " + (borda ? 1 : 0) + " 0 0;");
        return l;
    }

    /** Tabela ORÇAMENTO: barra azul + Nº Orçamento / Data / Código cliente. */
    private VBox tabelaOrcamento() {
        String titulo = "ORÇAMENTO";
        if (orc.getEstado() == EstadoOrcamento.ANULADO) {
            titulo += " (" + orc.getEstado().getDescricao().toUpperCase() + ")";
        }
        GridPane g = new GridPane();
        double[] pc = {25.3, 35.2, 39.5};
        for (double w : pc) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(w);
            g.getColumnConstraints().add(c);
        }
        String[] cab = {"Nº Orçamento", "Data", "Código cliente"};
        Cliente cli = orc.getCliente();
        String[] val = {orc.getNumero(), orc.getData().format(DATA), cli.getCodigo() == null ? "" : cli.getCodigo()};
        for (int i = 0; i < 3; i++) {
            g.add(celula(cab[i], 14, 32, i < 2), i, 0);
            Label vl = celula(val[i], 18.5, 46, i < 2);
            if (i == 2) {
                lblCodigo = vl;
            }
            g.add(vl, i, 1);
        }
        VBox v = new VBox(barraAzul(titulo), g);
        v.setStyle("-fx-border-color: black; -fx-border-width: 1;");
        return v;
    }

    /** Caixa CLIENTE: barra azul, nome (centrado) e morada, ambos editáveis. */
    /** Atualiza o «Código cliente» quando o cliente passa a estar registado. */
    private void mostrarCodigo() {
        if (lblCodigo != null) {
            String cod = orc.getCliente().getCodigo();
            lblCodigo.setText(cod == null ? "" : cod);
        }
    }

    private VBox caixaCliente() {
        Cliente c = orc.getCliente();
        TextField nome = campoCliente(c::getNome, "Nome do cliente", c::setNome);
        nome.getStyleClass().add("folha-cliente-nome");
        TextField morada = campoCliente(c::getMorada, "Morada", c::setMorada);
        TextField nif = campoCliente(c::getNif, "NIF", this::definirNif);
        TextField tel = campoCliente(c::getTelefone, "Telefone", c::setTelefone);
        TextField mail = campoCliente(c::getEmail, "E-mail", c::setEmail);
        for (TextField t : new TextField[]{morada, nif, tel, mail}) {
            t.setStyle("-fx-font-size: 14px;");
        }
        VBox corpo = new VBox(2, nome, morada, nif, tel, mail);
        corpo.setPadding(new Insets(12, 12, 12, 12));
        VBox v = new VBox(barraAzul("CLIENTE"), corpo);
        v.setStyle("-fx-border-color: black; -fx-border-width: 1;");
        return v;
    }

    private static Label rotuloFolha(String t) {
        Label l = new Label(t);
        l.getStyleClass().addAll("folha-pequeno", "folha-cinza");
        return l;
    }

    private static Label valorFolha(String t) {
        Label l = new Label(t);
        l.getStyleClass().addAll("folha-pequeno", "folha-forte");
        return l;
    }

    // =================================================================== campos

    /** Campo de texto "clique para editar": grava ao sair do campo; Enter confirma. */
    private TextField campo(String texto, String dica, Consumer<String> guardar) {
        TextField t = new TextField(texto == null ? "" : texto);
        t.setPromptText(dica);
        t.getStyleClass().add("folha-campo");
        t.setDisable(!editavel());
        Runnable gravar = () -> {
            if (editavel()) {
                guardar.accept(t.getText() == null ? "" : t.getText().trim());
            }
        };
        pendentes.add(gravar);
        t.focusedProperty().addListener((o, a, f) -> {
            if (!f) {
                gravar.run();
            }
        });
        t.setOnAction(e -> folha.requestFocus());
        return t;
    }

    /** Executa uma alteração ao cliente e mostra o erro (repondo o valor) se for inválida. */
    private TextField campoCliente(Supplier<String> atual, String dica, Consumer<String> aplicar) {
        TextField[] ref = new TextField[1];
        ref[0] = campo(atual.get(), dica, novo -> {
            if (novo.equals(atual.get() == null ? "" : atual.get())) {
                return;
            }
            try {
                aplicar.accept(novo);
                ctx.clientes.registarSeCompleto(orc.getCliente());
                ctx.clientes.guardarAlteracoes();
                mostrarCodigo();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
                ref[0].setText(atual.get());
            }
        });
        return ref[0];
    }

    // ================================================================== cliente

    /** NIF do cliente; se já existir um cliente com esse NIF, oferece-se para o usar neste orçamento. */
    private void definirNif(String valor) {
        Cliente c = orc.getCliente();
        String nif = domain.Validacao.nif(valor);
        java.util.Optional<Cliente> existente = ctx.repo.procurarClientePorNif(nif);
        if (existente.isPresent() && existente.get() != c) {
            Cliente outro = existente.get();
            if (Ui.confirmar(janela(), "Já existe o cliente «" + outro.getNome() + "» com este NIF.\n"
                    + "Usar esse cliente neste orçamento?")) {
                ctx.orcamentos.alterarCliente(orc, outro);
                Platform.runLater(this::reconstruir);
                return;
            }
            throw new IllegalArgumentException("Já existe outro cliente com esse NIF.");
        }
        c.setNif(nif);
    }

    private static HBox linhaCampo(String rotulo, Node campo) {
        Label l = new Label(rotulo);
        l.getStyleClass().add("folha-rotulo");
        l.setMinWidth(54);
        l.setPrefWidth(54);
        HBox h = new HBox(l, campo);
        h.setAlignment(Pos.CENTER_LEFT);
        if (campo instanceof Region) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return h;
    }

    // ================================================================== serviços

    private VBox seccaoServicos() {
        Region fecho = new Region();
        fecho.setStyle("-fx-background-color: black;");
        fecho.setMinHeight(1);
        fecho.setPrefHeight(1);
        fecho.setMaxHeight(1);
        fecho.setMaxWidth(LARG_TAB);
        VBox v = new VBox(0, cabecalhoTabela(), blocos, fecho);

        List<Capitulo> caps = orc.getCapitulos();
        if (caps.isEmpty()) {
            blocos.getChildren().add(new Bloco(null, 0).no);
        } else {
            int numero = 0;
            for (Capitulo c : caps) {
                if (c.temTitulo()) {
                    numero++;
                }
                blocos.getChildren().add(new Bloco(c, numero).no);
            }
        }

        if (editavel()) {
            Button cap = Ui.link("+ Adicionar capítulo");
            cap.setOnAction(e -> novoCapitulo());
            HBox botoes = new HBox(18);
            botoes.setAlignment(Pos.CENTER_LEFT);
            botoes.getChildren().add(cap);
            VBox.setMargin(botoes, new Insets(10, 0, 0, 0));
            v.getChildren().add(botoes);
        }
        return v;
    }

    private HBox cabecalhoTabela() {
        GridPane g = grelhaLinha();
        String[] t = {"", "Descrição", "Un.", "Preço Unit.", "Preço Total"};
        int[] col = {0, 2, 4, 7, 9};
        int[] span = {1, 1, 2, 1, 1};
        for (int i = 0; i < t.length; i++) {
            Label l = new Label(t[i]);
            l.getStyleClass().add("folha-th");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setMinHeight(40);
            l.setAlignment(Pos.CENTER);
            g.add(l, col[i], 0, span[i], 1);
        }
        for (int c : new int[]{1, 3, 6, 8}) {
            g.add(separador(), c, 0);
        }
        g.setStyle("-fx-border-color: black; -fx-border-width: 1;");
        return new HBox(g);
    }

    private static Region separador() {
        Region r = new Region();
        r.setStyle("-fx-border-color: black; -fx-border-width: 0 0 0 1;");
        return r;
    }

    private static GridPane grelhaLinha() {
        GridPane g = new GridPane();
        for (double w : LARG) {
            ColumnConstraints c = new ColumnConstraints(w);
            c.setMinWidth(w);
            c.setMaxWidth(w);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    private void novoCapitulo() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Novo capítulo");
        d.setHeaderText(null);
        d.setContentText("Título do capítulo (ex.: Eletricidade):");
        d.getDialogPane().getStylesheets().add(Ui.class.getResource("/estilo.css").toExternalForm());
        if (janela() != null) {
            d.initOwner(janela());
        }
        d.showAndWait().ifPresent(titulo -> {
            if (titulo.isBlank()) {
                return;
            }
            try {
                gravarPendentes();
                ctx.orcamentos.adicionarCapitulo(orc, titulo.trim());
                reconstruir();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });
    }

    /** Acrescenta uma linha em branco ao bloco do capítulo (ou ao único bloco, se não há capítulos). */
    private void novaLinha(Capitulo cap) {
        for (javafx.scene.Node n : blocos.getChildren()) {
            Object dono = n.getUserData();
            if (dono instanceof Bloco && ((Bloco) dono).cap == cap) {
                ((Bloco) dono).acrescentarLinha(null);
                return;
            }
        }
        if (!blocos.getChildren().isEmpty() && blocos.getChildren().get(0).getUserData() instanceof Bloco) {
            ((Bloco) blocos.getChildren().get(0).getUserData()).acrescentarLinha(null);
        }
    }

    /** Um capítulo (ou o bloco único, sem título) com as suas linhas. */
    private final class Bloco {
        final Capitulo cap;
        final VBox no = new VBox(0);
        final VBox corpo = new VBox(0);

        Bloco(Capitulo cap, int numero) {
            this.cap = cap;
            no.setUserData(this);
            if (cap != null && cap.temTitulo()) {
                Label num = new Label(numero + ".");
                num.getStyleClass().add("folha-capitulo-num");
                TextField titulo = campo(cap.getTitulo(), "Título do capítulo", v -> {
                    if (v.equals(cap.getTitulo())) {
                        return;
                    }
                    try {
                        ctx.orcamentos.renomearCapitulo(orc, cap, v);
                    } catch (RuntimeException ex) {
                        Ui.erro(janela(), Ui.mensagem(ex));
                    }
                });
                titulo.getStyleClass().add("folha-capitulo");
                HBox.setHgrow(titulo, Priority.ALWAYS);
                Button x = botaoRemover("Remover capítulo");
                x.setOnAction(e -> {
                    if (Ui.confirmar(janela(), "Remover o capítulo «" + cap.getTitulo() + "» e as suas linhas?")) {
                        try {
                            gravarPendentes();
                            ctx.orcamentos.removerCapitulo(orc, cap);
                            reconstruir(false);
                        } catch (RuntimeException ex) {
                            Ui.erro(janela(), Ui.mensagem(ex));
                        }
                    }
                });
                HBox cab = new HBox(6, num, titulo, x);
                cab.setAlignment(Pos.CENTER_LEFT);
                cab.setPadding(new Insets(8, 4, 2, 10));
                cab.setMaxWidth(LARG_TAB);
                cab.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 1;");
                no.getChildren().add(cab);
            } else if (editavel() && !orc.temCapitulos()) {
                // modelo em branco: linha de capítulo com dica a cinzento, para escrever por cima
                Label num = new Label("1.");
                num.getStyleClass().add("folha-capitulo-num");
                TextField titulo = campo("", "Capítulo 1 — escreva o título (ex.: Eletricidade)", v -> {
                    if (v.isBlank()) {
                        return;
                    }
                    try {
                        if (cap != null) {
                            ctx.orcamentos.renomearCapitulo(orc, cap, v);
                        } else if (!orc.getCapitulos().isEmpty()) {
                            ctx.orcamentos.renomearCapitulo(orc, orc.getCapitulos().get(0), v);
                        } else {
                            ctx.orcamentos.adicionarCapitulo(orc, v);
                        }
                        Platform.runLater(OrcamentoView.this::reconstruir);
                    } catch (RuntimeException ex) {
                        Ui.erro(janela(), Ui.mensagem(ex));
                    }
                });
                titulo.getStyleClass().add("folha-capitulo");
                HBox.setHgrow(titulo, Priority.ALWAYS);
                HBox cab = new HBox(6, num, titulo);
                cab.setAlignment(Pos.CENTER_LEFT);
                cab.setPadding(new Insets(8, 4, 2, 10));
                cab.setMaxWidth(LARG_TAB);
                cab.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 1;");
                no.getChildren().add(cab);
            }
            no.getChildren().add(corpo);
            if (cap != null) {
                for (LinhaOrcamento l : cap.getLinhas()) {
                    acrescentarLinha(l);
                }
            }
            if (restauro != null && blocoIdx < restauro.size()) {
                Object[] e = restauro.get(blocoIdx);
                @SuppressWarnings("unchecked")
                List<Resto> restos = (List<Resto>) e[1];
                for (Resto x : restos) {
                    Linha l = new Linha(this, null);
                    linhas.add(l);
                    corpo.getChildren().add(Math.min(x.pos, corpo.getChildren().size()), l.fila);
                    l.desc.setText(x.desc);
                    l.qtd.setText(x.qtd);
                    l.preco.setText(x.preco);
                    if (x.un != null) {
                        l.un.setValue(x.un);
                    }
                }
                while (corpo.getChildren().size() < (Integer) e[0]) {
                    acrescentarLinha(null, false);
                }
            }
            blocoIdx++;
            if (editavel() && cap == null) {
                while (corpo.getChildren().size() < 6) { // modelo com linhas vazias prontas a preencher
                    acrescentarLinha(null, false);
                }
            }
            garantirLinhaLivre();
            if (editavel()) {
                Button add = new Button("+ Adicionar linha");
                add.getStyleClass().add("folha-adicionar");
                add.setOnAction(e -> acrescentarLinha(null));
                HBox caixaAdd = new HBox(add);
                caixaAdd.setPadding(new Insets(6, 0, 6, 10));
                caixaAdd.setMaxWidth(LARG_TAB);
                caixaAdd.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 1;");
                no.getChildren().add(caixaAdd);
            }
        }

        void acrescentarLinha(LinhaOrcamento modelo) {
            acrescentarLinha(modelo, modelo == null);
        }

        void acrescentarLinha(LinhaOrcamento modelo, boolean foco) {
            Linha l = new Linha(this, modelo);
            linhas.add(l);
            corpo.getChildren().add(l.fila);
            if (foco) {
                Platform.runLater(() -> l.desc.requestFocus());
            }
        }

        /** Há sempre uma linha vazia no fim: ao escrever na última, aparece outra por baixo. */
        void garantirLinhaLivre() {
            if (!editavel()) {
                return;
            }
            Object ultimo = corpo.getChildren().isEmpty() ? null
                    : corpo.getChildren().get(corpo.getChildren().size() - 1).getUserData();
            if (!(ultimo instanceof Linha) || ((Linha) ultimo).modelo != null
                    || !((Linha) ultimo).desc.getText().isBlank()) {
                acrescentarLinha(null, false);
            }
        }

        void removida(Linha l) {
            corpo.getChildren().remove(l.fila);
            linhas.remove(l);
            garantirLinhaLivre();
            if (corpo.getChildren().isEmpty()) { // nunca fica sem linhas: aparece uma em branco
                acrescentarLinha(null, false); // aparece uma linha em branco
            }
            atualizarTotais();
        }
    }

    private Button botaoRemover(String dica) {
        Button x = new Button("×");
        x.getStyleClass().add("folha-x");
        x.setTooltip(new javafx.scene.control.Tooltip(dica));
        x.setVisible(editavel());
        x.setManaged(editavel());
        return x;
    }

    /** Uma linha do orçamento: descrição, quantidade, unidade, preço, total e botão de remover. */
    private final class Linha {
        final Bloco bloco;
        LinhaOrcamento modelo;
        final GridPane no = grelhaLinha();
        final HBox fila = new HBox();
        final Label num = new Label();
        final TextArea desc = new TextArea();
        final TextField qtd = new TextField();
        final ComboBox<UnidadeMedida> un = new ComboBox<>(FXCollections.observableArrayList(UnidadeMedida.values()));
        final TextField preco = new TextField();
        final Label total = new Label();

        Linha(Bloco bloco, LinhaOrcamento modelo) {
            this.bloco = bloco;
            this.modelo = modelo;
            no.getStyleClass().add("folha-linha");
            no.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 1;");
            fila.setUserData(this);
            fila.getStyleClass().add("folha-fila");

            num.getStyleClass().add("folha-pequeno");
            num.setMaxWidth(Double.MAX_VALUE);
            num.setAlignment(Pos.TOP_CENTER);
            desc.getStyleClass().add("folha-campo");
            desc.getStyleClass().add("folha-desc");
            desc.setWrapText(true);
            desc.setPromptText("Descrição do trabalho ou material");
            desc.setFont(Font.font("Montserrat", 13.5));
            desc.setText(modelo == null ? "" : modelo.getDescricao());
            ajustarAltura();
            desc.textProperty().addListener((o, a, b) -> ajustarAltura());

            qtd.getStyleClass().addAll("folha-campo", "folha-direita");
            qtd.setPromptText("Qtd.");
            
            qtd.setText(modelo == null ? "" : Ui.semZeros(modelo.getQuantidade()).replace('.', ','));
            preco.getStyleClass().addAll("folha-campo", "folha-centro");
            preco.setPromptText("Preço");
            
            preco.setText(modelo == null ? "" : Dinheiro.formatar(modelo.getPrecoUnitario()));
            un.getStyleClass().add("folha-un");
            un.setValue(modelo == null ? UnidadeMedida.UNIDADE : modelo.getUnidade());
            total.getStyleClass().add("folha-total-linha");
            total.setMaxWidth(Double.MAX_VALUE);
            total.setAlignment(Pos.TOP_CENTER);
            total.setText(modelo == null ? "" : Dinheiro.formatar(modelo.getTotal()));

            Button x = botaoRemover("Remover linha");
            x.setOnAction(e -> remover());
            HBox.setMargin(x, new Insets(6, 0, 0, 4));

            no.add(num, 0, 0);
            no.add(desc, 2, 0);
            no.add(qtd, 4, 0);
            no.add(un, 5, 0);
            no.add(preco, 7, 0);
            no.add(total, 9, 0);
            for (int c : new int[]{1, 3, 6, 8}) {
                no.add(separador(), c, 0);
            }
            Insets m = new Insets(6, 2, 6, 2);
            for (javafx.scene.Node n : new javafx.scene.Node[]{num, desc, qtd, un, preco, total}) {
                GridPane.setValignment(n, javafx.geometry.VPos.TOP);
                GridPane.setMargin(n, m);
            }
            GridPane.setMargin(num, new Insets(11, 0, 6, 0));
            GridPane.setMargin(total, new Insets(10, 0, 6, 0));
            fila.getChildren().addAll(no, x);

            for (javafx.scene.Node n : new javafx.scene.Node[]{desc, qtd, preco}) {
                n.setDisable(!editavel());
            }
            un.setDisable(!editavel());

            Runnable gravar = this::gravar;
            pendentes.add(gravar);
            for (javafx.scene.Node n : new javafx.scene.Node[]{desc, qtd, preco}) {
                n.focusedProperty().addListener((o, a, f) -> {
                    if (!f) {
                        gravar.run();
                    }
                });
            }
            qtd.setOnAction(e -> folha.requestFocus());
            preco.setOnAction(e -> folha.requestFocus());
            un.valueProperty().addListener((o, a, b) -> gravar.run());

            // contas ao vivo: a linha e os totais atualizam enquanto se escreve
            javafx.beans.value.ChangeListener<String> aoVivo = (o, a, b) -> recalcularAoVivo();
            qtd.textProperty().addListener(aoVivo);
            preco.textProperty().addListener(aoVivo);
            desc.textProperty().addListener(aoVivo);
            desc.textProperty().addListener((o, a, b) -> {
                if (!b.isBlank()) {
                    bloco.garantirLinhaLivre();
                }
            });
        }

        /** Total da linha com o que está escrito agora; null se a linha ainda não estiver completa. */
        BigDecimal totalAoVivo() {
            try {
                String d = desc.getText() == null ? "" : desc.getText().trim();
                if (d.isEmpty() || preco.getText() == null || preco.getText().isBlank()) {
                    return null;
                }
                BigDecimal q = qtd.getText() == null || qtd.getText().isBlank() ? BigDecimal.ONE
                        : Ui.paraDecimal(qtd.getText());
                BigDecimal p = Ui.paraDecimal(preco.getText().replace("€", "").trim());
                if (q.signum() <= 0 || p.signum() <= 0) {
                    return null;
                }
                return Dinheiro.arredondar(q.multiply(p));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        /** A caixa de descrição cresce com o texto (como no site). */
        private void ajustarAltura() {
            Text medida = new Text(desc.getText().isEmpty() ? " " : desc.getText() + "​");
            medida.setFont(desc.getFont());
            medida.setWrappingWidth(LARG_DESC - 15 - 12);
            double h = Math.ceil(medida.getLayoutBounds().getHeight()) + 14;
            desc.setPrefHeight(h);
            desc.setMinHeight(h);
            desc.setMaxHeight(h);
        }

        void gravar() {
            if (!editavel()) {
                return;
            }
            String d = desc.getText() == null ? "" : desc.getText().trim();
            BigDecimal q;
            BigDecimal p;
            try {
                q = qtd.getText() == null || qtd.getText().isBlank() ? BigDecimal.ONE : Ui.paraDecimal(qtd.getText());
                p = preco.getText() == null || preco.getText().isBlank() ? null
                        : Ui.paraDecimal(preco.getText().replace("€", "").trim());
            } catch (NumberFormatException ex) {
                assinalar(true);
                return;
            }
            boolean completo = !d.isEmpty() && p != null && p.signum() > 0 && q != null && q.signum() > 0;
            if (!completo) {
                // linha ainda por preencher: não conta para o orçamento
                assinalar(modelo != null || !d.isEmpty() || p != null);
                return;
            }
            try {
                if (modelo == null) {
                    modelo = ctx.orcamentos.adicionarLinha(orc, bloco.cap, d, q, un.getValue(), p);
                } else {
                    ctx.orcamentos.editarLinha(orc, modelo, d, q, un.getValue(), p);
                }
                assinalar(false);
                total.setText(Dinheiro.formatar(modelo.getTotal()));
                qtd.setText(Ui.semZeros(q).replace('.', ','));
                preco.setText(Dinheiro.formatar(p));
                atualizarTotais();
            } catch (RuntimeException ex) {
                assinalar(true);
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        }

        private void assinalar(boolean incompleta) {
            if (incompleta) {
                if (!no.getStyleClass().contains("folha-incompleta")) {
                    no.getStyleClass().add("folha-incompleta");
                }
            } else {
                no.getStyleClass().remove("folha-incompleta");
            }
        }

        void remover() {
            try {
                if (modelo != null) {
                    ctx.orcamentos.removerLinha(orc, modelo);
                    modelo = null;
                }
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
                return;
            }
            bloco.removida(this);
            atualizarTotais();
        }
    }

    // =================================================================== totais

    private HBox seccaoTotais() {
        // Esquerda: válido até
        DatePicker validoAte = new DatePicker(orc.getValidoAte());
        Ui.formatoData(validoAte);
        validoAte.getStyleClass().add("folha-data");
        validoAte.setDisable(!editavel());
        Label dias = new Label();
        dias.getStyleClass().addAll("folha-pequeno", "folha-cinza");
        Runnable mostrarDias = () -> {
            if (validoAte.getValue() == null) {
                dias.setText("");
                return;
            }
            long n = ChronoUnit.DAYS.between(orc.getData(), validoAte.getValue());
            dias.setText(n < 0 ? "data anterior ao orçamento" : n + " dias");
        };
        mostrarDias.run();
        validoAte.valueProperty().addListener((o, a, b) -> {
            mostrarDias.run();
            if (editavel() && b != null) {
                guardarCondicoes(b);
            }
        });
        Label vTitulo = new Label("Válido até");
        vTitulo.getStyleClass().add("folha-valido");
        VBox esquerda = new VBox(6, vTitulo, new HBox(10, validoAte, dias));
        ((HBox) esquerda.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        // Direita: subtotal, IVA, total
        ComboBox<String> iva = new ComboBox<>(FXCollections.observableArrayList(IVAS));
        String atual = orc.isIvaIncluido() ? "IVA " + Ui.semZeros(orc.getTaxaIva()) + "%" : IVAS[0];
        if (!iva.getItems().contains(atual)) {
            iva.getItems().add(atual);
        }
        iva.setValue(atual);
        iva.getStyleClass().add("folha-iva");
        iva.setDisable(!editavel());
        iva.valueProperty().addListener((o, a, b) -> {
            if (!editavel() || b == null) {
                return;
            }
            try {
                if (b.equals(IVAS[0])) {
                    ctx.orcamentos.definirIva(orc, false, orc.getTaxaIva());
                } else {
                    ctx.orcamentos.definirIva(orc, true, new BigDecimal(b.replaceAll("[^0-9,.]", "").replace(',', '.')));
                }
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
            atualizarTotais();
        });

        lblRotuloTotal.getStyleClass().add("folha-total-rotulo");
        lblTotal.getStyleClass().add("folha-total-grande");
        lblTotal.setMinWidth(Region.USE_PREF_SIZE);
        dias.setMinWidth(Region.USE_PREF_SIZE);
        validoAte.setPrefWidth(150);
        lblSubtotal.getStyleClass().add("folha-forte");
        lblIva.getStyleClass().add("folha-forte");
        GridPane g = new GridPane();
        g.setHgap(24);
        g.setVgap(6);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHalignment(javafx.geometry.HPos.LEFT);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHalignment(javafx.geometry.HPos.RIGHT);
        c1.setMinWidth(250);
        g.getColumnConstraints().addAll(c0, c1);
        Label rSub = rotuloFolha("Subtotal");
        g.add(rSub, 0, 0);
        g.add(lblSubtotal, 1, 0);
        g.add(iva, 0, 1);
        g.add(lblIva, 1, 1);
        Region fio = new Region();
        fio.getStyleClass().add("folha-regra");
        fio.setMinHeight(2.5);
        fio.setPrefHeight(2.5);
        GridPane.setMargin(fio, new Insets(6, 0, 4, 0));
        g.add(fio, 0, 2, 2, 1);
        g.add(lblRotuloTotal, 0, 3);
        g.add(lblTotal, 1, 3);
        GridPane.setValignment(lblRotuloTotal, javafx.geometry.VPos.BOTTOM);

        HBox h = new HBox(esquerda, Ui.espaco(), g);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    private void guardarCondicoes(java.time.LocalDate validoAte) {
        try {
            ctx.orcamentos.definirCondicoes(orc, orc.isIvaIncluido(), validoAte, orc.getPrazoExecucao(),
                    orc.getCondicoesPagamento(), orc.getExclusoes(), orc.getInclusoes());
            atualizarTotais();
        } catch (RuntimeException ex) {
            Ui.erro(janela(), Ui.mensagem(ex));
        }
    }

    /** Recalcula linhas e totais com o que está escrito, sem esperar que se saia do campo. */
    private void recalcularAoVivo() {
        BigDecimal soma = BigDecimal.ZERO;
        for (Linha l : linhas) {
            BigDecimal t = l.totalAoVivo();
            l.total.setText(t == null ? "" : Dinheiro.formatar(t));
            if (t != null) {
                soma = soma.add(t);
            }
        }
        BigDecimal sub = Dinheiro.arredondar(soma);
        BigDecimal iva = orc.isIvaIncluido()
                ? Dinheiro.arredondar(sub.multiply(orc.getTaxaIva()).divide(new BigDecimal("100")))
                : Dinheiro.arredondar(BigDecimal.ZERO);
        lblSubtotal.setText(Dinheiro.formatar(sub));
        lblIva.setText(orc.isIvaIncluido() ? Dinheiro.formatar(iva) : "—");
        lblTotal.setText(Dinheiro.formatar(Dinheiro.arredondar(sub.add(iva))));
    }

    /** Numeração 1), 2)… das linhas já completas, como no PDF. */
    private void renumerar() {
        int n = 0;
        for (Node b : blocos.getChildren()) {
            if (b.getUserData() instanceof Bloco) {
                for (Node f : ((Bloco) b.getUserData()).corpo.getChildren()) {
                    if (f.getUserData() instanceof Linha) {
                        Linha l = (Linha) f.getUserData();
                        l.num.setText(l.modelo != null ? (++n) + ")" : "");
                    }
                }
            }
        }
    }

    private void atualizarTotais() {
        renumerar();
        lblSubtotal.setText(Dinheiro.formatar(orc.getSubtotal()));
        lblIva.setText(orc.isIvaIncluido() ? Dinheiro.formatar(orc.getValorIva()) : "—");
        lblRotuloTotal.setText(orc.isIvaIncluido() ? "TOTAL" : "PREÇO GLOBAL");
        lblTotal.setText(Dinheiro.formatar(orc.getTotal()));
        lblRodape.setText(orc.getValidoAte() == null ? "Valores em euros."
                : "Orçamento válido até " + orc.getValidoAte().format(DATA) + ". Valores em euros.");
    }

    // ============================================================== observações

    private VBox seccaoObservacoes() {
        VBox v = new VBox(8, h2("Observações"));
        v.getChildren().add(areaNota("Prazo de execução", orc.getPrazoExecucao(),
                "Ex.: 3 semanas após adjudicação", t -> ctx.orcamentos.definirCondicoes(orc, orc.isIvaIncluido(),
                        orc.getValidoAte(), t, orc.getCondicoesPagamento(), orc.getExclusoes(), orc.getInclusoes())));
        v.getChildren().add(areaNota("Condições de pagamento", orc.getCondicoesPagamento(),
                "Ex.: 40% na adjudicação, restante no fim da obra", t -> ctx.orcamentos.definirCondicoes(orc,
                        orc.isIvaIncluido(), orc.getValidoAte(), orc.getPrazoExecucao(), t, orc.getExclusoes(),
                        orc.getInclusoes())));
        v.getChildren().add(areaNota("Exclusões", orc.getExclusoes(), "Trabalhos ou materiais não incluídos…",
                t -> ctx.orcamentos.definirCondicoes(orc, orc.isIvaIncluido(), orc.getValidoAte(),
                        orc.getPrazoExecucao(), orc.getCondicoesPagamento(), t, orc.getInclusoes())));
        v.getChildren().add(areaNota("Inclusões", orc.getInclusoes(), "Trabalhos ou materiais incluídos…",
                t -> ctx.orcamentos.definirCondicoes(orc, orc.isIvaIncluido(), orc.getValidoAte(),
                        orc.getPrazoExecucao(), orc.getCondicoesPagamento(), orc.getExclusoes(), t)));
        VBox.setMargin(v, new Insets(30, 0, 0, 0));
        return v;
    }

    private VBox areaNota(String rotulo, String texto, String dica, Consumer<String> guardar) {
        Label l = new Label(rotulo);
        l.getStyleClass().addAll("folha-pequeno", "folha-cinza");
        TextArea t = new TextArea(texto == null ? "" : texto);
        t.setPromptText(dica);
        t.setWrapText(true);
        t.getStyleClass().addAll("folha-campo", "folha-nota");
        t.setFont(Font.font("Montserrat", 13));
        t.setDisable(!editavel());
        Runnable ajustar = () -> {
            Text medida = new Text((t.getText().isEmpty() ? " " : t.getText()) + "​");
            medida.setFont(t.getFont());
            medida.setWrappingWidth(794 - 58 - 14);
            double h = Math.ceil(medida.getLayoutBounds().getHeight()) + 14;
            t.setPrefHeight(h);
            t.setMinHeight(h);
            t.setMaxHeight(h);
        };
        ajustar.run();
        t.textProperty().addListener((o, a, b) -> ajustar.run());
        Runnable gravar = () -> {
            if (!editavel()) {
                return;
            }
            try {
                guardar.accept(t.getText());
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        };
        pendentes.add(gravar);
        t.focusedProperty().addListener((o, a, f) -> {
            if (!f) {
                gravar.run();
            }
        });
        return new VBox(1, l, t);
    }

}
