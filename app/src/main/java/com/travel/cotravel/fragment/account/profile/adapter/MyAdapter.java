package com.travel.cotravel.fragment.account.profile.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import com.travel.cotravel.R;
import com.travel.cotravel.fragment.account.profile.module.Upload;
import com.travel.cotravel.fragment.account.profile.ui.EditPhotoActivity;
import com.wajahatkarim3.easyflipview.EasyFlipView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ImageViewHolder> {
    private Context mcontext;
    private List<Upload> mUploads;
    String TAG = "AdapterClass";
    String uid;
    String previousValue="";
    String gender;

    public MyAdapter(Context context, String uid, String gender, List<Upload> uploads, PhotoInterface listener) {
        this.uid=uid;
        mcontext =context;
        mUploads =uploads;
        this.gender=gender;
        this.listener=listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(mcontext).inflate(R.layout.layout_images, parent,false);
        return new ImageViewHolder(v);

    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        Upload uploadCurrent = mUploads.get(position);

        if(mUploads.get(position).getName().equalsIgnoreCase("Video"))
        {
            holder.set_main.setText("View");
            holder.delete.setText("Remove video");
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVideoURI(Uri.parse(mUploads.get(position).getUrl()));
            holder.videoView.seekTo(1000);
            holder.progressBar.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.VISIBLE);

            holder.videoView.setOnPreparedListener(mp -> {
                mp.setVolume(0,0);
                mp.start();
            });
        }
        else {

            if(gender.equalsIgnoreCase("Female"))
            {
               holder.imageView.setImageResource(R.drawable.no_photo_female);
            }
            else {
                holder.imageView.setImageResource(R.drawable.no_photo_male);
            }

            holder.progressBar.setVisibility(View.GONE);

            if(!uploadCurrent.getUrl().equalsIgnoreCase(""))
            {
                Glide.with(mcontext).asBitmap().load(uploadCurrent.getUrl())
                        .centerCrop()
                        .override(450, 600)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                holder.imageView.setImageBitmap(resource);
                            }
                        });
                holder.imageView.setAdjustViewBounds(true);
                holder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            }
        }


        if(uploadCurrent.getType()==3)
        {
            holder.pp_eye.setText("Make Public");
        }
        else if(uploadCurrent.getType()==2)
        {
            holder.pp_eye.setText("Make Private");
        }
        else if(uploadCurrent.getType()==1){
            holder.ivTitle.setVisibility(View.VISIBLE);
            holder.pp_eye.setText("Make Private");

            ((EditPhotoActivity)mcontext).appDetails("CurProfilePhoto",uploadCurrent.getId());
        }

        holder.flipView.setOnFlipListener((flipView, newCurrentSide) -> {
            holder.set_main.setOnClickListener(view -> {

                if(mUploads.get(position).getName().equalsIgnoreCase("Video"))
                {
                    listener.playVideo(mUploads.get(position).getUrl());
                }
                else {
                    listener.setProfilePhoto(mUploads.get(position).getId(), ((EditPhotoActivity) mcontext).getAppDetails("CurProfilePhoto"), position);
                    holder.ivTitle.setVisibility(View.VISIBLE);
                }

            });

            holder.pp_eye.setOnClickListener(view -> listener.setPhotoAsPrivate(mUploads.get(position).getId()));

            holder.delete.setOnClickListener(view -> listener.removePhoto(mUploads.get(position).getId()));
        });
    }



    @Override
    public int getItemCount() {
        return mUploads.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageView,ivTitle;
        TextView set_main, pp_eye, delete;
        EasyFlipView flipView;
        ProgressBar progressBar;
        VideoView videoView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            videoView=itemView.findViewById(R.id.videoView);
            ivTitle=itemView.findViewById(R.id.ivTitle);

            flipView=itemView.findViewById(R.id.flipView);
            set_main=itemView.findViewById(R.id.set_main);
            pp_eye=itemView.findViewById(R.id.pp_eye);
            delete=itemView.findViewById(R.id.delete);
            progressBar=itemView.findViewById(R.id.progressBar);

        }
    }

    PhotoInterface listener;
    public interface PhotoInterface{
        void playVideo(String url);
        void setProfilePhoto(String id, String previousValue, int position);
        void removePhoto(String id);
        void setPhotoAsPrivate(String id);
    }

}