package com.litaal.callee.service.bg;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.widget.LinearLayout;

import com.litaal.callee.R;
import com.litaal.callee.helper.rtc.PeerConnObserverImpl;
import com.litaal.callee.helper.rtc.SdpObserverImpl;
import com.litaal.callee.helper.serv.ServiceBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class WebRTCWorker extends Service {

    private static Logger log = LoggerFactory.getLogger(WebRTCWorker.class);
    private final IBinder binder = new WebRTCWorkerBinder();

    private GLSurfaceView videoView;

    private PeerConnectionFactory peerConnFactory;
    private PeerConnection peerConnection;
    private MediaStream mediaStream;
    private SdpObserver sdpObserver;

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

    public void onReceivedOffer(Activity activity, SessionDescription sdp) {
        //log.info(">>> RECEIVED SDP TYPE: " + sdp.type.canonicalForm());
        //log.info(">>> RECEIVED SDP DESC:\n" + sdp.description);
        try {
            if (PeerConnectionFactory.initializeAndroidGlobals(getBaseContext(), true, true, false)) {
                peerConnFactory = new PeerConnectionFactory();

                videoView = new GLSurfaceView(getBaseContext());
                videoView.setEGLContextClientVersion(2);
                VideoRendererGui.setView(videoView, new Runnable() {
                    @Override
                    public void run() {}
                });
                VideoRenderer vidRenderer = VideoRendererGui.createGui(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);

                LinearLayout layout = (LinearLayout) activity.findViewById(R.id.video_view_container);
                layout.addView(videoView);

                List<PeerConnection.IceServer> iceServers = new ArrayList<>();
                iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

                PeerConnection.Observer connObserver = new PeerConnObserverImpl();
                ((PeerConnObserverImpl) connObserver).setActivity(activity);
                ((PeerConnObserverImpl) connObserver).setVidRenderer(vidRenderer);

                peerConnection = peerConnFactory.createPeerConnection(iceServers, new MediaConstraints(), connObserver);

                sdpObserver = new SdpObserverImpl();
                ((SdpObserverImpl) sdpObserver).setActivity(activity);
                ((SdpObserverImpl) sdpObserver).setPeerConnection(peerConnection);
                ((SdpObserverImpl) sdpObserver).setSdpObserver(sdpObserver);

                peerConnection.setRemoteDescription(sdpObserver, sdp);
                peerConnection.createAnswer(sdpObserver, new MediaConstraints());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReceiveCandidate(IceCandidate candidate) {
        //log.info(">>> RECEIVED CANDIDATE SDP: " + candidate.sdp);
        //log.info(">>> RECEIVED CANDIDATE MID: " + candidate.sdpMid);
        //log.info(">>> RECEIVED CANDIDATE IDX: " + candidate.sdpMLineIndex);
        peerConnection.addIceCandidate(candidate);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
