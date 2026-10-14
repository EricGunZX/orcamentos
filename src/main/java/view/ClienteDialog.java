package view;

import domain.Cliente;
import domain.ClienteEmpresa;
import domain.Validacao;
import javafx.geometry.Insets;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.function.Supplier;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/** Diálogo para registar ou editar um cliente (US04). */
final class ClienteDialog {

    private ClienteDialog() {
    }

    /** Se {@code existente} for null, regista um novo cliente; caso contrário edita-o. */
    static Optional<Cliente> mostrar(Contexto ctx, Window dono, Cliente existente) {
        boolean novo = existente == null;

        ToggleGroup grupo = new ToggleGroup();
        RadioButton rbPart = new RadioButton("Particular");
        RadioButton rbEmp = new RadioButton("Empresa");
        rbPart.setToggleGroup(grupo);
        rbEmp.setToggleGroup(grupo);
        boolean eEmpresa = !novo && existente instanceof ClienteEmpresa;
        (eEmpresa ? rbEmp : rbPart).setSelected(true);
        rbPart.setDisable(!novo);
        rbEmp.setDisable(!novo);
        HBox tipoBox = new HBox(24, rbPart, rbEmp);
        tipoBox.setPadding(new Insets(4, 0, 0, 0));

        TextField nome = new TextField();
        TextField nif = new TextField();
        nif.setPromptText("9 dígitos");
        TextField morada = new TextField();
        morada.setPromptText("Rua, n.º, código postal e localidade");
        TextField telefone = new TextField();
        TextField email = new TextField();
        TextField contacto = new TextField();
        contacto.setPromptText("Pessoa de contacto");

        if (!novo) {
            nome.setText(existente.getNome());
            nif.setText(existente.getNif());
            morada.setText(existente.getMorada());
            telefone.setText(existente.getTelefone() == null ? "" : existente.getTelefone());
            email.setText(existente.getEmail() == null ? "" : existente.getEmail());
            if (existente instanceof ClienteEmpresa) {
                String c = ((ClienteEmpresa) existente).getContacto();
                contacto.setText(c == null ? "" : c);
            }
        }

        Label rotuloNome = new Label("Nome");
        rotuloNome.getStyleClass().add("form-rotulo");
        Label ast = new Label("*");
        ast.getStyleClass().add("form-asterisco");
        VBox campoNome = new VBox(5, new HBox(2, rotuloNome, ast), nome);
        VBox campoContacto = Ui.campoForm("Contacto (A/C)", false, contacto);

        GridPane g = Ui.gridForm();
        g.add(Ui.campoForm("Tipo de cliente", true, tipoBox), 0, 0, 2, 1);
        g.add(campoNome, 0, 1);
        g.add(Ui.campoForm("NIF", true, nif), 1, 1);
        g.add(Ui.campoForm("Telefone", false, telefone), 0, 2);
        g.add(Ui.campoForm("E-mail", false, email), 1, 2);
        g.add(campoContacto, 0, 3, 2, 1);
        g.add(Ui.campoForm("Morada", true, morada), 0, 4, 2, 1);

        VBox conteudo = new VBox(2, Ui.secaoForm("Informações básicas"), g);
        conteudo.setPrefWidth(620);

        Runnable ajustar = () -> {
            boolean empresa = rbEmp.isSelected();
            rotuloNome.setText(empresa ? "Designação social" : "Nome");
            campoContacto.setVisible(empresa);
            campoContacto.setManaged(empresa);
        };
        grupo.selectedToggleProperty().addListener((o, x, y) -> ajustar.run());
        ajustar.run();
        Supplier<String> tipoEscolhido = () -> rbEmp.isSelected() ? "Empresa" : "Particular";

        Dialog<ButtonType> d = Ui.dialogo(dono, novo ? "Adicionar cliente" : "Editar cliente", conteudo,
                novo ? "Registar" : "Guardar");
        Cliente[] resultado = new Cliente[1];

        Ui.aoConfirmar(d, () -> {
            String nifLimpo = Validacao.nif(nif.getText());
            Validacao.textoObrigatorio(nome.getText(), "Nome");
            Validacao.textoObrigatorio(morada.getText(), "Morada");
            if (ctx.repo.procurarClientePorNif(nifLimpo).filter(c -> c != existente).isPresent()) {
                throw new IllegalArgumentException("Já existe um cliente com esse NIF.");
            }
            if (novo) {
                if ("Empresa".equals(tipoEscolhido.get())) {
                    resultado[0] = ctx.clientes.registarEmpresa(nome.getText(), nifLimpo, morada.getText(),
                            telefone.getText(), email.getText(), contacto.getText());
                } else {
                    resultado[0] = ctx.clientes.registarParticular(nome.getText(), nifLimpo, morada.getText(),
                            telefone.getText(), email.getText());
                }
            } else {
                existente.setNome(nome.getText());
                existente.setNif(nifLimpo);
                existente.setMorada(morada.getText());
                existente.setTelefone(telefone.getText());
                existente.setEmail(email.getText());
                if (existente instanceof ClienteEmpresa) {
                    ((ClienteEmpresa) existente).setContacto(contacto.getText());
                }
                ctx.clientes.guardarAlteracoes();
                resultado[0] = existente;
            }
        });

        return Ui.confirmado(d.showAndWait()) ? Optional.ofNullable(resultado[0]) : Optional.empty();
    }
}
