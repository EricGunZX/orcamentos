package view;

import domain.Orcamento;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Janela principal: barra superior com navegação e área central com o ecrã escolhido. */
public class JanelaPrincipal extends BorderPane {

    private final Contexto ctx;
    private final StackPane centro = new StackPane();
    private final ToggleButton navOrcamentos;
    private final ToggleButton navClientes;
    private final ToggleButton navAdministracao;

    public JanelaPrincipal(Contexto ctx) {
        this.ctx = ctx;

        ToggleGroup grupo = new ToggleGroup();
        grupo.selectedToggleProperty().addListener((obs, antes, agora) -> {
            if (agora == null && antes != null) {
                antes.setSelected(true); // há sempre um separador ativo
            }
        });

        navOrcamentos = nav("Orçamentos", grupo);
        navClientes = nav("Clientes", grupo);
        navOrcamentos.setOnAction(e -> mostrarOrcamentos());
        navClientes.setOnAction(e -> mostrar(new ClientesView(ctx)));

        Label marca = new Label("Números Profícuos");
        marca.getStyleClass().add("barra-marca");

        HBox barra = new HBox(marca, navOrcamentos, navClientes);
        barra.getStyleClass().add("barra");

        navAdministracao = nav("Empresa", grupo);
        navAdministracao.setOnAction(e -> mostrar(new AdministracaoView(ctx)));
        barra.getChildren().add(navAdministracao);

        setTop(barra);
        setCenter(centro);

        navOrcamentos.setSelected(true);
        mostrarOrcamentos();
    }

    private ToggleButton nav(String texto, ToggleGroup grupo) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("nav-botao");
        b.setToggleGroup(grupo);
        return b;
    }

    private void mostrar(Node no) {
        centro.getChildren().setAll(no);
    }

    private void mostrarOrcamentos() {
        mostrar(new OrcamentosView(ctx, this::abrirOrcamento));
    }

    private void abrirOrcamento(Orcamento orcamento) {
        mostrar(new OrcamentoView(ctx, orcamento, this::mostrarOrcamentos, this::abrirOrcamento));
    }
}
