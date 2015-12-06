package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.MainScreenFragment;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();
    private static final String[] SCORE_COLUMNS = {
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.SCORES_TABLE + "." + DatabaseContract.scores_table._ID
    };

    // these indices must match the projection
    static final int INDEX_SCORE_HOME = 0;
    static final int INDEX_SCORE_AWAY = 1;
    static final int INDEX_SCORE_MATCH_TIME = 2;
    static final int INDEX_SCORE_HOME_GOALS = 3;
    static final int INDEX_SCORE_AWAY_GOALS = 4;
    static final int INDEX_SCORE_MATCH_ID = 5;
    static final int INDEX_SCORE_ID = 6;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Date date = new Date(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String scoreDate = dateFormat.format(date);
                Uri scoreWithDateUri = DatabaseContract.scores_table.buildScoreWithDate();
                data = getContentResolver().query(scoreWithDateUri,
                        SCORE_COLUMNS,
                        null,
                        new String[] {scoreDate},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String homeName = data.getString(INDEX_SCORE_HOME);
                String awayName = data.getString(INDEX_SCORE_AWAY);
                String matchTime = data.getString(INDEX_SCORE_MATCH_TIME);
                String score = Utilies.getScores(data.getInt(INDEX_SCORE_HOME_GOALS), data.getInt(INDEX_SCORE_AWAY_GOALS));
                Double matchId = data.getDouble(INDEX_SCORE_MATCH_ID);
                int homeResourceId = Utilies.getTeamCrestByTeamName(homeName);
                int awayResoruceId = Utilies.getTeamCrestByTeamName(awayName);

                views.setTextViewText(R.id.widget_home_name, homeName);
                views.setTextViewText(R.id.widget_away_name, awayName);
                views.setTextViewText(R.id.widget_data_textview, matchTime);
                views.setTextViewText(R.id.widget_score_textview, score);
                views.setImageViewResource(R.id.widget_home_crest, homeResourceId);
                views.setImageViewResource(R.id.widget_away_crest, awayResoruceId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    setRemoteContentDescription(views, R.id.widget_home_crest, homeName);
//                    setRemoteContentDescription(views, R.id.widget_away_crest, awayName);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MainActivity.SELECTED_MATCH_ID_KEY, matchId);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, int resourceId, String description) {
                views.setContentDescription(resourceId, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_SCORE_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
