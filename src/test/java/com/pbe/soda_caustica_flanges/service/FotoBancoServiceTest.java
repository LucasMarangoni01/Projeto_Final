package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.ImagemPrivada;
import com.pbe.soda_caustica_flanges.repository.ImagemPrivadaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FotoBancoServiceTest {

    private MockMultipartFile fotoPequena() throws Exception {
        BufferedImage imagem = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagem, "png", saida));
        return new MockMultipartFile("imagem", "foto.png", "image/png", saida.toByteArray());
    }

    @Test
    void fotoDePerfilEhSalvaECarregadaDoBanco() throws Exception {
        ImagemPrivadaRepository repositorio = mock(ImagemPrivadaRepository.class);
        FotoPerfilService fotos = new FotoPerfilService("uploads/perfil", "database", repositorio);
        String nome = fotos.salvar(fotoPequena());

        ArgumentCaptor<ImagemPrivada> captor = ArgumentCaptor.forClass(ImagemPrivada.class);
        verify(repositorio).save(captor.capture());
        ImagemPrivada registro = captor.getValue();
        assertEquals("perfil:" + nome, registro.getChave());
        assertEquals("image/png", registro.getTipoMime());
        assertTrue(registro.getDados().length > 0);

        when(repositorio.findById("perfil:" + nome)).thenReturn(Optional.of(registro));
        var carregada = fotos.carregar(nome);
        assertArrayEquals(registro.getDados(), carregada.bytes());
        assertEquals("image/png", carregada.contentType().toString());
    }

    @Test
    void fotoDeFlangeEhSalvaECarregadaDoBanco() throws Exception {
        ImagemPrivadaRepository repositorio = mock(ImagemPrivadaRepository.class);
        FotoFlangeService fotos = new FotoFlangeService("uploads/flanges", "database", repositorio);
        String nome = fotos.salvar(fotoPequena());

        ArgumentCaptor<ImagemPrivada> captor = ArgumentCaptor.forClass(ImagemPrivada.class);
        verify(repositorio).save(captor.capture());
        ImagemPrivada registro = captor.getValue();
        assertEquals("flange:" + nome, registro.getChave());

        when(repositorio.findById("flange:" + nome)).thenReturn(Optional.of(registro));
        var carregada = fotos.carregar(nome);
        assertArrayEquals(registro.getDados(), carregada.bytes());
        assertEquals("image/png", carregada.contentType().toString());
    }
}
