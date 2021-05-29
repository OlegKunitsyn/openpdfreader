package com.github.olegkunitsyn.openpdfreader;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.Constants;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.github.olegkunitsyn.openpdfreader.databinding.ActivityMainBinding;
import com.github.olegkunitsyn.openpdfreader.databinding.PasswordDialogBinding;
import com.shockwave.pdfium.PdfPasswordException;

import java.io.FileNotFoundException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final int PREF_SPACING = 10;
    private static final String PREF_PAGE_NUMBER = "pageNumber";
    private static final String PREF_URI = "uri";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_SCREEN_ON = "screen_on";
    static final String PREF_NIGHT = "night";
    private static final String PREF_QUALITY = "quality";
    private static final String PREF_ALIASING = "aliasing";
    private static final String PREF_SCROLLING = "scrolling";
    private static final String PREF_SNAP = "snap";
    private static final String PREF_FLING = "fling";

    private SharedPreferences prefManager;
    private Uri uri;
    private int pageNumber = 0;
    private String password;
    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefManager.getBoolean(PREF_NIGHT, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        Constants.THUMBNAIL_RATIO = 1f;

        // Workaround for https://stackoverflow.com/questions/38200282/
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (savedInstanceState != null) {
            uri = savedInstanceState.getParcelable(PREF_URI);
            pageNumber = savedInstanceState.getInt(PREF_PAGE_NUMBER);
            password = savedInstanceState.getString(PREF_PASSWORD);
        } else {
            pageNumber = prefManager.getInt(PREF_PAGE_NUMBER, pageNumber);
            uri = getIntent().getData();
            if (uri == null)
                pickFile();
        }
        displayFromUri(uri);
    }

    @Override
    public void onResume() {
        super.onResume();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (prefManager.getBoolean(PREF_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PREF_URI, uri);
        outState.putInt(PREF_PAGE_NUMBER, pageNumber);
        outState.putString(PREF_PASSWORD, password);
        prefManager.edit().putInt(PREF_PAGE_NUMBER, pageNumber).apply();

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.settings:
                settingsLauncher.launch(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                viewBinding.pdfView.jumpTo(viewBinding.pdfView.getCurrentPage() - 1, true);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                viewBinding.pdfView.jumpTo(viewBinding.pdfView.getCurrentPage() + 1, true);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    void configurePdfViewAndLoad(PDFView.Configurator viewConfigurator) {
        viewBinding.pdfView.useBestQuality(prefManager.getBoolean(PREF_QUALITY, false));
        viewBinding.pdfView.setMinZoom(0.5f);
        viewBinding.pdfView.setMidZoom(2.0f);
        viewBinding.pdfView.setMaxZoom(5.0f);
        viewConfigurator
                .defaultPage(pageNumber)
                .onPageChange(this::setCurrentPage)
                .enableAnnotationRendering(true)
                .enableAntialiasing(prefManager.getBoolean(PREF_ALIASING, true))
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(PREF_SPACING)
                .onError(this::handleFileOpeningError)
                .onPageError(this::handlePageOpeningError)
                .pageFitPolicy(FitPolicy.WIDTH)
                .password(password)
                .swipeHorizontal(prefManager.getBoolean(PREF_SCROLLING, false))
                .autoSpacing(prefManager.getBoolean(PREF_SCROLLING, false))
                .pageSnap(prefManager.getBoolean(PREF_SNAP, false))
                .pageFling(prefManager.getBoolean(PREF_FLING, false))
                .nightMode(prefManager.getBoolean(PREF_NIGHT, false))
                .load();
    }

    private void handleFileOpeningError(Throwable e) {
        if (e instanceof PdfPasswordException) {
            if (password != null) {
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
                password = null;  // prevent the toast from being shown again if the user rotates the screen
            }
            PasswordDialogBinding dialogBinding = PasswordDialogBinding.inflate(getLayoutInflater());
            AlertDialog alert = new AlertDialog.Builder(this)
                    .setTitle(R.string.protected_pdf)
                    .setView(dialogBinding.getRoot())
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        password = dialogBinding.passwordInput.getText().toString();
                        displayFromUri(uri);
                    })
                    .setIcon(R.drawable.lock_icon)
                    .create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();
        } else if (couldNotOpenFileDueToMissingPermission(e)) {
            readFileErrorPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
        }
    }

    private void handlePageOpeningError(int page, Throwable e) {
        Toast.makeText(this, R.string.page_opening_error, Toast.LENGTH_LONG).show();
    }

    private boolean couldNotOpenFileDueToMissingPermission(Throwable e) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            return false;
        }
        return e instanceof FileNotFoundException && e.getMessage() != null && e.getMessage().contains("Permission denied");
    }

    private void restartAppIfGranted(boolean isPermissionGranted) {
        if (isPermissionGranted) {
            // This is a quick and dirty way to make the system restart the current activity *and the current app process*.
            // This is needed because on Android 6 storage permission grants do not take effect until
            // the app process is restarted.
            System.exit(0);
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
        }
    }

    void displayFromUri(Uri uri) {
        setTitle("");
        if (uri == null) {
            return;
        }
        configurePdfViewAndLoad(viewBinding.pdfView.fromUri(uri));
    }

    private void setCurrentPage(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s/%s %s", page + 1, pageCount, viewBinding.pdfView.getDocumentMeta().getTitle()));
    }

    private final ActivityResultLauncher<String[]> documentPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::openSelectedDocument
    );

    private final ActivityResultLauncher<String> readFileErrorPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            this::restartAppIfGranted
    );

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> displayFromUri(uri)
    );

    private void openSelectedDocument(Uri selectedDocumentUri) {
        if (selectedDocumentUri == null) {
            return;
        }

        if (uri == null || selectedDocumentUri.equals(uri)) {
            uri = selectedDocumentUri;
            displayFromUri(uri);
        } else {
            Intent intent = new Intent(this, getClass());
            intent.setData(selectedDocumentUri);
            startActivity(intent);
        }
    }

    private void pickFile() {
        try {
            documentPickerLauncher.launch(new String[]{"application/pdf"});
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }
}