package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.Endereco;
import br.com.itau.geradornotafiscal.model.Finalidade;
import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import br.com.itau.geradornotafiscal.service.frete.CalculadorFrete;
import br.com.itau.geradornotafiscal.service.tributacao.faixa.CalculadorTributacaoPorFaixa;
import br.com.itau.geradornotafiscal.service.tributacao.faixa.FaixaAliquota;
import br.com.itau.geradornotafiscal.service.tributacao.tabela.CatalogoTributario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GeradorNotaFiscalServiceImpl implements GeradorNotaFiscalService{
	private final CatalogoTributario catalogoTributario;
	private final CalculadorTributacaoPorFaixa calculadorTributacao;
	private final CalculadorFrete calculadorFrete;
	private final EstoqueService estoqueService;
	private final RegistroService registroService;
	private final EntregaService entregaService;
	private final FinanceiroService financeiroService;

	public GeradorNotaFiscalServiceImpl(
			CatalogoTributario catalogoTributario,
			CalculadorTributacaoPorFaixa calculadorTributacao,
			CalculadorFrete calculadorFrete,
			EstoqueService estoqueService,
			RegistroService registroService,
			EntregaService entregaService,
			FinanceiroService financeiroService) {
		this.catalogoTributario = catalogoTributario;
		this.calculadorTributacao = calculadorTributacao;
		this.calculadorFrete = calculadorFrete;
		this.estoqueService = estoqueService;
		this.registroService = registroService;
		this.entregaService = entregaService;
		this.financeiroService = financeiroService;
	}

	@Override
	public NotaFiscal gerarNotaFiscal(Pedido pedido) {

		Destinatario destinatario = pedido.getDestinatario();
		double valorTotalItensCalculado = calcularValorTotalItens(pedido.getItens());
		List<FaixaAliquota> faixas = catalogoTributario.buscar(
				destinatario.getTipoPessoa(),
				destinatario.getRegimeTributacao());
		List<ItemNotaFiscal> itemNotaFiscalList = calculadorTributacao.calcular(
				pedido.getItens(),
				valorTotalItensCalculado,
				faixas);


		Regiao regiao = destinatario.getEnderecos().stream()
				.filter(endereco -> endereco.getFinalidade() == Finalidade.ENTREGA || endereco.getFinalidade() == Finalidade.COBRANCA_ENTREGA)
				.map(Endereco::getRegiao)
				.findFirst()
				.orElse(null);

		double valorFrete = pedido.getValorFrete();
		double valorFreteComPercentual = calculadorFrete.calcular(valorFrete, regiao);

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
