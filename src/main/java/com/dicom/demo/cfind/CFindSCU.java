package com.dicom.demo.cfind;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CFindSCU {

    private Device device = new Device("TEST");

    private ApplicationEntity ae = new ApplicationEntity();

    private Connection conn = new Connection();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public CFindSCU(String aeTitle) {
        this.ae.setAETitle(aeTitle);
        this.device.addApplicationEntity(ae);
        this.device.setExecutor(executorService);
        this.device.addConnection(conn);
        this.ae.addConnection(conn);
    }

    public void find(String calledAET, String host, int port) {
        Connection remote = new Connection(null, host, port);
        Association as = null;

        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setString(Tag.PatientName, VR.PN, "*");
        keys.setString(Tag.StudyInstanceUID, VR.UI, "12344.111");

        try {
            as = this.ae.connect(remote, mkRq(calledAET));
            DimseRSP rsp = as.cfind(UID.StudyRootQueryRetrieveInformationModelFind, Priority.NORMAL, keys,
                    UID.ImplicitVRLittleEndian, 1);


            boolean completed = false;
            while (rsp.next()) {
                Attributes cmd = rsp.getCommand();
                Attributes data = rsp.getDataset();
                int status = cmd.getInt(Tag.Status, -1);
                if (status == Status.Success) {
                    System.out.println("success");
                    System.out.println(data);
                    System.out.println("============");
                    completed = true;
                } else if (status == Status.Pending) {
                    System.out.println("pending");
                    System.out.println(data);
                    System.out.println("============");
                }

                if (completed) {
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

    private AAssociateRQ mkRq(String calledAET) {
        AAssociateRQ rq = new AAssociateRQ();
        rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1,
                UID.Verification,
                UID.ImplicitVRLittleEndian));
        rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1,
                UID.StudyRootQueryRetrieveInformationModelFind,
                UID.ImplicitVRLittleEndian));
        rq.setCalledAET(calledAET);
        return rq;
    }

    public static void main(String[] args) {
        CFindSCU cFindSCU = new CFindSCU("find-scu");
        cFindSCU.find("find-scp", "127.0.0.1", 9999);
        cFindSCU.shutdown();
    }

}
