package view;

import controller.AdministracaoController;
import controller.AutenticacaoController;
import controller.ClienteController;
import controller.OrcamentoController;
import javafx.application.HostServices;
import repository.Repositorio;

/** Reúne o repositório e os controladores partilhados por todas as vistas. */
public final class Contexto {
    public final Repositorio repo;
    public final AutenticacaoController auth;
    public final AdministracaoController admin;
    public final ClienteController clientes;
    public final OrcamentoController orcamentos;
    public final HostServices host;

    public Contexto(Repositorio repo, HostServices host) {
        this.repo = repo;
        this.host = host;
        this.auth = new AutenticacaoController(repo);
        this.admin = new AdministracaoController(repo, auth);
        this.clientes = new ClienteController(repo, auth);
        this.orcamentos = new OrcamentoController(repo, auth);
    }
}
