package view;

import domain.Cliente;
import domain.Dinheiro;
import domain.EstadoOrcamento;
import domain.Orcamento;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Lista e pesquisa de orçamentos (US09), criação (US05) e duplicação (US10). */
public class OrcamentosView extends VBox {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Contexto ctx;
    private final Consumer<Orcamento> abrir;
    private final TableView<Orcamento> tabela = new TableView<>();
    private final TextField fNumero = new TextField();
    private final TextField fCliente = new TextField();
    private final ComboBox<EstadoOrcamento> fEstado = new ComboBox<>();
    private final DatePicker fDe = new DatePicker();
    private final DatePicker fAte = new DatePicker();
    private final Label lblTotalOrc = new Label();
    private final Label lblRascunhos = new Label();
    private final Label lblAdjudicados = new Label();
    private final Label lblValor = new Label();

    private static VBox cartaoResumo(String titulo, Label valor, boolean azul) {
        Label t = new Label(titulo);
        t.getStyleClass().add("stat-titulo");
        valor.getStyleClass().add("stat-valor");
        VBox v = new VBox(2, t, valor);
        v.getStyleClass().add("stat");
        if (azul) {
            v.getStyleClass().add("stat-azul");
        }
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private void atualizarResumo() {
        List<Orcamento> todos = ctx.orcamentos.listar();
        lblTotalOrc.setText(String.valueOf(todos.size()));
        lblRascunhos.setText(String.valueOf(todos.stream()
                .filter(o -> o.getEstado() == EstadoOrcamento.RASCUNHO).count()));
        lblAdjudicados.setText(String.valueOf(todos.stream()
                .filter(o -> o.getEstado() == EstadoOrcamento.ADJUDICADO).count()));
        lblValor.setText(Dinheiro.formatar(todos.stream()
                .filter(o -> o.getEstado() == EstadoOrcamento.ADJUDICADO)
                .map(Orcamento::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)));
    }

    public OrcamentosView(Contexto ctx, Consumer<Orcamento> abrir) {
        this.ctx = ctx;
        this.abrir = abrir;
        getStyleClass().add("pagina");
        setMaxWidth(1280);

        Button novo = Ui.primario("Novo orçamento");
        novo.setOnAction(e -> novoOrcamento());
        HBox topo = new HBox(12, Ui.titulo("Orçamentos"), Ui.espaco(), novo);
        topo.setAlignment(Pos.CENTER_LEFT);

        // ---- resumo
        HBox resumo = new HBox(14, cartaoResumo("ORÇAMENTOS", lblTotalOrc, false),
                cartaoResumo("EM RASCUNHO", lblRascunhos, false),
                cartaoResumo("ADJUDICADOS", lblAdjudicados, false),
                cartaoResumo("VALOR ADJUDICADO", lblValor, true));

        // ---- filtros
        fNumero.setPromptText("Número");
        fNumero.setPrefWidth(120);
        fCliente.setPromptText("Cliente (nome ou NIF)");
        fCliente.setPrefWidth(220);
        fEstado.getItems().addAll(EstadoOrcamento.values());
        fEstado.setPromptText("Todos os estados");
        fEstado.setPrefWidth(170);
        Ui.formatoData(fDe);
        Ui.formatoData(fAte);
        fDe.setPromptText("Desde");
        fDe.setPrefWidth(160);
        fAte.setPromptText("Até");
        fAte.setPrefWidth(160);
        Button limpar = Ui.secundario("Limpar");
        limpar.setOnAction(e -> {
            fNumero.clear();
            fCliente.clear();
            fEstado.setValue(null);
            fDe.setValue(null);
            fAte.setValue(null);
        });
        FlowPane filtros = new FlowPane(10, 10, fNumero, fCliente, fEstado, fDe, fAte, limpar);
        filtros.getStyleClass().add("filtros");
        fNumero.textProperty().addListener((o, a, b) -> atualizar());
        fCliente.textProperty().addListener((o, a, b) -> atualizar());
        fEstado.valueProperty().addListener((o, a, b) -> atualizar());
        fDe.valueProperty().addListener((o, a, b) -> atualizar());
        fAte.valueProperty().addListener((o, a, b) -> atualizar());

        // ---- tabela
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Sem orçamentos. Use «Novo orçamento» para criar o primeiro."));
        tabela.getColumns().add(Ui.fixa(Ui.<Orcamento>coluna("Número", Orcamento::getNumero, 110), 110));
        tabela.getColumns().add(Ui.fixa(Ui.<Orcamento>coluna("Data", o -> o.getData().format(DATA), 110), 110));
        tabela.getColumns().add(Ui.<Orcamento>coluna("Cliente", o -> o.getCliente().getNome(), 240));
        tabela.getColumns().add(Ui.<Orcamento>coluna("Assunto", o -> o.getAssunto() == null ? "" : o.getAssunto(), 240));
        TableColumn<Orcamento, String> cTotal =
                Ui.<Orcamento>coluna("Total", o -> Dinheiro.formatar(o.getTotal()), 110);
        cTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        Ui.fixa(cTotal, 140);
        tabela.getColumns().add(cTotal);
        TableColumn<Orcamento, String> cEstado =
                Ui.<Orcamento>coluna("Estado", o -> o.getEstado().getDescricao(), 120);
        cEstado.setCellFactory(col -> new TableCell<Orcamento, String>() {
            @Override
            protected void updateItem(String texto, boolean vazio) {
                super.updateItem(texto, vazio);
                Orcamento o = getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex()) : null;
                if (vazio || o == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(texto);
                badge.getStyleClass().addAll("badge-celula", "estado-" + o.getEstado().name().toLowerCase());
                setGraphic(badge);
                setText(null);
            }
        });
        Ui.fixa(cEstado, 140);
        tabela.getColumns().add(cEstado);
        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirSelecionado();
            }
        });
        VBox.setVgrow(tabela, Priority.ALWAYS);

        // ---- ações
        Button bAbrir = Ui.secundario("Abrir");
        bAbrir.setOnAction(e -> abrirSelecionado());
        Button bDuplicar = Ui.secundario("Duplicar");
        bDuplicar.setOnAction(e -> duplicar());
        Button bPdf = Ui.secundario("Guardar PDF");
        bPdf.setOnAction(e -> {
            Orcamento o = tabela.getSelectionModel().getSelectedItem();
            if (o != null) {
                Ui.guardarPdf(ctx, o, janela());
            }
        });
        for (Button b : new Button[]{bAbrir, bDuplicar, bPdf}) {
            b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        HBox rodape = new HBox(10, bAbrir, bDuplicar, bPdf);

        getChildren().addAll(topo, resumo, filtros, tabela, rodape);
        atualizar();
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private static String textoOuNulo(TextField t) {
        String s = t.getText() == null ? "" : t.getText().trim();
        return s.isEmpty() ? null : s;
    }

    private void atualizar() {
        atualizarResumo();
        List<Orcamento> lista = new ArrayList<>(ctx.orcamentos.pesquisar(textoOuNulo(fNumero),
                textoOuNulo(fCliente), fDe.getValue(), fAte.getValue(), fEstado.getValue()));
        lista.sort(Comparator.comparing(Orcamento::getData).thenComparing(Orcamento::getNumero).reversed());
        tabela.getItems().setAll(lista);
    }

    private void abrirSelecionado() {
        Orcamento o = tabela.getSelectionModel().getSelectedItem();
        if (o != null) {
            abrir.accept(o);
        }
    }

    private void duplicar() {
        Orcamento o = tabela.getSelectionModel().getSelectedItem();
        if (o == null) {
            return;
        }
        try {
            Orcamento copia = ctx.orcamentos.duplicar(o);
            abrir.accept(copia);
        } catch (RuntimeException ex) {
            Ui.erro(janela(), Ui.mensagem(ex));
        }
    }

    /** US05: escolhe o cliente e o assunto e abre o novo rascunho. */
    private void novoOrcamento() {
        dialogoNovo(ctx, janela(), abrir);
    }

    /** Diálogo de novo orçamento; também usado pelo botão «Novo orçamento» do editor. */
    static void dialogoNovo(Contexto ctx, javafx.stage.Window janela, java.util.function.Consumer<Orcamento> abrir) {
        ComboBox<Cliente> cliente = new ComboBox<>(FXCollections.observableArrayList(ctx.clientes.listar()));
        cliente.setPromptText("Escolha o cliente");
        cliente.setPrefWidth(320);
        Button novoCliente = Ui.secundario("Novo cliente…");
        novoCliente.setOnAction(e -> ClienteDialog.mostrar(ctx, janela, null).ifPresent(c -> {
            cliente.getItems().add(c);
            cliente.setValue(c);
        }));
        TextField assunto = new TextField();
        assunto.setPromptText("Assunto (opcional)");

        javafx.scene.layout.GridPane g = Ui.grelha();
        Ui.linha(g, 0, "Cliente", new HBox(8, cliente, novoCliente));
        Ui.linha(g, 1, "Assunto", assunto);

        Dialog<ButtonType> d = Ui.dialogo(janela, "Novo orçamento", g, "Criar orçamento");
        Orcamento[] criado = new Orcamento[1];
        Ui.aoConfirmar(d, () -> {
            if (cliente.getValue() == null) {
                throw new IllegalArgumentException("Escolha um cliente ou registe um novo.");
            }
            criado[0] = ctx.orcamentos.criar(cliente.getValue(), assunto.getText().trim());
        });
        if (Ui.confirmado(d.showAndWait()) && criado[0] != null) {
            abrir.accept(criado[0]);
        }
    }
}
