

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class Driver {
    String nodeip;
    String ovsnodeport;
    String ovsmsg;

    public Driver(String nodeip, String ovsmsg) {
        this.nodeip = nodeip;
        this.ovsmsg = ovsmsg;
    }

    public String getNodeip() {
        return nodeip;
    }

    public void setNodeip(String nodeip) {
        this.nodeip = nodeip;
    }

    public String getOvsnodeport() {
        return ovsnodeport;
    }

    public void setOvsnodeport(String ovsnodeport) {
        this.ovsnodeport = ovsnodeport;
    }

    public String getOvsmsg() {
        return ovsmsg;
    }

    public void setOvsmsg(String ovsmsg) {
        this.ovsmsg = ovsmsg;
    }


    private static void LaunchAwesomeHere() {

        final Logger log = getLogger(ServerHandler.class);

        int serverport = 6634;
        Server server = new Server(serverport);
        server.start();

        String clienthost = "192.168.153.128";
        int clientport = 6634;

        Client client = new Client(clienthost, clientport);
        client.start();


        log.info("LaunchAwesomeHere() : processing goes here...");

        //client.write("{ \"method\": \"echo\", \"params\": [], \"id\": \"echo\"}");
        //String echoReply = "{ \"result\": [], \"id\": \"echo\"} ";
        client.write("{\"method\":\"monitor\",\"id\":0,\"params\":[\"Open_vSwitch\"," +
                "null,{\"Port\":{\"columns\":[\"interfaces\",\"name\",\"tag\",\"trunks\"]}," +
                "\"Controller\":{\"columns\":[\"is_connected\",\"target\"]},\"Interface\":" +
                "{\"columns\":[\"name\",\"options\",\"type\"]},\"Open_vSwitch\":{\"columns\":" +
                "[\"bridges\",\"cur_cfg\",\"manager_options\",\"ovs_version\"]},\"Manager\":" +
                "{\"columns\":[\"is_connected\",\"target\"]},\"Bridge\":{\"columns\":" +
                "[\"controller\",\"fail_mode\",\"name\",\"ports\"]}}]}");

        ObjectMapper mapper = new ObjectMapper();
        EchoReplyPojo echoreply = new EchoReplyPojo();
        EchoRequestPojo echorequest = new EchoRequestPojo();

        echorequest.setMethod("echo");
        echorequest.setId("echo");
        echoreply.setId("echo");
        try {
            String erequest = mapper.writeValueAsString(echorequest);
            String ereply = mapper.writeValueAsString(echoreply);
            System.out.println(erequest);
            System.out.println(ereply);
            client.write(erequest);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
/*
         Stop Services
         client.stop();
         server.stop();
*/
        //  client.stop();
    }

    public static void main(String[] args) {
        //  Client exObjectMain = new Client();
        LaunchAwesomeHere();
    }
}

