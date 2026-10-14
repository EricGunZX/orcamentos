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
    private static final double LARG_DESC = 292;
    private static final double[] LARG = {LARG_DESC, 52, 60, 84, 84, 28};
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
    private final VBox blocos = new VBox(14);
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

        // A barra azul fica colada ao lado esquerdo da folha, em toda a altura
        Region barra = new Region();
        barra.getStyleClass().add("folha-barra");
        barra.setPrefWidth(42);
        barra.setMinWidth(42);
        barra.setMaxWidth(42);
        StackPane papel = new StackPane(folha, barra);
        StackPane.setAlignment(barra, Pos.TOP_LEFT);
        barra.setMaxHeight(Double.MAX_VALUE);
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
        folha.getChildren().setAll(cabecalho(), regra(), seccaoCliente(), regra(), seccaoServicos(),
                regra(), seccaoTotais(), seccaoObservacoes(), lblRodape);
        lblRodape.getStyleClass().add("folha-rodape");
        atualizarTotais();
    }

    private void reconstruir() {
        double posicao = rolagem.getVvalue();
        setTop(barraFerramentas());
        construir();
        Platform.runLater(() -> rolagem.setVvalue(posicao));
    }

    private void gravarPendentes() {
        for (Runnable r : new ArrayList<>(pendentes)) {
            r.run();
        }
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
                ? "Clique em qualquer texto do orçamento para o editar."
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
            if (Ui.confirmar(janela(), "Começar um novo orçamento? Este fica guardado na lista.")) {
                OrcamentosView.dialogoNovo(ctx, janela(), abrir);
            }
        });

        Button bPdf = new Button("Guardar PDF");
        bPdf.getStyleClass().addAll("tb-botao", "tb-primario");
        bPdf.setOnAction(e -> {
            gravarPendentes();
            Ui.guardarPdf(ctx, orc, janela());
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

    private GridPane cabecalho() {
        Empresa emp = ctx.repo.getEmpresa();

        ImageView logo = Ui.logotipo(70);

        VBox dados = new VBox(1);
        Label nome = new Label(emp.getDesignacao());
        nome.getStyleClass().add("folha-empresa-nome");
        dados.getChildren().add(nome);
        String[] linhasEmpresa = {
                "NIF " + emp.getNif(), emp.getMorada(), "Tel. " + emp.getTelefone(), emp.getEmail()};
        for (String s : linhasEmpresa) {
            if (s != null && !s.isBlank() && !s.endsWith(" null")) {
                Label l = new Label(s);
                l.getStyleClass().add("folha-pequeno");
                dados.getChildren().add(l);
            }
        }
        if (emp.getWebsite() != null && !emp.getWebsite().isBlank()) {
            Label web = new Label(emp.getWebsite());
            web.getStyleClass().addAll("folha-pequeno", "folha-web");
            dados.getChildren().add(web);
        }

        Label titulo = new Label("ORÇAMENTO");
        titulo.getStyleClass().add("folha-titulo");
        GridPane meta = new GridPane();
        meta.setHgap(10);
        meta.setVgap(2);
        meta.add(rotuloFolha("N.º"), 0, 0);
        meta.add(valorFolha(orc.getNumero()), 1, 0);
        meta.add(rotuloFolha("Data"), 0, 1);
        meta.add(valorFolha(orc.getData().format(DATA)), 1, 1);
        VBox direita = new VBox(6, titulo, meta);
        direita.setAlignment(Pos.TOP_RIGHT);
        meta.setAlignment(Pos.TOP_RIGHT);

        GridPane g = new GridPane();
        g.setHgap(18);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHalignment(javafx.geometry.HPos.RIGHT);
        g.getColumnConstraints().addAll(c0, c1, c2);
        g.add(logo, 0, 0);
        g.add(dados, 1, 0);
        g.add(direita, 2, 0);
        GridPane.setValignment(logo, javafx.geometry.VPos.TOP);
        return g;
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
                ctx.clientes.guardarAlteracoes();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
                ref[0].setText(atual.get());
            }
        });
        return ref[0];
    }

    // ================================================================== cliente

    private VBox seccaoCliente() {
        Cliente c = orc.getCliente();

        ComboBox<Cliente> troca = Ui.comboClientes(ctx.clientes.listar());
        troca.setPrefWidth(250);
        troca.setPromptText("Trocar cliente…");
        troca.getStyleClass().add("folha-troca");
        troca.setDisable(!editavel());
        troca.setOnAction(e -> {
            Cliente escolhido = troca.getValue();
            if (escolhido == null || escolhido == orc.getCliente()) {
                return;
            }
            try {
                gravarPendentes();
                ctx.orcamentos.alterarCliente(orc, escolhido);
                reconstruir();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        });
        Button novo = Ui.link("+ Novo cliente");
        novo.setDisable(!editavel());
        novo.setOnAction(e -> ClienteDialog.mostrar(ctx, janela(), null).ifPresent(cli -> {
            try {
                gravarPendentes();
                ctx.orcamentos.alterarCliente(orc, cli);
                reconstruir();
            } catch (RuntimeException ex) {
                Ui.erro(janela(), Ui.mensagem(ex));
            }
        }));
        HBox cab = new HBox(12, h2("Cliente"), Ui.espaco(), troca, novo);
        cab.setAlignment(Pos.CENTER_LEFT);

        TextField nome = campoCliente(c::getNome, "Nome do cliente", c::setNome);
        nome.getStyleClass().add("folha-cliente-nome");

        GridPane g = new GridPane();
        g.setHgap(28);
        g.setVgap(4);
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(50);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(50);
        g.getColumnConstraints().addAll(a, b);
        g.add(nome, 0, 0, 2, 1);
        g.add(linhaCampo("NIF", campoCliente(c::getNif, "—", v -> c.setNif(ctx.clientes.validarNif(c, v)))), 0, 1);
        g.add(linhaCampo("Morada", campoCliente(c::getMorada, "Rua, n.º, código postal e localidade",
                c::setMorada)), 0, 2);
        g.add(linhaCampo("Telefone", campoCliente(c::getTelefone, "—", c::setTelefone)), 1, 1);
        g.add(linhaCampo("E-mail", campoCliente(c::getEmail, "—", c::setEmail)), 1, 2);
        int linha = 3;
        if (c.getContacto() != null) {
            Label ac = new Label(c.getContacto());
            ac.getStyleClass().add("folha-texto");
            g.add(linhaCampo("A/C", ac), 0, linha++);
        }
        TextField assunto = campo(orc.getAssunto(), "Assunto do orçamento", v -> {
            if (!v.equals(orc.getAssunto() == null ? "" : orc.getAssunto())) {
                try {
                    ctx.orcamentos.definirAssunto(orc, v);
                } catch (RuntimeException ex) {
                    Ui.erro(janela(), Ui.mensagem(ex));
                }
            }
        });
        g.add(linhaCampo("Assunto", assunto), 0, linha, 2, 1);

        return new VBox(10, cab, g);
    }

    private static HBox linhaCampo(String rotulo, Node campo) {
        Label l = new Label(rotulo);
        l.getStyleClass().add("folha-rotulo");
        l.setMinWidth(64);
        l.setPrefWidth(64);
        HBox h = new HBox(l, campo);
        h.setAlignment(Pos.CENTER_LEFT);
        if (campo instanceof Region) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return h;
    }

    // ================================================================== serviços

    private VBox seccaoServicos() {
        VBox v = new VBox(8, h2("Serviços"), cabecalhoTabela(), blocos);

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
            if (!orc.temCapitulos()) {
                Button add = new Button("+ Adicionar linha");
                add.getStyleClass().add("folha-adicionar");
                add.setOnAction(e -> novaLinha(null));
                botoes.getChildren().add(add);
            }
            botoes.getChildren().add(cap);
            VBox.setMargin(botoes, new Insets(2, 0, 0, 0));
            v.getChildren().add(botoes);
        }
        return v;
    }

    private GridPane cabecalhoTabela() {
        GridPane g = grelhaLinha();
        String[] t = {"Descrição", "Qtd.", "Un.", "Preço un.", "Total", ""};
        for (int i = 0; i < t.length; i++) {
            Label l = new Label(t[i]);
            l.getStyleClass().add("folha-th");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(i == 0 || i == 2 ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            g.add(l, i, 0);
        }
        g.getStyleClass().add("folha-th-linha");
        return g;
    }

    private static GridPane grelhaLinha() {
        GridPane g = new GridPane();
        g.setHgap(4);
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
        d.setContentText("Título (ex.: Construção civil):");
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
                            reconstruir();
                        } catch (RuntimeException ex) {
                            Ui.erro(janela(), Ui.mensagem(ex));
                        }
                    }
                });
                HBox cab = new HBox(6, num, titulo, x);
                cab.setAlignment(Pos.CENTER_LEFT);
                cab.setPadding(new Insets(10, 0, 2, 0));
                no.getChildren().add(cab);
            }
            no.getChildren().add(corpo);
            if (cap != null) {
                for (LinhaOrcamento l : cap.getLinhas()) {
                    acrescentarLinha(l);
                }
            }
            if (corpo.getChildren().isEmpty()) {
                acrescentarLinha(null, false);
            }
            if (editavel() && cap != null && cap.temTitulo()) {
                Button add = new Button("+ Adicionar linha");
                add.getStyleClass().add("folha-adicionar");
                add.setOnAction(e -> acrescentarLinha(null));
                VBox.setMargin(add, new Insets(8, 0, 0, 0));
                no.getChildren().add(add);
            }
        }

        void acrescentarLinha(LinhaOrcamento modelo) {
            acrescentarLinha(modelo, modelo == null);
        }

        void acrescentarLinha(LinhaOrcamento modelo, boolean foco) {
            Linha l = new Linha(this, modelo);
            linhas.add(l);
            corpo.getChildren().add(l.no);
            if (foco) {
                Platform.runLater(() -> l.desc.requestFocus());
            }
        }

        void removida(Linha l) {
            corpo.getChildren().remove(l.no);
            linhas.remove(l);
            if (corpo.getChildren().isEmpty()) { // nunca fica sem linhas: aparece uma em branco
                acrescentarLinha(null, false); // aparece uma linha em branco
            }
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
        final TextArea desc = new TextArea();
        final TextField qtd = new TextField();
        final ComboBox<UnidadeMedida> un = new ComboBox<>(FXCollections.observableArrayList(UnidadeMedida.values()));
        final TextField preco = new TextField();
        final Label total = new Label();

        Linha(Bloco bloco, LinhaOrcamento modelo) {
            this.bloco = bloco;
            this.modelo = modelo;
            no.getStyleClass().add("folha-linha");

            Region ponto = new Region();
            ponto.getStyleClass().add("folha-ponto");
            ponto.setMinSize(7, 7);
            ponto.setMaxSize(7, 7);
            desc.getStyleClass().add("folha-campo");
            desc.getStyleClass().add("folha-desc");
            desc.setWrapText(true);
            desc.setPromptText("Descrição do trabalho ou material");
            desc.setFont(Font.font("Montserrat", 13.5));
            desc.setText(modelo == null ? "" : modelo.getDescricao());
            ajustarAltura();
            desc.textProperty().addListener((o, a, b) -> ajustarAltura());
            HBox celulaDesc = new HBox(8, ponto, desc);
            celulaDesc.setAlignment(Pos.TOP_LEFT);
            HBox.setMargin(ponto, new Insets(11, 0, 0, 0));
            HBox.setHgrow(desc, Priority.ALWAYS);

            qtd.getStyleClass().addAll("folha-campo", "folha-direita");
            qtd.setPromptText("1");
            qtd.setText(modelo == null ? "" : Ui.semZeros(modelo.getQuantidade()).replace('.', ','));
            preco.getStyleClass().addAll("folha-campo", "folha-direita");
            preco.setPromptText("0,00 €");
            preco.setText(modelo == null ? "" : Dinheiro.formatar(modelo.getPrecoUnitario()));
            un.getStyleClass().add("folha-un");
            un.setValue(modelo == null ? UnidadeMedida.UNIDADE : modelo.getUnidade());
            total.getStyleClass().add("folha-total-linha");
            total.setMaxWidth(Double.MAX_VALUE);
            total.setAlignment(Pos.TOP_RIGHT);
            total.setText(modelo == null ? "" : Dinheiro.formatar(modelo.getTotal()));

            Button x = botaoRemover("Remover linha");
            x.setOnAction(e -> remover());
            GridPane.setValignment(x, javafx.geometry.VPos.TOP);

            no.add(celulaDesc, 0, 0);
            no.add(qtd, 1, 0);
            no.add(un, 2, 0);
            no.add(preco, 3, 0);
            no.add(total, 4, 0);
            no.add(x, 5, 0);
            GridPane.setValignment(total, javafx.geometry.VPos.TOP);
            GridPane.setValignment(qtd, javafx.geometry.VPos.TOP);
            GridPane.setValignment(un, javafx.geometry.VPos.TOP);
            GridPane.setValignment(preco, javafx.geometry.VPos.TOP);

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

    private void atualizarTotais() {
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
            medida.setWrappingWidth(794 - 113 - 60 - 14);
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
