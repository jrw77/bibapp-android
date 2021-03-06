package de.eww.bibapp.tasks;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.eww.bibapp.R;
import de.eww.bibapp.model.DaiaItem;
import de.eww.bibapp.model.ModsItem;

/**
* @author Christoph Schönfeld - effective WEBWORK GmbH
*
* This file is part of the Android BibApp Project
* =========================================================
* Callback for daia communication
*/
public class DaiaLoaderCallback implements
	LoaderManager.LoaderCallbacks<List<DaiaItem>> {

    private boolean mIsLocalSearch = true;

	private DaiaLoaderInterface daiaLoaderInterface = null;

    public interface DaiaLoaderInterface {
        public ModsItem getModsItem();
        public void onDaiaRequestDone(List<DaiaItem> daiaItems);
    }

	public DaiaLoaderCallback(DaiaLoaderInterface daiaLoaderInterface) {
		this.daiaLoaderInterface = daiaLoaderInterface;
	}

	@Override
	public Loader<List<DaiaItem>> onCreateLoader(int loaderIndex, Bundle arg1) {
		Loader<List<DaiaItem>> loader = new DaiaLoader(((Fragment) this.daiaLoaderInterface).getActivity(), (Fragment) this.daiaLoaderInterface);
		((DaiaLoader) loader).setPpn(this.daiaLoaderInterface.getModsItem().ppn);
		((DaiaLoader) loader).setFromLocalSearch(this.daiaLoaderInterface.getModsItem().isLocalSearch);
		((DaiaLoader) loader).setItem(this.daiaLoaderInterface.getModsItem());

        mIsLocalSearch = this.daiaLoaderInterface.getModsItem().isLocalSearch;

		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<DaiaItem>> loader, List<DaiaItem> data) {
        List<DaiaItem> daiaList = data;

        // Group by department, if we are processing a gvk search
        if (!mIsLocalSearch) {
            daiaList = groupByDepartment(data);
        }

        this.daiaLoaderInterface.onDaiaRequestDone(daiaList);
	}

	@Override
	public void onLoaderReset(Loader<List<DaiaItem>> arg0) {
		// empty
	}

    /**
     * Iterates over a list of given daia items and groups them by department. If any item does not
     * have a department value (or the value of "Ohne Zuordnung"), a default one will be created and
     * all related items will be grouped under the default one.
     *
     * @param ungroupedList The list of ungrouped daia items loaded online
     *
     * @return The daia item list grouped by department
     */
    private List<DaiaItem> groupByDepartment(List<DaiaItem> ungroupedList) {
        HashMap<String, DaiaItem> hashMap = new HashMap<String, DaiaItem>();

        Resources resources = ((Fragment) this.daiaLoaderInterface).getResources();
        String daiaDefaultDepartment = resources.getString(R.string.daia_default_department);

        Iterator<DaiaItem> it = ungroupedList.iterator();
        while (it.hasNext()) {
            DaiaItem daiaItem = it.next();

            // Update department "Ohne Zuordnung"
            if (!daiaItem.hasDepartment() || daiaItem.getDepartment().equals("Ohne Zuordnung")) {
                daiaItem.setDepartment(daiaDefaultDepartment);
            }

            // Grouping
            if (hashMap.containsKey(daiaItem.getDepartment())) {
                DaiaItem daiaHashItem = hashMap.get(daiaItem.getDepartment());

                if (daiaItem.hasLabel()) {
                    daiaHashItem.label += ", " + daiaItem.label;
                }

                hashMap.put(daiaItem.getDepartment(), daiaHashItem);
            } else {
                hashMap.put(daiaItem.getDepartment(), daiaItem);
            }
        }

        ArrayList<DaiaItem> daiaResponseList = new ArrayList<DaiaItem>(hashMap.values());

        // Sort alphabetically
        Collections.sort(daiaResponseList, new Comparator<DaiaItem>() {
            @Override
            public int compare(DaiaItem lhs, DaiaItem rhs) {
                return lhs.getDepartment().compareTo(rhs.getDepartment());
            }
        });

        // Make sure, that the default department fallback is the last one in the list
        it = daiaResponseList.iterator();
        while (it.hasNext()) {
            DaiaItem daiaItem = it.next();

            if (daiaItem.getDepartment().equals(daiaDefaultDepartment)) {
                daiaResponseList.remove(daiaItem);
                daiaResponseList.add(daiaItem);
                break;
            }
        }

        return daiaResponseList;
    }
}
