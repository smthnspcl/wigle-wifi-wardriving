package net.wigle.wigleandroid;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DashboardFragment extends Fragment {
  private final Handler timer = new Handler();
  private AtomicBoolean finishing;
  private NumberFormat numberFormat;

  private static final int MENU_EXIT = 11;
  private static final int MENU_SETTINGS = 12;

  /** Called when the activity is first created. */
  @Override
  public void onCreate( final Bundle savedInstanceState ) {
    MainActivity.info("DASH: onCreate");
    super.onCreate( savedInstanceState );
    setHasOptionsMenu(true);
    // set language
    MainActivity.setLocale( getActivity() );

    // media volume
    getActivity().setVolumeControlStream( AudioManager.STREAM_MUSIC );

    finishing = new AtomicBoolean( false );
    numberFormat = NumberFormat.getNumberInstance( Locale.US );
    if ( numberFormat instanceof DecimalFormat ) {
      ((DecimalFormat) numberFormat).setMaximumFractionDigits( 2 );
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final int orientation = getResources().getConfiguration().orientation;
    MainActivity.info("DASH: onCreateView. orientation: " + orientation);
    if (orientation == 2) {
      return inflater.inflate(R.layout.dashlandscape, container, false);
    }

    return inflater.inflate(R.layout.dash, container, false);
  }

  private final Runnable mUpdateTimeTask = new Runnable() {
    @Override
    public void run() {
        // make sure the app isn't trying to finish
        if ( ! finishing.get() ) {
          final View view = getView();
          if (view != null) {
            updateUI( view );
          }

          final long period = 1000L;
          // info("wifitimer: " + period );
          timer.postDelayed( this, period );
        }
        else {
          MainActivity.info( "finishing mapping timer" );
        }
    }
  };

  private void setupTimer() {
    timer.removeCallbacks( mUpdateTimeTask );
    timer.postDelayed( mUpdateTimeTask, 250 );
  }

  public void updateUI() {
    final View view = getView();
    if (view != null) {
      updateUI(view);
    }
  }

  private void updateUI( final View view ) {
    TextView tv = (TextView) view.findViewById( R.id.runnets );
    tv.setText( ListFragment.lameStatic.runNets + " " + getString(R.string.run));

    tv = (TextView) view.findViewById( R.id.newwifi );
    final String scanning = MainActivity.isScanning(getActivity()) ? "" : getString(R.string.dash_scan_off) + "\n";
    final String newTitle = ListFragment.lameStatic.newWifi >= 1000 ? getString(R.string.new_word)
        : getString(R.string.dash_new_wifi);
    tv.setText( scanning + ListFragment.lameStatic.newWifi + " " + newTitle );

    tv = (TextView) view.findViewById( R.id.currnets );
    tv.setText( getString(R.string.dash_vis_nets) + " " + ListFragment.lameStatic.currNets );

    tv = (TextView) view.findViewById( R.id.newNetsSinceUpload );
    tv.setText( getString(R.string.dash_new_upload) + " " + newNetsSinceUpload() );

    tv = (TextView) view.findViewById( R.id.newcells );
    tv.setText( getString(R.string.dash_new_cells) + " " + ListFragment.lameStatic.newCells );

    updateDist( view, R.id.rundist, ListFragment.PREF_DISTANCE_RUN, getString(R.string.dash_dist_run) );
    updateDist( view, R.id.totaldist, ListFragment.PREF_DISTANCE_TOTAL, getString(R.string.dash_dist_total) );
    updateDist( view, R.id.prevrundist, ListFragment.PREF_DISTANCE_PREV_RUN, getString(R.string.dash_dist_prev) );

    tv = (TextView) view.findViewById( R.id.queuesize );
    tv.setText( getString(R.string.dash_db_queue) + " " + ListFragment.lameStatic.preQueueSize );

    tv = (TextView) view.findViewById( R.id.dbNets );
    tv.setText( getString(R.string.dash_db_nets) + " " + ListFragment.lameStatic.dbNets );

    tv = (TextView) view.findViewById( R.id.dbLocs );
    tv.setText( getString(R.string.dash_db_locs) + " " + ListFragment.lameStatic.dbLocs );

    tv = (TextView) view.findViewById( R.id.gpsstatus );
    Location location = ListFragment.lameStatic.location;
    String gpsStatus = getString(R.string.dash_no_loc);
    if ( location != null ) {
      gpsStatus = location.getProvider();
    }
    tv.setText( getString(R.string.dash_short_loc) + " " + gpsStatus );
  }

  private long newNetsSinceUpload() {
    final SharedPreferences prefs = getActivity().getSharedPreferences( ListFragment.SHARED_PREFS, 0 );
    final long marker = prefs.getLong( ListFragment.PREF_DB_MARKER, 0L );
    final long uploaded = prefs.getLong( ListFragment.PREF_NETS_UPLOADED, 0L );
    long newSinceUpload = 0;
    if ( marker != 0 && uploaded == 0 ) {
      // marker is set but no uploaded, a migration situation, so return zero
    }
    else {
      newSinceUpload = ListFragment.lameStatic.dbNets - uploaded;
      if ( newSinceUpload < 0 ) {
        newSinceUpload = 0;
      }
    }
    return newSinceUpload;
  }

  private void updateDist( final View view, final int id, final String pref, final String title ) {
    final SharedPreferences prefs = getActivity().getSharedPreferences( ListFragment.SHARED_PREFS, 0 );

    float dist = prefs.getFloat( pref, 0f );
    final String distString = metersToString( numberFormat, getActivity(), dist, false );
    final TextView tv = (TextView) view.findViewById( id );
    tv.setText( title + " " + distString );
  }

  public static String metersToString(final NumberFormat numberFormat, final Context context, final float meters,
      final boolean useShort ) {
    final SharedPreferences prefs = context.getSharedPreferences( ListFragment.SHARED_PREFS, 0 );
    final boolean metric = prefs.getBoolean( ListFragment.PREF_METRIC, false );

    String retval = null;
    if ( meters > 1000f ) {
      if ( metric ) {
        retval = numberFormat.format( meters / 1000f ) + " " + context.getString(R.string.km_short);
      }
      else {
        retval = numberFormat.format( meters / 1609.344f ) + " " +
            (useShort ? context.getString(R.string.mi_short) : context.getString(R.string.miles));
      }
    }
    else if ( metric ){
      retval = numberFormat.format( meters ) + " " +
          (useShort ? context.getString(R.string.m_short) : context.getString(R.string.meters));
    }
    else {
      retval = numberFormat.format( meters * 3.2808399f  ) + " " +
          (useShort ? context.getString(R.string.ft_short) : context.getString(R.string.feet));
    }
    return retval;
  }

  // XXX
//  @Override
//  public void finish() {
//    ListActivity.info( "finish dash." );
//    finishing.set( true );
//
//    super.finish();
//  }

  @Override
  public void onDestroy() {
    MainActivity.info( "DASH: onDestroy" );
    finishing.set( true );

    super.onDestroy();
  }

  @Override
  public void onResume() {
    MainActivity.info( "DASH: onResume" );
    super.onResume();
    setupTimer();
    getActivity().setTitle(R.string.dashboard_app_name);
  }

  /* Creates the menu items */
  @Override
  public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
    MenuItem item = menu.add(0, MENU_SETTINGS, 0, getString(R.string.menu_settings));
    item.setIcon( android.R.drawable.ic_menu_preferences );

    item = menu.add(0, MENU_EXIT, 0, getString(R.string.menu_exit));
    item.setIcon( android.R.drawable.ic_menu_close_clear_cancel );

    super.onCreateOptionsMenu(menu, inflater);
  }

  /* Handles item selections */
  @Override
  public boolean onOptionsItemSelected( final MenuItem item ) {
      switch ( item.getItemId() ) {
        case MENU_EXIT:
          final MainActivity main = MainActivity.getMainActivity();
          main.finish();
          return true;
        case MENU_SETTINGS:
          final Intent settingsIntent = new Intent( getActivity(), SettingsActivity.class );
          startActivity( settingsIntent );
          break;
      }
      return false;
  }

}