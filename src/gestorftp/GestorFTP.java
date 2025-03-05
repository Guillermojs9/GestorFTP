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
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class GestorFTP {

    //172.26.157.64
    private FTPClient clienteFTP;
    private static final String SERVIDOR = "172.26.157.64";
    private static final int PUERTO = 21;
    private static final String USUARIO = "guillermo";
    private static final String PASSWORD = "guillermo";

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
            System.out.println("Enviado: " + enviado);
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
        File localDir = new File(pathLocal);
        if (!localDir.exists()) {
            boolean dirCreated = localDir.mkdirs();
            if (!dirCreated) {
                System.out.println("No se pudo crear el directorio local: " + pathLocal);
                return false;
            }
        }
        File localFile = new File(localDir, ficheroRemoto);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
        boolean recibido = clienteFTP.retrieveFile(ficheroRemoto, os);
        System.out.println("Respuesta del servidor: " + clienteFTP.getReplyString());
        System.out.println("Descargado: " + recibido);
        
        os.close();
        exito = recibido;
    } catch (FileNotFoundException ex) {
        System.out.println("Error, archivo no encontrado. Excepción: " + ex.getMessage());
    } catch (IOException ex) {
        System.out.println("Error durante la descarga. Excepción: " + ex.getMessage());
    }
    return exito;
}

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            String fichero_descarga = "test.txt";

            GestorFTP gestorFTP = new GestorFTP();
            gestorFTP.conectar();
            System.out.println("Conectado");
            /*
            boolean subido = gestorFTP.subirFichero("C:/Users/guillermo/Desktop/test.txt");
            if (subido) {
                System.out.println("Fichero subido correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar subir el fichero");
            }

             */
            boolean descargado = gestorFTP.descargarFichero(fichero_descarga, "C:/ftpPrueba2");
            if (descargado) {
                System.out.println("Fichero descargado correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar descargar el fichero.");
            }

            gestorFTP.desconectar();
            System.out.println("Desconectado");
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        }
    }
}
