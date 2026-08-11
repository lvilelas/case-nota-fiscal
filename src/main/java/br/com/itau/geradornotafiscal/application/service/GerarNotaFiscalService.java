package br.com.itau.geradornotafiscal.application.service;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.domain.model.Destinatario;
import br.com.itau.geradornotafiscal.domain.model.Endereco;
import br.com.itau.geradornotafiscal.domain.model.Finalidade;
import br.com.itau.geradornotafiscal.domain.model.Item;
import br.com.itau.geradornotafiscal.domain.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import br.com.itau.geradornotafiscal.domain.model.Regiao;
import br.com.itau.geradornotafiscal.domain.service.frete.CalculadorFrete;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CalculadorTributacaoPorFaixa;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CatalogoTributario;
import br.com.itau.geradornotafiscal.domain.service.tributacao.FaixaAliquota;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GerarNotaFiscalService implements GerarNotaFiscalUseCase {
	private final CatalogoTributario catalogoTributario;
	private final CalculadorTributacaoPorFaixa calculadorTributacao;
	private final CalculadorFrete calculadorFrete;
	private final BaixarEstoquePort baixarEstoquePort;
	private final RegistrarNotaFiscalPort registrarNotaFiscalPort;
	private final AgendarEntregaPort agendarEntregaPort;
	private final EnviarNotaFiscalFinanceiroPort enviarNotaFiscalFinanceiroPort;

	public GerarNotaFiscalService(
			CatalogoTributario catalogoTributario,
			CalculadorTributacaoPorFaixa calculadorTributacao,
			CalculadorFrete calculadorFrete,
			BaixarEstoquePort baixarEstoquePort,
			RegistrarNotaFiscalPort registrarNotaFiscalPort,
			AgendarEntregaPort agendarEntregaPort,
			EnviarNotaFiscalFinanceiroPort enviarNotaFiscalFinanceiroPort) {
		this.catalogoTributario = catalogoTributario;
		this.calculadorTributacao = calculadorTributacao;
		this.calculadorFrete = calculadorFrete;
		this.baixarEstoquePort = baixarEstoquePort;
		this.registrarNotaFiscalPort = registrarNotaFiscalPort;
		this.agendarEntregaPort = agendarEntregaPort;
		this.enviarNotaFiscalFinanceiroPort = enviarNotaFiscalFinanceiroPort;
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

		baixarEstoquePort.baixarEstoque(notaFiscal);
		registrarNotaFiscalPort.registrar(notaFiscal);
		agendarEntregaPort.agendar(notaFiscal);
		enviarNotaFiscalFinanceiroPort.enviar(notaFiscal);

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
