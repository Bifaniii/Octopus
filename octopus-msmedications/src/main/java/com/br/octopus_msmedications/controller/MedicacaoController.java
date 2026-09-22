package com.br.octopus_msmedications.Controller;

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

@RestController
@RequestMapping("/medicacao")
public class MedicacaoController {

    private final MedicacaoService service;

    public MedicacaoController(MedicacaoService service) {
        this.service = service;

    }

    @GetMapping
    public ResponseEntity<List<MedicacaoResponse>>listarTodos(){
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<MedicacaoResponse>>findById(@PathVariable Long id){
        return  ResponseEntity.ok(service.listarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MedicacaoResponse> criar(@Valid @RequestBody MedicacaoRequest request){
       MedicacaoResponse novaMedicacao = service.criar(request);
       return  ResponseEntity.status(HttpStatus.CREATED).body(novaMedicacao);
    }


    @PatchMapping("/{id}")
    public ResponseEntity<MedicacaoResponse>update(
            @PathVariable Long id,
            @Valid @RequestBody MedicacaoResquestUpdate resquest){
        MedicacaoResponse response = service.atualiarParcial(id,resquest);
        return  ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/{id}")
    public MedicacaoResponse update(@PathVariable Long id, @Valid @RequestBody MedicacaoResquestUpdate request) {
        return service.atualiarParcial(id, request);
    }
}

