package view;

import domain.Cliente;
import domain.ClienteEmpresa;
import domain.Validacao;
import javafx.scene.control.ChoiceBox;
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

        ChoiceBox<String> tipo = new ChoiceBox<>();
        tipo.getItems().addAll("Particular", "Empresa");
        tipo.setValue(novo || !(existente instanceof ClienteEmpresa) ? "Particular" : "Empresa");
        tipo.setDisable(!novo);

        Label rotuloNome = Ui.rotulo("Nome");
        TextField nome = new TextField();
        TextField nif = new TextField();
        nif.setPromptText("9 dígitos");
        TextField morada = new TextField();
        TextField telefone = new TextField();
        TextField email = new TextField();
        TextField contacto = new TextField();
        contacto.setPromptText("Pessoa de contacto");
        Label rotuloContacto = Ui.rotulo("A/C");

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

        GridPane g = Ui.grelha();
        Ui.linha(g, 0, "Tipo", tipo);
        g.add(rotuloNome, 0, 1);
        g.add(nome, 1, 1);
        Ui.linha(g, 2, "NIF", nif);
        Ui.linha(g, 3, "Morada", morada);
        Ui.linha(g, 4, "Telefone", telefone);
        Ui.linha(g, 5, "E-mail", email);
        g.add(rotuloContacto, 0, 6);
        g.add(contacto, 1, 6);

        Runnable ajustar = () -> {
            boolean empresa = "Empresa".equals(tipo.getValue());
            rotuloNome.setText(empresa ? "Designação social" : "Nome");
            rotuloContacto.setVisible(empresa);
            rotuloContacto.setManaged(empresa);
            contacto.setVisible(empresa);
            contacto.setManaged(empresa);
        };
        tipo.valueProperty().addListener((o, a, b) -> ajustar.run());
        ajustar.run();

        Dialog<ButtonType> d = Ui.dialogo(dono, novo ? "Novo cliente" : "Editar cliente", g,
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
                if ("Empresa".equals(tipo.getValue())) {
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
