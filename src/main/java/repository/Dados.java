package repository;

import domain.Cliente;
import domain.Empresa;
import domain.Orcamento;
import domain.Utilizador;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Conjunto de todos os dados da aplicação; é o objeto que se serializa. */
public class Dados implements Serializable {
    private static final long serialVersionUID = 1L;

    Empresa empresa = Empresa.numerosProficuos();
    final List<Utilizador> utilizadores = new ArrayList<>();
    final List<Cliente> clientes = new ArrayList<>();
    final List<Orcamento> orcamentos = new ArrayList<>();
    /** Último número sequencial usado em cada ano. */
    final Map<Integer, Integer> sequenciaPorAno = new HashMap<>();
    int ultimoCodigoCliente = 0;
}
