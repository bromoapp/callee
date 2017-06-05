package com.litaal.callee.service.bg;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.litaal.callee.helper.serv.ServiceBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class WebRTCWorker extends Service {

    private static Logger log = LoggerFactory.getLogger(WebRTCWorker.class);
    private final IBinder binder = new WebRTCWorkerBinder();

    public class WebRTCWorkerBinder extends ServiceBinder {
        @Override
        public Service service() {
            return WebRTCWorker.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public void onReceiveOffer(SessionDescription sdp) {
        log.info(">>> RECEIVED SDP TYPE: " + sdp.type.canonicalForm());
        log.info(">>> RECEIVED SDP DESC:\n" + sdp.description);
    }

    public void onReceiveCandidate(IceCandidate candidate) {
        log.info(">>> RECEIVED CANDIDATE SDP: " + candidate.sdp);
        log.info(">>> RECEIVED CANDIDATE MID: " + candidate.sdpMid);
        log.info(">>> RECEIVED CANDIDATE IDX: " + candidate.sdpMLineIndex);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
