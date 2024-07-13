package com.mycompany.practica4redes;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WGet {

    //HashSet para guardar las URL que ya fueron descargadas
    private Set<String> descargadas = ConcurrentHashMap.newKeySet();
    //URL base de la página que sirve para checar desde que ruta descargar y no regresarse a superiores (si es que se entrega así)
    private String urlBase;

    public void descargar(String urlString, String directorio, int numHilos, int nivelAnidamiento) throws IOException {
        //Asignamos la URL base
        this.urlBase = urlString;
        //Creamos el pool de hilos con la cantidad de hilos indicada
        ExecutorService pool = Executors.newFixedThreadPool(numHilos);
        //Iniciamos la descarga recursiva
        descargarRecursivo(urlString, directorio, pool, nivelAnidamiento);
        //Cerramos el pool de hilos
        pool.shutdown();
        //Esperamos a que todos los hilos terminen
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void descargarRecursivo(String urlString, String directorio, ExecutorService pool, int nivelAnidamiento) throws IOException {
        // Si el nivel de anidamiento es negativo o la URL ya fue descargada, salimos
        if (nivelAnidamiento < 0 || !descargadas.add(urlString)) {
            // System.out.println("URL ya descargada o nivel de anidamiento negativo: " + urlString);
            return;
        }

        //Creamos la URL a partir de la cadena dada
        URL url = new URL(urlString);
        System.out.println("URL a descargar en descargarRecursivo: " + url.toString());
        //Verificar si la URL existe en la página antes de crear un hilo para descargarla
        if (!existeURL(url)) {
            System.out.println("URL no existe (HTTP 404): " + url.toString());
            return;
        }

        //Si sí existe, empezamos a descargar la URL
        pool.execute(new Hilos(url, directorio));

        //Obtenemos el contenido de la URL
        String contenido = obtenerContenido(url);
        // System.out.println("Directorio a crear: " + directorio);

        //Extraemos los recursos de la URL
        List<URL> recursos = extraerRecursos(contenido, url);

        //Recorremos los recursos y los descargamos
        for (URL recurso : recursos) {
            //Checamos si el recurso es una subruta válida (Si es que se entrega así)
            if (!esSubrutaValida(recurso.toString())) {
                System.out.println("Recurso no valido: " + recurso.toString());
                continue;
            }
            //Checamos si el recurso es un directorio
            if (recurso.toString().endsWith("/")) {
                // System.out.println("Recurso es un directorio: " + recurso.toString());
                //Creamos el nuevo directorio con el nombre de la carpeta y no con la ruta completa, unicamente con el nombre de la carpeta
                String newDirectorio = directorio + "/" + obtenerNombreCarpeta(recurso.toString());
                
                // System.out.println("Directorio a crear nuevo: " + newDirectorio + " con recurso: " + recurso.toString());
                descargarRecursivo(recurso.toString(), newDirectorio, pool, nivelAnidamiento - 1);
                continue;
            }

            //Si no, entonces es archivo o recurso y se mantiene el directorio actual
            // System.out.println("Recurso es un archivo: " + recurso.toString() + " en directorio: " + directorio);
            descargarRecursivo(recurso.toString(), directorio, pool, nivelAnidamiento-1); //No sé si el nivel de anidamiento se etá trabajando bien así, creo que sí
        }
    }

    private boolean existeURL(URL url) {
        //Checamos si la URL existe
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    private String obtenerContenido(URL url) throws IOException {
        //Obtenemos el contenido de la URL con un BufferedReader y lo guardamos en un StringBuilder
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        
        //Regresamos el contenido en forma de cadena
        return content.toString();
    }

    private List<URL> extraerRecursos(String content, URL baseURL) throws MalformedURLException {
        System.out.println("Recibiendo URL base: " + baseURL.toString());
        // System.out.println("Recibiendo contenido: " + content);
        List<URL> recursos = new ArrayList<>();
        //Extraemos los recursos de la URL con Jsoup, que es una librería para extraer recursos de HTML haciendo web scraping
        Document doc = Jsoup.parse(content, baseURL.toString());

        //Extraemos los enlaces de la URL, tanto de src como de href
        Elements links = doc.select("[src], [href]");

        //Recorremos los enlaces y los guardamos en la lista de recursos
        for (Element element : links) {
            String src = element.hasAttr("src") ? element.attr("src") : element.attr("href");
            // System.out.println("Enlace encontrado: " + src);

            //Ignoraramos enlaces con caracteres raros en la URL y con \ y con http:// o https:// o ”
            if (!src.matches(".*[?;=&].*") && !src.contains("\\") && !src.contains("http://") && !src.contains("https://") && !src.contains("”")){
                System.out.println("Enlace encontrado y agregado: " + src);
                URL resourceUrl = new URL(baseURL, src);
                recursos.add(resourceUrl);
            }
        }

        return recursos;
    }

    private boolean esSubrutaValida(String urlString) {
        //Verificar si la URL no es una ruta superior de la ruta base¿?
        //Esto es por si la entregamos con que descargue todooooo, pero lo descarga en desorden, bueno, desde la raiz que le fue dada y se desordena poquito
        if(urlString.startsWith(this.urlBase) ){
            return true;
        }else 
        try {
            URL url = new URL(urlString);
            return url.getHost().equals(new URL(this.urlBase).getHost());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        //Si no, de esta forma hacemos que solo descargue lo que está en la misma ruta que la URL base
        // return urlString.startsWith(this.urlBase);
    }

    private String obtenerNombreCarpeta(String url) {
        //Obtener el nombre de las subcarpetas
        String[] urlSplit = url.split("/");
        return urlSplit[urlSplit.length - 1];
    }
}
