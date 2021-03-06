package de.eww.bibapp.fragment.info;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;

import java.util.List;

import de.eww.bibapp.AsyncCanceledInterface;
import de.eww.bibapp.R;
import de.eww.bibapp.adapter.LocationAdapter;
import de.eww.bibapp.decoration.DividerItemDecoration;
import de.eww.bibapp.listener.RecyclerViewOnGestureListener;
import de.eww.bibapp.model.LocationItem;
import de.eww.bibapp.model.source.LocationSource;
import de.eww.bibapp.tasks.LocationsJsonLoader;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

/**
 * Created by christoph on 25.10.14.
 */
public class LocationsFragment extends RoboFragment implements
        RecyclerViewOnGestureListener.OnGestureListener,
        LoaderManager.LoaderCallbacks<List<LocationItem>>,
        AsyncCanceledInterface {

    @Inject LocationSource mLocationSource;

    @InjectView(R.id.recycler) RecyclerView mRecyclerView;
    @InjectView(R.id.progressbar) ProgressBar mProgressBar;
    @InjectView(R.id.empty) TextView mEmptyView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // The listener we are to notify when a location is selected
    OnLocationSelectedListener mLocationSelectedListener = null;

    /**
     * Represents a listener that will be notified of location selections.
     */
    public interface OnLocationSelectedListener {
        /**
         * Call when a given location is selected.
         *
         * @param index the index of the selected location.
         */
        public void onLocationSelected(int index);

        public void onLocationsLoaded();
    }

    /**
     * Sets the listener that should be notified of location selection events.
     *
     * @param listener the listener to notify.
     */
    public void setOnLocationSelectedListener(OnLocationSelectedListener listener) {
        mLocationSelectedListener = listener;
    }

    public void setSelection(int position) {
        mRecyclerView.scrollToPosition(position);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        RecyclerViewOnGestureListener gestureListener = new RecyclerViewOnGestureListener(getActivity(), mRecyclerView);
        gestureListener.setOnGestureListener(this);
        mRecyclerView.addOnItemTouchListener(gestureListener);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        // Start the Request
        mEmptyView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(0);
        loaderManager.initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public Loader<List<LocationItem>> onCreateLoader(int arg0, Bundle arg1) {
        return new LocationsJsonLoader(getActivity().getApplicationContext(), this);
    }

    @Override
    public void onLoadFinished(Loader<List<LocationItem>> loader, List<LocationItem> locations) {
        mLocationSource.clear();
        mLocationSource.addLocations(locations);
        if (mLocationSelectedListener != null) {
            mLocationSelectedListener.onLocationsLoaded();
        }

        mProgressBar.setVisibility(View.GONE);

        if (locations.isEmpty()) {
            mEmptyView.setVisibility(View.GONE);
        }

        mAdapter = new LocationAdapter(locations);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<LocationItem>> loader) {
        // empty
    }

    @Override
    public void onAsyncCanceled() {
        Toast toast = Toast.makeText(getActivity(), R.string.toast_locations_error, Toast.LENGTH_LONG);
        toast.show();

        mProgressBar.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view, int position) {
        if (mLocationSelectedListener != null) {
            mLocationSelectedListener.onLocationSelected(position);
        }
    }

    @Override
    public void onLongPress(View view, int position) {

    }
}
