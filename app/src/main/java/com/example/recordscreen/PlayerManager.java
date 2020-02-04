package com.example.recordscreen;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class PlayerManager {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final String TAG = "ExoManager";
    private static PlayerManager mInstance = null;
    PlayerView playerView;
    DefaultDataSourceFactory dataSourceFactory;
    String uriString = "";
    ArrayList<String> playList = null;
    Integer playlistIndex = 0;
    CallBacks.playerCallBack listner;
    private SimpleExoPlayer player;

    private PlayerManager(Context context){
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

        playerView = new PlayerView(context);
        playerView.setUseController(true);
        playerView.requestFocus();
        playerView.setPlayer(player);

        Uri mp4VideoUri = Uri.parse(uriString);

        dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "RecordScreen"), BANDWIDTH_METER);

        final MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(mp4VideoUri);

        player.prepare(videoSource);
        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
                Log.i(TAG, "onTimelineChanged: ");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                Log.i(TAG, "onTracksChanged: ");
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.i(TAG, "onLoadingChanged: ");
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.i(TAG, "onPlayerStateChanged: ");
                if (playbackState == 4 && playList != null && playlistIndex + 1 < playList.size()){
                    Log.e(TAG, "Song Changed...");

                    playlistIndex++;
                    listner.onItemClickOnItem(playlistIndex);
                    playStream(playList.get(playlistIndex));
                }else if (playbackState == 4 && playList != null && playlistIndex + 1 == playList.size()) {
                    player.setPlayWhenReady(false);
                }
                if (playbackState == 4 && listner != null) {
                    listner.onPlayingEnd();
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                Log.i(TAG, "onRepeatModeChanged: ");
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                Log.i(TAG, "onShuffleModeEnabledChanged: ");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.i(TAG, "onPlayerError: ");
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                Log.i(TAG, "onPositionDiscontinuity: ");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                Log.i(TAG, "onPlaybackParametersChanged: ");
            }

            @Override
            public void onSeekProcessed() {
                Log.i(TAG, "onSeekProcessed: ");
            }
        });
    }

    public static PlayerManager getSharedInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PlayerManager(context);
        }
        return mInstance;
    }
    public void setPlayerListener(CallBacks.playerCallBack playerCallBack) {
        listner = playerCallBack;
    }

    public PlayerView getPlayerView() {
        return playerView;
    }

    public void playStream(String urlToPlay) {
        uriString = urlToPlay;
        Uri mp4VideoUri = Uri.parse(uriString);
        MediaSource videoSource;
        // String filenameArray[] = urlToPlay.split("\\.");
        if (uriString.toUpperCase().contains("M3U8")) {
            videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mp4VideoUri, null, null);
        } else {
            mp4VideoUri = Uri.parse(urlToPlay);
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).setExtractorsFactory(new DefaultExtractorsFactory()).createMediaSource((mp4VideoUri));
        }


        // Prepare the player with the source.
        if (player != null && videoSource != null) {
            player.prepare(videoSource);
            player.setPlayWhenReady(true);
        }

    }

    public void pausePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    public void resumePlayer() {
        if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    public Boolean isPlayerPlaying() {
        return player.getPlayWhenReady();
    }

    public ArrayList<String> readURLs(String url) {
        if (url == null) return null;
        ArrayList<String> allURls = new ArrayList<String>();
        try {

            URL urls = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(urls
                    .openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                allURls.add(str);
            }
            in.close();
            return allURls;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
