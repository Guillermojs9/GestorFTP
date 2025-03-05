package gestorftp;

import java.io.BufferedReader;
import java.io.File;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestorFTP {

    private static final String DIRECTORIO_LOCAL = "C:/localDir/";

    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "172.26.157.64";
    private static final int PUERTO = 21;
    private static final String USUARIO = "guillermo";
    private static final String PASSWORD = "guillermo";

    public GestorFTP() {
        clienteFTP = new FTPClient();
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

    private boolean subirFichero(Path path) {
        boolean exito = false;
        try {
            FileInputStream is = new FileInputStream(path.toFile());
            boolean enviado = clienteFTP.storeFile(path.getFileName().toString(), is);
            is.close();
            System.out.println("Archivo subido: " + path.getFileName() + " - " + enviado);
            exito = enviado;
        } catch (IOException ex) {
            System.out.println("Error al subir archivo. Excepción: " + ex.getMessage());
        }
        return exito;
    }

    private boolean eliminarFichero(String nombreArchivo) {
        boolean exito = false;
        try {
            boolean eliminado = clienteFTP.deleteFile(nombreArchivo);
            System.out.println("Archivo eliminado: " + nombreArchivo + " - " + eliminado);
            return eliminado;
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
                        FileReader fr = new FileReader("C:/localDir/" + archivoCambiado.toString());
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
                        File nuevoArchivo = new File("C:/localDir/" + archivoCambiado.toString());
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
