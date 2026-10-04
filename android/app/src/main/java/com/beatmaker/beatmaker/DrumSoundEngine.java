package com.beatmaker.beatmaker;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiny self-contained drum synth for the basic APK.
 * No external MIDI device or sample pack is required.
 */
public class DrumSoundEngine {
    public static final String[] NAMES = {"KICK", "RIM", "SNARE", "CLAP", "TOM", "CLOSED HAT", "OPEN HAT", "CRASH"};
    private static final int SR = 44100;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final short[][] sounds = new short[NAMES.length][];

    public DrumSoundEngine() {
        sounds[0] = kick();
        sounds[1] = rim();
        sounds[2] = snare();
        sounds[3] = clap();
        sounds[4] = tom();
        sounds[5] = hat(false);
        sounds[6] = hat(true);
        sounds[7] = crash();
    }

    public void play(int drum) {
        if (drum < 0 || drum >= sounds.length) return;
        final short[] data = sounds[drum];
        pool.execute(() -> {
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SR)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(data.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
            track.write(data, 0, data.length);
            track.setNotificationMarkerPosition(data.length);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                public void onMarkerReached(AudioTrack audioTrack) { audioTrack.release(); }
                public void onPeriodicNotification(AudioTrack audioTrack) {}
            });
            track.play();
        });
    }

    public void release() { pool.shutdownNow(); }

    private short[] make(double seconds, Voice voice) {
        int n = (int)(SR * seconds);
        short[] out = new short[n];
        Random random = new Random(1337);
        for (int i=0;i<n;i++) {
            double t=(double)i/SR;
            double v=voice.sample(t, seconds, random);
            v=Math.max(-1.0, Math.min(1.0, v));
            out[i]=(short)(v*32767.0);
        }
        return out;
    }
    private interface Voice { double sample(double t,double len,Random r); }

    private short[] kick() { return make(.42,(t,l,r)->{
        double env=Math.exp(-9*t); double f=150*Math.exp(-18*t)+48;
        return Math.sin(2*Math.PI*f*t)*env*.95;
    });}
    private short[] rim() { return make(.11,(t,l,r)->{
        double env=Math.exp(-38*t); return (Math.sin(2*Math.PI*920*t)+Math.sin(2*Math.PI*1470*t))*.35*env;
    });}
    private short[] snare() { return make(.24,(t,l,r)->{
        double env=Math.exp(-15*t); double noise=(r.nextDouble()*2-1);
        return (noise*.72+Math.sin(2*Math.PI*185*t)*.28)*env;
    });}
    private short[] clap() { return make(.25,(t,l,r)->{
        double burst=(Math.exp(-55*t)+.75*Math.exp(-55*Math.max(0,t-.035))+.55*Math.exp(-55*Math.max(0,t-.07)));
        if(t>.025&&t<.035) burst*=.15; if(t>.06&&t<.07) burst*=.15;
        return (r.nextDouble()*2-1)*burst*.55;
    });}
    private short[] tom() { return make(.34,(t,l,r)-> Math.sin(2*Math.PI*(120-45*t)*t)*Math.exp(-9*t)*.8); }
    private short[] hat(boolean open) { return make(open?.48:.09,(t,l,r)->{
        double env=Math.exp(-(open?8:48)*t); double noise=r.nextDouble()*2-1;
        double metallic=Math.sin(2*Math.PI*6200*t)*.25+Math.sin(2*Math.PI*9100*t)*.2;
        return (noise*.55+metallic)*env*.65;
    });}
    private short[] crash() { return make(.85,(t,l,r)->{
        double env=Math.exp(-4.5*t); double noise=r.nextDouble()*2-1;
        return (noise*.65+Math.sin(2*Math.PI*7300*t)*.2)*env*.55;
    });}
}
