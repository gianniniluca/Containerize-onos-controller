package org.quantum.app;

import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.util.Frequency;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netconf.*;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

@Component(immediate = true, service = QkdLinkManager.class)
public class QkdLinkManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private static final Map<String, QkdLink> qkdLinkDatabase = new HashMap<>();
    private static final String RPC_TAG_NETCONF_BASE = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String RPC_CLOSE_TAG = "</rpc>";
    private static final String ETSI_TYPE_PREFIX = "xmlns:etsi-qkdn-types=\"urn:etsi:qkd:yang:etsi-qkd-node-types\"";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetconfController netconfController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Activate
    protected void activate() {

        appId = coreService.registerApplication("org.quantum.app");

        log.info("STARTED QkdLink Manager appId {}", appId);
    }

    @Deactivate
    protected void deactivate() {
        log.info("STOPPED QkdLink Manager");
    }

    public void addQkdLink(String key, QkdLink value) {
        qkdLinkDatabase.put(key, value);
    }

    public void removeQkdLink(String key) {
        qkdLinkDatabase.remove(key);
    }

    public QkdLink getQkdLink(String key) {
        return qkdLinkDatabase.get(key);
    }

    public Collection<QkdLink> getQkdLinks() {
        return qkdLinkDatabase.values();
    }

    public String getQkdDeviceLinks(String key) {
        String reply;
        QkdLink link = getQkdLink(key);

        NetconfSession session = getNetconfSession(link.src.deviceId());

        try {
            reply = session.get(getDeviceDetailsBuilder());
            log.info("REPLY to DeviceDescription {}", reply);

            //Convert string to XML configuration
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
        } catch (Exception e) {
            log.error("discoverDeviceDetails - Failed to retrieve session {}", link.src.deviceId());
            throw new IllegalStateException(new NetconfException("Failed to retrieve session.", e));
        }

        return reply;
    }


    public boolean configureQkdLinks(String key) {
        QkdLink link = getQkdLink(key);

        NetconfSession session = getNetconfSession(link.src.deviceId());

        try {
            log.info("REQUEST sent to src: {}", setLinkFilterDestination(link));
            boolean replySrc = session.editConfig(DatastoreId.RUNNING, null, setLinkFilterSource(link));
            boolean replyDst = session.editConfig(DatastoreId.RUNNING, null, setLinkFilterSource(link));

            log.info("REPLY to DeviceConfiguration src: {}", replySrc);
            log.info("REPLY to DeviceConfiguration dst: {}", replyDst);

        } catch (Exception e) {
            log.error("configureQkdLinks - Failed to retrieve session {}", link.src.deviceId());
            throw new IllegalStateException(new NetconfException("Failed to retrieve session.", e));
        }

        return true;
    }

    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfDevice netconfDevice = netconfController.getDevicesMap().get(deviceId);
        if (netconfDevice == null) {
            log.error("No netconf device, returning null session");
            return null;
        }
        return netconfDevice.getSession();
    }

    private String getDeviceDetailsBuilder() {
        StringBuilder filter = new StringBuilder();
        filter.append("<qkd_node xmlns='urn:etsi:qkd:yang:etsi-qkd-node'>");
        filter.append("<qkd_links>");
        filter.append("</qkd_links>");
        filter.append("</qkd_node>");
        return filteredGetBuilder(filter.toString());
    }

    private double freqToLambdaNm(Frequency frequency) {
        int c = 299792458; //Expressed in m/s

        double lambda = c / frequency.asGHz();

        return lambda;
    }

    private String setLinkFilterSource(QkdLink link) {
        String localNodeId = deviceService.getDevice(link.src.deviceId()).serialNumber();
        String localPortId = link.src.port().toString();
        String remoteNodeId = deviceService.getDevice(link.dst.deviceId()).serialNumber();
        String remotePortId = link.dst.port().toString();
        String role = "etsi-qkdn-types:TRANSMITTER";

        StringBuilder filter = new StringBuilder();
        filter.append("<qkd_node xmlns=\"urn:etsi:qkd:yang:etsi-qkd-node\">");
        filter.append("  <qkd_links>");
        filter.append("    <qkd_link>");
        filter.append("      <qkdl_id>" + link.key + "</qkdl_id>");
        filter.append("      <qkdl_status " + ETSI_TYPE_PREFIX + ">" + "etsi-qkdn-types:OFF" + "</qkdl_status>");
        filter.append("      <qkdl_enable>true</qkdl_enable>");
        filter.append("      <qkdl_local>");
        filter.append("        <qkdn_id>" + localNodeId + "</qkdn_id>");
        filter.append("        <qkdi_id>" + localPortId + "</qkdi_id>");
        filter.append("      </qkdl_local>");
        filter.append("      <qkdl_remote>");
        filter.append("        <qkdn_id>" + remoteNodeId + "</qkdn_id>");
        filter.append("        <qkdi_id>" + remotePortId + "</qkdi_id>");
        filter.append("      </qkdl_remote>");
        filter.append("      <qkdl_type " + ETSI_TYPE_PREFIX + ">" + "etsi-qkdn-types:PHYS" + "</qkdl_type>");
        filter.append("      <phys_channel_att>" + String.format("%.2f", link.attenuation) + "</phys_channel_att>");
        filter.append("      <phys_wavelength>" + link.signal.spacingMultiplier() + "</phys_wavelength>");
        filter.append("      <phys_qkd_role " + ETSI_TYPE_PREFIX + ">" + role + "</phys_qkd_role>");
        filter.append("    </qkd_link>");
        filter.append("  </qkd_links>");
        filter.append("</qkd_node>");
        return filter.toString();
    }

    private String setLinkFilterDestination(QkdLink link) {

        String localNodeId = deviceService.getDevice(link.dst.deviceId()).serialNumber();
        String localPortId = link.dst.port().toString();
        String remoteNodeId = deviceService.getDevice(link.src.deviceId()).serialNumber();
        String remotePortId = link.src.port().toString();
        String role = "etsi-qkdn-types:RECEIVER";

        StringBuilder filter = new StringBuilder();
        filter.append("<qkd_node xmlns=\"urn:etsi:qkd:yang:etsi-qkd-node\">");
        filter.append("  <qkd_links>");
        filter.append("    <qkd_link>");
        filter.append("      <qkdl_id>" + link.key + "</qkdl_id>");
        filter.append("      <qkdl_status " + ETSI_TYPE_PREFIX + ">" + "etsi-qkdn-types:OFF" + "</qkdl_status>");
        filter.append("      <qkdl_enable>true</qkdl_enable>");
        filter.append("      <qkdl_local>");
        filter.append("        <qkdn_id>" + localNodeId + "</qkdn_id>");
        filter.append("        <qkdi_id>" + localPortId + "</qkdi_id>");
        filter.append("      </qkdl_local>");
        filter.append("      <qkdl_remote>");
        filter.append("        <qkdn_id>" + remoteNodeId + "</qkdn_id>");
        filter.append("        <qkdi_id>" + remotePortId + "</qkdi_id>");
        filter.append("      </qkdl_remote>");
        filter.append("      <qkdl_type " + ETSI_TYPE_PREFIX + ">" + "etsi-qkdn-types:PHYS" + "</qkdl_type>");
        filter.append("      <phys_channel_att>" + String.format("%.2f", link.attenuation) + "</phys_channel_att>");
        filter.append("      <phys_wavelength>" + "1552" + "</phys_wavelength>");
        filter.append("      <phys_qkd_role " + ETSI_TYPE_PREFIX + ">" + role + "</phys_qkd_role>");
        filter.append("    </qkd_link>");
        filter.append("  </qkd_links>");
        filter.append("</qkd_node>");
        return filter.toString();
    }

    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    private String filteredEditBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }
}


