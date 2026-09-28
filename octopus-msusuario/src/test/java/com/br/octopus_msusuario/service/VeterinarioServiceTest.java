package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.Veterinario;
import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import com.br.octopus_msusuario.dto.request.VeterinarioRequest;
import com.br.octopus_msusuario.dto.response.VeterinarioResponse;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeterinarioServiceTest {

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private VeterinarioService veterinarioService;

    private VeterinarioRequest request(Especializacao especializacao) {
        return new VeterinarioRequest("vet@vidapet.com", "senha-forte", "Dra. Carla", "12345678901", TipoPessoa.PF,
                LocalDate.of(1990, 5, 10), "11999990000", "12345", "SP", especializacao);
    }

    private Usuario usuario() {
        return Usuario.builder().email("vet@vidapet.com").senha("hash").role(Role.ROLE_VETERINARIO).build();
    }

    @Test
    void criar_deveCriarUsuarioComRoleVeterinarioEEspecializacaoGeralPorPadrao() {
        when(usuarioService.novoUsuario("vet@vidapet.com", "senha-forte", Role.ROLE_VETERINARIO)).thenReturn(usuario());
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        VeterinarioResponse response = veterinarioService.criar(request(null));

        assertThat(response.especializacao()).isEqualTo(Especializacao.GERAL);
        assertThat(response.usuario().role()).isEqualTo(Role.ROLE_VETERINARIO);
        assertThat(response.crmvUf()).isEqualTo("SP");
    }

    @Test
    void criar_deveManterEspecializacaoInformada() {
        when(usuarioService.novoUsuario(any(), any(), any())).thenReturn(usuario());
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(veterinarioService.criar(request(Especializacao.CARDIOLOGIA)).especializacao())
                .isEqualTo(Especializacao.CARDIOLOGIA);
    }

    @Test
    void criar_deveRejeitarCpfDuplicado() {
        when(veterinarioRepository.existsByCpfCnpj("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> veterinarioService.criar(request(null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("CPF/CNPJ");
        verifyNoInteractions(usuarioService);
        verify(veterinarioRepository, never()).save(any());
    }

    @Test
    void criar_deveRejeitarCrmvDuplicadoNaMesmaUf() {
        when(veterinarioRepository.existsByCrmvAndCrmvUf("12345", "SP")).thenReturn(true);

        assertThatThrownBy(() -> veterinarioService.criar(request(null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("12345/SP");
        verify(veterinarioRepository, never()).save(any());
    }

    @Test
    void desativar_deveArquivarSemApagar() {
        UUID id = UUID.randomUUID();
        Veterinario veterinario = Veterinario.builder().id(id).nome("Dra. Carla").usuario(usuario()).build();
        when(veterinarioRepository.findById(id)).thenReturn(Optional.of(veterinario));

        VeterinarioResponse response = veterinarioService.desativar(id);

        assertThat(response.usuario().ativo()).isFalse();
        verify(veterinarioRepository, never()).delete(any());
    }

    @Test
    void buscar_deveLancarExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(veterinarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veterinarioService.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
