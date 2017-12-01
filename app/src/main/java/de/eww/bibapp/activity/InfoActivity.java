package de.eww.bibapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.eww.bibapp.R;
import de.eww.bibapp.adapter.RssAdapter;
import de.eww.bibapp.constants.Constants;
import de.eww.bibapp.decoration.DividerItemDecoration;
import de.eww.bibapp.network.RssService;
import me.toptas.rssconverter.RssConverterFactory;
import me.toptas.rssconverter.RssFeed;
import me.toptas.rssconverter.RssItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class InfoActivity extends BaseActivity implements Callback<RssFeed> {

    RecyclerView mRecyclerView;
    ProgressBar mProgressBar;
    TextView mEmptyView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<RssItem> mItemList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mEmptyView = (TextView) findViewById(R.id.empty);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        // Use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        // Do we have a rss feed to display?
        if (!Constants.NEWS_URL.isEmpty()) {
            // Start the Request
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.NEWS_URL)
                    .addConverterFactory(RssConverterFactory.create())
                    .build();

            RssService service = retrofit.create(RssService.class);
            service.getRss(Constants.NEWS_URL)
                    .enqueue(this);

            mEmptyView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
        }

        Button infoButton = (Button) findViewById(R.id.info_button_contact);
        Button locationsButton = (Button) findViewById(R.id.info_button_locations);
        Button impressumButton = (Button) findViewById(R.id.info_button_impressum);

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickContactButton();
            }
        });
        locationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickLocationsButton();
            }
        });
        impressumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickImpressumButton();
            }
        });
    }

    @Override
    public void onResponse(Call<RssFeed> call, Response<RssFeed> response) {
        mItemList.addAll(response.body().getItems());

        mProgressBar.setVisibility(View.GONE);
        if (response.body().getItems().isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        }

        mAdapter = new RssAdapter(mItemList);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onFailure(Call<RssFeed> call, Throwable t) {
        Toast toast = Toast.makeText(InfoActivity.this, R.string.toast_info_rss_error, Toast.LENGTH_LONG);
        toast.show();

        mProgressBar.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    private void onClickContactButton() {
        Intent contactIntent = new Intent(this, ContactActivity.class);
        startActivityForResult(contactIntent, 99);
    }

    private void onClickLocationsButton() {
        Intent locationsIntent = new Intent(this, LocationsActivity.class);
        startActivityForResult(locationsIntent, 99);
    }

    private void onClickImpressumButton() {
        Intent impressumIntent = new Intent(this, ImpressumActivity.class);
        startActivityForResult(impressumIntent, 99);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 99) {
            if (resultCode == this.RESULT_OK) {
                // Set navigation position
                int navigationPosition = data.getIntExtra("navigationIndex", 0);
                ((BaseActivity) this).selectItem(navigationPosition);
            }
        }
    }
}
