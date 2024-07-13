package com.mycompany.practica2;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import javax.swing.JFileChooser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javafx.scene.chart.XYChart.Data;



public class Cliente {
    private static final String IP = "127.0.0.1";
    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        try {
            Scanner lectura = new Scanner(System.in);
            System.out.println("Ingrese el tamaño del paquete: ");
            int tamPaquete = lectura.nextInt();
            System.out.println("Ingrese el tamaño de la ventana: ");
            int tamVentana = lectura.nextInt();

            DatagramSocket cS = new DatagramSocket();
            InetAddress sA = InetAddress.getByName(IP);

            //Se envia el tamaño del paquete y ventana al servidor
            enviarTams(cS, sA, PUERTO, tamPaquete, tamVentana);

            //Se espera la confirmación del servidor de recibido
            byte[] datoRecibido = new byte[1];
            DatagramPacket packRecibido = new DatagramPacket(datoRecibido, datoRecibido.length);
            cS.receive(packRecibido);

            if(datoRecibido[0] == 1){
                System.out.println("Conexión establecida con el servidor...");
                System.out.println("Confirmación recibida.");
            } else {
                System.out.println("Error en la confirmación.");
                cS.close();
                lectura.close();
                return;
            }

            //Se envia el archivo al servidor
            enviarArchivo(cS, sA, PUERTO, tamPaquete, tamVentana);

            cS.close();
            lectura.close();

            System.out.println("Archivo enviado al servidor.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void enviarTams(DatagramSocket cS, InetAddress aS, int puertoSer, int tamPaquete, int tamVentana) throws IOException {
        //Se envia el tamaño del paquete y ventana al servidor como cadena, pero en bytes
        byte[] tamVentanaD = (tamPaquete + "," + tamVentana).getBytes();
        DatagramPacket tamVentanaP = new DatagramPacket(tamVentanaD, tamVentanaD.length, aS, puertoSer);
        cS.send(tamVentanaP);
    }

    private static void enviarArchivo(DatagramSocket cS, InetAddress aS, int puertoSer, int tamPaquete, int tamVentana) throws IOException {
        
        JFileChooser chooser = new JFileChooser();
        chooser.requestFocus();
        chooser.setDialogTitle("Seleccione archivo a enviar.");

        int returnVal;
        
        do {
            returnVal = chooser.showOpenDialog(null);
        } while(returnVal != JFileChooser.APPROVE_OPTION);
        
        File archivo = chooser.getSelectedFile();
        
        //Se envia el tamaño del archivo al servidor
        String tamArchivoStr = String.valueOf(archivo.length());
        byte[] tamArchivoBytes = tamArchivoStr.getBytes();
        DatagramPacket packEnviar = new DatagramPacket(tamArchivoBytes, tamArchivoBytes.length, aS, puertoSer);
        cS.send(packEnviar);
        
        //Se envía el nombre y extensión
        byte[] nombreArchBytes = archivo.getName().getBytes("UTF-8");
        DatagramPacket packNombre = new DatagramPacket(nombreArchBytes, nombreArchBytes.length, aS, puertoSer);
        cS.send(packNombre);
        

        //Se envia el archivo al servidor
        byte[] buffer = new byte[tamPaquete];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivo));

        //ArrayList para almacenar los paquetes de la ventana
        ArrayList<byte[]> PacksVentana = new ArrayList<>();
        int i = 1;
        int bytesLectura = 0;
        //Ciclo para obtener los paquetes del archivo
        while((bytesLectura = bis.read(buffer)) != -1){
            //Agregamos el paquete al ArrayList de la ventana
            PacksVentana.add(Arrays.copyOf(buffer, bytesLectura));
            //Checamos si la ventana está llena
            if(PacksVentana.size() == tamVentana){
                boolean salir=true;
                //Ciclo para enviar la ventana completa
                while(salir){
                    System.out.println("Enviando ventana " + i + " de " + tamVentana + " paquetes");
                    enviarVentana(cS, aS, puertoSer, PacksVentana);
                    //Checamos que lleguen todos los ACKs
                    if(cacharACKs(PacksVentana.size(), cS)){
                        System.out.println("ACKs recibidos correctamente (1)");
                        salir = false;
                    }else{
                        System.out.println("No se recibieron todos los ACKs, reenviando ventana");
                    }
                }
                //Limpiamos la ventana
                PacksVentana.clear();
                i++;
            }
        }

        //Enviamos los paquetes restantes
        if(!PacksVentana.isEmpty()){
            System.out.println("Entrando a paquetes restantes");
            boolean salir=true;
            //Ciclo para enviar la ventana completa
            while(salir){
                System.out.println("Enviando ventana " + i + " de " + tamVentana + " paquetes");
                enviarVentana(cS, aS, puertoSer, PacksVentana);
                //Checamos que lleguen todos los ACKs
                if(cacharACKs(PacksVentana.size(), cS)){
                    System.out.println("ACKs recibidos correctamente (2)");
                    salir = false;
                }else{
                    System.out.println("No se recibieron todos los ACKs, reenviando ventana");
                }
            }
            PacksVentana.clear();
        }
        bis.close();
    }

    private static void enviarVentana(DatagramSocket cS, InetAddress aS, int puertoSer, ArrayList<byte[]> PacksVentana) throws IOException {
        for(int i = 0; i < PacksVentana.size(); i++){

            Random rn = new Random();
            int n = rn.nextInt(1000); // posibilidad de 1 en 1000 de fallar
            if(n > 0){
                String numPaquete = String.valueOf(i+1);
                byte[] bytesNumPacket = numPaquete.getBytes();
                DatagramPacket packEnviar = new DatagramPacket(bytesNumPacket, bytesNumPacket.length, aS, puertoSer);
                cS.send(packEnviar);

                byte[] datos = PacksVentana.get(i);
                DatagramPacket packDatos = new DatagramPacket(datos, datos.length, aS, puertoSer);
                cS.send(packDatos);

                System.out.println("Enviando paquete: " + numPaquete + " con un tamaño de: " + datos.length + " bytes");
            }
        }

        String numPaquete = String.valueOf(PacksVentana.size() + 1);
        byte[] bytesNumPacket = numPaquete.getBytes();
        DatagramPacket packEnviar = new DatagramPacket(bytesNumPacket, bytesNumPacket.length, aS, puertoSer);
        cS.send(packEnviar);

        String fin = "FIN";
        byte[] finBytes = fin.getBytes();
        DatagramPacket packFin = new DatagramPacket(finBytes, finBytes.length, aS, puertoSer);
        cS.send(packFin);

        System.out.println("Enviando paquete FIN");
    }

    private static boolean cacharACKs(int numACKsEsperados, DatagramSocket cS) throws IOException {
        int ackNum = 0, aux = 0;
        cS.setSoTimeout(10000);
        while(true){
            byte[] ackBytes = new byte[1024];
            DatagramPacket ackPack = new DatagramPacket(ackBytes, ackBytes.length);
            cS.receive(ackPack);

            String ackJSON= new String(ackPack.getData(), 0, ackPack.getLength());
            JSONParser parser = new JSONParser();

            try{
                JSONObject ackObj = (JSONObject) parser.parse(ackJSON);
                ackNum = Integer.parseInt(ackObj.get("numero_ack").toString());
                
                if((ackNum == 0) && ackObj.get("contenido").equals("VACIO")){
                    System.out.println("ACK recibido: VACIO");
                    return false;
                } else if(ackObj.get("contenido").equals("FIN")){
                    break;
                }

                System.out.println("ACK recibido: " + ackNum);
                aux = ackNum;
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        System.out.println("ACKs recibidos: " + aux);

        if(aux == numACKsEsperados){
            return true;
        } else {
            return false;
        }
    }
}

