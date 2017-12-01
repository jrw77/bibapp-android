package de.eww.bibapp.network;

import me.toptas.rssconverter.RssFeed;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by cschoenf on 01.12.17.
 */

public interface RssService {

    @GET
    Call<RssFeed> getRss(@Url String url);
}
