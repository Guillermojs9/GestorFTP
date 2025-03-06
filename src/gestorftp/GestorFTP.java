package gestorftp;

import java.io.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class GestorFTP {

    private static final String DIRECTORIO_LOCAL = "C:/localDir/";
    private static final String CLAVE_AES = "Guillermojs9";
    private static final String EXTENSION_CIFRADO = ".gui";

    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "172.26.157.64";
    private static final int PUERTO = 21;
    private static final String USUARIO = "guillermo";
    private static final String PASSWORD = "guillermo";

    private Key claveSecreta;

    public GestorFTP() {
        clienteFTP = new FTPClient();
        try {
            byte[] claveCodificada = CLAVE_AES.getBytes(StandardCharsets.UTF_8);
            byte[] clave16Bytes = new byte[16];
            System.arraycopy(claveCodificada, 0, clave16Bytes, 0,
                    Math.min(claveCodificada.length, 16));
            claveSecreta = new SecretKeySpec(clave16Bytes, "AES");
        } catch (Exception ex) {
            System.err.println("Error al inicializar el cifrado AES: " + ex.getMessage());
        }
    }

    private void conectar() throws IOException {
        clienteFTP.connect(SERVIDOR, PUERTO);
        int respuesta = clienteFTP.getReplyCode();
        if (!FTPReply.isPositiveCompletion(respuesta)) {
            clienteFTP.disconnect();
            throw new IOException("Error al conectar con el servidor FTP");
        }
        boolean credencialesOK = clienteFTP.login(USUARIO, PASSWORD);
        if (!credencialesOK) {
            throw new IOException("Error al conectar con el servidor FTP. Credenciales incorrectas");
        }
        clienteFTP.setFileType(FTP.BINARY_FILE_TYPE);
    }

    private void desconectar() {
        try {
            clienteFTP.disconnect();
        } catch (IOException ex) {
            System.out.println("Ocurrió un error al desconectarse. Excepción: " + ex.getMessage());
        }
    }

    private byte[] cifrarTexto(String texto) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, claveSecreta);
        return cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
    }

    public String descifrarTexto(byte[] textoCifrado) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, claveSecreta);
        byte[] textoPlano = cipher.doFinal(textoCifrado);
        return new String(textoPlano, StandardCharsets.UTF_8);
    }

    private boolean esArchivoTexto(Path path) {
        String nombre = path.getFileName().toString().toLowerCase();
        return nombre.endsWith(".txt");
    }

    private boolean subirFichero(Path path) {
        boolean exito = false;
        try {
            String nombreArchivo = path.getFileName().toString();
            if (esArchivoTexto(path)) {
                StringBuilder contenido = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        contenido.append(linea).append("\n");
                    }
                }
                byte[] contenidoCifrado = cifrarTexto(contenido.toString());
                Path archivoCifrado = Files.createTempFile("cifrado_", EXTENSION_CIFRADO);
                Files.write(archivoCifrado, contenidoCifrado);
                FileInputStream is = new FileInputStream(archivoCifrado.toFile());
                boolean enviado = clienteFTP.storeFile(nombreArchivo + EXTENSION_CIFRADO, is);
                is.close();
                Files.delete(archivoCifrado);
                System.out.println("Archivo cifrado y subido: " + nombreArchivo + EXTENSION_CIFRADO + " - " + enviado);
                exito = enviado;
            } else {
                FileInputStream is = new FileInputStream(path.toFile());
                boolean enviado = clienteFTP.storeFile(nombreArchivo, is);
                is.close();
                System.out.println("Archivo subido sin cifrar: " + nombreArchivo + " - " + enviado);
                exito = enviado;
            }
        } catch (Exception ex) {
            System.out.println("Error al subir archivo. Excepción: " + ex.getMessage());
        }
        return exito;
    }

    private boolean eliminarFichero(String nombreArchivo) {
        boolean exito = false;
        try {
            if (nombreArchivo.toLowerCase().endsWith(".txt")) {
                boolean eliminado = clienteFTP.deleteFile(nombreArchivo + EXTENSION_CIFRADO);
                System.out.println("Archivo cifrado eliminado: " + nombreArchivo + EXTENSION_CIFRADO + " - " + eliminado);
                return eliminado;
            } else {
                boolean eliminado = clienteFTP.deleteFile(nombreArchivo);
                System.out.println("Archivo eliminado: " + nombreArchivo + " - " + eliminado);
                return eliminado;
            }
        } catch (IOException ex) {
            System.out.println("Error al eliminar archivo. Excepción: " + ex.getMessage());
        }
        return exito;
    }

    private void monitoreoCarpetaLocal() {
        try {
            Path path = Paths.get(DIRECTORIO_LOCAL);
            WatchService ws = FileSystems.getDefault().newWatchService();
            path.register(ws,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            while (true) {
                WatchKey key = ws.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path archivoCambiado = (Path) event.context();
                    Path rutaCompleta = path.resolve(archivoCambiado);
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Thread.sleep(500);
                        conectar();
                        subirFichero(rutaCompleta);
                        desconectar();
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        conectar();
                        eliminarFichero(archivoCambiado.toString());
                        desconectar();
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        FileReader fr = new FileReader(DIRECTORIO_LOCAL + archivoCambiado.toString());
                        BufferedReader br = new BufferedReader(fr);
                        String mensaje;
                        List<String> contenido = new ArrayList<>();
                        while ((mensaje = br.readLine()) != null) {
                            contenido.add(mensaje);
                        }
                        br.close();
                        fr.close();
                        Thread.sleep(5);
                        conectar();
                        eliminarFichero(archivoCambiado.toString());
                        File nuevoArchivo = new File(DIRECTORIO_LOCAL + archivoCambiado.toString());
                        subirFichero(nuevoArchivo.toPath());
                        desconectar();
                    }
                }
                key.reset();
            }
        } catch (IOException ex) {
            System.err.println("Error al encontrar el directorio. Excepción: " + ex.getMessage());
        } catch (InterruptedException ex) {
            System.err.println("Error relacionado con hilos y procesos (Interrumpidos). Excepción: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        GestorFTP gestorFTP = new GestorFTP();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> gestorFTP.monitoreoCarpetaLocal());
    }
}
