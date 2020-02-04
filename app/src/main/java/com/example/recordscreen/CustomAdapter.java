package com.example.recordscreen;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class CustomAdapter extends BaseAdapter {

    Context c;
    ArrayList<Video> videos;
    private PopupMenu popUp;

    public CustomAdapter(Context c, ArrayList<Video> videos) {
        this.c = c;
        this.videos = videos;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Object getItem(int i) {
        return videos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(c).inflate(R.layout.custom_list_view,viewGroup,false);
        }
        final Video v = (Video) this.getItem(i);

        TextView videoName = (TextView) view.findViewById(R.id.videoName);
        ImageView videoImage = (ImageView) view.findViewById(R.id.videoImg);
        ImageButton menuBtn = (ImageButton) view.findViewById(R.id.menuButton);
        TextView videoDuration = (TextView) view.findViewById(R.id.videoDuration);
        TextView videoSize = (TextView) view.findViewById(R.id.videoSize);

        videoName.setText(v.getName());
        Glide.with(c).asBitmap().load(v.getUri()).into(videoImage);
        if (v.getUri() != null) {
            videoDuration.setText("Duration: " + videoDuration(v.getUri()));
        } else {
            videoDuration.setText("Duration: 00.00");
        }

        File file = new File(Environment.getExternalStorageDirectory(), "ScreenCapture/" + v.getName());
        long size = file.length();
        size = (long) size/1024;
        videoSize.setText("Size: " + size + "KB");

        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popUp = new PopupMenu(c, view);
                MenuInflater inflater = popUp.getMenuInflater();
                inflater.inflate(R.menu.menu, popUp.getMenu());
                popUp.show();
                popUp.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {

                            case R.id.play:
                                Intent playerIntent = PlayerActivity.getStartIntent(c,v.getUri().toString());
                                c.startActivity(playerIntent);
                                break;

                            case R.id.delete:
                                deleteVideos(i);
                                break;

                            case R.id.share:

                                break;

                        }
                        return false;
                    }
                });
            }
        });


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(c, v.getUri().toString(),Toast.LENGTH_SHORT).show();
                Intent playerIntent = PlayerActivity.getStartIntent(c,v.getUri().toString());
                c.startActivity(playerIntent);
            }
        });

        return view;

    }

    public void updateVideosList(ArrayList<Video> v) {
        this.videos = v;
        this.notifyDataSetChanged();
    }

    public void deleteVideos(int position){
        String videoFile = this.videos.get(position).getName();

        this.videos.remove(position);
        this.notifyDataSetChanged();

        File fileName = new File(Environment.getExternalStorageDirectory(), "ScreenCapture/" + videoFile);
        fileName.delete();
        Log.d("Folder check", videoFile + "");

    }

    public String videoDuration(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        //use one of overloaded setDataSource() functions to set your data source

        retriever.setDataSource(c, uri);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMillisec = Long.parseLong(time );
        retriever.release();

        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeInMillisec),
                TimeUnit.MILLISECONDS.toMinutes(timeInMillisec) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeInMillisec)),
                TimeUnit.MILLISECONDS.toSeconds(timeInMillisec) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMillisec)));

        return hms;
    }
}
