package com.br.octopus_msrelatorios.relatorio;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uma aba por tabela. Cada aba começa com a logo, o nome da clínica, o título, os filtros e quem emitiu; a
 * tabela vem logo abaixo, com filtro automático no cabeçalho e o cabeçalho congelado na rolagem. Datas e
 * números vão com o tipo certo, então dá para ordenar e somar na própria planilha.
 */
@Component
public class XlsxRelatorio {

    private static final int LINHA_CABECALHO = 5;
    private static final int LARGURA_MAXIMA = 60;
    private static final int LADO_LOGO_PX = 34;
    // A coluna A precisa caber a logo.
    private static final int LARGURA_MINIMA_A = 8;

    public byte[] gerar(Relatorio relatorio) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(planilha);
            int logo = planilha.addPicture(Marca.logoPng(), Workbook.PICTURE_TYPE_PNG);
            Set<String> nomesUsados = new HashSet<>();
            for (Relatorio.Tabela tabela : relatorio.tabelas()) {
                XSSFSheet aba = planilha.createSheet(nomeDaAba(tabela.titulo(), nomesUsados));
                preencher(aba, relatorio, tabela, estilos, logo);
            }
            planilha.getProperties().getCoreProperties().setTitle(relatorio.titulo());
            planilha.getProperties().getCoreProperties().setCreator(Marca.NOME);
            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void preencher(XSSFSheet aba, Relatorio relatorio, Relatorio.Tabela tabela, Estilos estilos, int logo) {
        int colunas = tabela.colunas().size();
        int[] larguras = new int[colunas];

        Row marca = aba.createRow(0);
        marca.setHeightInPoints(32);
        texto(marca, 0, Marca.NOME + "  ·  " + Marca.SUBTITULO, estilos.marca);
        texto(aba.createRow(1), 0, relatorio.titulo() + (relatorio.tabelas().size() > 1 ? ": " + tabela.titulo() : ""),
                estilos.titulo);
        texto(aba.createRow(2), 0, String.join("   ·   ", relatorio.filtros()), estilos.suave);
        texto(aba.createRow(3), 0, "Emitido por " + relatorio.emitidoPor() + " em "
                + Valores.texto(relatorio.emitidoEm()), estilos.suave);

        Row cabecalho = aba.createRow(LINHA_CABECALHO);
        for (int i = 0; i < colunas; i++) {
            String titulo = tabela.colunas().get(i).titulo();
            texto(cabecalho, i, titulo, estilos.cabecalho);
            larguras[i] = titulo.length() + 4;
        }

        int numero = LINHA_CABECALHO + 1;
        for (List<Object> valores : tabela.linhas()) {
            Row linha = aba.createRow(numero++);
            for (int i = 0; i < colunas; i++) {
                Object valor = valores.get(i);
                Cell celula = linha.createCell(i);
                if (valor instanceof LocalDateTime dataHora) {
                    celula.setCellValue(dataHora);
                    celula.setCellStyle(estilos.dataHora);
                } else if (valor instanceof LocalDate data) {
                    celula.setCellValue(data);
                    celula.setCellStyle(estilos.data);
                } else if (valor instanceof Number n) {
                    celula.setCellValue(n.doubleValue());
                    celula.setCellStyle(estilos.numero);
                } else if (valor != null) {
                    celula.setCellValue(valor.toString());
                    celula.setCellStyle(estilos.celula);
                } else {
                    celula.setCellStyle(estilos.celula);
                }
                larguras[i] = Math.max(larguras[i], Valores.texto(valor).length() + 2);
            }
        }

        // Largura pela quantidade de caracteres: autoSizeColumn depende das fontes do sistema, que o container não tem.
        for (int i = 0; i < colunas; i++) {
            int largura = Math.min(larguras[i], LARGURA_MAXIMA);
            aba.setColumnWidth(i, (i == 0 ? Math.max(largura, LARGURA_MINIMA_A) : largura) * 256);
        }
        // Tamanho fixo na âncora (dentro de A1): o resize() do POI depende de ler a imagem e saía com tamanho zero.
        int lado = LADO_LOGO_PX * Units.EMU_PER_PIXEL;
        XSSFClientAnchor ancora = new XSSFClientAnchor(Units.EMU_PER_PIXEL * 2, Units.EMU_PER_PIXEL * 2,
                lado, lado, 0, 0, 0, 0);
        ancora.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
        XSSFDrawing desenho = aba.createDrawingPatriarch();
        desenho.createPicture(ancora, logo);

        if (!tabela.linhas().isEmpty()) {
            aba.setAutoFilter(new CellRangeAddress(LINHA_CABECALHO, numero - 1, 0, colunas - 1));
        } else {
            texto(aba.createRow(numero), 0, "Nenhum registro para os filtros escolhidos.", estilos.suave);
        }
        aba.createFreezePane(0, LINHA_CABECALHO + 1);
    }

    private static void texto(Row linha, int coluna, String valor, CellStyle estilo) {
        Cell celula = linha.createCell(coluna);
        celula.setCellValue(valor);
        celula.setCellStyle(estilo);
    }

    // O Excel limita o nome da aba a 31 caracteres, sem : \ / ? * [ ], e não aceita dois iguais.
    private static String nomeDaAba(String titulo, Set<String> usados) {
        String base = WorkbookUtil.createSafeSheetName(titulo);
        String nome = base;
        for (int n = 2; !usados.add(nome.toLowerCase()); n++) {
            String sufixo = " (" + n + ")";
            nome = base.substring(0, Math.min(base.length(), 31 - sufixo.length())) + sufixo;
        }
        return nome;
    }

    private static final class Estilos {

        final CellStyle marca;
        final CellStyle titulo;
        final CellStyle suave;
        final CellStyle cabecalho;
        final CellStyle celula;
        final CellStyle numero;
        final CellStyle data;
        final CellStyle dataHora;

        Estilos(XSSFWorkbook planilha) {
            marca = planilha.createCellStyle();
            marca.setFont(fonte(planilha, 14, true, Marca.PRIMARIA_FORTE));
            marca.setVerticalAlignment(VerticalAlignment.CENTER);
            marca.setAlignment(HorizontalAlignment.LEFT);
            // Recuo para o texto não ficar embaixo da logo.
            marca.setIndention((short) 4);

            titulo = planilha.createCellStyle();
            titulo.setFont(fonte(planilha, 12, true, Marca.TEXTO));

            suave = planilha.createCellStyle();
            suave.setFont(fonte(planilha, 9, false, Marca.TEXTO_SUAVE));

            XSSFCellStyle c = planilha.createCellStyle();
            c.setFont(fonte(planilha, 10, true, Color.WHITE));
            c.setFillForegroundColor(cor(Marca.PRIMARIA));
            c.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            c.setVerticalAlignment(VerticalAlignment.CENTER);
            bordas(c);
            cabecalho = c;

            celula = planilha.createCellStyle();
            bordas((XSSFCellStyle) celula);
            celula.setVerticalAlignment(VerticalAlignment.TOP);

            numero = planilha.createCellStyle();
            numero.cloneStyleFrom(celula);
            numero.setDataFormat(planilha.createDataFormat().getFormat("0"));

            data = planilha.createCellStyle();
            data.cloneStyleFrom(celula);
            data.setDataFormat(planilha.createDataFormat().getFormat("dd/mm/yyyy"));

            dataHora = planilha.createCellStyle();
            dataHora.cloneStyleFrom(celula);
            dataHora.setDataFormat(planilha.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        }

        private static XSSFFont fonte(XSSFWorkbook planilha, int tamanho, boolean negrito, Color cor) {
            XSSFFont fonte = planilha.createFont();
            fonte.setFontHeightInPoints((short) tamanho);
            fonte.setBold(negrito);
            fonte.setColor(cor(cor));
            return fonte;
        }

        private static XSSFColor cor(Color cor) {
            return new XSSFColor(new byte[]{(byte) cor.getRed(), (byte) cor.getGreen(), (byte) cor.getBlue()}, null);
        }

        private static void bordas(XSSFCellStyle estilo) {
            XSSFColor borda = cor(Marca.BORDA);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            estilo.setTopBorderColor(borda);
            estilo.setBottomBorderColor(borda);
            estilo.setLeftBorderColor(borda);
            estilo.setRightBorderColor(borda);
        }
    }
}
