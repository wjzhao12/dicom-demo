package com.dicom.demo.echo;

import org.dcm4che3.net.*;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceRegistry;

import java.util.concurrent.Executors;

public class CEchoSCP {

    private Device device = new Device("MYPACS");

    private ApplicationEntity ae = new ApplicationEntity();

    private Connection conn = new Connection();

    public CEchoSCP(String aeTitle) {
        this.ae.setAETitle(aeTitle);
        this.ae.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP,
                "*"));

        DicomServiceRegistry registry = new DicomServiceRegistry();
        registry.addDicomService(createHandler());
        this.device.setDimseRQHandler(registry);
        this.device.addApplicationEntity(ae);
    }

    public void bindConnection(String host, int port) {
        this.conn.setHostname(host);
        this.conn.setPort(port);
        this.device.addConnection(conn);
        this.ae.addConnection(conn);
    }

    public void start() {
        try {
            this.device.setExecutor(Executors.newCachedThreadPool());
            this.device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());
            this.device.bindConnections();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DicomService createHandler() {
        DicomService echoSCP = new BasicCEchoSCP();
        return echoSCP;
    }

    public static void main(String[] args) {
        CEchoSCP echoSCP = new CEchoSCP("echo-scp");
        echoSCP.bindConnection("127.0.0.1", 9999);
        echoSCP.start();
        System.out.println("已开启EchoSCP监听");
    }

}
