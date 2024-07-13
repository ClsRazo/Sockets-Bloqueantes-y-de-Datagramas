package com.maciasrazo.practica1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;

import javax.swing.JFileChooser;

public class Servidor {
    public static void main(String[] args) {

        File directorio;
        do {
            directorio = seleccionarDirectorio();
        }
        while(directorio == null);

        ServerSocket s = null;
        try{
            s = new ServerSocket(1234);
            System.out.println("Servidor iniciado en el pruerto "+s.getLocalPort());
            for(;;) {
                Socket cl = s.accept();
                System.out.println("Cliente conectado desde "+cl.getInetAddress()+":"+cl.getPort());
                DataInputStream dis = new DataInputStream(cl.getInputStream());
                DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                boolean salir = false;
                while(!salir){
                    int opcion = dis.readInt();

                    switch(opcion) {
                        //------------------LISTAR CONTENIDO------------------
                        case 1:
                            StringBuilder a = new StringBuilder ();
                            listarContenido(directorio, 0, a);
                            File arch = new File(directorio + "\\" + "temp.txt");
                            FileWriter b = new FileWriter(directorio + "\\" + "temp.txt");

                            b.write(a.toString());
                            b.close();
                            dos.writeLong(arch.length());
                            dos.writeUTF(arch.getName());
                            dos.flush();

                            enviarArchivo(arch, dos);
                            // dos.writeUTF(a.toString());
                            // dos.flush();
                            
                            try {
                                Files.delete(arch.toPath());
                            } catch (NoSuchFileException x) {
                                // System.err.format("%s: no such" + " file or directory%n", chooser.getSelectedFile().toPath());
                            } catch (IOException x) {
                                System.err.println(x); 
                            }

                            break;
                        //------------------CREAR ARCHIVO O CARPETA------------------
                        case 2:
                            String coa = dis.readUTF();
                            String nombre = dis.readUTF();
                            if(coa.equals("a") || coa.equals("A")) {
                                crearArchivo(directorio, nombre);
                            } else {
                                crearCarpeta(directorio, nombre);
                            }
                            dos.writeUTF("T");
                            break;
                        //------------------ELIMINAR ARCHIVO O CARPETA------------------
                        case 3:
                            coa = dis.readUTF();
                            if(coa.equals("a") || coa.equals("A")) {eliminarArchivo(directorio, dis, dos);}
                            else {
                                eliminarCarpeta(directorio, dis, dos);
                                do{
                                    directorio = directorioActivo();
                                }while(directorio == null);
                            }
                            // dos.writeUTF("T");
                            break;
                        //------------------CAMBIAR DIRECTORIO ACTIVO------------------
                        case 4:
                            do{
                                directorio = seleccionarDirectorio();
                            }while(directorio == null);
                            break;
                        //------------------RECIBIR ARCHIVOS DESDE EL CLIENTE------------------
                        case 5:
                            //Checamos si es archivo o carpeta
                            String tipo = dis.readUTF();
                            // System.out.println("Tipo: " + tipo);
                            //Nombre del archivo o carpeta


                            if(tipo.equals("a") || tipo.equals("A")){ //Si es archivo
                                long tamArchivo = dis.readLong();
                                String nombreAC = dis.readUTF();

                                System.out.println("\nRecibiendo archivo: " + nombreAC + " del cliente.");
                                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(directorio, nombreAC)));

                                recibirArchivo(nombreAC, tamArchivo, bos, dis);
                                bos.close();
                                dos.writeUTF("T");
                                dos.flush();
                                System.out.println("\nArchivo recibido.");
                            }else if(tipo.equals("c") || tipo.equals("C")){ //Si es carpeta
                                String nombreAC = dis.readUTF();

                                System.out.println("\nRecibiendo carpeta: " + nombreAC + " del cliente.");
                                int tam = dis.readInt();
                                recibirCarpeta(directorio, nombreAC, dis, tam);
                                dos.writeUTF("T");
                                dos.flush();
                                System.out.println("\nCarpeta recibida.");
                            }
                            break;
                        //------------------ENVIAR ARCHIVOS AL CLIENTE------------------
                        case 6:
                            JFileChooser chooser = new JFileChooser(directorio.getAbsolutePath());
                            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                            chooser.setAcceptAllFileFilterUsed(false);
                            chooser.setCurrentDirectory(directorio);
                            int returnVal = chooser.showOpenDialog(null);

                            if(returnVal == JFileChooser.APPROVE_OPTION) {
                                File seleccionado = chooser.getSelectedFile();

                                if(seleccionado.isFile()){
                                    System.out.println("\nEnviando archivo " + seleccionado.getName() + " al cliente.");
                                    //Indicamos que es un archivo y enviamos el nombre
                                    dos.writeUTF("a");
                                    dos.writeLong(seleccionado.length());
                                    dos.writeUTF(seleccionado.getName());
                                    dos.flush();

                                    //Enviamos el archivo
                                    enviarArchivo(chooser.getSelectedFile(), dos);
                                    System.out.println("\nArchivo enviado.");
                                }else if(seleccionado.isDirectory()){
                                    System.out.println("\nEnviando carpeta " + seleccionado.getName() + " al cliente.");
                                    //Indicamos que es una carpeta y enviamos el nombre
                                    dos.writeUTF("c");
                                    dos.writeUTF(seleccionado.getName());
                                    dos.flush();

                                    //Enviamos la carpeta
                                    enviarCarpeta(seleccionado, dos);
                                    System.out.println("\nCarpeta enviada.");
                                }

                                //Esperamos confirmación de recibido del servidor
                                String conf = dis.readUTF();
                                if(conf.equals("T")) dos.writeUTF("Mandado");
                            } else {
                                dos.writeUTF("U");
                                dos.writeUTF("Abortado");}
                            break;
                        case 7:
                            System.out.println("\nCerrando Conexion...");
                            salir = true;
                            dos.flush();
                            dos.close();
                            dis.close();
                            break;
                        default:
                            System.out.println("\n---OPCION INVALIDA---");
                            break;
                    }

                    dos.flush();
                }

                cl.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        } finally {
            try{
            s.close();
            }catch(Exception e2){}
        }
    }

    public static File seleccionarDirectorio() {
        // Ventana para escoger directorio
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccione directorio remoto.");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Sólo muestra directorios
        chooser.setAcceptAllFileFilterUsed(false);

        File directorio; // Directorio local

        // Selecciona directorio
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("\nCarpeta elegida: " + chooser.getSelectedFile());
            directorio = chooser.getSelectedFile();
        } else {
            System.out.println("\nNo se seleccionó un directorio: Terminando programa...");
            return null;
        }

        return directorio;
    }

    public static File directorioActivo(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccione nuevo directorio activo.");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Sólo muestra directorios
        chooser.setAcceptAllFileFilterUsed(false);

        // Selecciona directorio
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("\nCarpeta elegida: " + chooser.getSelectedFile());
            return chooser.getSelectedFile();
        } else {
            System.out.println("\nNo se seleccionó un directorio: Terminando programa...");
            return null;
        }
    }

    public static void listarContenido(File direc, int profundidad, StringBuilder hastaAhora) {
        try {
            File[] listOfFiles = direc.listFiles();
            String espacios = "";
            for(int i = 0; i < profundidad; i++) espacios += "\t";
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    hastaAhora.append(espacios + listOfFile.getName() + "\n");
                } else if (listOfFile.isDirectory()) {
                    hastaAhora.append(espacios + listOfFile.getName() + "/" + "\n");
                    listarContenido(listOfFile, profundidad + 1, hastaAhora);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void crearArchivo(File direc, String nombre) {
        try {
            File nuevoArchivo = new File(direc + "\\" + nombre);
            if (nuevoArchivo.createNewFile()) {
              System.out.println("\nArchivo creado: " + nuevoArchivo.getName());
            } else {
              System.out.println("\nEl archivo ya existe.");
            }
        } catch (IOException e) {
            System.out.println("\nOcurrió un error.");
            e.printStackTrace();
        }
    }

    public static void crearCarpeta(File direc, String nombre) {
        new File(direc + "\\" + nombre).mkdirs();
    }

    public static void eliminarArchivo(File direc, DataInputStream dis, DataOutputStream dos) throws IOException{
        JFileChooser chooser = new JFileChooser(direc.getAbsolutePath());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(direc);

        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            dos.writeUTF("\n¿Seguro que desea eliminar " + chooser.getSelectedFile().getName() + "? (s/n)");
            dos.flush();

            String seleccion = "";
            seleccion = dis.readUTF();
            // System.out.println("Seleccion: " + seleccion);
            // Scanner lectura = new Scanner(System.in);  // Create a Scanner object
            // seleccion = lectura.nextLine();  // Leer entrada

            if(!seleccion.equals("s") && !seleccion.equals("S")) {
                // System.out.println("\n---OPERACION ABORTADA---");
                dos.writeUTF("U");
                return;
            }

            try {
                Files.delete(chooser.getSelectedFile().toPath());
                // System.out.println("Eliminación completada");
                dos.writeUTF("T");
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", chooser.getSelectedFile().toPath());
            } catch (IOException x) {
                System.err.println(x);
            }
        }
    }

    // FUNCIÓN AUXILIAR
    private static boolean isDirEmpty(final Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static void eliminarCarpeta(File direc, DataInputStream dis, DataOutputStream dos) throws IOException{
        dos.writeUTF("\n¿Seguro que desea eliminar la carpeta activa: " + direc.getName() + "? (s/n)");
        dos.flush();

        String seleccion = "";
        seleccion = dis.readUTF();

        // Scanner lectura = new Scanner(System.in);  // Create a Scanner object
        // seleccion = lectura.nextLine();  // Leer entrada

        if(!seleccion.equals("s") && !seleccion.equals("S")) {
            // System.out.println("Eliminación abortada");
            dos.writeUTF("U");
            return;
        }

        eliminaCarpeta(direc);
        dos.writeUTF("T");
    }

    public static void eliminaCarpeta(File direc) {
        try {
            if(!isDirEmpty(direc.toPath())) {
                File[] listOfFiles = direc.listFiles();
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        Files.delete(listOfFile.toPath());
                    } else if (listOfFile.isDirectory()) {
                        eliminaCarpeta(listOfFile);
                    }
                }
            }
            Files.delete(direc.toPath());
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", direc.toPath());
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", direc.toPath());
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    private static void recibirArchivo(String nombreAC, long tamArchivo, BufferedOutputStream bos, DataInputStream dis) throws IOException{
        byte[] buffer = new byte[1024];
        long bytesLectura = 0;
         System.out.println("\nEntrando a recibir archivo\n");

        while (bytesLectura < tamArchivo) {
            //Obtenemos el numero de bytes a leer comparando el tamaño del buffer con el tamaño del archivo
            int bytesALeer = (int) Math.min(buffer.length, tamArchivo - bytesLectura);
            int bytesRecibidos = dis.read(buffer, 0, bytesALeer);
            //checamos si se termino de leer el archivo
            if(bytesRecibidos == -1){
                break;
            }
            System.out.println("Recibiendo archivo: " + nombreAC + " con "+ bytesLectura + " bytes");
            //Aumentamos el numero de bytes leidos
            bytesLectura += bytesRecibidos;
            //Escribimos los bytes leidos en el archivo
            bos.write(buffer, 0, bytesRecibidos);
        }
        bos.flush();
    }

    private static void recibirCarpeta(File direc, String nombreCarpeta, DataInputStream dis, int tam) throws IOException{
        System.out.println("\nRecibiendo carpeta: " + nombreCarpeta + " Con tamaño: " + tam);
        File nuevaCarpeta = new File(direc + "\\" + nombreCarpeta);
        nuevaCarpeta.mkdirs();
        direc = nuevaCarpeta;

        int i = 0;
        while(i < tam){
            String tipo = dis.readUTF();
            if(tipo.equals("a")){ //Si es archivo
                //leemos el nombre del archivo y su tamaño en bytes
                long tamArchivo = dis.readLong();
                String nombreAC = dis.readUTF();
                // System.out.println("Entrando a recibir archivo con: " + nombreAC);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(nuevaCarpeta, nombreAC)));
                recibirArchivo(nombreAC, tamArchivo, bos, dis);
                // System.out.println("xdxdxdxd");

                bos.close();
            }else if(tipo.equals("c")){
                String nombreAC = dis.readUTF();
                // System.out.println("Entrando en recursion con: " + nombreAC);
                int tamC = dis.readInt();
                recibirCarpeta(direc, nombreAC, dis, tamC);
            }
            i++;
        }
        System.out.println("\nCarpeta creada: " + nombreCarpeta);
    }

    private static void enviarArchivo(File archivo, DataOutputStream dos) throws IOException{
        byte[] buffer = new byte[1024];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivo));
        int bytesLectura;
        while((bytesLectura = bis.read(buffer)) != -1){
            System.out.println("Enviando archivo: " + archivo.getName() + " con " + bytesLectura + " bytes");
            dos.write(buffer, 0, bytesLectura);
            // dos.flush();
        }
        // System.out.println("xdxdxdxd");
        bis.close();
    }

    private static void enviarCarpeta(File carpeta, DataOutputStream dos) throws IOException{
        File[] archivos = carpeta.listFiles();
        dos.writeInt(archivos.length);

        for(File archivo : archivos){
            if(archivo.isFile()){
                System.out.println("Enviando archivo " + archivo.getName() + " de la carpeta " + carpeta.getName());
                dos.writeUTF("a");
                //Escribimos el tamaño del archivo en bytes
                dos.writeLong(archivo.length());
                dos.writeUTF(archivo.getName());
                dos.flush();

                enviarArchivo(archivo, dos);
            }else if(archivo.isDirectory()){
                System.out.println("Enviando carpeta " + archivo.getName() + " de la carpeta " + carpeta.getName());
                dos.writeUTF("c");
                dos.writeUTF(archivo.getName());
                dos.flush();

                enviarCarpeta(archivo, dos);
            }
        }
    }
}
