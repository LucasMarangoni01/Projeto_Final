package com.pbe.soda_caustica_flanges.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FotoFlangeService {

    private static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;
    private static final long PIXELS_MAXIMOS = 18_000_000L;

    // Aceita nomes gerados pelo sistema e nomes legados, mas nunca caminhos.
    private static final Pattern NOME_VALIDO =
            Pattern.compile("^[a-zA-Z0-9_-]{1,160}\\.(jpg|jpeg|png)$");

    private final Path diretorio;

    public FotoFlangeService(
            @Value("${app.upload.flange-dir:uploads/flanges}") String diretorio) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    public String salvar(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para enviar.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A foto da flange deve ter no máximo 2 MB.");
        }

        byte[] bytes = arquivo.getBytes();
        String extensao = detectarExtensao(bytes);

        if (extensao == null) {
            throw new IllegalArgumentException("Formato não permitido. Envie uma imagem JPEG ou PNG.");
        }

        validarContentType(arquivo.getContentType(), extensao);
        BufferedImage imagem = decodificarImagem(bytes);

        Files.createDirectories(diretorio);

        String nomeArquivo = UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + extensao;
        Path destino = resolverCaminho(nomeArquivo);
        String formato = extensao.equals(".png") ? "png" : "jpg";

        if (!ImageIO.write(imagem, formato, destino.toFile())) {
            throw new IOException("Não foi possível processar a imagem enviada.");
        }

        return nomeArquivo;
    }

    public FotoCarregada carregar(String nomeArquivo) throws IOException {
        Path arquivo = resolverCaminho(nomeArquivo);

        if (Files.isRegularFile(arquivo)) {
            byte[] bytes = Files.readAllBytes(arquivo);
            MediaType tipo = detectarMediaType(bytes);
            if (tipo == null) {
                throw new FileNotFoundException("A imagem armazenada não possui formato permitido.");
            }
            return new FotoCarregada(bytes, tipo);
        }

        // Migração compatível: imagens antigas estavam em static/foto_flange.
        ClassPathResource legado = new ClassPathResource("static/foto_flange/" + nomeArquivo);
        if (!legado.exists()) {
            throw new FileNotFoundException("Foto da flange não encontrada.");
        }

        byte[] bytes;
        try (InputStream entrada = legado.getInputStream()) {
            bytes = entrada.readAllBytes();
        }

        MediaType tipo = detectarMediaType(bytes);
        if (tipo == null || bytes.length > TAMANHO_MAXIMO) {
            throw new FileNotFoundException("A foto legada não é uma imagem válida.");
        }

        Files.createDirectories(diretorio);
        Files.copy(new ByteArrayInputStream(bytes), arquivo, StandardCopyOption.REPLACE_EXISTING);

        return new FotoCarregada(bytes, tipo);
    }

    public void remover(String nomeArquivo) throws IOException {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return;
        }
        Files.deleteIfExists(resolverCaminho(nomeArquivo));
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

    private void validarContentType(String contentType, String extensao) {
        if (contentType == null || contentType.isBlank()
                || contentType.equalsIgnoreCase("application/octet-stream")) {
            return;
        }

        boolean compativel = (extensao.equals(".jpg")
                && (contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/jpg")))
                || (extensao.equals(".png")
                && contentType.equalsIgnoreCase("image/png"));

        if (!compativel) {
            throw new IllegalArgumentException("O conteúdo enviado não corresponde ao formato da imagem.");
        }
    }

    private BufferedImage decodificarImagem(byte[] bytes) throws IOException {
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

                BufferedImage imagem = leitor.read(0);
                if (imagem == null) {
                    throw new IllegalArgumentException("Não foi possível decodificar a imagem enviada.");
                }
                return imagem;
            } finally {
                leitor.dispose();
            }
        }
    }

    private String detectarExtensao(byte[] bytes) {
        MediaType tipo = detectarMediaType(bytes);
        if (MediaType.IMAGE_JPEG.equals(tipo)) {
            return ".jpg";
        }
        if (MediaType.IMAGE_PNG.equals(tipo)) {
            return ".png";
        }
        return null;
    }

    private MediaType detectarMediaType(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return null;
        }

        boolean jpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        if (jpeg) {
            return MediaType.IMAGE_JPEG;
        }

        byte[] assinaturaPng = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        for (int i = 0; i < assinaturaPng.length; i++) {
            if (bytes[i] != assinaturaPng[i]) {
                return null;
            }
        }
        return MediaType.IMAGE_PNG;
    }

    public record FotoCarregada(byte[] bytes, MediaType contentType) {
    }
}
