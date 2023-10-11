package org.quantum.app;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OchSignal;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.cli.AbstractShellCommand.get;

public class QkdLink {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected String key;
    protected ConnectPoint src;
    protected ConnectPoint dst;
    protected double attenuation;
    protected Intent intent;

    protected enum LinkStatus {ACTIVE, PASSIVE, PENDING, OFF}
    protected LinkStatus linkStatus;

    public QkdLink(ConnectPoint srcCP, ConnectPoint dstCP, double att, Intent in) {
        DeviceService deviceService = get(DeviceService.class);

        log.info("serial source {}", deviceService.getDevice(srcCP.deviceId()).serialNumber());
        log.info("serial destination {}", deviceService.getDevice(dstCP.deviceId()).serialNumber());

        String source = deviceService.getDevice(srcCP.deviceId()).serialNumber().split("-")[3];
        String destination = deviceService.getDevice(dstCP.deviceId()).serialNumber().split("-")[3];

        key = "bbbbbbbb-" + source + "-bbbb-" + destination + "-bbbbbbbbbbbb";

        src = srcCP;
        dst = dstCP;
        attenuation = att;
        intent = in;

        linkStatus = LinkStatus.OFF;
    }
}
