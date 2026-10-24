package repository;

import domain.Administrador;
import domain.Cliente;
import domain.Empresa;
import domain.Orcamento;
import domain.Utilizador;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Guarda e carrega os dados por serialização de objetos (persistência). */
public class Repositorio {

    private final Path ficheiro;
    private Dados dados;

    public Repositorio(Path ficheiro) {
        this.ficheiro = ficheiro;
        this.dados = carregar();
    }

    private Dados carregar() {
        if (!Files.exists(ficheiro)) {
            return new Dados();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ficheiro))) {
            return (Dados) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Não foi possível ler " + ficheiro + ": " + e.getMessage(), e);
        }
    }

    /** Grava todos os dados; escreve primeiro num temporário para não corromper o ficheiro. */
    public void guardar() {
        Path temp = ficheiro.resolveSibling(ficheiro.getFileName() + ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(dados);
            }
            Files.move(temp, ficheiro, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível guardar os dados.", e);
        }
    }

    // ---------- Empresa ----------

    public Empresa getEmpresa() {
        return dados.empresa;
    }

    // ---------- Utilizadores ----------

    public List<Utilizador> getUtilizadores() {
        return Collections.unmodifiableList(dados.utilizadores);
    }

    public Optional<Utilizador> procurarUtilizador(String nomeUtilizador) {
        return dados.utilizadores.stream()
                .filter(u -> u.getNomeUtilizador().equalsIgnoreCase(nomeUtilizador))
                .findFirst();
    }

    public void adicionarUtilizador(Utilizador u) {
        if (procurarUtilizador(u.getNomeUtilizador()).isPresent()) {
            throw new IllegalArgumentException("O nome de utilizador já existe.");
        }
        dados.utilizadores.add(u);
    }

    public long contarAdministradoresAtivos() {
        return dados.utilizadores.stream()
                .filter(u -> u instanceof Administrador && u.isAtivo()).count();
    }

    // ---------- Clientes ----------

    public List<Cliente> getClientes() {
        return Collections.unmodifiableList(dados.clientes);
    }

    public Optional<Cliente> procurarClientePorNif(String nif) {
        return dados.clientes.stream().filter(c -> c.getNif().equals(nif)).findFirst();
    }

    public void adicionarCliente(Cliente c) {
        if (procurarClientePorNif(c.getNif()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cliente com esse NIF.");
        }
        c.setCodigo(String.format("C%04d", ++dados.ultimoCodigoCliente));
        dados.clientes.add(c);
    }

    // ---------- Orçamentos ----------

    public List<Orcamento> getOrcamentos() {
        return Collections.unmodifiableList(dados.orcamentos);
    }

    public Optional<Orcamento> procurarOrcamento(String numero) {
        return dados.orcamentos.stream().filter(o -> o.getNumero().equals(numero)).findFirst();
    }

    /** Gera o próximo número sequencial do ano, no formato 2026/014. */
    public String proximoNumeroOrcamento() {
        int ano = Year.now().getValue();
        int seq = dados.sequenciaPorAno.merge(ano, 1, Integer::sum);
        return String.format("%d/%03d", ano, seq);
    }

    public void adicionarOrcamento(Orcamento o) {
        dados.orcamentos.add(o);
    }

    /** Elimina um rascunho; se for o último número do ano, o número volta a ficar livre (sem buracos). */
    public void removerOrcamento(Orcamento o) {
        if (!o.podeEliminar()) {
            throw new IllegalStateException("Só os rascunhos podem ser eliminados. Use «Anular».");
        }
        dados.orcamentos.remove(o);
        String[] p = o.getNumero().split("/");
        try {
            int ano = Integer.parseInt(p[0]);
            int seq = Integer.parseInt(p[1]);
            Integer atual = dados.sequenciaPorAno.get(ano);
            if (atual != null && atual == seq) {
                dados.sequenciaPorAno.put(ano, seq - 1);
            }
        } catch (RuntimeException ex) {
            // número fora do formato: não mexe na sequência
        }
    }

    public void removerCliente(Cliente c) {
        if (!orcamentosDoCliente(c).isEmpty()) {
            throw new IllegalStateException("Este cliente tem orçamentos e não pode ser eliminado.");
        }
        dados.clientes.remove(c);
    }

    public List<Orcamento> orcamentosDoCliente(Cliente c) {
        List<Orcamento> r = new ArrayList<>();
        for (Orcamento o : dados.orcamentos) {
            if (o.getCliente().getNif().equals(c.getNif())) {
                r.add(o);
            }
        }
        return r;
    }
}
