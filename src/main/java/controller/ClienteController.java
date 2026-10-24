package controller;

import domain.Cliente;
import domain.ClienteEmpresa;
import domain.ClienteParticular;
import repository.Repositorio;

import java.util.List;
import java.util.stream.Collectors;

/** US04: gestão de clientes. */
public class ClienteController {

    private final Repositorio repositorio;
    private final AutenticacaoController auth;

    public ClienteController(Repositorio repositorio, AutenticacaoController auth) {
        this.repositorio = repositorio;
        this.auth = auth;
    }

    public ClienteParticular registarParticular(String nome, String nif, String morada,
                                                String telefone, String email) {
        auth.exigirSessao();
        ClienteParticular c = new ClienteParticular(nome, nif, morada, telefone, email);
        repositorio.adicionarCliente(c);
        repositorio.guardar();
        return c;
    }

    public ClienteEmpresa registarEmpresa(String designacao, String nif, String morada,
                                          String telefone, String email, String contacto) {
        auth.exigirSessao();
        ClienteEmpresa c = new ClienteEmpresa(designacao, nif, morada, telefone, email, contacto);
        repositorio.adicionarCliente(c);
        repositorio.guardar();
        return c;
    }

    /** Regista um cliente que foi preenchido diretamente no orçamento (quando fica completo). */
    public void registarSeCompleto(Cliente c) {
        auth.exigirSessao();
        if (c.isCompleto() && !repositorio.getClientes().contains(c)) {
            repositorio.adicionarCliente(c);
            repositorio.guardar();
        }
    }

    public void eliminar(Cliente c) {
        auth.exigirSessao();
        repositorio.removerCliente(c);
        repositorio.guardar();
    }

    /** Valida um NIF novo para um cliente existente (formato e duplicados). */
    public String validarNif(Cliente c, String nif) {
        auth.exigirSessao();
        String v = domain.Validacao.nif(nif);
        repositorio.procurarClientePorNif(v).ifPresent(outro -> {
            if (outro != c) {
                throw new IllegalArgumentException("Já existe outro cliente com esse NIF.");
            }
        });
        return v;
    }

    /** Grava alterações feitas a um cliente já registado. */
    public void guardarAlteracoes() {
        auth.exigirSessao();
        repositorio.guardar();
    }

    /** Pesquisa por nome ou NIF (parte do texto, sem distinguir maiúsculas). */
    public List<Cliente> pesquisar(String texto) {
        auth.exigirSessao();
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return repositorio.getClientes().stream()
                .filter(c -> c.getNome().toLowerCase().contains(t) || c.getNif().contains(t))
                .collect(Collectors.toList());
    }

    public List<Cliente> listar() {
        auth.exigirSessao();
        return repositorio.getClientes();
    }
}
