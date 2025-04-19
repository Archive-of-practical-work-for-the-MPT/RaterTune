package com.example.ratertune.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.models.Release;
import com.example.ratertune.utils.PicassoCache;
import com.squareup.picasso.Picasso;
import java.util.List;

public class TopMonthlyReleasesAdapter extends RecyclerView.Adapter<TopMonthlyReleasesAdapter.ViewHolder> {
    
    private final List<Release> releases;
    private final Context context;
    private final OnReleaseClickListener listener;
    private final Picasso picasso;
    
    public interface OnReleaseClickListener {
        void onReleaseClick(Release release);
    }
    
    public TopMonthlyReleasesAdapter(List<Release> releases, Context context, OnReleaseClickListener listener) {
        this.releases = releases;
        this.context = context;
        this.listener = listener;
        this.picasso = PicassoCache.getInstance(context);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_release, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Release release = releases.get(position);
        
        // Set position badge (position + 1 to display 1-based indexing)
        int displayPosition = position + 1;
        holder.positionBadge.setText(String.valueOf(displayPosition));
        
        // Setup crown based on position
        if (displayPosition == 1) {
            // Gold crown for 1st place
            holder.crownImage.setImageResource(R.drawable.gold_crown);
            holder.crownImage.setVisibility(View.VISIBLE);
            holder.positionBadge.setBackgroundResource(R.drawable.circle_background);
        } else if (displayPosition == 2) {
            // Silver crown for 2nd place
            holder.crownImage.setImageResource(R.drawable.silver_crown);
            holder.crownImage.setVisibility(View.VISIBLE);
            holder.positionBadge.setBackgroundResource(R.drawable.silver_circle);
        } else if (displayPosition == 3) {
            // Bronze crown for 3rd place
            holder.crownImage.setImageResource(R.drawable.bronze_crown);
            holder.crownImage.setVisibility(View.VISIBLE);
            holder.positionBadge.setBackgroundResource(R.drawable.bronze_circle);
        } else {
            // Hide crown for other positions
            holder.crownImage.setVisibility(View.GONE);
            holder.positionBadge.setBackgroundResource(R.drawable.regular_circle);
        }
        
        // Load album cover
        if (release.getImageUrl() != null && !release.getImageUrl().isEmpty()) {
            picasso.load(release.getImageUrl())
                  .tag("top_monthly")
                  .into(holder.releaseImage);
        } else {
            holder.releaseImage.setImageResource(R.drawable.ic_album_placeholder);
        }
        
        // Set artist and album title
        holder.artistName.setText(release.getArtist());
        holder.albumTitle.setText(release.getTitle());
        
        // Set rating text only
        holder.ratingText.setText(String.format("%.1f", release.getRating()));
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReleaseClick(release);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return releases.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView positionBadge;
        final ImageView crownImage;
        final ImageView releaseImage;
        final TextView artistName;
        final TextView albumTitle;
        final TextView ratingText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            positionBadge = itemView.findViewById(R.id.positionBadge);
            crownImage = itemView.findViewById(R.id.crownImage);
            releaseImage = itemView.findViewById(R.id.releaseImage);
            artistName = itemView.findViewById(R.id.artistName);
            albumTitle = itemView.findViewById(R.id.albumTitle);
            ratingText = itemView.findViewById(R.id.ratingText);
        }
    }
} 