package com.maciasrazo.practica1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;
import javax.swing.JFileChooser;

public class Cliente {
    public static void main(String args[]) {
        
        File directorio;
        do {
            directorio = seleccionarDirectorio();
        }
        while(directorio == null);
        
        boolean salir = true;
        
        try{
            String dst = "127.0.0.1";
            int pto = 1234;
            Socket cl = new Socket(dst,pto);
            DataInputStream dis = new DataInputStream(cl.getInputStream());
            DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
            
            String seleccion = "";
            Scanner lectura = new Scanner(System.in);  // Create a Scanner object

            while(salir) {
                System.out.println("\n\n***** OPCIONES *****");
                System.out.println("1. Listar contenido del directorio elegido");
                System.out.println("2. Crear Carpeta o Archivo");
                System.out.println("3. Eliminar carpeta o Archivo");
                System.out.println("4. Cambiar Ruta Elegida");
                System.out.println("5. Enviar Archivos/Carpetas Local a Remoto");
                System.out.println("6. Solicitar Envío de Archivos/Carpetas de Remoto a Local");
                System.out.println("7. Salir");
                
                seleccion = lectura.nextLine();  // Leer entrada

                //------------------SALIR------------------
                if(seleccion.equals("7")) {
                    System.out.println("\n\nSaliendo de la aplicación...");
                    dos.writeInt(7);
                    dos.flush();
                    dos.close();
                    dis.close();
                    cl.close();
                    lectura.close();
                    return;
                }
                
                //------------------LISTAR CONTENIDO------------------
                if(seleccion.equals("1")) {
                    System.out.println("\n¿Remoto o Local? (r/l)");
                    seleccion = lectura.nextLine();  // Leer entrada
                    System.out.println("\nLista de archivos y carpetas:");
                    if(seleccion.equals("r") || seleccion.equals("R")) {
                        dos.writeInt(1);
                        dos.flush();
                        
                        long tamArchivo = dis.readLong();
                        String nombreAC = dis.readUTF();

                        File arch = new File(directorio, nombreAC);
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(arch));
                        recibirArchivo(nombreAC, tamArchivo, bos, dis);

                        bos.close();
                        // dos.writeUTF("T");
                        // dos.flush();

                        try (BufferedReader br = new BufferedReader(new FileReader(directorio + "\\" + "temp.txt"))){
                            String listado;
                            while((listado = br.readLine()) != null){
                                System.out.println(listado);
                            }
                        }catch (Exception e) {
                            // TODO: handle exception
                        }
                        
                        try {
                            Files.delete(arch.toPath());
                        } catch (NoSuchFileException x) {
                            // System.err.format("%s: no such" + " file or directory%n", chooser.getSelectedFile().toPath());
                        } catch (IOException x) {
                            System.err.println(x); 
                        }
                    } else if(seleccion.equals("l") || seleccion.equals("L")) {
                        listarContenido(directorio, 0);
                    } else {
                        System.out.println("\n---OPCION INVALIDA---");
                    }
                }

                //------------------CREAR CARPETA O ARCHIVO------------------
                else if(seleccion.equals("2")) {
                    System.out.println("\n¿Desea crear archivo o carpeta? (a/c)");
                    seleccion = lectura.nextLine();  // Leer entrada
                    String nombre;
                    String rol;
                    
                    if(seleccion.equals("a") || seleccion.equals("A")) {
                        System.out.println("De el nombre del archivo a crear (con extensión): ");
                        nombre = lectura.nextLine();
                    } else if(seleccion.equals("c") || seleccion.equals("C")) {
                        System.out.println("De el nombre de la carpeta a crear: ");
                        nombre = lectura.nextLine();
                    } else {
                        System.out.println("\n---OPCION INVALIDA---");
                        continue;
                    }
                    
                    System.out.println("\n¿Remoto o Local? (r/l)");
                    rol = lectura.nextLine();  // Leer entrada
                    if(rol.equals("r") || rol.equals("R")) {
                        dos.writeInt(2);
                        dos.writeUTF(seleccion);
                        dos.writeUTF(nombre);
                        String conf = dis.readUTF();
                        if(conf.equals("T")) System.out.println("\n---OPERACION COMPLETADA---");
                        
                        dos.flush();
                    } else if(rol.equals("l") || rol.equals("L")) {
                        if(seleccion.equals("a") || seleccion.equals("A")) {
                            crearArchivo(directorio, nombre);
                        } else {
                            crearCarpeta(directorio, nombre);
                        }
                        System.out.println("\n---OPERACION COMPLETADA---");
                    } else {
                        System.out.println("\n---OPCION INVALIDA---");
                    }
                }

                //------------------ELIMINAR CARPETA O ARCHIVO------------------
                else if(seleccion.equals("3")){
                    System.out.println("\n¿Desea eliminar archivo o carpeta? (a/c)");
                    seleccion = lectura.nextLine();  // Leer entrada
                    String nombre;
                    String rol;

                    if(!seleccion.equals("a") && !seleccion.equals("c")) {
                        System.out.println("\n---OPCION INVALIDA---");
                        continue;
                    }

                    System.out.println("\n¿Remoto o Local? (r/l)");
                    rol = lectura.nextLine();  // Leer entrada

                    if(rol.equals("r") || rol.equals("R")) {
                        dos.writeInt(3);
                        dos.writeUTF(seleccion);

                        //Mostramos la pregunta de confirmación
                        String pregunta = dis.readUTF();
                        System.out.println(pregunta);
                        pregunta = "";
                        pregunta = lectura.nextLine();  // Leer entrada
                        // System.out.println("Respuesta: " + pregunta);
                        dos.writeUTF(pregunta);
                        dos.flush();

                        String conf = dis.readUTF();
                        if(conf.equals("T")) {System.out.println("\n---OPERACION COMPLETADA---");}
                        if(conf.equals("U")) {System.out.println("\n---OPERACION ABORTADA---");}
                        
                        dos.flush();
                    }else if(rol.equals("l") || rol.equals("L")) {
                        if(seleccion.equals("a")) eliminarArchivo(directorio);
                        else if(seleccion.equals("c")){
                            eliminarCarpeta(directorio);
                            do{
                                directorio = directorioActivo();
                            }while(directorio == null);
                        }
                        else System.out.println("\nSe ha seleccionado una opción inválida.");
                    }else{
                        System.out.println("\n---OPCION INVALIDA---");
                    }
                }

                //------------------CAMBIAR RUTA ELEGIDA------------------
                else if(seleccion.equals("4")) {
                    System.out.println("\n¿Desea camabiar de directorio Remoto o Local? (r/l)");
                    seleccion = lectura.nextLine();  // Leer entrada

                    if(seleccion.equals("r") || seleccion.equals("R")) {
                        dos.writeInt(4);
                        dos.flush();
                    } else if(seleccion.equals("l") || seleccion.equals("L")) {
                        do{
                            directorio = seleccionarDirectorio();
                        }while(directorio == null);
                    } else {
                        System.out.println("\n---OPCION INVALIDA---");
                    }
                }

                //------------------ENVIAR ARCHIVOS/CARPETAS LOCAL A REMOTO------------------
                else if(seleccion.equals("5")) {
                    dos.writeInt(5);
                    dos.flush();
                    
                    JFileChooser chooser = new JFileChooser(directorio.getAbsolutePath());
                    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    chooser.setAcceptAllFileFilterUsed(false);
                    chooser.setCurrentDirectory(directorio);
                    int returnVal = chooser.showOpenDialog(null);

                    if(returnVal == JFileChooser.APPROVE_OPTION) {
                        File seleccionado = chooser.getSelectedFile();

                        if(seleccionado.isFile()){
                            System.out.println("\nEnviando archivo " + seleccionado.getName() + " al servidor.");
                            //Indicamos que es un archivo y enviamos el nombre
                            dos.writeUTF("a");
                            dos.writeLong(seleccionado.length());
                            dos.writeUTF(seleccionado.getName());
                            dos.flush();

                            //Enviamos el archivo
                            enviarArchivo(chooser.getSelectedFile(), dos);
                            System.out.println("\nArchivo enviado.");
                        }else if(seleccionado.isDirectory()){
                            System.out.println("\nEnviando carpeta " + seleccionado.getName() + " al servidor.");
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
                        if(conf.equals("T")) System.out.println("\n---OPERACION COMPLETADA---");
                    } else {System.out.println("\n---OPERACION ABORTADA---");
                        dos.writeUTF("U");
                    }
                }
                
                //------------------SOLICITAR ENVÍO DE ARCHIVOS/CARPETAS DE REMOTO A LOCAL------------------
                else if(seleccion.equals("6")) {
                    dos.writeInt(6);
                    dos.flush();
                    
                    //Checamos si es archivo o carpeta
                    String tipo = dis.readUTF();
                    // System.out.println("Tipo: " + tipo);

                    if(tipo.equals("a")){ //Si es archivo
                        long tamArchivo = dis.readLong();
                        String nombreAC = dis.readUTF();

                        System.out.println("\nRecibiendo archivo: " + nombreAC + " del servidor.");
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(directorio, nombreAC)));
                        recibirArchivo(nombreAC, tamArchivo, bos, dis);
                        bos.close();
                        dos.writeUTF("T");
                        dos.flush();
                        System.out.println("\nArchivo recibido.");
                    }else if(tipo.equals("c")){ //Si es carpeta
                        String nombreAC = dis.readUTF();

                        System.out.println("\nRecibiendo carpeta: " + nombreAC + " del servidor.");
                        int tam = dis.readInt();
                        recibirCarpeta(directorio, nombreAC, dis, tam);
                        dos.writeUTF("T");
                        dos.flush();
                        System.out.println("\nCarpeta recibida.");
                    }

                    //Esperamos confirmación
                    String conf = dis.readUTF();
                    if(conf.equals("Mandado")) System.out.println("\n---OPERACION COMPLETADA---");
                    if(conf.equals("Abortado")) System.out.println("\n---OPERACION ABORTADA---");
                }
                //------------------OPCION INVALIDA------------------
                else{
                    System.out.println("\n---OPCION INVALIDA---");
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static File seleccionarDirectorio() {
        // Ventana para escoger directorio
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccione directorio local.");
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
    
    public static void listarContenido(File direc, int profundidad) {
        try {
            File[] listOfFiles = direc.listFiles();
            String espacios = "";
            for(int i = 0; i < profundidad; i++) espacios += "\t";
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    System.out.println(espacios + listOfFile.getName());
                } else if (listOfFile.isDirectory()) {
                    System.out.println(espacios + listOfFile.getName() + "/");
                    listarContenido(listOfFile, profundidad + 1);
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
            System.out.println("\nOcurrió un Error.");
            e.printStackTrace();
        }
    }
    
    public static void crearCarpeta(File direc, String nombre) {
        new File(direc + "\\" + nombre).mkdirs();
    }

    public static void eliminarArchivo(File direc) {
        JFileChooser chooser = new JFileChooser(direc.getAbsolutePath());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(direc);

        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("\n¿Seguro que desea eliminar " +
                    chooser.getSelectedFile().getName() + "? (s/n)");
            
            String seleccion = "";
            Scanner lectura = new Scanner(System.in);  // Create a Scanner object
            seleccion = lectura.nextLine();  // Leer entrada
            
            if(!seleccion.equals("s")) {
                System.out.println("\n---OPERACION ABORTADA---");
                return;
            }
            
            try {
                Files.delete(chooser.getSelectedFile().toPath());
                System.out.println("\n---OPERACION COMPLETADA---");
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
    
    public static void eliminarCarpeta(File direc) {
        System.out.println("\n¿Seguro que desea eliminar la carpeta activa? (s/n)");
            
        String seleccion = "";
        Scanner lectura = new Scanner(System.in);  // Create a Scanner object
        seleccion = lectura.nextLine();  // Leer entrada

        if(!seleccion.equals("s")) {
            System.out.println("\n---OPERACION ABORTADA---");
            return;
        }
        
        eliminaCarpeta(direc);
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
            System.out.println("\n---OPERACION COMPLETADA---");
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", direc.toPath());
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", direc.toPath());
        } catch (IOException x) {
            System.err.println(x);
        }
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
}
