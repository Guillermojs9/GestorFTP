package gestorftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class GestorFTP {

    private FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "guillermo";
    private static final String PASSWORD = "1234";

    public GestorFTP() {
        clienteFTP = new FTPClient();
    }

    private void conectar() {
        try {
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
        } catch (IOException ex) {
            System.out.println("No se ha conectar al servidor. Excepción: " + ex.getMessage());
        }
    }

    private void desconectar() {
        try {
            clienteFTP.disconnect();
        } catch (IOException ex) {
            System.out.println("Ocurrió un error al desconectarse. Excepción: " + ex.getMessage());
        }
    }

    private boolean subirFichero(String path) {
        boolean exito = false;
        try {
            File ficheroLocal = new File(path);
            InputStream is = new FileInputStream(ficheroLocal);
            boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
            is.close();
            exito = enviado;
        } catch (FileNotFoundException ex) {
            System.out.println("Error al buscar el archivo. Excepción: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error al subir el archivo. Excepción: " + ex.getMessage());
        }
        return exito;
    }

    private boolean descargarFichero(String ficheroRemoto, String pathLocal) {
        boolean exito = false;
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(pathLocal));
            boolean recibido = clienteFTP.retrieveFile(ficheroRemoto, os);
            os.close();
            exito = recibido;
        } catch (FileNotFoundException ex) {
            System.out.println("Error al buscar el archivo. Excepción: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error al descargar el archivo. Excepción: " + ex.getMessage());
        }
        return exito;
    }

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));

        } catch (UnsupportedEncodingException ex) {
            System.out.println("Error al intentar codificar: Excepción: " + ex.getMessage());
        }

    }

}
