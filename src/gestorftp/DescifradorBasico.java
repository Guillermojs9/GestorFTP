package gestorftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class DescifradorBasico {

    private static final String SERVIDOR = "172.26.157.64";
    private static final int PUERTO = 21;
    private static final String USUARIO = "guillermo";
    private static final String PASSWORD = "guillermo";

    public static void main(String[] args) {
        final String NOMBRE_FICHERO = "textoCifrado15.txt.gui";
        try {
            boolean descarga = descargarArchivo(NOMBRE_FICHERO);
            if (!descarga) {
                System.err.println("No se pudo descargar el archivo desde el servidor FTP");
                return;
            }
            GestorFTP gestor = new GestorFTP();
            File file = new File(NOMBRE_FICHERO);
            byte[] datoCifrado = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(datoCifrado);
            }
            String textoDescifrado = gestor.descifrarTexto(datoCifrado);
            try (FileOutputStream fos = new FileOutputStream(NOMBRE_FICHERO)) {
                fos.write(textoDescifrado.getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("Texto descifrado: ");
            System.out.println(textoDescifrado);
        } catch (IOException ex) {
            System.err.println("Error al encontrar el archivo. Excepción: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Ha ocurrido un error. Excepción: " + ex.getMessage());
        }
    }

    private static boolean descargarArchivo(String archivo) {
        FTPClient clienteFTP = new FTPClient();
        boolean exito = false;
        try {
            clienteFTP.connect(SERVIDOR, PUERTO);
            int respuesta = clienteFTP.getReplyCode();

            if (!FTPReply.isPositiveCompletion(respuesta)) {
                clienteFTP.disconnect();
                System.err.println("Error al conectar con el servidor FTP");
                return false;
            }
            boolean credencialesOK = clienteFTP.login(USUARIO, PASSWORD);
            if (!credencialesOK) {
                System.err.println("Error al conectar con el servidor FTP. Credenciales incorrectas");
                return false;
            }
            clienteFTP.setFileType(FTP.BINARY_FILE_TYPE);
            File downloadFile = new File(archivo);
            try (FileOutputStream fos = new FileOutputStream(downloadFile); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                System.out.println("Descargando archivo: " + archivo);
                exito = clienteFTP.retrieveFile(archivo, bos);

                if (exito) {
                    System.out.println("Archivo descargado exitosamente");
                } else {
                    System.err.println("No se pudo descargar el archivo");
                }
            }

            clienteFTP.disconnect();
        } catch (IOException ex) {
            System.err.println("Error en la conexión FTP: " + ex.getMessage());
        }

        return exito;
    }

}
