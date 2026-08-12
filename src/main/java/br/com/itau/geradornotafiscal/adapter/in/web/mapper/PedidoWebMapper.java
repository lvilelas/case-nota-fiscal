package br.com.itau.geradornotafiscal.adapter.in.web.mapper;

import br.com.itau.geradornotafiscal.adapter.in.web.dto.DestinatarioRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.DocumentoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.EnderecoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.ItemRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.PedidoRequest;
import br.com.itau.geradornotafiscal.domain.model.Destinatario;
import br.com.itau.geradornotafiscal.domain.model.Documento;
import br.com.itau.geradornotafiscal.domain.model.Endereco;
import br.com.itau.geradornotafiscal.domain.model.Item;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.springframework.stereotype.Component;

@Component
public class PedidoWebMapper {
    public Pedido paraDominio(PedidoRequest request) {
        return Pedido.builder()
                .idPedido(request.idPedido())
                .data(request.data())
                .valorTotalItens(request.valorTotalItens())
                .valorFrete(request.valorFrete())
                .itens(request.itens().stream().map(this::paraItem).toList())
                .destinatario(paraDestinatario(request.destinatario()))
                .build();
    }

    private Item paraItem(ItemRequest request) {
        return new Item(
                request.idItem(),
                request.descricao(),
                request.valorUnitario(),
                request.quantidade());
    }

    private Destinatario paraDestinatario(DestinatarioRequest request) {
        return Destinatario.builder()
                .nome(request.nome())
                .tipoPessoa(request.tipoPessoa())
                .regimeTributacao(request.regimeTributacao())
                .documentos(request.documentos().stream().map(this::paraDocumento).toList())
                .enderecos(request.enderecos().stream().map(this::paraEndereco).toList())
                .build();
    }

    private Documento paraDocumento(DocumentoRequest request) {
        return new Documento(request.numero(), request.tipo());
    }

    private Endereco paraEndereco(EnderecoRequest request) {
        return Endereco.builder()
                .cep(request.cep())
                .logradouro(request.logradouro())
                .numero(request.numero())
                .estado(request.estado())
                .complemento(request.complemento())
                .finalidade(request.finalidade())
                .regiao(request.regiao())
                .build();
    }
}
