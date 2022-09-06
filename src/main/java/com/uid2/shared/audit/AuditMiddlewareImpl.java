package com.uid2.shared.audit;

import com.uid2.shared.auth.IAuthorizable;
import io.vertx.ext.web.RoutingContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AuditMiddlewareImpl implements IAuditMiddleware{
    private final IAuditWriter auditWriter;

    public AuditMiddlewareImpl(IAuditWriter writer){
        this.auditWriter = writer;
    }

    @Override
    public Function<List<OperationModel>, Boolean> handle(RoutingContext rc) {
        InnerAuditHandler auditHandler = new InnerAuditHandler(rc, auditWriter);
        return auditHandler::writeLogs;
    }

    private static class InnerAuditHandler{
        private final RoutingContext rc;
        private final IAuditWriter auditWriter;
        private InnerAuditHandler(RoutingContext rc, IAuditWriter auditWriter) {
            this.rc = rc;
            this.auditWriter = auditWriter;
        }

        public boolean writeLogs(List<OperationModel> modelList){
            String ipAddress = getIPAddress(rc);
            List<IAuditModel> auditModelList = new ArrayList<>();
            for(OperationModel model : modelList) {
                auditModelList.add(new QLDBAuditModel(model.itemType, model.itemKey, model.actionTaken, ipAddress,
                        rc != null ? ((IAuthorizable) rc.data().get("api-client")).getContact() : null,
                        System.getenv("HOSTNAME"), Instant.now().getEpochSecond(), model.itemHash, model.summary));
            }
            return auditWriter.writeLogs(auditModelList);
        }

        private static String getIPAddress(RoutingContext rc) {
            if(rc == null){
                return null;
            }
            List<String> listIP = rc.request().headers().getAll("X-Forwarded-For");
            List<InetAddress> publicIPs = new ArrayList<>();
            for(String str : listIP){
                try {
                    InetAddress address = InetAddress.getByName(str);
                    if(!address.isSiteLocalAddress()){
                        publicIPs.add(address);
                    }
                }
                catch(UnknownHostException ignored){

                }

            }
            if(publicIPs.isEmpty()){
                return rc.request().remoteAddress().toString();
            }
            else{
                return publicIPs.get(0).getHostAddress(); //arbitrary if multiple
            }
        }
    }
}
