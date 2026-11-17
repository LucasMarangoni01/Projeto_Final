package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.ImagemPrivada;
import com.pbe.soda_caustica_flanges.repository.ImagemPrivadaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FotoPerfilService {

    private static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;
    private static final long PIXELS_MAXIMOS = 18_000_000L;
    private static final Pattern NOME_VALIDO =
            Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png)$");

    private final Path diretorio;
    private final boolean armazenarNoBanco;
    private final ImagemPrivadaRepository imagens;

    public FotoPerfilService(
            @Value("${app.upload.profile-dir:uploads/perfil}") String diretorio,
            @Value("${app.upload.storage:filesystem}") String armazenamento,
            ImagemPrivadaRepository imagens) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
        this.armazenarNoBanco = "database".equalsIgnoreCase(armazenamento);
        this.imagens = imagens;
    }

    public String salvar(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para enviar.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A foto deve ter no máximo 2 MB.");
        }

        byte[] bytes = arquivo.getBytes();
        String extensao = detectarExtensao(bytes);

        if (extensao == null) {
            throw new IllegalArgumentException(
                    "Formato não permitido. Envie uma imagem JPEG ou PNG."
            );
        }

        String contentType = arquivo.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !contentType.equalsIgnoreCase("application/octet-stream")
                && !(extensao.equals(".jpg")
                    && (contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/jpg")))
                && !(extensao.equals(".png")
                    && contentType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("O conteúdo enviado não corresponde ao formato da imagem.");
        }

        BufferedImage imagem;
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (entrada == null) {
                throw new IllegalArgumentException("Não foi possível ler a imagem enviada.");
            }

            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem compatível.");
            }

            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);

                if (largura < 1 || altura < 1 || largura > 6000 || altura > 6000
                        || (long) largura * altura > PIXELS_MAXIMOS) {
                    throw new IllegalArgumentException("A imagem possui dimensões muito grandes.");
                }

                imagem = leitor.read(0);
                if (imagem == null) {
                    throw new IllegalArgumentException("Não foi possível decodificar a imagem enviada.");
                }
            } finally {
                leitor.dispose();
            }
        }

        String nomeArquivo = UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + extensao;
        Path destino = resolverCaminho(nomeArquivo);
        String formato = extensao.equals(".png") ? "png" : "jpg";

        if (armazenarNoBanco) {
            try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
                if (!ImageIO.write(imagem, formato, saida)) {
                    throw new IOException("Não foi possível processar a imagem enviada.");
                }
                imagens.save(new ImagemPrivada("perfil:" + nomeArquivo,
                        extensao.equals(".png") ? "image/png" : "image/jpeg",
                        saida.toByteArray()));
            }
        } else {
            Files.createDirectories(diretorio);
            if (!ImageIO.write(imagem, formato, destino.toFile())) {
                throw new IOException("Não foi possível processar a imagem enviada.");
            }
        }
        return nomeArquivo;
    }

    public FotoCarregada carregar(String nomeArquivo) throws IOException {
        Path arquivo = resolverCaminho(nomeArquivo);
        MediaType tipo = nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

        if (armazenarNoBanco) {
            ImagemPrivada registro = imagens.findById("perfil:" + nomeArquivo)
                    .orElse(null);
            if (registro != null) {
                return new FotoCarregada(registro.getDados(), tipo);
            }
        }

        if (!Files.isRegularFile(arquivo)) {
            throw new FileNotFoundException("Foto de perfil não encontrada.");
        }

        return new FotoCarregada(Files.readAllBytes(arquivo), tipo);
    }

    public void remover(String nomeArquivo) throws IOException {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return;
        }

        Path arquivo = resolverCaminho(nomeArquivo);
        if (armazenarNoBanco) {
            imagens.deleteById("perfil:" + nomeArquivo);
        } else {
            Files.deleteIfExists(arquivo);
        }
    }

    private Path resolverCaminho(String nomeArquivo) throws IOException {
        if (nomeArquivo == null || !NOME_VALIDO.matcher(nomeArquivo).matches()) {
            throw new FileNotFoundException("Nome de arquivo inválido.");
        }

        Path arquivo = diretorio.resolve(nomeArquivo).normalize();
        if (!arquivo.startsWith(diretorio)) {
            throw new FileNotFoundException("Caminho de arquivo inválido.");
        }

        return arquivo;
    }

    private String detectarExtensao(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return null;
        }

        boolean jpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;

        if (jpeg) {
            return ".jpg";
        }

        byte[] assinaturaPng = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        for (int i = 0; i < assinaturaPng.length; i++) {
            if (bytes[i] != assinaturaPng[i]) {
                return null;
            }
        }

        return ".png";
    }

    public record FotoCarregada(byte[] bytes, MediaType contentType) {
    }
}
