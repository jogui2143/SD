import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;

public class Downloader {
    private static final String MULTICAST_ADDRESS = "225.1.2.3";
    private static final int MT_PORT = 7002;

    private static GatewayInterface gateway;

    public static void main(String[] args) {
        try {
            Registry reg = LocateRegistry.getRegistry("localhost");
            gateway = (GatewayInterface) reg.lookup("Gateway");

            String url;
            while(true){
                url = gateway.getNewUrl();
                if(url != null){
                    System.out.println("URL: " + url);
                    startCrawling(url);
                } else{
                    System.out.println("No new URL to crawl");
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            System.err.println("Exception on downloader" + e.toString());
            e.printStackTrace();
        }
    }
    
    private static void startCrawling(String url){
        try{
            Document doc = Jsoup.connect(url).get();

            System.out.println("Processing URL: " + url);
            Elements links = doc.select("a[href]");
            for(Element link : links){
                String newUrl = link.attr("abs:href");
                gateway.queueUpUrl(newUrl);
            }
            String title = doc.title();
            String text = doc.body().text();
            PageContent info = new PageContent(title, text, url);

            sendInfo(info);

        } catch (IOException e){
            System.err.println("Exception on startCrawling" + e.toString());
            e.printStackTrace();
        } catch (Exception e){
            System.err.println("Error queueing up :) CARALHO" + e.toString());
            e.printStackTrace();
        }
    }

    private static final int MAX_PACKET_SIZE = 65000; //datagram packet size (só tem isso bruh :))))))) )
    private static int msgId = 0; // hummm uncle roger, msg == make shit good

private static void sendInfo(PageContent info) throws IOException{
    InetAddress gpAddress = InetAddress.getByName(MULTICAST_ADDRESS);
    NetworkInterface netInterface = NetworkInterface.getNetworkInterfaces().nextElement();
    msgId++; //vai contando as msgs 😳

    try(ByteArrayOutputStream byteChad = new ByteArrayOutputStream();
        GZIPOutputStream outZip = new GZIPOutputStream(byteChad);
        ObjectOutputStream oos = new ObjectOutputStream(outZip);){

        oos.writeObject(info);
        oos.close();
        byte[] data = byteChad.toByteArray();

        int allParts = (data.length + MAX_PACKET_SIZE -1)/ MAX_PACKET_SIZE;
        try (MulticastSocket socket = new MulticastSocket()){
            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF,netInterface);
            for(int i = 0; i < allParts; i++){
                int start = i * MAX_PACKET_SIZE;
                int end = Math.min(data.length, (i+1)*MAX_PACKET_SIZE);
                byte[] section = Arrays.copyOfRange(data, start, end);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(msgId);
                dos.writeInt(i);
                dos.writeInt(allParts);
                dos.write(section);
                byte[] packData = baos.toByteArray();

                DatagramPacket packet = new DatagramPacket(packData, packData.length, gpAddress, MT_PORT);
                socket.send(packet);
            }
        }
    }
}


} 
