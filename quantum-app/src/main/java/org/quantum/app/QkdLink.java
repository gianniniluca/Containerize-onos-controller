package org.quantum.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.Frequency;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;

import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class QkdLink {
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected int id;
    protected ConnectPoint src;
    protected ConnectPoint dst;
    protected OchSignal signal;
    protected double attenuation;
    protected String key;
    protected Intent intent;

    public QkdLink(int index, ConnectPoint srcCP, ConnectPoint dstCP, OchSignal s, double att, Intent in) {
        //Requires that id is a positive integer less < 10000, four digits
        String prefix = "12345678-abcd-abcd-abcd-00000000";

        key = prefix + String.format("%04d", index);

        id = index;
        src = srcCP;
        dst = dstCP;
        signal = s;
        attenuation = att;
        intent = in;
    }
}
