package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.adapters.ReleasesAdapter;
import com.example.ratertune.adapters.ReviewsAdapter;
import com.example.ratertune.adapters.StoriesAdapter;
import com.example.ratertune.adapters.TopMonthlyReleasesAdapter;
import com.example.ratertune.adapters.PopularUsersAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.api.StoriesListCallback;
import com.example.ratertune.models.Release;
import com.example.ratertune.models.Review;
import com.example.ratertune.models.Story;
import com.example.ratertune.models.PopularUser;
import com.example.ratertune.utils.PicassoCache;
import com.example.ratertune.utils.SessionManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements ReleasesAdapter.OnReleaseClickListener, TopMonthlyReleasesAdapter.OnReleaseClickListener {
    private static final String TAG = "MainActivity";
    private static final int MAX_TOP_RELEASES = 5;

    private RecyclerView latestReviewsRecyclerView;
    private TextView noLatestReviewsText;
    
    private RecyclerView topMonthlyReleasesRecyclerView;
    private TextView noMonthlyReleasesText;
    
    private RecyclerView popularUsersRecyclerView;
    private TextView noPopularUsersText;
    
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    private List<Story> storiesList;
    private StoriesAdapter storiesAdapter;
    
    private List<Release> releasesList;
    private ReleasesAdapter releasesAdapter;
    
    private List<Release> topMonthlyReleasesList;
    private TopMonthlyReleasesAdapter topMonthlyReleasesAdapter;
    
    private List<Review> latestReviewsList;
    private ReviewsAdapter latestReviewsAdapter;
    
    private List<PopularUser> popularUsersList;
    private PopularUsersAdapter popularUsersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация SupabaseClient и SessionManager
        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);
        supabaseClient.setSessionManager(sessionManager);

        // Инициализация UI элементов
        RecyclerView storiesRecyclerView = findViewById(R.id.storiesRecycler);
        RecyclerView releasesRecyclerView = findViewById(R.id.releasesRecyclerView);
        latestReviewsRecyclerView = findViewById(R.id.latestReviewsRecyclerView);
        noLatestReviewsText = findViewById(R.id.noLatestReviewsText);
        
        topMonthlyReleasesRecyclerView = findViewById(R.id.topMonthlyReleasesRecyclerView);
        noMonthlyReleasesText = findViewById(R.id.noMonthlyReleasesText);
        
        popularUsersRecyclerView = findViewById(R.id.popularUsersRecyclerView);
        noPopularUsersText = findViewById(R.id.noPopularUsersText);
        
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton addStoryButton = findViewById(R.id.addStoryButton);
        ImageButton addReleaseButton = findViewById(R.id.addReleaseButton);
        TextView viewAllReleasesButton = findViewById(R.id.viewAllReleasesButton);
        TextView viewAllReviewsButton = findViewById(R.id.viewAllReviewsButton);

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        
        addStoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddStoryActivity.class);
            startActivity(intent);
        });
        
        addReleaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReleaseActivity.class);
            startActivity(intent);
        });
        
        viewAllReleasesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllReleasesActivity.class);
            startActivity(intent);
        });
        
        viewAllReviewsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllReviewsActivity.class);
            startActivity(intent);
        });
        
        // Настройка RecyclerView для сторизов
        storiesList = new ArrayList<>();
        storiesAdapter = new StoriesAdapter(storiesList, this::onStoryClick, this);
        LinearLayoutManager storiesLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        storiesRecyclerView.setLayoutManager(storiesLayoutManager);
        storiesRecyclerView.setAdapter(storiesAdapter);
        
        // Настройка RecyclerView для релизов
        releasesList = new ArrayList<>();
        releasesAdapter = new ReleasesAdapter(releasesList, this, this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        releasesRecyclerView.setLayoutManager(layoutManager);
        releasesRecyclerView.setAdapter(releasesAdapter);

        // Настройка RecyclerView для последних рецензий
        latestReviewsList = new ArrayList<>();
        latestReviewsAdapter = new ReviewsAdapter(latestReviewsList, this::onReviewClick);
        LinearLayoutManager reviewsLayoutManager = new LinearLayoutManager(this);
        latestReviewsRecyclerView.setLayoutManager(reviewsLayoutManager);
        latestReviewsRecyclerView.setAdapter(latestReviewsAdapter);
        
        // Настройка RecyclerView для топ релизов месяца
        topMonthlyReleasesList = new ArrayList<>();
        topMonthlyReleasesAdapter = new TopMonthlyReleasesAdapter(topMonthlyReleasesList, this, this);
        
        // Create a grid layout with 2 columns for the podium-style layout
        GridLayoutManager topReleasesLayoutManager = new GridLayoutManager(this, 2);
        
        // Configure span sizes: first place takes up full width (2 spans)
        topReleasesLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // First position (position 0) takes full width
                if (position == 0) {
                    return 2;
                }
                // Other positions take 1 column
                return 1;
            }
        });
        
        topMonthlyReleasesRecyclerView.setLayoutManager(topReleasesLayoutManager);
        topMonthlyReleasesRecyclerView.setAdapter(topMonthlyReleasesAdapter);
        
        // Настройка RecyclerView для популярных пользователей
        popularUsersList = new ArrayList<>();
        popularUsersAdapter = new PopularUsersAdapter(popularUsersList, this, this::onUserClick);
        LinearLayoutManager popularUsersLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        popularUsersRecyclerView.setLayoutManager(popularUsersLayoutManager);
        popularUsersRecyclerView.setAdapter(popularUsersAdapter);
        
        // Загрузка данных
        loadUserStories();
        loadUserReleases();
        loadLatestReviews();
        loadMonthlyStatistics();
        loadPopularUsers();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на экран
        loadUserStories();
        loadUserReleases();
        loadLatestReviews();
        loadMonthlyStatistics();
        loadPopularUsers();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Отменяем загрузки изображений с тегами "main_activity" при уходе с экрана
        PicassoCache.cancelTag("main_activity");
        PicassoCache.cancelTag("top_monthly");
    }
    
    @Override
    protected void onDestroy() {
        // Отменяем все активные загрузки для предотвращения утечек памяти
        if (releasesList != null) {
            for (Release release : releasesList) {
                PicassoCache.cancelTag(release.getId());
            }
        }
        
        if (storiesList != null) {
            for (Story story : storiesList) {
                PicassoCache.cancelTag(story.getId());
            }
        }
        
        super.onDestroy();
    }
    
    private void onStoryClick(Story story) {
        // Отмечаем сториз как просмотренный
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        supabaseClient.markStoryAsViewed(story.getId(), userId, token, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Обновляем статус просмотра
                runOnUiThread(() -> {
                    storiesAdapter.updateStoryViewedStatus(story.getId(), true);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error marking story as viewed: " + errorMessage);
            }
        });
        
        Intent intent = new Intent(MainActivity.this, StoryViewActivity.class);
        intent.putExtra("id", story.getId());
        intent.putExtra("imageUrl", story.getImageUrl());
        intent.putExtra("text", story.getText());
        startActivity(intent);
    }
    
    private void loadUserStories() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Сначала загрузим просмотренные сторизы
        supabaseClient.getViewedStoriesForUser(userId, token, new SupabaseClient.ViewedStoriesCallback() {
            @Override
            public void onSuccess(List<String> viewedStoryIds) {
                // Теперь загрузим все сторизы
                supabaseClient.getAllStories(token, new StoriesListCallback() {
                    @Override
                    public void onSuccess(List<Story> stories) {
                        // Отмечаем просмотренные сторизы
                        for (Story story : stories) {
                            if (viewedStoryIds.contains(story.getId())) {
                                story.setViewed(true);
                            }
                        }
                        
                        runOnUiThread(() -> {
                            storiesList.clear();
                            storiesList.addAll(stories);
                            storiesAdapter.notifyDataSetChanged();
                            
                            // Предварительно загружаем все изображения в кэш
                            for (Story story : stories) {
                                PicassoCache.preloadImage(MainActivity.this, story.getImageUrl());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Ошибка загрузки сторизов: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                // Даже если ошибка с просмотренными сторизами, всё равно загружаем сторизы
                supabaseClient.getAllStories(token, new StoriesListCallback() {
                    @Override
                    public void onSuccess(List<Story> stories) {
                        runOnUiThread(() -> {
                            storiesList.clear();
                            storiesList.addAll(stories);
                            storiesAdapter.notifyDataSetChanged();
                            
                            // Предварительно загружаем все изображения в кэш
                            for (Story story : stories) {
                                PicassoCache.preloadImage(MainActivity.this, story.getImageUrl());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Ошибка загрузки сторизов: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }
    
    private void loadUserReleases() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        supabaseClient.getAllReleases(token, new SupabaseClient.ReleasesListCallback() {
            @Override
            public void onSuccess(List<SupabaseClient.Release> releases) {
                // Конвертируем SupabaseClient.Release в нашу модель Release
                releasesList.clear();
                
                // Сначала добавляем все релизы с временными оценками 0
                for (SupabaseClient.Release release : releases) {
                    Release modelRelease = new Release(
                            String.valueOf(release.getId()),
                            release.getTitle(),
                            release.getArtist(),
                            release.getCoverUrl(),
                            0.0f,
                            release.getReleaseDate()
                    );
                    releasesList.add(modelRelease);
                }
                
                // Обновляем UI в основном потоке
                runOnUiThread(() -> {
                    releasesAdapter.notifyDataSetChanged();
                    
                    // Для каждого релиза запрашиваем средний рейтинг
                    for (int i = 0; i < releasesList.size(); i++) {
                        final int position = i;
                        String releaseId = releasesList.get(i).getId();
                        supabaseClient.calculateAverageRating(releaseId, token, new SupabaseClient.AverageRatingCallback() {
                            @Override
                            public void onSuccess(float averageRating, int reviewsCount) {
                                // Обновляем рейтинг в модели и в адаптере
                                runOnUiThread(() -> {
                                    releasesList.get(position).setRating(averageRating);
                                    releasesAdapter.notifyItemChanged(position);
                                });
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Просто логируем ошибку, не показываем пользователю
                                Log.e(TAG, "Error calculating rating for release " + releaseId + ": " + errorMessage);
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки релизов: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadLatestReviews() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Получаем последние 4 рецензии
        supabaseClient.getLatestReviews(token, 4, new SupabaseClient.ReviewsCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    latestReviewsList.clear();
                    latestReviewsList.addAll(reviews);
                    
                    if (reviews.isEmpty()) {
                        noLatestReviewsText.setVisibility(View.VISIBLE);
                        latestReviewsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noLatestReviewsText.setVisibility(View.GONE);
                        latestReviewsRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Предварительно загружаем аватары пользователей в кэш
                        for (Review review : reviews) {
                            if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
                                PicassoCache.preloadImage(MainActivity.this, review.getUserAvatarUrl());
                            }
                        }
                        
                        latestReviewsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + reviews.size() + " latest reviews");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки рецензий: " + error, Toast.LENGTH_SHORT).show();
                    noLatestReviewsText.setVisibility(View.VISIBLE);
                    latestReviewsRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }
    
    /**
     * Загружает статистику релизов за текущий месяц
     */
    private void loadMonthlyStatistics() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки
        noMonthlyReleasesText.setVisibility(View.GONE);
        topMonthlyReleasesRecyclerView.setVisibility(View.GONE);
        
        // Для упрощения получаем все релизы без фильтрации по дате на стороне Supabase
        // Фильтрацию будем делать на стороне клиента после получения релизов
        supabaseClient.getAllReleases(token, new SupabaseClient.ReleasesListCallback() {
            @Override
            public void onSuccess(List<SupabaseClient.Release> releases) {
                if (releases.isEmpty()) {
                    runOnUiThread(() -> {
                        noMonthlyReleasesText.setVisibility(View.VISIBLE);
                        topMonthlyReleasesRecyclerView.setVisibility(View.GONE);
                    });
                    return;
                }
                
                Log.d(TAG, "Получено " + releases.size() + " релизов");
                
                // Фильтруем релизы за текущий месяц
                List<SupabaseClient.Release> monthlyReleases = new ArrayList<>();
                
                // Получаем текущий месяц и год
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentYear = calendar.get(Calendar.YEAR);
                
                // Debug: логируем текущий месяц и год
                Log.d(TAG, "Фильтрация релизов за " + (currentMonth + 1) + "." + currentYear);
                
                // Анализируем каждый релиз
                for (SupabaseClient.Release release : releases) {
                    String releaseDateStr = release.getReleaseDate();
                    Log.d(TAG, "Релиз: " + release.getArtist() + " - " + release.getTitle() + ", дата: " + releaseDateStr);
                    
                    try {
                        // Проверяем формат даты и парсим её
                        SimpleDateFormat sdf;
                        if (releaseDateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        } else if (releaseDateStr.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                            sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        } else {
                            Log.e(TAG, "Неподдерживаемый формат даты: " + releaseDateStr);
                            continue;
                        }
                        
                        Date releaseDate = sdf.parse(releaseDateStr);
                        if (releaseDate == null) continue;
                        
                        Calendar releaseCal = Calendar.getInstance();
                        releaseCal.setTime(releaseDate);
                        
                        int releaseMonth = releaseCal.get(Calendar.MONTH);
                        int releaseYear = releaseCal.get(Calendar.YEAR);
                        
                        // Debug: логируем месяц и год релиза
                        Log.d(TAG, "Месяц релиза: " + (releaseMonth + 1) + ", год: " + releaseYear);
                        
                        // Проверяем, соответствует ли релиз текущему месяцу
                        if (releaseMonth == currentMonth && releaseYear == currentYear) {
                            monthlyReleases.add(release);
                            Log.d(TAG, "Релиз добавлен в статистику этого месяца");
                        } else {
                            Log.d(TAG, "Релиз не входит в текущий месяц");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке даты релиза: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "За текущий месяц найдено " + monthlyReleases.size() + " релизов");
                
                if (monthlyReleases.isEmpty()) {
                    runOnUiThread(() -> {
                        noMonthlyReleasesText.setVisibility(View.VISIBLE);
                        topMonthlyReleasesRecyclerView.setVisibility(View.GONE);
                    });
                    return;
                }
                
                // Создаем список для релизов с рейтингами
                final List<Release> ratedReleases = new ArrayList<>();
                
                // Счетчик для отслеживания полученных рейтингов
                final int[] ratingCounter = {0};
                
                // Конвертируем SupabaseClient.Release в нашу модель Release и получаем рейтинги
                for (SupabaseClient.Release release : monthlyReleases) {
                    String releaseId = String.valueOf(release.getId());
                    
                    // Получаем средний рейтинг для релиза
                    supabaseClient.calculateAverageRating(releaseId, token, new SupabaseClient.AverageRatingCallback() {
                        @Override
                        public void onSuccess(float averageRating, int reviewsCount) {
                            // Создаем объект релиза с рейтингом
                            Release ratedRelease = new Release(
                                    releaseId,
                                    release.getTitle(),
                                    release.getArtist(),
                                    release.getCoverUrl(),
                                    averageRating,
                                    release.getReleaseDate()
                            );
                            ratedRelease.setReviewsCount(reviewsCount);
                            
                            // Добавляем в список для дальнейшей сортировки
                            synchronized (ratedReleases) {
                                ratedReleases.add(ratedRelease);
                                ratingCounter[0]++;
                                
                                // Когда получены все рейтинги, сортируем и отображаем результаты
                                if (ratingCounter[0] == monthlyReleases.size()) {
                                    displayTopMonthlyReleases(ratedReleases);
                                }
                            }
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error calculating rating for release " + releaseId + ": " + errorMessage);
                            
                            // Создаем объект релиза без рейтинга
                            Release ratedRelease = new Release(
                                    releaseId,
                                    release.getTitle(),
                                    release.getArtist(),
                                    release.getCoverUrl(),
                                    0.0f,
                                    release.getReleaseDate()
                            );
                            
                            // Добавляем в список для дальнейшей сортировки
                            synchronized (ratedReleases) {
                                ratedReleases.add(ratedRelease);
                                ratingCounter[0]++;
                                
                                // Когда получены все рейтинги, сортируем и отображаем результаты
                                if (ratingCounter[0] == monthlyReleases.size()) {
                                    displayTopMonthlyReleases(ratedReleases);
                                }
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    noMonthlyReleasesText.setText("Ошибка загрузки: " + errorMessage);
                    noMonthlyReleasesText.setVisibility(View.VISIBLE);
                    topMonthlyReleasesRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }
    
    /**
     * Отображает визуально топ релизы за месяц
     * @param ratedReleases список релизов с рейтингами
     */
    private void displayTopMonthlyReleases(List<Release> ratedReleases) {
        // Сортируем релизы по рейтингу (от высокого к низкому)
        Collections.sort(ratedReleases, new Comparator<Release>() {
            @Override
            public int compare(Release r1, Release r2) {
                // Сначала сравниваем по рейтингу (в обратном порядке)
                int ratingComparison = Float.compare(r2.getRating(), r1.getRating());
                if (ratingComparison != 0) {
                    return ratingComparison;
                }
                
                // Если рейтинги равны, сравниваем по количеству отзывов (в обратном порядке)
                return Integer.compare(r2.getReviewsCount(), r1.getReviewsCount());
            }
        });
        
        // Ограничиваем список до MAX_TOP_RELEASES
        final List<Release> topReleases = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_TOP_RELEASES, ratedReleases.size()); i++) {
            topReleases.add(ratedReleases.get(i));
        }
        
        // Обновляем UI в основном потоке
        runOnUiThread(() -> {
            if (topReleases.isEmpty()) {
                noMonthlyReleasesText.setVisibility(View.VISIBLE);
                topMonthlyReleasesRecyclerView.setVisibility(View.GONE);
            } else {
                noMonthlyReleasesText.setVisibility(View.GONE);
                topMonthlyReleasesRecyclerView.setVisibility(View.VISIBLE);
                
                topMonthlyReleasesList.clear();
                topMonthlyReleasesList.addAll(topReleases);
                topMonthlyReleasesAdapter.notifyDataSetChanged();
                
                // Предварительная загрузка обложек альбомов
                for (Release release : topReleases) {
                    if (release.getImageUrl() != null && !release.getImageUrl().isEmpty()) {
                        PicassoCache.preloadImage(MainActivity.this, release.getImageUrl());
                    }
                }
            }
        });
    }
    
    // Обработка нажатия на релиз
    @Override
    public void onReleaseClick(Release release) {
        Intent intent = new Intent(MainActivity.this, ReleaseDetailsActivity.class);
        intent.putExtra("id", release.getId());
        intent.putExtra("title", release.getTitle());
        intent.putExtra("artist", release.getArtist());
        intent.putExtra("imageUrl", release.getImageUrl());
        intent.putExtra("rating", release.getRating());
        intent.putExtra("releaseDate", release.getReleaseDate());
        startActivity(intent);
    }

    private void onReviewClick(Review review) {
        // Получаем ID релиза из рецензии
        String releaseId = review.getReleaseId();
        
        // Получаем данные релиза перед открытием экрана
        String token = sessionManager.getAccessToken();
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки
        Toast.makeText(this, "Загрузка релиза...", Toast.LENGTH_SHORT).show();
        
        // Получаем данные о релизе
        supabaseClient.getRelease(releaseId, token, new SupabaseClient.ReleaseCallback() {
            @Override
            public void onSuccess(SupabaseClient.Release release) {
                runOnUiThread(() -> {
                    // Открываем экран с деталями релиза
                    Intent intent = new Intent(MainActivity.this, ReleaseDetailsActivity.class);
                    intent.putExtra("id", String.valueOf(release.getId()));
                    intent.putExtra("title", release.getTitle());
                    intent.putExtra("artist", release.getArtist());
                    intent.putExtra("imageUrl", release.getCoverUrl());
                    intent.putExtra("rating", 0.0f); // Временно используем 0
                    intent.putExtra("releaseDate", release.getReleaseDate());
                    startActivity(intent);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки релиза: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onUserClick(PopularUser user) {
        // Запуск активности просмотра профиля пользователя
        UserProfileActivity.start(
                this,
                user.getUserId(),
                user.getUserName(),
                user.getAvatarUrl()
        );
    }
    
    /**
     * Загружает список популярных пользователей
     */
    private void loadPopularUsers() {
        String token = sessionManager.getAccessToken();
        
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки
        noPopularUsersText.setVisibility(View.VISIBLE);
        noPopularUsersText.setText("Загрузка...");
        popularUsersRecyclerView.setVisibility(View.GONE);
        
        // Максимальное количество пользователей для отображения
        final int MAX_POPULAR_USERS = 5;
        
        supabaseClient.getPopularUsers(token, MAX_POPULAR_USERS, new SupabaseClient.PopularUsersCallback() {
            @Override
            public void onSuccess(List<PopularUser> users) {
                runOnUiThread(() -> {
                    if (users == null || users.isEmpty()) {
                        noPopularUsersText.setText("Нет популярных пользователей");
                        noPopularUsersText.setVisibility(View.VISIBLE);
                        popularUsersRecyclerView.setVisibility(View.GONE);
                    } else {
                        noPopularUsersText.setVisibility(View.GONE);
                        popularUsersRecyclerView.setVisibility(View.VISIBLE);
                        
                        popularUsersList.clear();
                        popularUsersList.addAll(users);
                        popularUsersAdapter.notifyDataSetChanged();
                        
                        // Предварительно загружаем аватарки в кэш
                        for (PopularUser user : users) {
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                PicassoCache.preloadImage(MainActivity.this, user.getAvatarUrl());
                            }
                        }
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка загрузки популярных пользователей: " + errorMessage);
                    noPopularUsersText.setText("Ошибка загрузки данных");
                    noPopularUsersText.setVisibility(View.VISIBLE);
                    popularUsersRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }
}