// Copyright 2015-present 650 Industries. All rights reserved.

package host.exp.exponent.storage;

import androidx.annotation.NonNull;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.CursorResult;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import host.exp.exponent.ExponentManifest;
import host.exp.exponent.analytics.EXL;
import host.exp.expoview.Exponent;

@Database(version = ExponentDB.VERSION)
public class ExponentDB {

  private static final String TAG = ExponentDB.class.getSimpleName();

  public interface ExperienceResultListener {
    void onSuccess(ExperienceDBObject experience);

    void onFailure();
  }

  public static final String NAME = "ExponentKernel";
  public static final int VERSION = 1;

  public static void saveExperience(String manifestUrl, JSONObject manifest, String bundleUrl) {
    try {
      ExperienceDBObject experience = new ExperienceDBObject();

      // Only the legacyExperienceId and expoProjectId may be provided in notification payloads, so
      // we need to index on one of them rather than scopeKey.
      experience.id = ExponentManifest.getProjectId(manifest);
      experience.manifestUrl = manifestUrl;
      experience.bundleUrl = bundleUrl;
      experience.manifest = manifest.toString();
      FlowManager.getDatabase(ExponentDB.class).getTransactionManager().getSaveQueue().add(experience);
    } catch (JSONException e) {
      EXL.e(TAG, e.getMessage());
    }
  }

  public static void projectIdToExperience(String projectId, final ExperienceResultListener listener) {
    SQLite.select()
        .from(ExperienceDBObject.class)
        .where(ExperienceDBObject_Table.id.is(projectId))
        .async()
        .queryResultCallback(new QueryTransaction.QueryResultCallback<ExperienceDBObject>() {
          @Override
          public void onQueryResult(QueryTransaction<ExperienceDBObject> transaction, @NonNull CursorResult<ExperienceDBObject> tResult) {
            if (tResult.getCount() == 0) {
              listener.onFailure();
            } else {
              listener.onSuccess(tResult.getItem(0));
            }
          }
        }).execute();
  }
}
