package view;

import domain.Cliente;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Lista, pesquisa e registo de clientes (US04). */
public class ClientesView extends VBox {

    private final Contexto ctx;
    private final TableView<Cliente> tabela = new TableView<>();
    private final TextField pesquisa = new TextField();

    public ClientesView(Contexto ctx) {
        this.ctx = ctx;
        getStyleClass().add("pagina");
        setMaxWidth(1280);

        Button novo = Ui.primario("Novo cliente");
        novo.setOnAction(e -> ClienteDialog.mostrar(ctx, janela(), null).ifPresent(c -> atualizar()));
        HBox topo = new HBox(12, Ui.titulo("Clientes"), Ui.espaco(), novo);
        topo.setAlignment(Pos.CENTER_LEFT);

        pesquisa.setPromptText("Pesquisar por nome ou NIF");
        pesquisa.setPrefWidth(320);
        pesquisa.textProperty().addListener((o, a, b) -> atualizar());

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Ainda não há clientes registados."));
        tabela.getColumns().add(Ui.fixa(Ui.<Cliente>coluna("Código", c -> c.getCodigo() == null ? "" : c.getCodigo(), 90), 90));
        tabela.getColumns().add(Ui.fixa(Ui.<Cliente>coluna("Tipo", Cliente::getTipo, 110), 110));
        tabela.getColumns().add(Ui.<Cliente>coluna("Nome / Designação", Cliente::getNome, 260));
        tabela.getColumns().add(Ui.fixa(Ui.<Cliente>coluna("NIF", Cliente::getNif, 110), 110));
        tabela.getColumns().add(Ui.fixa(Ui.<Cliente>coluna("Telefone", c -> nulo(c.getTelefone()), 130), 130));
        tabela.getColumns().add(Ui.fixa(Ui.<Cliente>coluna("E-mail", c -> nulo(c.getEmail()), 230), 230));
        tabela.getColumns().add(Ui.<Cliente>coluna("Morada", Cliente::getMorada, 300));
        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editar();
            }
        });
        VBox.setVgrow(tabela, Priority.ALWAYS);

        Button editar = Ui.secundario("Editar selecionado");
        editar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        editar.setOnAction(e -> editar());
        HBox rodape = new HBox(10, editar);

        getChildren().addAll(topo, pesquisa, tabela, rodape);
        atualizar();
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }

    private javafx.stage.Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void editar() {
        Cliente c = tabela.getSelectionModel().getSelectedItem();
        if (c != null) {
            ClienteDialog.mostrar(ctx, janela(), c).ifPresent(x -> atualizar());
        }
    }

    private void atualizar() {
        tabela.getItems().setAll(ctx.clientes.pesquisar(pesquisa.getText()));
        tabela.refresh();
    }
}
