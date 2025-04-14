package com.example.ratertune.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.models.Review;
import com.example.ratertune.utils.PicassoCache;
import com.squareup.picasso.Picasso;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView usernameText;
        TextView ratingText;
        TextView reviewText;
        TextView dateText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.reviewAvatar);
            usernameText = itemView.findViewById(R.id.reviewUsername);
            ratingText = itemView.findViewById(R.id.reviewRating);
            reviewText = itemView.findViewById(R.id.reviewText);
            dateText = itemView.findViewById(R.id.reviewDate);
        }

        void bind(Review review) {
            usernameText.setText(review.getUserName());
            ratingText.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));
            reviewText.setText(review.getText());
            
            // Загрузка аватара с кэшированием
            if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
                Picasso picasso = PicassoCache.getInstance(itemView.getContext());
                picasso.load(review.getUserAvatarUrl())
                        .noFade()
                        .noPlaceholder()
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_profile);
            }
            
            // Используем метод getFormattedDate() из модели Review
            String formattedDate = review.getFormattedDate();
            Log.d("ReviewsAdapter", "Using formatted date from model: " + formattedDate);
            dateText.setText(formattedDate);
        }
    }
} 