package org.quantum.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Key sessions APIs.
 */
@Path("keySessions")
public class QkdKeySessionWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    QkdAppManager appManager = get(QkdAppManager.class);

    QkdKeySessionManager sessionManager = get(QkdKeySessionManager.class);

    QkdLinkManager linkManager = get(QkdLinkManager.class);

    RestUtilities restUtilities = get(RestUtilities.class);

    /**
     * Get list of key sessions.
     *
     * @return 200 OK
     */
    @GET
    @Path("getKeySessions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKeySessions() {

        ArrayNode sessions = mapper().createArrayNode();

        for (QkdKeySession session : sessionManager.getKeySessions()) {
            ObjectNode node = mapper().createObjectNode()
                    .put("master_sae_id", session.appMaster.saeId)
                    .put("slave_sae_id", session.appSlave.saeId)
                    .put("app_id", session.appId)
                    .put("link_id", session.linkId);

            sessions.add(node);
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("QkdKeySessions", sessions);
        return ok(root).build();
    }

    /**
     * Open Key Session between two apps.
     *
     * @return 200 OK
     * @onos.rsModel appOpenKeySession
     */
    @POST
    @Path("openKeySession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response openKeySession(InputStream stream) {
        String masterSae, slaveSae;
        QkdApp master, slave;
        QkdKeySession keySession;

        try {
            ObjectNode root = readTreeFromStream(mapper(), stream);

            master = appManager.getQkdApp(root.get("masterAddress").asText(), root.get("masterPort").asText());
            slave = appManager.getQkdApp(root.get("slaveAddress").asText(), root.get("slavePort").asText());

            if ((master == null)) {
                throw new IllegalArgumentException("Master app is not registered");
            }

            if ((slave == null)) {
                throw new IllegalArgumentException("Slave app is not registered");
            }

            if ((master == slave)) {
                throw new IllegalArgumentException("Master and slave apps must be different");
            }

            if (master.device.id().equals(slave.device.id())) {
                throw new IllegalArgumentException("Master and slave apps are co-located");
            }

            masterSae = master.saeId;
            slaveSae = slave.saeId;

            keySession = new QkdKeySession(
                    master,
                    slave
            );

            //Check if a suitable quantum link has been already created
            if (linkManager.getQkdLink(keySession.linkId) == null) {
                throw new IllegalArgumentException("A quantum link is not available for this session");
            }

            if (linkManager.getQkdLink(keySession.linkId).linkStatus == QkdLink.LinkStatus.OFF) {
                throw new IllegalArgumentException("The requested quantum link is OFF");
            }

            //Agent configuration via netconf
            sessionManager.configureKeySession(keySession);

            //Configure the km using REST APIs
            log.info("REST APIs at km may be invoked with {}", keySession.linkId);
            if (keySession.linkId.toString().equals("bbbbbbbb-7901-bbbb-7092-bbbbbbbbbbbb")) {

                log.info("REST APIs at km INVOKED slavesae {} app {} link {}",
                        slaveSae,
                        keySession.appId,
                        keySession.linkId);

                restUtilities.postKeyManager("10.79.1.47", "8001", slaveSae, keySession.appId, keySession.linkId);
                restUtilities.postKeyManager("10.79.1.47", "8002", slaveSae, keySession.appId, keySession.linkId);
            }

        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }

        //Add the key session to the local database
        sessionManager.addKeySession(keySession.appId, keySession);

        ObjectNode node = mapper().createObjectNode()
                .put("master_sae_id", masterSae)
                .put("slave_sae_id", slaveSae)
                //.put("link_id", keySession.linkId)
                .put("app_id", keySession.appId);

        return Response.ok(node).build();
    }

    /**
     * Close a key session.
     *
     * @param key
     * @return 200 OK
     */
    @DELETE
    @Path("closeKeySession")
    public Response deleteSession(@QueryParam("key") String key) {


        if (sessionManager.getKeySession(key) == null) {
            throw new IllegalArgumentException("Key session does not exist");
        }

        QkdKeySession keySession = sessionManager.getKeySession(key);

        //Delete configuration on devices
        sessionManager.deleteKeySession(keySession);

        //Delete from local database
        sessionManager.removeKeySession(key);

        return Response.ok().build();
    }

    /**
     * Set the key session status.
     *
     * @param key
     * @param status
     * @return 200 OK
     */
    @POST
    @Path("setKeySessionStatus")
    public Response setSessionStatus(@QueryParam("key") String key,
                                     @QueryParam("status") String status) {

        QkdKeySession session = sessionManager.getKeySession(key);
        QkdLink link = linkManager.getQkdLink(session.linkId);

        switch (status) {
            case "SUCCESS":
                if (link.linkStatus == QkdLink.LinkStatus.OFF) {
                    link.linkStatus = QkdLink.LinkStatus.PASSIVE;
                };
                break;
            case "ERROR":
                if (link.linkStatus == QkdLink.LinkStatus.PASSIVE) {
                    link.linkStatus = QkdLink.LinkStatus.OFF;
                }
                break;
            default:
                throw new IllegalArgumentException("Status not valid, expected SUCCESS or ERROR provided: " + status);
        }

        return Response.ok().build();
    }
}
