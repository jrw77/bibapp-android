package de.eww.bibapp.fragment.search;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.eww.bibapp.AsyncCanceledInterface;
import de.eww.bibapp.PaiaHelper;
import de.eww.bibapp.R;
import de.eww.bibapp.activity.BaseActivity;
import de.eww.bibapp.activity.LocationActivity;
import de.eww.bibapp.activity.WebViewActivity;
import de.eww.bibapp.adapter.DaiaAdapter;
import de.eww.bibapp.constants.Constants;
import de.eww.bibapp.fragment.dialog.DetailActionsDialogFragment;
import de.eww.bibapp.fragment.dialog.InsufficentRightsDialogFragment;
import de.eww.bibapp.fragment.dialog.PaiaActionDialogFragment;
import de.eww.bibapp.listener.RecyclerViewOnGestureListener;
import de.eww.bibapp.model.DaiaItem;
import de.eww.bibapp.model.LocationItem;
import de.eww.bibapp.model.ModsItem;
import de.eww.bibapp.model.source.LocationSource;
import de.eww.bibapp.tasks.DaiaLoaderCallback;
import de.eww.bibapp.tasks.DownloadImageTask;
import de.eww.bibapp.tasks.LocationsJsonTask;
import de.eww.bibapp.tasks.UnApiLoaderCallback;
import de.eww.bibapp.tasks.paia.PaiaRequestTask;

/**
 * Created by christoph on 08.11.14.
 */
public class ModsFragment extends Fragment implements
        DaiaLoaderCallback.DaiaLoaderInterface,
        UnApiLoaderCallback.UnApiLoaderInterface,
        RecyclerViewOnGestureListener.OnGestureListener,
        PaiaHelper.PaiaListener,
        DetailActionsDialogFragment.DetailActionsDialogLisener,
        PaiaActionDialogFragment.PaiaActionDialogListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        AsyncCanceledInterface {

    @BindView(R.id.title) TextView mTitleView;
    @BindView(R.id.sub) TextView mSubView;
    @BindView(R.id.image) ImageView mImageView;
    @BindView(R.id.author_extended) TextView mAuthorExtendedView;
    @BindView(R.id.index_container) LinearLayout mIndexContainer;
    @BindView(R.id.interlanding_container) LinearLayout mInterlandingContainer;

    ModsItem mModsItem = null;

    @BindView(R.id.recycler) RecyclerView mRecyclerView;
    @BindView(R.id.progressbar) ProgressBar mProgressBar;
    @BindView(R.id.unapi_progressbar) ProgressBar mUnApiProgressBar;
    @BindView(R.id.empty) TextView mEmptyView;

    private Unbinder unbinder;

    LinearLayout mModsView;
    RelativeLayout mNoneView;

    private DaiaAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private PaiaActionDialogFragment mPaiaDialog;

    private int mLastClickedPosition;
    private boolean mIsWatchlistFragment = false;

    private MenuItem mMenuItem;

    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (this.googleApiClient == null) {
            this.googleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onStart() {
        this.googleApiClient.connect();

        super.onStart();
    }

    @Override
    public void onStop() {
        this.googleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_mods, container, false);

        unbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Improve performance for RecyclerView by setting it to a fixed size,
        // since we now that changes in content do not change the layout size
        // of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        RecyclerViewOnGestureListener gestureListener = new RecyclerViewOnGestureListener(getActivity(), mRecyclerView);
        gestureListener.setOnGestureListener(this);
        mRecyclerView.addOnItemTouchListener(gestureListener);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mModsView = (LinearLayout) view.findViewById(R.id.mods_layout);
        mNoneView = (RelativeLayout) view.findViewById(R.id.mods_none);

        displayModsItem();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void removeModsItem() {
        mModsView.setVisibility(View.GONE);
        mNoneView.setVisibility(View.VISIBLE);
    }

    public void setModsItem(ModsItem item) {
        mModsItem = item;
        displayModsItem();
    }

    public void setIsWatchlistFragment(boolean isWatchlistFragment) {
        mIsWatchlistFragment = isWatchlistFragment;
    }

    private void loadData() {
        // Perform requests
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(0);
        loaderManager.destroyLoader(1);
        loaderManager.initLoader(0, null, new DaiaLoaderCallback(this));
        loaderManager.initLoader(1, null, new UnApiLoaderCallback(this, mModsItem));

        mProgressBar.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
    }

    @Override
    public ModsItem getModsItem() {
        return mModsItem;
    }

    @Override
    public void onDaiaRequestDone(List<DaiaItem> daiaItems) {
        mProgressBar.setVisibility(View.GONE);
        if (daiaItems.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        }

        mAdapter = new DaiaAdapter(daiaItems);
        mAdapter.setIsLocalSearch(mModsItem.isLocalSearch);
        mRecyclerView.setAdapter(mAdapter);

        // distance
        requestGeoLocation();
    }

    @Override
    public void onUnApiRequestDone(String authorExtended) {
        // Set extended author information
        mAuthorExtendedView.setText(authorExtended);

        // Hide UnApi loading animation
        mUnApiProgressBar.setVisibility(View.GONE);
    }

    @Override
	public void onAsyncCanceled() {
        Toast toast = Toast.makeText(getActivity(), R.string.toast_mods_error, Toast.LENGTH_LONG);
        toast.show();

        mProgressBar.setVisibility(View.GONE);
        mUnApiProgressBar.setVisibility(View.GONE);
	}

    @Override
    public void onActionLocation(DialogFragment dialog) {
        AsyncTask<String, Void, LocationItem> locationsTask = new LocationsJsonTask(this);
        locationsTask.execute(mAdapter.getItem(mLastClickedPosition).uriUrl);
    }

    @Override
    public void onActionRequest(DialogFragment dialog) {
        // Ensure paia connection
        PaiaHelper.getInstance().ensureConnection(this, getActivity(), this);
    }

    @Override
    public void onActionOrder(DialogFragment dialog) {
        // Ensure paia connection
        PaiaHelper.getInstance().ensureConnection(this, getActivity(), this);
    }

    @Override
    public void onActionOnline(DialogFragment dialog) {
        Uri uri = Uri.parse(mModsItem.onlineUrl);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(launchBrowser);
    }

    @Override
    public void onPaiaConnected() {
        // Check scope
        if (PaiaHelper.getInstance().hasScope(PaiaHelper.SCOPES.WRITE_ITEMS)) {
            // start async task to send paia request
            JSONObject jsonRequest = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            JSONObject itemObject = new JSONObject();

            try {
                // get uri from daia and assemble request array
                DaiaItem daiaEntry = mAdapter.getItem(mLastClickedPosition);

                itemObject.put("item", daiaEntry.itemUriUrl);
                jsonArray.put(itemObject);
                jsonRequest.put("doc", jsonArray);

                AsyncTask<String, Void, JSONObject> requestTask = new PaiaRequestTask(this, getActivity(), this);
                requestTask.execute(jsonRequest.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // show the action dialog
            mPaiaDialog = new PaiaActionDialogFragment();
            mPaiaDialog.show(this.getChildFragmentManager(), "paia_action");
        } else {
            InsufficentRightsDialogFragment dialog = new InsufficentRightsDialogFragment();
            dialog.show(this.getChildFragmentManager(), "insufficent_rights");
        }
    }

    public void onLocationLoadFinished(LocationItem locationItem) {
        LocationSource.clear();
        LocationSource.addLocation(locationItem);

        Intent locationIntent = new Intent(getActivity(), LocationActivity.class);
        locationIntent.putExtra("locationIndex", 0);

        if (mIsWatchlistFragment) {
            locationIntent.putExtra("source", "watchlist");
        } else {
            locationIntent.putExtra("source", "search");
        }

        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            parentFragment.startActivityForResult(locationIntent, 99);
        } else {
            startActivityForResult(locationIntent, 99);
        }
    }

    public void onPaiaRequestActionDone(JSONObject response) {
		// determ text to display
		String responseText = "";

		Resources resources = getActivity().getResources();

		try {
            if (response.has("doc")) {
                JSONArray docArray = response.getJSONArray("doc");
                int docArrayLength = docArray.length();
                int numFailedItems = 0;

                for (int i=0; i < docArrayLength; i++) {
                    JSONObject docEntry = docArray.getJSONObject(i);

                    if (docEntry.has("error")) {
                        numFailedItems++;
                        continue;
                    }
                }

                if (numFailedItems > 0) {
                    responseText = (String) resources.getText(R.string.paiadialog_general_failure);

                    JSONObject errorObject = docArray.getJSONObject(0);
                    if (errorObject.has("error")) {
                        responseText += " " + errorObject.get("error");
                    }
                } else {
                    responseText = (String) resources.getText(R.string.paiadialog_general_success);
                }
            }
		} catch (JSONException e) {
			responseText = (String) resources.getText(R.string.paiadialog_general_failure);
		}

		mPaiaDialog.paiaActionDone(responseText);

		// reload daia information
        mAdapter.clear();


		mAdapter.clear();
        mProgressBar.setVisibility(View.VISIBLE);
        getLoaderManager().getLoader(0).forceLoad();
	}

    @Override
    public void onActionDialogPositiveClick(DialogFragment dialog) {
        // Close dialog
        mPaiaDialog.dismiss();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the toolbar
        inflater.inflate(R.menu.mods_fragment_mode_actions, menu);

        mMenuItem = menu.findItem(R.id.menu_mods_add_to_watchlist);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mModsItem != null) {
            if (mIsWatchlistFragment) {
                mMenuItem.setVisible(false);
                mMenuItem.setEnabled(false);
                mMenuItem.setTitle(R.string.menu_detail_addtowatchlist_duplicate);
            } else {
                // Disable "add to watchlist" if the item is already in it and change the text
                ArrayList<ModsItem> watchlistEntries = new ArrayList<ModsItem>();

                File file = getActivity().getFileStreamPath("watchlist");
                if (file.isFile()) {
                    try {
                        FileInputStream fis = getActivity().openFileInput("watchlist");

                        ObjectInputStream ois = new ObjectInputStream(fis);
                        watchlistEntries = (ArrayList<ModsItem>) ois.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (watchlistEntries.contains(mModsItem)) {
                    mMenuItem.setTitle(R.string.menu_detail_addtowatchlist_duplicate);
                    mMenuItem.setEnabled(false);
                } else {
                    mMenuItem.setEnabled(true);
                }
            }
        } else {
            mMenuItem.setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_mods_add_to_watchlist:
                // Add to watchlist
                if (mModsItem != null) {
                    // Get actual watchlist
                    ArrayList<ModsItem> watchlistEntries = new ArrayList<ModsItem>();

                    File file = getActivity().getFileStreamPath("watchlist");
                    if (file.isFile()) {
                        try {
                            FileInputStream fis = getActivity().openFileInput("watchlist");

                            ObjectInputStream ois = new ObjectInputStream(fis);
                            watchlistEntries = (ArrayList<ModsItem>) ois.readObject();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Add new entry and store the watchlist
                    watchlistEntries.add(mModsItem);

                    try {
                        FileOutputStream fos = getActivity().openFileOutput("watchlist", Context.MODE_PRIVATE);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(watchlistEntries);
                        oos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Display Toast
                    Context context = getActivity().getApplicationContext();
                    Resources resources = getActivity().getResources();

                    Toast toast = Toast.makeText(context, resources.getString(R.string.toast_watchlist_added), Toast.LENGTH_LONG);
                    toast.show();

                    // Disable menu item
                    item.setTitle(R.string.menu_detail_addtowatchlist_duplicate);
                    item.setEnabled(false);
                }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view, int position) {
        mLastClickedPosition = position;

        DaiaItem daiaItem = mAdapter.getItem(position);
        String actions = daiaItem.actions;

        // Determine which actions are available for this location
        ArrayList<String> actionList = new ArrayList<String>();
        if (mModsItem.onlineUrl.isEmpty()) {            // This is not an online resource
            // location | request | order
            if (actions.contains("location")) {
                actionList.add("location");
            }
            if (actions.contains("request")) {
                actionList.add("request");
            }
            if (actions.contains("order")) {
                actionList.add("order");
            }
        } else {
            // location
            if (actions.contains("location")) {
                actionList.add("location");
            }
            actionList.add("online");
        }

        // Open a dialog with all available actions
        DetailActionsDialogFragment dialogFragment = new DetailActionsDialogFragment();
        dialogFragment.setActionList(actionList);
        dialogFragment.show(getChildFragmentManager(), "details_actions");
    }

    @Override
    public void onLongPress(View view, int position) {

    }

    private void displayModsItem() {
        if (mTitleView == null || mModsItem == null) {
            return;
        }

        mModsView.setVisibility(View.VISIBLE);
        mNoneView.setVisibility(View.GONE);

        getActivity().supportInvalidateOptionsMenu();

        loadData();

        // title
        mTitleView.setText(mModsItem.title);

        // sub
        String subTitle = mModsItem.subTitle;
        if (mModsItem.partName.isEmpty() && mModsItem.partNumber.isEmpty() && !subTitle.isEmpty()) {
            subTitle += "\n";
        }
        if (!mModsItem.partName.isEmpty()) {
            subTitle += mModsItem.partName;
        }
        if (!mModsItem.partName.isEmpty()) {
            subTitle += "; " + mModsItem.partNumber;
        }
        mSubView.setText(subTitle);

        // image
        AsyncTask<String, Void, Bitmap> imageTask = new DownloadImageTask(mImageView, mModsItem, getActivity());
        imageTask.execute(Constants.getImageUrl(mModsItem.isbn));

        // index
        if (!mModsItem.indexArray.isEmpty()) {
            mIndexContainer.setVisibility(View.VISIBLE);

            mIndexContainer.setOnClickListener((View v) -> {
                onClickIndex();
            });
        } else {
            mIndexContainer.setVisibility(View.GONE);
        }

        // interlanding
        if (mModsItem.isLocalSearch == false) {
            mInterlandingContainer.setVisibility(View.VISIBLE);

            mInterlandingContainer.setOnClickListener((View v) -> {
                onClickInterlanding();
            });
        }
    }

    private void onClickIndex() {
        if (mModsItem.indexArray.size() == 1) {
            // open directly
            openIndexAsset(mModsItem.indexArray.get(0));
        } else {
            // create a dialag allowing selecting of an url to open
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            List<String> indexList = mModsItem.indexArray;
            CharSequence[] indexItems = indexList.toArray(new CharSequence[indexList.size()]);

            builder.setTitle(R.string.indexactionsdialog_title)
                    .setItems(indexItems, (DialogInterface dialogInterface, int which) -> {
                        openIndexAsset(indexList.get(which));
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void openIndexAsset(String url) {
        Intent webIntent = new Intent(getActivity(), WebViewActivity.class);
        webIntent.putExtra("url", getIndexUrl());

        if (mIsWatchlistFragment) {
            webIntent.putExtra("source", "watchlist");
        } else {
            webIntent.putExtra("source", "search");
        }

        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            parentFragment.startActivityForResult(webIntent, 99);
        } else {
            startActivityForResult(webIntent, 99);
        }
    }

    private String getIndexUrl() {
        String indexUrl = "";

        if (!mModsItem.indexArray.isEmpty()) {
            // determine the index to display
            Iterator<String> it = mModsItem.indexArray.iterator();

            while (it.hasNext()) {
                // Try to find pdf version
                indexUrl = it.next();

                if (indexUrl.substring(indexUrl.length() - 3, indexUrl.length()).equals("pdf")) {
                    break;
                }
            }
        }

        return indexUrl;
    }

    private void onClickInterlanding() {
        Uri uri = Uri.parse(Constants.getInterlendingUrl(mModsItem.ppn));
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(launchBrowser);
    }

    private void requestGeoLocation() {
        if (this.mModsItem == null || this.mModsItem.isLocalSearch) {
            return;
        }

        if (this.mAdapter == null) {
            return;
        }

        if (this.lastLocation != null) {
            return;
        }

        if (!this.googleApiClient.isConnected()) {
            return;
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            this.lastLocation = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);

            if (this.lastLocation != null) {
                // determine distance
                for (int i=0; i < mAdapter.getItemCount(); i++) {
                    // Get the item
                    DaiaItem daiaItem = mAdapter.getItem(i);

                    if (daiaItem.hasLocation()) {
                        LocationItem locationItem = daiaItem.getLocation();

                        if (locationItem.hasPosition()) {
                            // Determine distance and update item in adapter
                            double itemLat = Double.parseDouble(locationItem.getLat());
                            double itemLong = Double.parseDouble(locationItem.getLong());

                            LatLng itemLocation = new LatLng(itemLat, itemLong);
                            LatLng currentLocation = new LatLng(this.lastLocation.getLatitude(), this.lastLocation.getLongitude());
                            double itemDistance = SphericalUtil.computeDistanceBetween(itemLocation, currentLocation) / 1000;
                            daiaItem.setDistance(itemDistance);
                        }
                    }
                }

                // Sort by distance
                mAdapter.sortByDistance();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        requestGeoLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 99) {
            if (resultCode == Activity.RESULT_OK) {
                // Set navigation position
                if (data.hasExtra("navigationIndex")) {
                    int navigationPosition = data.getIntExtra("navigationIndex", 0);
                    ((BaseActivity) getActivity()).selectItem(navigationPosition);
                }
            }
        }
    }
}