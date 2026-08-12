package br.com.itau.geradornotafiscal.domain.model;

import java.time.LocalDate;
import java.util.List;

import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
	private int idPedido;

	private LocalDate data;

	private double valorTotalItens;

	private double valorFrete;

	private List<Item> itens;

	private Destinatario destinatario;

}
