package com.maciasrazo.practica1;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;
import javax.swing.JFileChooser;

public class Practica1 {
    public static void main(String[] args) {
        
        // Ventana para escoger directorio
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccione directorio");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Sólo muestra directorios
        chooser.setAcceptAllFileFilterUsed(false);
        
        File directorio; // Directorio local

        // Selecciona directorio
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Carpeta elegida: " + chooser.getSelectedFile());
            directorio = chooser.getSelectedFile();
        } else {
            System.out.println("No se seleccionó un directorio: Terminando programa");
            return;
        }
        
        String seleccion = "";
        Scanner lectura = new Scanner(System.in);  // Create a Scanner object

        while(true) {
            System.out.println("\n\n\n***** OPCIONES *****");
            System.out.println("1. Listar contenido del directorio elegido");
            System.out.println("2. Crear Carpeta o Archivo");
            System.out.println("3. Eliminar carpeta o Archivo");
            System.out.println("4. Cambiar Ruta Elegida");
            System.out.println("5. Enviar Archivos/Carpetas Local a Remoto");
            System.out.println("6. Solicitar Envío de Archivos/Carpetas de Remoto a Local");
            System.out.println("7. Salir");
            
            seleccion = lectura.nextLine();  // Leer entrada
            
            if(seleccion.equals("7")) {
                System.out.println("Hasta la vista baby");
                return;
            }
            
            if(seleccion.equals("1")) listarContenido(directorio, 0);
            else if(seleccion.equals("2")) {
                System.out.println("¿Desea crear archivo o carpeta? (a/c)");
                seleccion = lectura.nextLine();  // Leer entrada
                if(seleccion.equals("a")) crearArchivo(directorio);
                else if(seleccion.equals("c")) crearCarpeta(directorio);
                else System.out.println("Seleccione una opción válida");
            }
            else if(seleccion.equals("3")) {
                System.out.println("¿Desea eliminar archivo o carpeta? (a/c)");
                seleccion = lectura.nextLine();  // Leer entrada
                if(seleccion.equals("a")) eliminarArchivo(directorio);
                else if(seleccion.equals("c")) {
                    eliminarCarpeta(directorio);
                    
                    chooser = new JFileChooser();
                    chooser.setDialogTitle("Seleccione nuevo directorio activo");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Sólo muestra directorios
                    chooser.setAcceptAllFileFilterUsed(false);

                    // Selecciona directorio
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        System.out.println("Carpeta elegida: " + chooser.getSelectedFile());
                        directorio = chooser.getSelectedFile();
                    } else {
                        System.out.println("No se seleccionó un directorio: Terminando programa");
                        return;
                    }
                }
                else System.out.println("Seleccione una opción válida");
            }
            else if(seleccion.equals("4")) {
                chooser = new JFileChooser();
                chooser.setDialogTitle("Seleccione directorio");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Sólo muestra directorios
                chooser.setAcceptAllFileFilterUsed(false);

                // Selecciona directorio
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    System.out.println("Carpeta elegida: " + chooser.getSelectedFile());
                    directorio = chooser.getSelectedFile();
                } else {
                    System.out.println("No se seleccionó un directorio: Terminando programa");
                    return;
                }
            }
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
    
    public static void crearArchivo(File direc) {
        try {
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            System.out.println("De el nombre del archivo a crear (con extensión):");

            String nombreArc = myObj.nextLine();  // Read user input
            File nuevoArchivo = new File(direc + "\\" + nombreArc);
            if (nuevoArchivo.createNewFile()) {
              System.out.println("File created: " + nuevoArchivo.getName());
            } else {
              System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    public static void crearCarpeta(File direc) {
        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println("De el nombre de la carpeta a crear:");

        String nombreCarp = myObj.nextLine();  // Read user input
        new File(direc + "\\" + nombreCarp).mkdirs();
    }
    
    public static void eliminarArchivo(File direc) {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("Seguro que desea eliminar " +
                    chooser.getSelectedFile().getName() + "? (s/n)");
            
            String seleccion = "";
            Scanner lectura = new Scanner(System.in);  // Create a Scanner object
            seleccion = lectura.nextLine();  // Leer entrada
            
            if(!seleccion.equals("s")) {
                System.out.println("Eliminación abortada");
                return;
            }
            
            try {
                Files.delete(chooser.getSelectedFile().toPath());
                System.out.println("Eliminación completada");
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", chooser.getSelectedFile().toPath());
            } catch (IOException x) {
                System.err.println(x); // File permission problems are caught here.
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
        System.out.println("Seguro que desea eliminar la carpeta activa? (s/n)");
            
        String seleccion = "";
        Scanner lectura = new Scanner(System.in);  // Create a Scanner object
        seleccion = lectura.nextLine();  // Leer entrada

        if(!seleccion.equals("s")) {
            System.out.println("Eliminación abortada");
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
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", direc.toPath());
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", direc.toPath());
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
    }
}
