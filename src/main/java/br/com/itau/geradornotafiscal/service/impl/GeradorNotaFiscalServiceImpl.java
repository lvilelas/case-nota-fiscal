package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.*;
import br.com.itau.geradornotafiscal.service.CalculadoraAliquotaProduto;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GeradorNotaFiscalServiceImpl implements GeradorNotaFiscalService{
	private final CalculadoraAliquotaProduto calculadoraAliquotaProduto;
	private final EstoqueService estoqueService;
	private final RegistroService registroService;
	private final EntregaService entregaService;
	private final FinanceiroService financeiroService;

	public GeradorNotaFiscalServiceImpl(
			CalculadoraAliquotaProduto calculadoraAliquotaProduto,
			EstoqueService estoqueService,
			RegistroService registroService,
			EntregaService entregaService,
			FinanceiroService financeiroService) {
		this.calculadoraAliquotaProduto = calculadoraAliquotaProduto;
		this.estoqueService = estoqueService;
		this.registroService = registroService;
		this.entregaService = entregaService;
		this.financeiroService = financeiroService;
	}

	@Override
	public NotaFiscal gerarNotaFiscal(Pedido pedido) {

		Destinatario destinatario = pedido.getDestinatario();
		TipoPessoa tipoPessoa = destinatario.getTipoPessoa();
		List<ItemNotaFiscal> itemNotaFiscalList = new ArrayList<>();
		double valorTotalItensCalculado = calcularValorTotalItens(pedido.getItens());

		if (tipoPessoa == TipoPessoa.FISICA) {
			double aliquota;

			if (valorTotalItensCalculado < 500) {
				aliquota = 0;
			} else if (valorTotalItensCalculado <= 2000) {
				aliquota = 0.12;
			} else if (valorTotalItensCalculado <= 3500) {
				aliquota = 0.15;
			} else {
				aliquota = 0.17;
			}
			itemNotaFiscalList = calculadoraAliquotaProduto.calcularAliquota(pedido.getItens(), aliquota);
		} else if (tipoPessoa == TipoPessoa.JURIDICA) {

			RegimeTributacaoPJ regimeTributacao = destinatario.getRegimeTributacao();

			if (regimeTributacao == RegimeTributacaoPJ.SIMPLES_NACIONAL) {
				double aliquota;

				if (valorTotalItensCalculado < 1000) {
					aliquota = 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					aliquota = 0.07;
				} else if (valorTotalItensCalculado <= 5000) {
					aliquota = 0.13;
				} else {
					aliquota = 0.19;
				}
				itemNotaFiscalList = calculadoraAliquotaProduto.calcularAliquota(pedido.getItens(), aliquota);
			} else if (regimeTributacao == RegimeTributacaoPJ.LUCRO_REAL) {
				double aliquota;

				if (valorTotalItensCalculado < 1000) {
					aliquota = 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					aliquota = 0.09;
				} else if (valorTotalItensCalculado <= 5000) {
					aliquota = 0.15;
				} else {
					aliquota = 0.20;
				}
				itemNotaFiscalList= calculadoraAliquotaProduto.calcularAliquota(pedido.getItens(),aliquota);
			} else if (regimeTributacao == RegimeTributacaoPJ.LUCRO_PRESUMIDO) {
				double aliquota;

				if (valorTotalItensCalculado < 1000) {
					aliquota = 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					aliquota = 0.09;
				} else if (valorTotalItensCalculado <= 5000) {
					aliquota = 0.16;
				} else {
					aliquota = 0.20;
				}
				itemNotaFiscalList = calculadoraAliquotaProduto.calcularAliquota(pedido.getItens(),aliquota);
			}
		}
		//Regras diferentes para frete

		Regiao regiao = destinatario.getEnderecos().stream()
				.filter(endereco -> endereco.getFinalidade() == Finalidade.ENTREGA || endereco.getFinalidade() == Finalidade.COBRANCA_ENTREGA)
				.map(Endereco::getRegiao)
				.findFirst()
				.orElse(null);

		double valorFrete = pedido.getValorFrete();
		double valorFreteComPercentual =0;

		if (regiao == Regiao.NORTE) {
			valorFreteComPercentual = valorFrete * 1.08;
		} else if (regiao == Regiao.NORDESTE) {
			valorFreteComPercentual = valorFrete * 1.085;
		} else if (regiao == Regiao.CENTRO_OESTE) {
			valorFreteComPercentual = valorFrete * 1.07;
		} else if (regiao == Regiao.SUDESTE) {
			valorFreteComPercentual = valorFrete * 1.048;
		} else if (regiao == Regiao.SUL) {
			valorFreteComPercentual = valorFrete * 1.06;
		}

		// Create the NotaFiscal object
		String idNotaFiscal = UUID.randomUUID().toString();

		NotaFiscal notaFiscal = NotaFiscal.builder()
				.idNotaFiscal(idNotaFiscal)
				.data(LocalDateTime.now())
				.valorTotalItens(valorTotalItensCalculado)
				.valorFrete(valorFreteComPercentual)
				.itens(itemNotaFiscalList)
				.destinatario(pedido.getDestinatario())
				.build();

		estoqueService.enviarNotaFiscalParaBaixaEstoque(notaFiscal);
		registroService.registrarNotaFiscal(notaFiscal);
		entregaService.agendarEntrega(notaFiscal);
		financeiroService.enviarNotaFiscalParaContasReceber(notaFiscal);

		return notaFiscal;
	}

	private double calcularValorTotalItens(List<Item> itens) {
		return itens.stream()
				.map(item -> BigDecimal.valueOf(item.getValorUnitario())
						.multiply(BigDecimal.valueOf(item.getQuantidade())))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.doubleValue();
	}
}
