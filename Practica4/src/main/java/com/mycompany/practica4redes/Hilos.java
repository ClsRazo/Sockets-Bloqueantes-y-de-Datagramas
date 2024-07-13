package com.mycompany.practica4redes;

import java.io.*;
import java.net.*;

public class Hilos implements Runnable {

    private URL url;
    private String directorio;

    public Hilos(URL url, String directorio) {
        this.url = url;
        this.directorio = directorio;
    }

    @Override
    public void run() {
        try {
            descargarRecurso(url, directorio);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void descargarRecurso(URL url, String directorio) throws IOException {
        //Conexión a la URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        System.out.println("URL recibida en el Hilo: " + url.toString());

        //Checamos si la URL existe en la página real
        int responseCode = connection.getResponseCode();
        //Si no, regresamos
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.out.println("Archivo no encontrado (HTTP " + responseCode + "): " + url.toString());
            return;
        }

        //Si sí, descargamos el archivo
        System.out.println("Descargando: " + url.toString() + " desde el hilo");

        //Con un BufferedInputStream leemos el contenido de la URL y lo guardamos en un archivo
        InputStream input = connection.getInputStream();
        BufferedInputStream in = new BufferedInputStream(input);

        //Guardamos el nombre del archivo
        String fileName = url.getFile();
        //Si la URL termina en /, entonces se le asigna index.html
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        if (fileName.equals("")) {
            fileName = "index.html";
        }
        
        //Si el directorio no existe, lo creamos
        File file = new File(directorio, fileName);
        File parentDir = file.getParentFile();
        //Checamos si el directorio tiene un directorio padre y si no existe
        if (parentDir != null && !parentDir.exists()) {
            //Si cumple ambas condiciones, creamos el directorio
            if (parentDir.mkdirs()) {
                System.out.println("Directorio creado: " + parentDir.getAbsolutePath());
            } else {
                throw new IOException("No se pudo crear el directorio: " + parentDir.getAbsolutePath());
            }
        }else{
            //Si ya existe, entonces no lo creamos y guardamos el recurso en el directorio
            // System.out.println("Directorio ya existe: " + parentDir.getAbsolutePath());
        }

        //Escribimos los datos del archivo
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }

        out.close();
        in.close();
    }
}
