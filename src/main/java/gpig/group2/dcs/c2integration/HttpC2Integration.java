package gpig.group2.dcs.c2integration;

import co.j6mes.infra.srf.query.QueryResponse;
import co.j6mes.infra.srf.query.ServiceQuery;
import co.j6mes.infra.srf.query.SimpleServiceQuery;
import gpig.group2.dcs.C2DroneInterface;
import gpig.group2.dcs.ConnectionManager;
import gpig.group2.dcs.DCSDroneConnectionManager;
import gpig.group2.dcs.wrapper.ResponseWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by james on 24/05/2016.
 */
public class HttpC2Integration implements C2Integration {

    private String vuiPath = "";
    private String mapsPath = "";

    private boolean connectionUpVUI = false;
    private boolean connectionUpMaps = false;

    private PollC2Thread rp;

    final HttpC2Integration tthis;

    public HttpC2Integration() {

        tthis = this;

        rp = new PollC2Thread();
        Thread requestPoll = new Thread(rp);
        requestPoll.start();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {


                while(true) {

                    synchronized (tthis){
                        connectionUpMaps = false;

                        ServiceQuery sq = new SimpleServiceQuery();

                        QueryResponse qr = sq.query("c2","maps");
                        if(qr.Path!=null) {
                            connectionUpMaps = true;
                            mapsPath = "http://"+qr.IP+":"+qr.Port+"/"+qr.Path;
                        }

                        tthis.notify();
                    }


                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }





            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {

                    synchronized (tthis){
                        connectionUpVUI = false;

                        ServiceQuery sq = new SimpleServiceQuery();

                        QueryResponse qr = sq.query("c2","vui");
                        if(qr.Path!=null) {
                            connectionUpMaps = true;
                            vuiPath = "http://"+qr.IP+":"+qr.Port+"/"+qr.Path;
                        }

                        tthis.notify();
                    }



                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }





            }
        });

        t1.start();
        t2.start();

    }




    static Logger log = LogManager.getLogger();
    @Override
    public void sendDroneStatus(StatusWrapper msg) {

        String url = "";
        synchronized (tthis) {
            while(!connectionUpMaps) {
                try {
                    tthis.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            url = mapsPath+"push";
        }

        int responseCode = -1;
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            log.debug("Sending " + msg.getText());
            StringEntity params =new StringEntity(msg.getText(),"UTF-8");
            params.setContentType("application/xml");
            request.addHeader("content-type", "application/xml");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);


            HttpResponse response = httpClient.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));

                String output;
                // System.out.println("Output from Server ...." + response.getStatusLine().getStatusCode() + "\n");
                while ((output = br.readLine()) != null) {
                    // System.out.println(output);
                }
            }
            else{
                log.error(response.getStatusLine().getStatusCode());

                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

        }catch (Exception ex) {
            log.error("ex Code sendPut: " + ex);
            log.error("url:" + url);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }



    }

    @Override
    public void sendPOI(ResponseData msg) {

        ResponseWrapper rw = new ResponseWrapper();
        rw.setSingleResponseData(msg);


        String url = "";
        synchronized (tthis) {
            while(!connectionUpVUI) {
                try {
                    tthis.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            url = vuiPath+"push";
        }

        int responseCode = -1;
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            log.debug("Sending " + rw.getTextForSingle());
            StringEntity params =new StringEntity(rw.getTextForSingle(),"UTF-8");
            params.setContentType("application/xml");
            request.addHeader("content-type", "application/xml");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);


            HttpResponse response = httpClient.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));

                String output;
                // System.out.println("Output from Server ...." + response.getStatusLine().getStatusCode() + "\n");
                while ((output = br.readLine()) != null) {
                    // System.out.println(output);
                }
            }
            else{
                log.error(response.getStatusLine().getStatusCode());

                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

        }catch (Exception ex) {
            log.error("ex Code sendPut: " + ex);
            log.error("url:" + url);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }






    }

    @Override
    public void sendCompletedDeploymentArea(ResponseData rd) {
        String url = "";
        synchronized (tthis) {
            while(!connectionUpMaps) {
                try {
                    tthis.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            url = mapsPath+"deployAreas/Complete";
        }

        ResponseWrapper rw = new ResponseWrapper();
        rw.setSingleResponseData(rd);

        int responseCode = -1;
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            log.debug("Sending " + rw.getTextForSingle());
            StringEntity params =new StringEntity(rw.getTextForSingle(),"UTF-8");
            params.setContentType("application/xml");
            request.addHeader("content-type", "application/xml");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);


            HttpResponse response = httpClient.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));

                String output;
                // System.out.println("Output from Server ...." + response.getStatusLine().getStatusCode() + "\n");
                while ((output = br.readLine()) != null) {
                    // System.out.println(output);
                }
            }
            else{
                log.error(response.getStatusLine().getStatusCode());

                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

        }catch (Exception ex) {
            log.error("ex Code sendPut: " + ex);
            log.error("url:" + url);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public RequestMessage getRequests() {
        return rp.getState();
    }


    public static void main(String[] args) {
        HttpC2Integration h = new HttpC2Integration();

        C2DroneInterface c2Bridge = new C2DroneInterface(h);

        try {
            DCSDroneConnectionManager connm = new DCSDroneConnectionManager(c2Bridge);
            c2Bridge.registerOutputHandler(connm);


            connm.listen();
        } catch (IOException e) {
            log.fatal(e);
        }

    }

}