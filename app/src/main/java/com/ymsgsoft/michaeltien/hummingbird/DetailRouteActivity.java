package com.ymsgsoft.michaeltien.hummingbird;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ymsgsoft.michaeltien.hummingbird.TransitNoView.TransitNoView;
import com.ymsgsoft.michaeltien.hummingbird.data.PrefUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailRouteActivity extends AppCompatActivity implements FavoriteDialog.FavoriteDialogListener {
    final String SAVE_ROUTE_KEY = "save_arg_key";
    final String SAVE_FROM_ID = "SAVE_FROM_ID";
    final String SAVE_TO_ID = "SAVE_TO_ID";
    public static final String ARG_ROUTE_KEY = "arg_intent_key_route_no";
    protected RouteParcelable mRouteObject;
    protected PlaceObject mFromObject;
    protected PlaceObject mToObject;
    @Bind(R.id.fab_add) FloatingActionButton mAddRemoveBtn;
    @Bind(R.id.transit_no_view) TransitNoView mTransitNoView;
    @Bind(R.id.detail_depart_time)
    TextView mDepartTime;
    @Bind(R.id.detail_duration) TextView mDuration;
    private boolean mConfigChanged = false;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if ( mRouteObject != null) {
            outState.putParcelable(SAVE_ROUTE_KEY, mRouteObject);
            PrefUtils.saveRouteParcelableToPref(this, DetailRouteActivity.ARG_ROUTE_KEY, mRouteObject);
        }
        if ( mFromObject != null) {
            outState.putParcelable(SAVE_FROM_ID, mFromObject);
        }
        if ( mToObject != null) {
            outState.putParcelable(SAVE_TO_ID, mToObject);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_route);
        ButterKnife.bind(this);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if ( savedInstanceState == null ) {
            mRouteObject = getIntent().getParcelableExtra(DetailRouteActivity.ARG_ROUTE_KEY);
            mFromObject = getIntent().getParcelableExtra(PlanningActivity.PLAN_FROM_ID);
            mToObject = getIntent().getParcelableExtra(PlanningActivity.PLAN_TO_ID);
        } else {
            if (savedInstanceState.containsKey(SAVE_ROUTE_KEY))
                mRouteObject = savedInstanceState.getParcelable(SAVE_ROUTE_KEY);
            if (savedInstanceState.containsKey(SAVE_FROM_ID))
                mFromObject = savedInstanceState.getParcelable(SAVE_FROM_ID);
            if ( savedInstanceState.containsKey(SAVE_TO_ID))
                mToObject = savedInstanceState.getParcelable(SAVE_TO_ID);
        }
        if ( mRouteObject == null) {
            mRouteObject = PrefUtils.restoreRouteParcelableFromPref(this, ARG_ROUTE_KEY);
//            mRouteObject = PrefUtils.restoreRouteParcelableFromPref(this, ARG_ROUTE_KEY_ID);
        }
        if ( mRouteObject.isFavorite) {
            mAddRemoveBtn.setImageResource(R.drawable.ic_remove);
            mAddRemoveBtn.setContentDescription(getString(R.string.remove_favorite_description));
        }
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ROUTE_KEY, mRouteObject);

        DetailRouteFragment fragment = new DetailRouteFragment();
        fragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_detail_container, fragment, "fragment_detail_tag")
                .commit();
        if ( mRouteObject != null && mRouteObject.transitNo != null) {
//            createDetailTitleView(getLayoutInflater());
            mDepartTime.setText(mRouteObject.departTime);
            mDepartTime.setContentDescription(getString(R.string.departure_time) + mRouteObject.departTime);
            mDuration.setText(mRouteObject.duration);
            mDuration.setContentDescription(getString(R.string.duration) + mRouteObject.duration );
            mTransitNoView.setTransitNo(mRouteObject.transitNo);
        }
    }

    @OnClick(R.id.fab_navigate)
    public void navigate(View view) {
        Intent intent = new Intent(DetailRouteActivity.this, NavigateActivity.class);
        intent.putExtra(DetailRouteActivity.ARG_ROUTE_KEY, mRouteObject);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(DetailRouteActivity.this
            ).toBundle();
            startActivity(intent, bundle);
        } else
        startActivity(intent);
    }
    @OnClick(R.id.fab_add)
    public void addFavoriteClick(View view) {
        if ( !mRouteObject.isFavorite) {
            String suggest_name;
            if (mFromObject.title.length() > 0)
                suggest_name = mFromObject.title + " to " + mToObject.title;
            else
                suggest_name = mToObject.title;
            DialogFragment newFragment = new FavoriteDialog();
            Bundle bundle = new Bundle();
            bundle.putString(FavoriteDialog.SUGGEST_NAME, suggest_name);
            newFragment.setArguments(bundle);
            newFragment.show(getFragmentManager(), "TagFavoriteDialog");
        } else {
            DialogFragment newFragment = new ConfirmRemoveDialog();
            newFragment.show(getFragmentManager(), "TagFavoriteDialog");
        }
    }
    public void removeFavorite() {
        DirectionService.startActionRemoveFavorite(this, mRouteObject.routeId);
        mRouteObject.isFavorite = false;
        mConfigChanged = true;
        mAddRemoveBtn.setImageResource(R.drawable.ic_add);
        mAddRemoveBtn.setContentDescription(getString(R.string.add_favorite_description));
    }
    @OnClick(R.id.action_up)
    public void backPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mConfigChanged) {
            supportFinishAfterTransition();
        } else {
            onSupportNavigateUp();
        }
    }
    @OnClick(R.id.action_home)
    public void homePressed() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(DetailRouteActivity.this
            ).toBundle();
            startActivity(intent, bundle);
        } else
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        backPressed();
    }

    //  FavoriteDialog.FavoriteDialogListener
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String saveName) {
        addFavorite(saveName);
    }

    private void addFavorite(String saveName) {
        DirectionService.startActionSaveFavorite(this,
                mFromObject,
                mToObject,
                saveName,
                mRouteObject,
                mRouteObject.departTimeValue == 0 ? System.currentTimeMillis() / 1000 : mRouteObject.departTimeValue);
        mRouteObject.isFavorite = true;
        mConfigChanged = true;
        mAddRemoveBtn.setImageResource(R.drawable.ic_remove);
        mAddRemoveBtn.setContentDescription(getString(R.string.remove_favorite_description));
    }

    public static class ConfirmRemoveDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_favorite_remove_title)
                    .setMessage(R.string.dialog_favorite_remove_message)
                    .setPositiveButton(R.string.dialog_favorite_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = getActivity();
                            if (activity instanceof DetailRouteActivity) {
                                ((DetailRouteActivity) activity).removeFavorite();
                            }
                        }
                    })
                    .setNegativeButton(R.string.dialog_favorite_negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            return builder.create();
        }
    }
}
