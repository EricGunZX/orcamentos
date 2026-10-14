package view;

import domain.Orcamento;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Janela principal: barra azul no topo (menu «hambúrguer», marca ao centro) e menu lateral
 * que se abre e fecha, com Orçamentos, Clientes e Empresa.
 */
public class JanelaPrincipal extends BorderPane {

    private final Contexto ctx;
    private final StackPane centro = new StackPane();
    private final VBox menu = new VBox(4);
    private final ToggleButton navOrcamentos;
    private final ToggleButton navClientes;
    private final ToggleButton navEmpresa;

    public JanelaPrincipal(Contexto ctx) {
        this.ctx = ctx;

        ToggleGroup grupo = new ToggleGroup();
        grupo.selectedToggleProperty().addListener((obs, antes, agora) -> {
            if (agora == null && antes != null) {
                antes.setSelected(true); // há sempre um item ativo
            }
        });
        navOrcamentos = nav("Orçamentos", grupo);
        navClientes = nav("Clientes", grupo);
        navEmpresa = nav("Empresa", grupo);
        navOrcamentos.setOnAction(e -> mostrarOrcamentos());
        navClientes.setOnAction(e -> mostrar(new ClientesView(ctx)));
        navEmpresa.setOnAction(e -> mostrar(new AdministracaoView(ctx)));

        Label seccao = new Label("MENU");
        seccao.getStyleClass().add("menu-seccao");
        menu.getChildren().addAll(seccao, navOrcamentos, navClientes, navEmpresa);
        menu.getStyleClass().add("menu-lateral");
        menu.setPrefWidth(230);
        menu.setMinWidth(230);

        // botão «hambúrguer»: três linhas
        VBox linhas = new VBox(4);
        for (int i = 0; i < 3; i++) {
            Region l = new Region();
            l.getStyleClass().add("hamburguer-linha");
            l.setPrefSize(20, 2.5);
            l.setMaxSize(20, 2.5);
            linhas.getChildren().add(l);
        }
        Button hamburguer = new Button();
        hamburguer.setGraphic(linhas);
        hamburguer.getStyleClass().add("barra-icone");
        hamburguer.setOnAction(e -> alternarMenu());

        Label marca = new Label("Números Profícuos");
        marca.getStyleClass().add("barra-marca");
        HBox marcaBox = new HBox(10, Ui.logotipoBranco(30), marca);
        marcaBox.setAlignment(Pos.CENTER);
        Label empresa = new Label(ctx.repo.getEmpresa().getEmail() == null ? "" : ctx.repo.getEmpresa().getEmail());
        empresa.getStyleClass().add("barra-utilizador");

        StackPane barra = new StackPane();
        HBox esquerda = new HBox(hamburguer);
        esquerda.setAlignment(Pos.CENTER_LEFT);
        HBox direita = new HBox(empresa);
        direita.setAlignment(Pos.CENTER_RIGHT);
        barra.getChildren().addAll(marcaBox, esquerda, direita);
        StackPane.setAlignment(marcaBox, Pos.CENTER);
        StackPane.setAlignment(esquerda, Pos.CENTER_LEFT);
        StackPane.setAlignment(direita, Pos.CENTER_RIGHT);
        barra.getStyleClass().add("barra");

        setTop(barra);
        setLeft(menu);
        setCenter(centro);

        navOrcamentos.setSelected(true);
        mostrarOrcamentos();
    }

    private ToggleButton nav(String texto, ToggleGroup grupo) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("menu-item-nav");
        b.setToggleGroup(grupo);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private void alternarMenu() {
        boolean visivel = !menu.isVisible();
        menu.setVisible(visivel);
        menu.setManaged(visivel);
    }

    private void mostrar(Node no) {
        StackPane.setAlignment(no, Pos.TOP_CENTER);
        centro.getChildren().setAll(no);
    }

    private void mostrarOrcamentos() {
        navOrcamentos.setSelected(true);
        mostrar(new OrcamentosView(ctx, this::abrirOrcamento));
    }

    private void abrirOrcamento(Orcamento orcamento) {
        mostrar(new OrcamentoView(ctx, orcamento, this::mostrarOrcamentos, this::abrirOrcamento));
    }
}
