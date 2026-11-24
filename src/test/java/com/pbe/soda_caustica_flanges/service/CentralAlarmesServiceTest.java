package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CentralAlarmesServiceTest {
    private UsuarioRepository usuarios;
    private FlangeRepository flanges;
    private IncidenteRepository incidentes;
    private TesteEmergenciaRepository testes;
    private UsuarioAuditoriaService auditoria;
    private CentralAlarmesService service;
    private Authentication auth;
    private Usuario admin, user;
    private Flange flange;

    @BeforeEach void preparar() {
        usuarios=mock(UsuarioRepository.class); flanges=mock(FlangeRepository.class);
        incidentes=mock(IncidenteRepository.class); testes=mock(TesteEmergenciaRepository.class);
        auditoria=mock(UsuarioAuditoriaService.class); auth=mock(Authentication.class);
        service=new CentralAlarmesService(usuarios,flanges,incidentes,testes,auditoria);
        admin=new Usuario("Admin","admin@example.com","hash",Role.ADMIN,true,null);
        Unidade u=new Unidade("A","Setor A",true); u.setId(1L);
        user=new Usuario("User","user@example.com","hash",Role.USER,true,u);
        flange=new Flange(); flange.setId(12L); flange.setNomeFlange("FL-012"); flange.setLocalizacao("Setor A");
        flange.setStatus(StatusFlange.NORMAL); flange.setUnidade(u);
        when(auth.isAuthenticated()).thenReturn(true);
    }

    private void login(Usuario u) {
        when(auth.getName()).thenReturn(u.getEmail());
        when(usuarios.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    @Test void usuarioComumNaoIniciaNemEncerraTeste() {
        login(user);
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            ()->service.iniciar(auth,12L,TesteEmergencia.Tipo.VAZAMENTO,TesteEmergencia.Gravidade.CRITICA,true)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            ()->service.transicao(auth,1L,"encerrar")).getStatusCode());
        verifyNoInteractions(testes,incidentes,flanges,auditoria);
    }

    @Test void adminPrecisaConfirmar() {
        login(admin);
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            ()->service.iniciar(auth,12L,TesteEmergencia.Tipo.VAZAMENTO,TesteEmergencia.Gravidade.CRITICA,false)).getStatusCode());
        verifyNoInteractions(testes,incidentes,flanges);
    }

    @Test void cicloDoTesteNaoEscreveIncidentesNemStatusDaFlange() {
        login(admin);
        when(flanges.findById(12L)).thenReturn(Optional.of(flange));
        when(testes.saveAndFlush(any(TesteEmergencia.class))).thenAnswer(inv->inv.getArgument(0));
        AlarmePainelDTO iniciado=service.iniciar(auth,12L,TesteEmergencia.Tipo.VAZAMENTO,TesteEmergencia.Gravidade.CRITICA,true);
        assertTrue(iniciado.teste()); assertTrue(iniciado.sonoro()); assertEquals("DISPARADO",iniciado.estado());
        TesteEmergencia t=new TesteEmergencia(flange,TesteEmergencia.Tipo.VAZAMENTO,TesteEmergencia.Gravidade.CRITICA,admin.getEmail(),LocalDateTime.now());
        when(testes.findById(5L)).thenReturn(Optional.of(t));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,()->service.transicao(auth,5L,"atender")).getStatusCode());
        assertFalse(service.transicao(auth,5L,"reconhecer").sonoro());
        assertEquals("EM_MANUTENCAO",service.transicao(auth,5L,"atender").estado());
        assertFalse(service.transicao(auth,5L,"encerrar").ativo());
        assertEquals(StatusFlange.NORMAL,flange.getStatus());
        verifyNoInteractions(incidentes);
        verify(auditoria,times(4)).registrar(eq(admin),eq(admin.getEmail()),anyString(),anyString());
    }

    @Test void usuarioSoConsultaUnidadeEJamaisTestesAdministrativos() {
        login(user);
        when(incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(1L)).thenReturn(List.of());
        when(incidentes.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(1L)).thenReturn(List.of());
        assertTrue(service.painel(auth).testesAtivos().isEmpty());
        verifyNoInteractions(testes);
        verify(incidentes,never()).findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc();
    }

    @Test void reconhecerRealNaoAlteraFlangeENaoConsultaTestes() {
        login(user);
        Incidente i=new Incidente(); i.setId(8L);i.setFlange(flange); i.setDataHora(LocalDateTime.now());i.setStatusAlarme(StatusAlarme.DISPARADO);
        when(incidentes.findById(8L)).thenReturn(Optional.of(i));
        when(incidentes.reconhecerAtivo(eq(8L),eq(StatusAlarme.DISPARADO),eq(StatusAlarme.RECONHECIDO),any(LocalDateTime.class),eq(user.getEmail()),eq("Verificação iniciada"))).thenReturn(1);
        service.reconhecerReal(auth,8L,"Verificação iniciada");
        assertEquals(StatusFlange.NORMAL,flange.getStatus());
        verifyNoInteractions(testes,flanges);
    }

    @Test void ocorrenciaRealReconhecidaPermaneceAtivaSemSom() {
        Incidente i=new Incidente(); i.setId(8L);i.setFlange(flange);i.setDataHora(LocalDateTime.now());i.setStatusAlarme(StatusAlarme.RECONHECIDO);
        AlarmePainelDTO dto=AlarmePainelDTO.real(i);
        assertTrue(dto.ativo());assertFalse(dto.sonoro());assertFalse(dto.teste());assertEquals("real:8",dto.chave());
    }
}
