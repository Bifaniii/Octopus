package com.br.octopus_msrelatorios.relatorio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ExportadorRelatorio {

    private final PdfRelatorio pdf;
    private final XlsxRelatorio xlsx;

    /** Arquivo para download, com nome como "relatorio-doses-2026-10-12.pdf". */
    public ResponseEntity<byte[]> exportar(Relatorio relatorio, Formato formato) {
        byte[] conteudo = switch (formato) {
            case PDF -> pdf.gerar(relatorio);
            case XLSX -> xlsx.gerar(relatorio);
        };
        String nome = relatorio.nomeArquivo() + "-" + relatorio.emitidoEm().toLocalDate() + "." + formato.extensao();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(formato.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nome, StandardCharsets.UTF_8).build().toString())
                .body(conteudo);
    }
}
