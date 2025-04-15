package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.adapters.ReleasesAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.models.Release;
import com.example.ratertune.utils.SessionManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllReleasesActivity extends AppCompatActivity implements ReleasesAdapter.OnReleaseClickListener {
    private RecyclerView releasesRecyclerView;
    private List<Release> releasesList;
    private ReleasesAdapter releasesAdapter;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private Spinner sortSpinner;
    
    // Константы для сортировки
    private static final int SORT_BY_DATE_NEW = 0;
    private static final int SORT_BY_DATE_OLD = 1;
    private static final int SORT_BY_TITLE_AZ = 2;
    private static final int SORT_BY_TITLE_ZA = 3;
    private static final int SORT_BY_ARTIST_AZ = 4;
    private static final int SORT_BY_ARTIST_ZA = 5;
    
    private int currentSortOption = SORT_BY_DATE_NEW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_releases);

        // Инициализация toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Все альбомы");

        // Инициализация SupabaseClient и SessionManager
        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);

        // Инициализация спиннера для сортировки
        sortSpinner = findViewById(R.id.sortSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = position;
                sortReleases();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Инициализация RecyclerView
        releasesRecyclerView = findViewById(R.id.allReleasesRecyclerView);
        releasesList = new ArrayList<>();
        releasesAdapter = new ReleasesAdapter(releasesList, this, this);
        
        // Настраиваем сетку 2x2 для альбомов
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        releasesRecyclerView.setLayoutManager(layoutManager);
        releasesRecyclerView.setAdapter(releasesAdapter);

        // Загружаем все релизы
        loadAllReleases();
    }

    private void loadAllReleases() {
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
                
                // Сортируем и обновляем UI в основном потоке
                runOnUiThread(() -> {
                    sortReleases();
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AllReleasesActivity.this, "Ошибка загрузки альбомов: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void sortReleases() {
        if (releasesList.isEmpty()) {
            return;
        }
        
        switch (currentSortOption) {
            case SORT_BY_DATE_NEW:
                // Сортируем по дате (сначала новые)
                Collections.sort(releasesList, (release1, release2) -> {
                    String date1Str = release1.getReleaseDate();
                    String date2Str = release2.getReleaseDate();
                    
                    // Проверка на null или пустые строки
                    if (date1Str == null || date1Str.isEmpty()) return 1;
                    if (date2Str == null || date2Str.isEmpty()) return -1;
                    if (date1Str.equals(date2Str)) return 0;
                    
                    try {
                        // Пробуем разные форматы даты
                        Date date1 = parseDate(date1Str);
                        Date date2 = parseDate(date2Str);
                        
                        if (date1 != null && date2 != null) {
                            return date2.compareTo(date1); // Обратный порядок для новых сначала
                        } else {
                            // Если не удалось распарсить даты, сравниваем как строки
                            return date2Str.compareTo(date1Str);
                        }
                    } catch (Exception e) {
                        // В случае ошибки просто сравниваем строки
                        return date2Str.compareTo(date1Str);
                    }
                });
                break;
                
            case SORT_BY_DATE_OLD:
                // Сортируем по дате (сначала старые)
                Collections.sort(releasesList, (release1, release2) -> {
                    String date1Str = release1.getReleaseDate();
                    String date2Str = release2.getReleaseDate();
                    
                    // Проверка на null или пустые строки
                    if (date1Str == null || date1Str.isEmpty()) return 1;
                    if (date2Str == null || date2Str.isEmpty()) return -1;
                    if (date1Str.equals(date2Str)) return 0;
                    
                    try {
                        // Пробуем разные форматы даты
                        Date date1 = parseDate(date1Str);
                        Date date2 = parseDate(date2Str);
                        
                        if (date1 != null && date2 != null) {
                            return date1.compareTo(date2); // Прямой порядок для старых сначала
                        } else {
                            // Если не удалось распарсить даты, сравниваем как строки
                            return date1Str.compareTo(date2Str);
                        }
                    } catch (Exception e) {
                        // В случае ошибки просто сравниваем строки
                        return date1Str.compareTo(date2Str);
                    }
                });
                break;
                
            case SORT_BY_TITLE_AZ:
                // Сортируем по названию (А-Я)
                Collections.sort(releasesList, (release1, release2) -> 
                    release1.getTitle().compareToIgnoreCase(release2.getTitle())
                );
                break;
                
            case SORT_BY_TITLE_ZA:
                // Сортируем по названию (Я-А)
                Collections.sort(releasesList, (release1, release2) -> 
                    release2.getTitle().compareToIgnoreCase(release1.getTitle())
                );
                break;
                
            case SORT_BY_ARTIST_AZ:
                // Сортируем по исполнителю (А-Я)
                Collections.sort(releasesList, (release1, release2) -> 
                    release1.getArtist().compareToIgnoreCase(release2.getArtist())
                );
                break;
                
            case SORT_BY_ARTIST_ZA:
                // Сортируем по исполнителю (Я-А)
                Collections.sort(releasesList, (release1, release2) -> 
                    release2.getArtist().compareToIgnoreCase(release1.getArtist())
                );
                break;
        }
        
        releasesAdapter.notifyDataSetChanged();
    }
    
    // Вспомогательный метод для парсинга дат разных форматов
    private Date parseDate(String dateStr) {
        // Пробуем разные форматы даты
        SimpleDateFormat[] formats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateStr);
            } catch (ParseException ignored) {
                // Пробуем следующий формат
            }
        }
        
        return null; // Не удалось распарсить дату
    }

    @Override
    public void onReleaseClick(Release release) {
        // Открываем экран деталей релиза при клике
        Intent intent = new Intent(this, ReleaseDetailsActivity.class);
        intent.putExtra("id", release.getId());
        intent.putExtra("title", release.getTitle());
        intent.putExtra("artist", release.getArtist());
        intent.putExtra("imageUrl", release.getImageUrl());
        intent.putExtra("rating", release.getRating());
        intent.putExtra("releaseDate", release.getReleaseDate());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 