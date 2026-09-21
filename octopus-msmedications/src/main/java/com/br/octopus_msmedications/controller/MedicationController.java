package com.br.octopus_msmedications.Controller;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.service.MedicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medications")
public class MedicationController {

    private final MedicationService service;

    public MedicationController(MedicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Medicacao> list(@RequestParam(required = false) String manufacturer) {
        return service.list(manufacturer);
    }

    @GetMapping("/{id}")
    public Medicacao findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Medicacao create(@RequestBody Medicacao medication) {
        return service.create(medication);
    }

    @PutMapping("/{id}")
    public Medicacao update(@PathVariable Long id, @RequestBody Medicacao medication) {
        return service.update(id, medication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
