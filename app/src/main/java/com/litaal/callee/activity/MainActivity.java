package com.litaal.callee.activity;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.gson.Gson;
import com.litaal.callee.R;
import com.litaal.callee.dto.CandidateSignalDTO;
import com.litaal.callee.dto.SdpSignalDTO;
import com.litaal.callee.dto.SignalDTO;
import com.litaal.callee.helper.Constant;
import com.litaal.callee.helper.serv.IConnectionListener;
import com.litaal.callee.helper.serv.IIntentReceiver;
import com.litaal.callee.helper.serv.IntentReceiver;
import com.litaal.callee.helper.serv.ServiceConnector;
import com.litaal.callee.service.bg.SignalingWorker;
import com.litaal.callee.service.bg.WebRTCWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class MainActivity extends AppCompatActivity implements IConnectionListener, IIntentReceiver {

    private static Logger log = LoggerFactory.getLogger(MainActivity.class);

    private Gson gson = new Gson();

    private SignalingWorker signalingWorker = null;
    private ServiceConnection signalingWorkerConn;
    private IntentReceiver signalingWorkerIntentReceiver;
    private boolean isSignalingWorkerIntentReceiverRegistered = false;

    private WebRTCWorker webRTCWorker = null;
    private ServiceConnection webRTCWorkerConn;
    private IntentReceiver webRTCWorkerIntentReceiver;
    private boolean isWebRTCWorkerIntentReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initiates background services
        signalingWorkerConn = new ServiceConnector(this);
        startService(new Intent(getBaseContext(), SignalingWorker.class));
        bindService(new Intent(getBaseContext(), SignalingWorker.class), signalingWorkerConn, Service.BIND_AUTO_CREATE);

        if (!isSignalingWorkerIntentReceiverRegistered) {
            signalingWorkerIntentReceiver = new IntentReceiver(this);
            registerReceiver(signalingWorkerIntentReceiver, new IntentFilter(Constant.IntentTopic.ON_SIGNAL_EVENT));
            isSignalingWorkerIntentReceiverRegistered = true;
        }

        webRTCWorkerConn = new ServiceConnector(this);
        startService(new Intent(getBaseContext(), WebRTCWorker.class));
        bindService(new Intent(getBaseContext(), WebRTCWorker.class), webRTCWorkerConn, Service.BIND_AUTO_CREATE);

        if (!isWebRTCWorkerIntentReceiverRegistered) {
            webRTCWorkerIntentReceiver = new IntentReceiver(this);
            registerReceiver(webRTCWorkerIntentReceiver, new IntentFilter(Constant.IntentTopic.ON_PEER_EVENT));
            isWebRTCWorkerIntentReceiverRegistered = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (signalingWorker != null) {
            unbindService(signalingWorkerConn);
        }
        if (isSignalingWorkerIntentReceiverRegistered) {
            unregisterReceiver(signalingWorkerIntentReceiver);
        }
        if (webRTCWorker != null) {
            unbindService(webRTCWorkerConn);
        }
        if (isWebRTCWorkerIntentReceiverRegistered) {
            unregisterReceiver(webRTCWorkerIntentReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onIntentReceived(Intent i) {
        if (i.getAction().equalsIgnoreCase(Constant.IntentTopic.ON_SIGNAL_EVENT)) {
            SignalDTO msg = (SignalDTO) i.getSerializableExtra(Constant.IntentExtraKey.MESSAGE);
            if (msg.getOrigin().equalsIgnoreCase("caller")) {
                if (msg.getBody().contains("{\"sdp\":")) {
                    SdpSignalDTO dto = gson.fromJson(msg.getBody(), SdpSignalDTO.class);
                    if (dto.getSdp().getType().equalsIgnoreCase(SessionDescription.Type.OFFER.canonicalForm())) {
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, dto.getSdp().getSdp());
                        webRTCWorker.onReceiveOffer(sdp);
                    }
                } else if (msg.getBody().contains("{\"candidate\":")) {
                    CandidateSignalDTO dto = gson.fromJson(msg.getBody(), CandidateSignalDTO.class);
                    IceCandidate can = new IceCandidate(dto.getCandidate().getSdpMid(),
                            dto.getCandidate().getSdpMLineIndex(), dto.getCandidate().getCandidate());
                    webRTCWorker.onReceiveCandidate(can);
                }
            }
        }
    }

    @Override
    public void onConnected(String name, Service service) {
        if (name.equalsIgnoreCase(SignalingWorker.class.getCanonicalName())) {
            signalingWorker = (SignalingWorker) service;
        }
        if (name.equalsIgnoreCase(WebRTCWorker.class.getCanonicalName())) {
            webRTCWorker = (WebRTCWorker) service;
        }
    }

    @Override
    public void onDisconnected(String name) {
        // TODO
    }
}
