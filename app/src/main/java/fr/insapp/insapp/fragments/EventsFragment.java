package fr.insapp.insapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fr.insapp.insapp.EventActivity;
import fr.insapp.insapp.models.Event;
import fr.insapp.insapp.adapters.EventRecyclerViewAdapter;
import fr.insapp.insapp.http.AsyncResponse;
import fr.insapp.insapp.http.HttpGet;
import fr.insapp.insapp.R;

/**
 * Created by thoma on 27/10/2016.
 */

public class EventsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private int layout;
    private String filter_club_id = null;

    private View view;
    private EventRecyclerViewAdapter adapterToday;
    private EventRecyclerViewAdapter adapterWeek;
    private EventRecyclerViewAdapter adapterMonth;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // arguments

        final Bundle bundle = getArguments();
        if (bundle != null) {
            this.layout = bundle.getInt("layout", R.layout.row_event_with_avatars);
            this.filter_club_id = bundle.getString("filter_club_id");
        }

        // adapters

        this.adapterToday = new EventRecyclerViewAdapter(getContext(), layout);
        adapterToday.setOnItemClickListener(new EventRecyclerViewAdapter.OnEventItemClickListener() {
            @Override
            public void onEventItemClick(Event event) {
                getContext().startActivity(new Intent(getContext(), EventActivity.class).putExtra("event", event));
            }
        });

        this.adapterWeek = new EventRecyclerViewAdapter(getContext(), layout);
        adapterWeek.setOnItemClickListener(new EventRecyclerViewAdapter.OnEventItemClickListener() {
            @Override
            public void onEventItemClick(Event event) {
                getContext().startActivity(new Intent(getContext(), EventActivity.class).putExtra("event", event));
            }
        });

        this.adapterMonth = new EventRecyclerViewAdapter(getContext(), layout);
        adapterMonth.setOnItemClickListener(new EventRecyclerViewAdapter.OnEventItemClickListener() {
            @Override
            public void onEventItemClick(Event event) {
                getContext().startActivity(new Intent(getContext(), EventActivity.class).putExtra("event", event));
            }
        });

        generateEvents();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.fragment_events, container, false);

        RecyclerView recyclerViewToday = (RecyclerView) view.findViewById(R.id.recyclerview_events_today);
        recyclerViewToday.setHasFixedSize(true);
        recyclerViewToday.setNestedScrollingEnabled(false);

        RecyclerView recyclerViewWeek = (RecyclerView) view.findViewById(R.id.recyclerview_events_week);
        recyclerViewWeek.setHasFixedSize(true);
        recyclerViewWeek.setNestedScrollingEnabled(false);

        RecyclerView recyclerViewMonth = (RecyclerView) view.findViewById(R.id.recyclerview_events_month);
        recyclerViewMonth.setHasFixedSize(true);
        recyclerViewMonth.setNestedScrollingEnabled(false);

        recyclerViewToday.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewToday.setAdapter(adapterToday);

        recyclerViewWeek.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewWeek.setAdapter(adapterWeek);

        recyclerViewMonth.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewMonth.setAdapter(adapterMonth);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_events);
        swipeRefreshLayout.setOnRefreshListener(this);

        return view;
    }

    private void generateEvents() {
        adapterToday.getEvents().clear();
        adapterToday.notifyDataSetChanged();
        adapterWeek.getEvents().clear();
        adapterWeek.notifyDataSetChanged();
        adapterMonth.getEvents().clear();
        adapterMonth.notifyDataSetChanged();

        HttpGet request = new HttpGet(new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                if (!output.equals("{\"events\":null}")) {



                    try {
                        JSONArray jsonarray = new JSONArray(output);
                        for (int i = 0; i < jsonarray.length(); i++) {
                            JSONObject jsonObject = jsonarray.getJSONObject(i);

                            Event event = new Event(jsonObject);
                            Date atm = Calendar.getInstance().getTime();

                            if (event.getDateEnd().getTime() > atm.getTime()) {
                                if (filter_club_id != null) {
                                    if (filter_club_id.equals(event.getAssociation()))
                                        addEventToAdapter(event);
                                }
                                else
                                    addEventToAdapter(event);

                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        request.execute(HttpGet.ROOTEVENT + "?token=" + HttpGet.credentials.getSessionToken());
    }

    private void addEventToAdapter(Event event) {
        Date atm = Calendar.getInstance().getTime();

        final long diff = event.getDateStart().getTime() - atm.getTime();
        final float diffInDays = ((float)(diff) / (float)(1000 * 60 * 60 * 24));

        if (diffInDays > 7) {
            adapterMonth.addItem(event);
        } else if (diffInDays > 1) {
            adapterWeek.addItem(event);
        } else {
            adapterToday.addItem(event);
        }
    }

    @Override
    public void onRefresh() {
        generateEvents();
        swipeRefreshLayout.setRefreshing(false);
    }
}