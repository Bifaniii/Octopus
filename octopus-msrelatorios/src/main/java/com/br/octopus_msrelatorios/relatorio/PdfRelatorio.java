package com.br.octopus_msrelatorios.relatorio;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Formato padrão do PDF: cabeçalho com a logo e o nome da clínica em toda página, título e filtros na primeira,
 * uma tabela por seção (o cabeçalho da tabela se repete quando ela quebra de página) e rodapé com quem emitiu
 * e "Página X de Y".
 */
@Component
public class PdfRelatorio {

    private static final float MARGEM = 36f;
    private static final float TOPO = 92f;
    private static final float BASE = 54f;

    private final BaseFont normal;
    private final BaseFont negrito;

    public PdfRelatorio() {
        try {
            // Helvetica com Cp1252 cobre os acentos do português sem embutir fonte no arquivo.
            normal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            negrito = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] gerar(Relatorio relatorio) {
        Rectangle pagina = relatorio.paisagem() ? PageSize.A4.rotate() : PageSize.A4;
        Document documento = new Document(pagina, MARGEM, MARGEM, TOPO, BASE);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(documento, saida);
        writer.setPageEvent(new CabecalhoERodape(relatorio));
        documento.addTitle(relatorio.titulo());
        documento.addAuthor(Marca.NOME);
        documento.addCreator("Octopus - MS Relatórios");
        documento.open();

        Paragraph titulo = new Paragraph(relatorio.titulo(), new Font(negrito, 17, Font.NORMAL, Marca.PRIMARIA_FORTE));
        titulo.setSpacingAfter(4);
        documento.add(titulo);
        if (!relatorio.filtros().isEmpty()) {
            Paragraph filtros = new Paragraph(String.join("   ·   ", relatorio.filtros()),
                    new Font(normal, 9, Font.NORMAL, Marca.TEXTO_SUAVE));
            filtros.setSpacingAfter(6);
            documento.add(filtros);
        }

        for (Relatorio.Tabela tabela : relatorio.tabelas()) {
            Paragraph secao = new Paragraph(tabela.titulo(), new Font(negrito, 11, Font.NORMAL, Marca.TEXTO));
            secao.setSpacingBefore(12);
            secao.setSpacingAfter(6);
            documento.add(secao);
            if (tabela.linhas().isEmpty()) {
                documento.add(new Paragraph("Nenhum registro para os filtros escolhidos.",
                        new Font(normal, 9, Font.ITALIC, Marca.TEXTO_SUAVE)));
            } else {
                documento.add(tabela(tabela));
            }
        }

        if (relatorio.assinatura() != null) {
            documento.add(assinatura(relatorio.assinatura()));
        }

        documento.close();
        return saida.toByteArray();
    }

    // Caixa com o nome de quem assina, o carimbo "ASSINADO DIGITALMENTE" e o momento, que não quebra de página.
    private PdfPTable assinatura(Relatorio.Assinatura assinatura) {
        PdfPTable caixa = new PdfPTable(1);
        caixa.setWidthPercentage(60);
        caixa.setHorizontalAlignment(Element.ALIGN_RIGHT);
        caixa.setSpacingBefore(36);
        caixa.setKeepTogether(true);
        caixa.setSplitLate(false);

        Phrase texto = new Phrase();
        texto.add(new com.lowagie.text.Chunk(assinatura.nome() + "\n", new Font(negrito, 12, Font.NORMAL, Marca.TEXTO)));
        texto.add(new com.lowagie.text.Chunk("ASSINADO DIGITALMENTE\n",
                new Font(negrito, 9.5f, Font.NORMAL, Marca.PRIMARIA_FORTE)));
        texto.add(new com.lowagie.text.Chunk(Valores.texto(assinatura.em()),
                new Font(normal, 8.5f, Font.NORMAL, Marca.TEXTO_SUAVE)));
        PdfPCell celula = new PdfPCell(texto);
        celula.setHorizontalAlignment(Element.ALIGN_CENTER);
        celula.setPadding(12);
        celula.setBorderColor(Marca.PRIMARIA);
        celula.setBorderWidth(1.2f);
        caixa.addCell(celula);
        return caixa;
    }

    private PdfPTable tabela(Relatorio.Tabela tabela) {
        List<Relatorio.Coluna> colunas = tabela.colunas();
        float[] pesos = new float[colunas.size()];
        for (int i = 0; i < pesos.length; i++) {
            pesos[i] = colunas.get(i).peso();
        }
        PdfPTable pdf = new PdfPTable(pesos);
        pdf.setWidthPercentage(100);
        pdf.setHeaderRows(1);

        Font fonteCabecalho = new Font(negrito, 8.5f, Font.NORMAL, Color.WHITE);
        for (Relatorio.Coluna coluna : colunas) {
            PdfPCell celula = celula(coluna.titulo(), fonteCabecalho);
            celula.setBackgroundColor(Marca.PRIMARIA);
            celula.setBorderColor(Marca.PRIMARIA);
            pdf.addCell(celula);
        }

        Font fonte = new Font(normal, 8.5f, Font.NORMAL, Marca.TEXTO);
        boolean alternada = false;
        for (List<Object> linha : tabela.linhas()) {
            for (Object valor : linha) {
                PdfPCell celula = celula(Valores.texto(valor), fonte);
                if (valor instanceof Number) {
                    celula.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                if (alternada) {
                    celula.setBackgroundColor(Marca.FUNDO_ALTERNADO);
                }
                pdf.addCell(celula);
            }
            alternada = !alternada;
        }
        return pdf;
    }

    private static PdfPCell celula(String texto, Font fonte) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte));
        celula.setPaddingTop(4);
        celula.setPaddingBottom(5);
        celula.setPaddingLeft(5);
        celula.setPaddingRight(5);
        celula.setBorderColor(Marca.BORDA);
        celula.setBorderWidth(0.5f);
        return celula;
    }

    private class CabecalhoERodape extends PdfPageEventHelper {

        private final Relatorio relatorio;
        private PdfTemplate totalDePaginas;

        CabecalhoERodape(Relatorio relatorio) {
            this.relatorio = relatorio;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document documento) {
            totalDePaginas = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle pagina = documento.getPageSize();
            float esquerda = documento.left();
            float direita = documento.right();
            float topo = pagina.getHeight() - 30;

            logo(cb, esquerda, topo - 30, 30);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(Marca.NOME, new Font(negrito, 15, Font.NORMAL, Marca.PRIMARIA_FORTE)),
                    esquerda + 40, topo - 13, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(Marca.SUBTITULO, new Font(normal, 8.5f, Font.NORMAL, Marca.TEXTO_SUAVE)),
                    esquerda + 40, topo - 26, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(relatorio.titulo(), new Font(negrito, 9, Font.NORMAL, Marca.TEXTO)),
                    direita, topo - 13, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Emitido em " + Valores.texto(relatorio.emitidoEm()),
                            new Font(normal, 8.5f, Font.NORMAL, Marca.TEXTO_SUAVE)),
                    direita, topo - 26, 0);

            cb.setColorStroke(Marca.PRIMARIA);
            cb.setLineWidth(1.2f);
            cb.moveTo(esquerda, topo - 38);
            cb.lineTo(direita, topo - 38);
            cb.stroke();

            float rodape = BASE - 26;
            cb.setColorStroke(Marca.BORDA);
            cb.setLineWidth(0.6f);
            cb.moveTo(esquerda, rodape + 12);
            cb.lineTo(direita, rodape + 12);
            cb.stroke();

            Font fonteRodape = new Font(normal, 8, Font.NORMAL, Marca.TEXTO_SUAVE);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Emitido por " + relatorio.emitidoPor() + ". Documento de uso interno da clínica.",
                            fonteRodape),
                    esquerda, rodape, 0);

            String textoPagina = "Página " + writer.getPageNumber() + " de ";
            float largura = normal.getWidthPoint(textoPagina, 8);
            float x = direita - largura - 14;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(textoPagina, fonteRodape), x, rodape, 0);
            cb.addTemplate(totalDePaginas, x + largura, rodape);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document documento) {
            totalDePaginas.beginText();
            totalDePaginas.setFontAndSize(normal, 8);
            totalDePaginas.setColorFill(Marca.TEXTO_SUAVE);
            totalDePaginas.showText(String.valueOf(writer.getPageNumber() - 1));
            totalDePaginas.endText();
        }

        // A logo do sistema, a mesma do front, como imagem.
        private void logo(PdfContentByte cb, float x, float y, float lado) {
            try {
                com.lowagie.text.Image imagem = com.lowagie.text.Image.getInstance(Marca.logoPng());
                imagem.scaleAbsolute(lado, lado);
                imagem.setAbsolutePosition(x, y);
                cb.addImage(imagem);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (com.lowagie.text.DocumentException e) {
                throw new IllegalStateException("Logo inválida", e);
            }
        }
    }
}
