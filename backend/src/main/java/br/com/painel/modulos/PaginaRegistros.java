package br.com.painel.modulos;

import java.util.List;

public record PaginaRegistros<T>(List<T> registros, long total, int pagina, int tamanho) {}
