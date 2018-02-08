package es.gpsou.vehiclealarm;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.socket.IceSocketWrapper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import static es.gpsou.vehiclealarm.MyFirebaseMessagingService.broadcastIntent;

/**
 * Created by Pedro on 16/12/2017.
 */

public class UDPLinkMgr {
    private static final short BIND_REQUEST=0x0001;
    private static final short BIND_RESPONSE=0x0101;
    private static final short MAPPED_ADDRESS=0x0001;
    private static final String STUNT_HOST="stun.stunprotocol.org";
    private static final int STUNT_PORT=3478;

    private static final int CONNECT_TIMEOUT=10; //Segundos

    private static final int SERVICE_PORT=21996;
    private static DatagramSocket dsocket=null;

    private static boolean continueAudio=false;
    final static int AUDIO_BUFFER_SIZE =8192;
    final static int DATAGRAM_BUFFER_SIZE=1400;
    final static int SAMPLE_RATE=11025;
    final static int AUDIO_BLOCKS=4;

    private static Agent agent=null;
    private static boolean activeAgent=false;
    private static String SDPString=null;

    private static CandidatePair rtpPair;

    private static final UDPLinkMgr ourInstance = new UDPLinkMgr();

    public static UDPLinkMgr getInstance() {
        return ourInstance;
    }

    private UDPLinkMgr() {
    }

    public String getSDPString(String stunServer) {
        if(agent!=null)
            return(SDPString);

        agent = new Agent(); // A simple ICE Agent

        String[] stunServerInfo=stunServer.split(":");
        String stunHost="";
        int stunPort=3478;
        if(stunServerInfo.length == 1) {
            stunHost=stunServerInfo[0];
        } else if (stunServerInfo.length == 2) {
            stunHost=stunServerInfo[0];
            try {
                stunPort = Integer.parseInt(stunServerInfo[1]);
            }catch(NumberFormatException e) {
                stunHost="";
            }
        }
        if(stunHost.length()==0)
            stunHost=null;

        SDPString="";

/*** Setup the STUN servers: ***/
//        String[] hostnames = new String[] {"jitsi.org","numb.viagenie.ca","stun.ekiga.net"};
        String[] hostnames = stunHost==null? new String [0]: new String[] {stunHost};

// Look online for actively working public STUN Servers. You can find free servers.
// Now add these URLS as Stun Servers with standard 3478 port for STUN servrs.
        for(String hostname: hostnames){
            Log.d(Globals.TAG, "Added stun host: " + hostname);
            try {
                // InetAddress qualifies a url to an IP Address, if you have an error here, make sure the url is reachable and correct
                TransportAddress ta = new TransportAddress(InetAddress.getByName(hostname), stunPort, Transport.UDP);
                // Currently Ice4J only supports UDP and will throw an Error otherwise
                agent.addCandidateHarvester(new TurnCandidateHarvester(ta));
            } catch (Exception e) {
                e.printStackTrace();
                stopAudio();
                return null;
            }
        }

        IceMediaStream stream = agent.createMediaStream("audio");
        int port = SERVICE_PORT; // Choose any port
        try {
            agent.createComponent(stream, Transport.UDP, port, port, port + 100);
        } catch(IOException e) {
            e.printStackTrace();
            stopAudio();
            return null;
        }
// The three last arguments are: preferredPort, minPort, maxPort

        try {
            SDPString = SdpUtils.createSDPDescription(agent); //Each computer sends this information
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            stopAudio();
            return null;
        }
// This information describes all the possible IP addresses and ports
        Log.d(Globals.TAG, "SDP String: "+SDPString);

        return(SDPString);
    }

    private void stablishConnection(String remoteReceived) {

        try {
            SdpUtils.parseSDP(agent, remoteReceived);
        } catch (Exception e) {
            Log.d(Globals.TAG, "Error en parseo del SDP");
            stopAudio();
            return;
        }

        agent.addStateChangeListener(new StateListener()); // We will define this class soon
// You need to listen for state change so that once connected you can then use the socket.
        agent.startConnectivityEstablishment(); // This will do all the work for you to connect

    }

    public void sendAudio(String remoteReceived) {
        synchronized (this) {
            if (agent == null)
                return;

            if (activeAgent)
                return;

            activeAgent = true;

            agent.setControlling(true);
            stablishConnection(remoteReceived);
        }


        final IceSocketWrapper wrapper;

        int connect_timeout = CONNECT_TIMEOUT;

        try {
            if (agent == null)
                return;

            while (agent.getState() == IceProcessingState.RUNNING || agent.getState() == IceProcessingState.COMPLETED) {
                try {
                    Log.d(Globals.TAG, "Esperando conexion: " + agent.getState());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (connect_timeout-- == 0) {
                    Log.d(Globals.TAG, "Timeout en la conexión con el dispositivo monitor");
                    stopAudio();
                    return;
                }
            }

            if (agent.getState() != IceProcessingState.TERMINATED) {
                Log.d(Globals.TAG, "No se ha conseguido establecer la conexión con el dispositivo monitor");
                stopAudio();
                return;
            }


            wrapper = rtpPair.getParentComponent().getSocketWrapper();

            if (wrapper == null) {
                stopAudio();
                return;
            }

        } catch (Exception e) {
            Log.d(Globals.TAG, "Error durante la negociación ICE");
            stopAudio();
            return;
        }

        byte audioBuffer[] = new byte[AUDIO_BUFFER_SIZE];

        DatagramPacket dgram = new DatagramPacket(audioBuffer, DATAGRAM_BUFFER_SIZE);

        Log.d(Globals.TAG, "Min audio buffer: "+AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
        final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AUDIO_BUFFER_SIZE);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(Globals.TAG, "Error al inicializar captura de audio");
            stopAudio();
            return;
        }

        int bytesRead;
        continueAudio = true;

        record.startRecording();


        int timeout = AUDIO_BLOCKS * AUDIO_BUFFER_SIZE / SAMPLE_RATE;
        try {
            wrapper.getUDPSocket().setSoTimeout(1000 * timeout);
        } catch (Exception e) {
            Log.d(Globals.TAG, "Error al establecer socket timeout de lectura");
            stopAudio();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int MAX_MISSED_PACKETS = 5;

                DatagramPacket activeMonitorDgram = new DatagramPacket(new byte[1], 1);

                int missedPackets = 0;
                while (continueAudio) {
                    try {
                        wrapper.receive(activeMonitorDgram);
                        missedPackets = 0;
                    } catch (SocketTimeoutException e) {
                        Log.d(Globals.TAG, "Timeout de lectura del socket");
                        if (missedPackets++ == MAX_MISSED_PACKETS) {
                            Log.d(Globals.TAG, "Se ha interrumpido la reproducción de audio en el dispositivo monitor");
                            continueAudio=false;
                        }
                    } catch (IOException e) {
                        Log.d(Globals.TAG, "Error en la lectura del socket");
                        continueAudio=false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();


        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        int pos;
        long count=0;
        while (continueAudio) {
            bytesRead = record.read(audioBuffer, 0, audioBuffer.length);

            try {
                pos=0;
                while(pos < bytesRead) {
                    if(bytesRead - pos >= DATAGRAM_BUFFER_SIZE)
                        dgram.setData(audioBuffer, pos, DATAGRAM_BUFFER_SIZE);
                    else
                        dgram.setData(audioBuffer, pos, bytesRead - pos);
//                    audioBuffer[pos]=(byte)(count++ & 0xFF);

                    pos += DATAGRAM_BUFFER_SIZE;

                    synchronized (this) {
                        if (agent != null)
                            wrapper.send(dgram);
                    }
                }
            } catch (IOException e) {
                Log.d(Globals.TAG, "Error al enviar datagrama UDP de audio");
                continueAudio=false;
            }
        }

        record.stop();
        record.release();

        stopAudio();
    }

    public void receiveAudio(final Context context, String remoteReceived) {

        synchronized (this) {

            if (agent == null)
                return;

            if (activeAgent)
                return;

            activeAgent = true;

            agent.setControlling(false);
            stablishConnection(remoteReceived);
        }

        IceSocketWrapper wrapper = null;

        int connect_timeout = 2 * CONNECT_TIMEOUT;
        try {
            if (agent == null)
                return;

            while (agent.getState() == IceProcessingState.RUNNING || agent.getState() == IceProcessingState.COMPLETED) {
                try {
                    Log.d(Globals.TAG, "Esperando audio: " + agent.getState());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (connect_timeout-- == 0) {
                    Log.d(Globals.TAG, "Timeout en la conexión con el dispositivo sensor");
                    stopAudio();
                    return;
                }
            }

            if (agent.getState() != IceProcessingState.TERMINATED) {
                stopAudio();
                return;
            }

            wrapper = rtpPair.getParentComponent().getSocketWrapper();

            if (wrapper == null) {
                stopAudio();
                return;
            }

            try {
                wrapper.getUDPSocket().setSoTimeout(10000);
            } catch (Exception e) {
                Log.d(Globals.TAG, "Error al establecer socket timeout de lectura");
                return;
            }
        } catch (Exception e) {
            Log.d(Globals.TAG, "Error en la negociación ICE");
            stopAudio();
            return;
        }

        Globals.vehicleDeviceStatus.audioOn = true;
        broadcastIntent.putExtra(Globals.P2P_OP, Globals.AUDIO);
        context.sendOrderedBroadcast(broadcastIntent, null);

        final byte audioBuffer[] = new byte[AUDIO_BLOCKS * AUDIO_BUFFER_SIZE];
        final byte silencio[] = new byte[AUDIO_BUFFER_SIZE];
        final boolean[] blockReady=new boolean[AUDIO_BLOCKS];

        DatagramPacket dgram = new DatagramPacket(audioBuffer, 0, DATAGRAM_BUFFER_SIZE);

        final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2 * AUDIO_BUFFER_SIZE,
                AudioTrack.MODE_STREAM);

        if (audioTrack.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(Globals.TAG, "Error al inicializar reproducción de audio");
            stopAudio();
            return;
        }

        continueAudio = true;

        audioTrack.play();

        final AudioControl audioControl=new AudioControl();

        for (int i = 0; i < AUDIO_BLOCKS; i++)
            blockReady[i] = true;

        new Thread(new Runnable() {
            @Override
            public void run() {

                long timeout=1000 * DATAGRAM_BUFFER_SIZE / (2 * SAMPLE_RATE);

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                int posAudio, i;
                while (continueAudio) {
                    posAudio = 0;
                    for (i = 0; i < AUDIO_BLOCKS; i++) {
                        while (!blockReady[i] && continueAudio) {
//                            audioTrack.write(silencio, 0, silencio.length);
                            try {
                                Thread.sleep(timeout);
                            }catch(InterruptedException e) {}
                        }

                        try {
                            if(i == AUDIO_BLOCKS - 1) {
                                audioTrack.write(audioBuffer, posAudio, audioControl.loopEndPos - posAudio);
                                audioControl.playPos += audioControl.loopEndPos - posAudio;
                            } else {
                                audioTrack.write(audioBuffer, posAudio, AUDIO_BUFFER_SIZE);
                                audioControl.playPos += AUDIO_BUFFER_SIZE;
                            }
                        } catch (Exception e) {
                        }
                        blockReady[i] = false;

                        Log.d(Globals.TAG, "Diff " + (audioControl.receivedPos - audioControl.playPos) );

                        posAudio += AUDIO_BUFFER_SIZE;
                    }
                }
                Log.d(Globals.TAG, "Finaliza la repriducción de audio");
            }
        }).start();

        long timeout=1000 * AUDIO_BUFFER_SIZE / (2 * SAMPLE_RATE);
        DatagramPacket activeMonitorDgram = new DatagramPacket(new byte[1], 1);
        int pos, blockPos;
        int nBlock;
        byte count=0;
        while (continueAudio) {
            try {

                pos=0;
                blockPos=0;
                nBlock=0;
                do {
                    while(blockReady[nBlock] && continueAudio) {
                        try {
                            Thread.sleep(timeout);
                        }catch(InterruptedException e) {}
                    }

                    if(blockPos + DATAGRAM_BUFFER_SIZE > AUDIO_BUFFER_SIZE) {
                        while(blockReady[nBlock + 1] && continueAudio) {
                            try {
                                Thread.sleep(timeout);
                            }catch(InterruptedException e) {}
                        }
                    }
                    dgram.setData(audioBuffer, pos, DATAGRAM_BUFFER_SIZE);
                    wrapper.receive(dgram);

/*                    if(++count != audioBuffer[pos]) {
                        Log.d(Globals.TAG, "Paquete perdido!!! "+count);
                        count=audioBuffer[pos];
                    } */

                    pos += dgram.getLength();
                    blockPos += dgram.getLength();
                    audioControl.receivedPos+=dgram.getLength();

                    if(blockPos >= AUDIO_BUFFER_SIZE) {
                        blockReady[nBlock]=true;
                        nBlock++;
                        blockPos -= AUDIO_BUFFER_SIZE;
                    }
                } while(pos + DATAGRAM_BUFFER_SIZE <= audioBuffer.length);

                blockReady[AUDIO_BLOCKS - 1] = true;
                audioControl.loopEndPos=pos;

                wrapper.send(activeMonitorDgram);

            }catch (SocketTimeoutException e) {
                Log.d(Globals.TAG, "Timeout de lectura del socket");
                continueAudio=false;
            } catch (IOException e) {
                Log.d(Globals.TAG, "Error en la lectura del socket");
                continueAudio = false;
            }catch (Exception e) {
                e.printStackTrace();
                Log.d(Globals.TAG, "Error en la reproducción del audio");
                continueAudio=false;
            }
        }

        Log.d(Globals.TAG, "Finaliza la recepción del streaming de auido");
        audioTrack.release();

        Globals.vehicleDeviceStatus.audioOn = false;
        broadcastIntent.putExtra(Globals.P2P_OP, Globals.AUDIO);
        context.sendOrderedBroadcast(broadcastIntent, null);

        stopAudio();
    }

    public void stopAudio() {

        Log.d(Globals.TAG, "Audio interrumpido");

        synchronized (this) {
            if (agent != null) {
                try {
                    agent.free();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            agent=null;

            activeAgent = false;

            continueAudio = false;
        }

    }


    public void bind(int mode) throws SocketException {

        int port=SERVICE_PORT;
        for(int i=0; i<100; i++) {
            port+=mode;
            try {
                dsocket = new DatagramSocket(port);
                return;
            } catch (SocketException e) {
                Log.d(Globals.TAG, "Puerto " + port + "no disponible");
            }
        }
        unbind();
        throw new SocketException();
    }

    public void unbind() {
        if(dsocket!=null)
            dsocket.close();
    }

    public InetSocketAddress getPublicAddr() {
        final int MAX_RETRIES=3;
        final int TIMEOUT=5000;

        if(dsocket==null)
            return(null);

        ByteBuffer buff=ByteBuffer.allocate(28);
        byte[] receiveData=new byte[128];

        buff.order(ByteOrder.BIG_ENDIAN);
        buff.putShort(BIND_REQUEST);
        buff.putShort((short)8); //Length

        Random r=new Random();
        long id1= r.nextLong();
        long id2=r.nextLong();

        buff.putLong(id1);
        buff.putLong(id2);

        buff.putShort((short)0x0003);
        buff.putShort((short)4);
        buff.putInt(2);

        SocketAddress dest = new InetSocketAddress(STUNT_HOST, STUNT_PORT);
        DatagramPacket reqDgram=null;
        DatagramPacket respDgram=new DatagramPacket(receiveData, receiveData.length);
        try {
            reqDgram = new DatagramPacket(buff.array(), 28, dest);
        } catch(SocketException e) {
            Log.d(Globals.TAG, "Error al crear el datagrama BIND_REQUEST");
            return(null);
        }

        int retry_count=0;
        do {
            try {
                Log.d(Globals.TAG, "PASO 1");
                dsocket.send(reqDgram);
                Log.d(Globals.TAG, "PASO 2");
                dsocket.setSoTimeout(TIMEOUT);
                try {
                    Log.d(Globals.TAG, "PASO 3");
                    dsocket.receive(respDgram);
Log.d(Globals.TAG, "STUNT IP Addr: "+respDgram.getAddress().getHostAddress()+":"+respDgram.getPort());
                    int size=respDgram.getLength();
                    int pos=0;
                    if(size<20) {
                        Log.d(Globals.TAG, "Formato incorrecto del mensaje UDPLinkMgr recibido");
                        return(null);
                    }
                    buff=ByteBuffer.wrap(respDgram.getData());
Log.d(Globals.TAG, "PASO 4");
                    int messageType=buff.getShort();
                    if(messageType!=BIND_RESPONSE) {
                        Log.d(Globals.TAG, "Tipo de mensaje  UDPLinkMgr de respuesta incorrecto");
                        return(null);
                    }
                    buff.getShort();
                    buff.getLong();
                    buff.getLong();
                    pos+=20;

                    short attrType, attrLength;
                    while(pos<size) {
                        if(pos + 12 > size) {
                            Log.d(Globals.TAG, "Formato incorrecto de los parámetros del mensaje UDPLinkMgr recibido");
                            return(null);
                        }
                        attrType=buff.getShort();
                        attrLength=buff.getShort();

                        if(attrType==MAPPED_ADDRESS) {
                            int family=buff.getShort() & 0xFF;
                            if(family!=0x01) {
                                Log.d(Globals.TAG, "Valor incorrecto de Address Family");
                                return(null);
                            }
                            int port=(0x000000FF & (int)buff.get())<<8 |
                                    (0x000000FF & (int)buff.get());
                            byte[] addr=new byte[4];
                            buff.get(addr);

                            return(new InetSocketAddress(InetAddress.getByAddress(addr), port));

                        } else {
                            pos+=attrLength;
                            buff.position(pos);
                        }
                    }

                } catch(SocketTimeoutException e) {
                    Log.d(Globals.TAG, "STUNT server timeout");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(Globals.TAG, "Error al obtener la dirección púbilca");
            }
        } while(retry_count++ < MAX_RETRIES);

        return(null);
    }

    public void sendUdpStream(final String host, final int port) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                byte audioBuffer[] = new byte[AUDIO_BUFFER_SIZE];

                final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        AUDIO_BUFFER_SIZE);


                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.d(Globals.TAG, "Error al inicializar captura de audio");
                    return;
                }

                SocketAddress dest = new InetSocketAddress(host, port);
                DatagramPacket dgram=null;
                try {
                    dgram = new DatagramPacket(audioBuffer, AUDIO_BUFFER_SIZE, dest);
                }catch(SocketException e) {
                    Log.d(Globals.TAG, "Error al crear datagreama UDP");
                    return;
                }

                int bytesRead;
                continueAudio = true;

                record.startRecording();


                while (continueAudio)

                {
                    bytesRead = record.read(audioBuffer, 0, audioBuffer.length);

                    try {
                        try {
                            Log.d(Globals.TAG, "Source: " + dsocket.getLocalAddress().getHostAddress() + ":" + dsocket.getPort() + " Destination: " + dgram.getAddress().getHostAddress() + ":" + dgram.getPort());
                        }catch (Exception e) {}
                        dgram = new DatagramPacket(audioBuffer, bytesRead, dest);
                        dsocket.send(dgram);
                    } catch (IOException e) {
                        Log.d(Globals.TAG, "Error al enviar datagrama UDP de audio");
                    }
                }

                record.stop();
                record.release();
            }
        }.start();
    }

    public void receiveUdpStream(final String host, final int port) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                byte audioBuffer[] = new byte[AUDIO_BUFFER_SIZE];

                SocketAddress dest = new InetSocketAddress("80.30.122.197", 21000);
                try {
                    DatagramPacket puchDatagram = new DatagramPacket(new byte[1], 1, dest);
                    dsocket.send(puchDatagram);
                } catch (Exception e) {
                    Log.d(Globals.TAG, "Error al enviar punch datagram");
                }

                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        AUDIO_BUFFER_SIZE,
                        AudioTrack.MODE_STREAM);

                DatagramPacket dgram = new DatagramPacket(audioBuffer, AUDIO_BUFFER_SIZE);

                try {
                    dsocket.setSoTimeout(1000);
                } catch (IOException e) {
                    Log.d(Globals.TAG, "Error al establecer socket timeout de lectura");
                    return;
                }

                try {
                    DatagramPacket puchDatagram = new DatagramPacket(new byte[1], 1, dest);
                    dsocket.send(puchDatagram);
                    Log.d(Globals.TAG, "Puch datagram enviado");
                } catch (Exception exception) {
                    Log.d(Globals.TAG, "Error al enviar punch datagram");
                }

                continueAudio = true;

                audioTrack.play();

                while (continueAudio) {
                    try {
                        dsocket.receive(dgram);
                        Log.d(Globals.TAG, new String(dgram.getData()));
                        audioTrack.write(audioBuffer, 0, dgram.getLength());
                    } catch (SocketTimeoutException e) {
                        Log.d(Globals.TAG, "Timeout de lectura del socket");
                        try {
                            DatagramPacket puchDatagram = new DatagramPacket(new byte[1], 1, dest);
                            dsocket.send(puchDatagram);
                            Log.d(Globals.TAG, "Source: " + dsocket.getLocalAddress().getHostAddress() + ":" + dsocket.getPort() + " Destination: " + dgram.getAddress().getHostAddress() + ":" + dgram.getPort());
                        } catch (Exception exception) {
                            Log.d(Globals.TAG, "Error al enviar punch datagram");
                        }
                    } catch (IOException e) {
                        Log.d(Globals.TAG, "Error en la lectura del socket");
                        return;
                    }
                }
                audioTrack.release();
            }
        }.start();
    }

    class AudioControl {
        public long playPos=0;
        public long receivedPos=0;
        public int loopEndPos=DATAGRAM_BUFFER_SIZE;
    }

    public class StateListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            synchronized (UDPLinkMgr.this) {
                if(agent==null)
                    return;

                if (evt.getSource() instanceof Agent) {
                    Agent agent = (Agent) evt.getSource();
                    if (agent.getState().equals(IceProcessingState.TERMINATED)) {
                        // Your agent is connected. Terminated means ready to communicate
                        for (IceMediaStream stream : agent.getStreams()) {
                            if (stream.getName().contains("audio")) {
                                Component rtpComponent = stream.getComponent(org.ice4j.ice.Component.RTP);
                                rtpPair = rtpComponent.getSelectedPair();
                                Log.d(Globals.TAG, "ICE Candidate selected");
                                // We use IceSocketWrapper, but you can just use the UDP socket
                                // The advantage is that you can change the protocol from UDP to TCP easily
                                // Currently only UDP exists so you might not need to use the wrapper.
                                // wrapper  = rtpPair.getIceSocketWrapper();

                                // Get information about remote address for packet settings
                                //                          wrapper=rtpPair.getParentComponent().getSocketWrapper();

//                            Log.d(Globals.TAG, "WRAPPER Local: "+wrapper.getLocalAddress().getHostAddress()+":"+wrapper.getLocalPort());

//                                Candidate c = rtpPair.getLocalCandidate().getRelatedCandidate();
//                            Log.d(Globals.TAG, "Related Local candidate: "+c.getHostAddress().getHostAddress()+":"+c.getHostAddress().getPort());


/*                                TransportAddress ta = rtpPair.getLocalCandidate().getTransportAddress();
                                hostname = ta.getAddress();
                                port = ta.getPort();

                                hostname = rtpPair.getLocalCandidate().getHostAddress().getAddress();
                                port = rtpPair.getLocalCandidate().getHostAddress().getPort();
                                Log.d(Globals.TAG, "Transport: " + hostname + ":" + port); */

                            }
                        }
                    }
                }
            }
        }
    }
}
