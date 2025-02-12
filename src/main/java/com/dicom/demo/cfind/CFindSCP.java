package com.dicom.demo.cfind;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCFindSCP;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.util.UIDUtils;

import java.io.IOException;
import java.util.concurrent.Executors;

public class CFindSCP {

    private Device device = new Device("MYPACS");

    private ApplicationEntity ae = new ApplicationEntity();

    private Connection conn = new Connection();

    public CFindSCP(String aeTitle) {
        this.ae.setAETitle(aeTitle);
        this.ae.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP,
                "*"));

        DicomServiceRegistry registry = new DicomServiceRegistry();
        registry.addDicomService(createHandler());
        this.device.addApplicationEntity(ae);
        this.device.setDimseRQHandler(registry);
    }

    public void bindConnection(String hostname, int port) {
        this.conn.setHostname(hostname);
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
        return new BasicCFindSCP(UID.StudyRootQueryRetrieveInformationModelFind) {
            @Override
            public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes rq, Attributes keys) throws IOException {
                System.out.println("收到请求");

                // 接收keys
                String levelKey = keys.getString(Tag.QueryRetrieveLevel);
                String suidKey = keys.getString(Tag.StudyInstanceUID);
                System.out.println("levelKey: " + levelKey);
                System.out.println("suidKey: " + suidKey);

                // 构造响应数据, 参数匹配响应数据
                Attributes response = null;
                if ("12344.111".equals(suidKey)) {
                    response = new Attributes();
                    response.setString(Tag.PatientID, VR.LO, "12345");
                    response.setString(Tag.PatientName, VR.PN, "Doe^John");
                    response.setString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
                    response.setString(Tag.StudyDate, VR.DA, "20230101");
                }
                System.out.println(response);
                as.tryWriteDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Pending), response);
                as.tryWriteDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Success));
            }
        };
    }

    public static void main(String[] args) {
        CFindSCP cFindSCP = new CFindSCP("find-scp");
        cFindSCP.bindConnection("127.0.0.1", 9999);
        cFindSCP.start();
        System.out.println("CFindSCP开始监听...");
    }

}
