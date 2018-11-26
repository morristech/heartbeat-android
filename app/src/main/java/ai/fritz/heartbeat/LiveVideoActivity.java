package ai.fritz.heartbeat;

import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.heartbeat.ui.ResultsView;
import ai.fritz.vision.inputs.FritzVisionImage;
import ai.fritz.vision.inputs.FritzVisionOrientation;
import ai.fritz.visionlabel.FritzVisionLabelPredictor;
import ai.fritz.visionlabel.FritzVisionLabelResult;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LiveVideoActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = LiveVideoActivity.class.getSimpleName();

    /**
     * Requests for the size of the preview depending on the camera results. We will try to match the closest
     * in terms of size and aspect ratio.
     */
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionLabelPredictor predictor;
    private FritzVisionLabelResult labelResult;

    private int imgRotation;

    @BindView(R.id.app_toolbar)
    Toolbar appBar;

    ResultsView resultsView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        setTitle(R.string.fritz_vision_title);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imgRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        predictor = new FritzVisionLabelPredictor();
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (!computing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        final FritzVisionImage fritzImage = FritzVisionImage.fromMediaImage(image, imgRotation);
        image.close();


        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        labelResult = predictor.predict(fritzImage);
                        labelResult.logResult();

                        if (resultsView == null) {
                            resultsView = findViewById(R.id.results);
                        }
                        resultsView.setResult(labelResult.getVisionLabels());
                        Log.d(TAG, "INFERENCE TIME:" + (SystemClock.uptimeMillis() - startTime));
                        requestRender();
                        computing.set(false);
                    }
                });
    }
}