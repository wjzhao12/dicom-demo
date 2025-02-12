package com.dicom.demo.echo;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CEchoSCU {

    private Device device = new Device("TEST");

    private ApplicationEntity ae = new ApplicationEntity();

    private Connection conn = new Connection();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CEchoSCU(String aeTitle) {
        this.ae.setAETitle(aeTitle);
        this.device.addApplicationEntity(ae);
        this.device.setExecutor(executorService);
        this.device.addConnection(conn);
        this.ae.addConnection(conn);
    }

    public void echo(String calledAET, String hostname, int port) {
        Connection remote = new Connection(null, hostname, port);
        Association as = null;
        try {
            as = this.ae.connect(remote, makeRq(calledAET));
            DimseRSP rsp = as.cecho();
            while (rsp.next()) {
                Attributes command = rsp.getCommand();
                Attributes dataset = rsp.getDataset();
                int status = command.getInt(Tag.Status, -1);
                System.out.println("status: " + status);
                if (status == Status.Success) {
                    System.out.println("success");
                    break;
                } else if (status == Status.Pending) {
                    System.out.println("pending");
                } else {
                    System.out.println("other status");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                as.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.executorService.shutdown();
    }

    private AAssociateRQ makeRq(String calledAET) {
        AAssociateRQ rq = new AAssociateRQ();
        rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1,
                UID.Verification,
                UID.ImplicitVRLittleEndian));
        rq.setCalledAET(calledAET);
        return rq;
    }

    public static void main(String[] args) {
        CEchoSCU echoSCU = new CEchoSCU("echo-scu");
        echoSCU.echo("echo-scp", "127.0.0.1", 9999);
        echoSCU.shutdown();
    }

}
