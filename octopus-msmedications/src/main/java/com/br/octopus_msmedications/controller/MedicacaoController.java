package com.br.octopus_msmedications.controller;

import com.br.octopus_msmedications.domain.dto.request.MedicacaoRequest;
import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.service.MedicacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.br.octopus_msmedications.domain.dto.response.MedicacaoResponse;
import java.util.List;
import com.br.octopus_msmedications.domain.dto.request.MedicacaoResquestUpdate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medicacao")
@RequiredArgsConstructor
public class MedicacaoController {

    private final MedicacaoService service; 

    @GetMapping
    public ResponseEntity<List<MedicacaoResponse>>listarTodos(){
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicacaoResponse>findById(@PathVariable Long id){
        return ResponseEntity.ok().body(service.listarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MedicacaoResponse> criar(@Valid @RequestBody MedicacaoRequest request){
       MedicacaoResponse novaMedicacao = service.criar(request);
       return  ResponseEntity.status(HttpStatus.CREATED).body(novaMedicacao);
    }


    @PatchMapping("/{id}")
    public ResponseEntity<MedicacaoResponse> update(@PathVariable Long id, @Valid @RequestBody MedicacaoResquestUpdate resquest){
        return  ResponseEntity.ok().body(service.atualizarParcial(id,resquest));
    }
}

