package gal.yonastudios.emotiontrainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageView iv_11,
            iv_21,
            iv_31,
            iv_41,
            iv_51;

    Button b_play,b_pause;

    Boolean isPaused,isPlay, isBackgroundMusicMuted, isSoundsMuted, init, isStopped;

    TextView score,time,best;

    private MediaPlayer riseAndShineBackgroundMusic, notPlayingGameMusic, happySound,neutralSound,fearSound,disgustSound,surpriseSound;

    Random r;

    int rockLocationRow1,rockLocationRow2,rockLocationRow3,rockLocationRow4,rockLocationRow5;

    int frameImage, pawInImage,happyImage, sadImage, angryImage, fearImage, surpriseImage, disgustImage, neutralImage, emptyImage;

    int currScore=0;

    int bestScore;

//    CountDownTimer timer;

    CountDownTimerWithPause timerWithPause;

    long currentMillisLeft;

    Interpreter tflite;

    private final Executor executor = Executors.newSingleThreadExecutor();

    private List<String> associatedAxisLabels;

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    PreviewView mPreviewView;

    private static int emotionPrediction;

    private static LinkedList<String> result;

    public TextView emotionPredictText;

    private Camera camera;

    private GraphicOverlay graphicOverlay;

    private LinkedList<FaceDrawRect> prevFacesBounds = null;
    private FaceDrawRect faceDrawRect = null;

    private String noFaceDetectedText;

    private Rect CurrFaceBounds = null;

    private SharedPreferences sharedPreferences;

//    stablizing variables
   HashMap<String,LinkedList<Float>> emotionMapLast5Samples1;
   HashMap<String,Float> emotionSums1;

   int gamesToShowAd,gamesCount;

   private InterstitialAd mInterstitialAd;

   private GoogleSignInClient mGoogleSignInClient;
   private GoogleSignInAccount mGoogleSignInAccount;

   private boolean isSignedIn;
   private int RC_SIGN_IN=1;

   private LeaderboardsClient leaderboardsClient;
    private static final int RC_LEADERBOARD_UI = 9004;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            associatedAxisLabels = FileUtil.loadLabels(this, "labels.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


        mPreviewView = (PreviewView) findViewById(R.id.campre);

        graphicOverlay= (GraphicOverlay) findViewById(R.id.graphic_overlay);
        graphicOverlay.setCameraInfo(mPreviewView.getWidth(), mPreviewView.getHeight(), CameraSource.CAMERA_FACING_FRONT);

        emotionPredictText=(TextView)findViewById(R.id.emotion_predict_text);
        emotionPredictText.setTextColor(getResources().getColor(R.color.black));

        MainActivity.emotionPrediction=-1;

//        result="";
        result = new LinkedList<>();
        prevFacesBounds=new LinkedList<>();

        riseAndShineBackgroundMusic =MediaPlayer.create(MainActivity.this, R.raw.riseandshine);
        notPlayingGameMusic = MediaPlayer.create(MainActivity.this, R.raw.holidayukulele);
        happySound = MediaPlayer.create(MainActivity.this, R.raw.drdrepiano);
        neutralSound = MediaPlayer.create(MainActivity.this, R.raw.gentlepianochordcminor);
        fearSound = MediaPlayer.create(MainActivity.this, R.raw.apianolowchordamajor);
        disgustSound = MediaPlayer.create(MainActivity.this, R.raw.synthcut034eminor);
        surpriseSound = MediaPlayer.create(MainActivity.this, R.raw.ontherhodesdminor);


        sharedPreferences=getSharedPreferences("PREFS",MODE_PRIVATE);
        bestScore=sharedPreferences.getInt("bestscore",0);

        iv_11=(ImageView) findViewById(R.id.iv_11);

        iv_21=(ImageView) findViewById(R.id.iv_21);

        iv_31=(ImageView) findViewById(R.id.iv_31);

        iv_41=(ImageView) findViewById(R.id.iv_41);

        iv_51=(ImageView) findViewById(R.id.iv_51);


        b_play=(Button) findViewById(R.id.b_play);

        b_pause=(Button) findViewById(R.id.b_pause);

        score=(TextView) findViewById(R.id.tv_score);
        score.setText("Score: "+currScore);

        time=(TextView) findViewById(R.id.tv_time);
        time.setText("Time: "+30);


        best=(TextView) findViewById(R.id.tv_best);
        best.setText("Best: "+bestScore);

        r=new Random();

        loadImages();

        isPaused=false;

        isPlay=false;

        init=false;

        isStopped=false;

        isBackgroundMusicMuted =false;
        isSoundsMuted = false;

        currentMillisLeft=30000;

        noFaceDetectedText = "No face detected";

        iv_31.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currScore=currScore-2;
                continueGame();
            }
        });

        b_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initGame();
            }
        });

        b_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isPaused) {
                    onPauseGame();
                    isPaused=true;
                }
                else{
//                    MainActivity.this.onResume();
                    onResumeGame();
                    isPaused=false;
                }
            }
        });

        b_pause.setVisibility(View.INVISIBLE);

        emotionSums1 =new HashMap<>();

        initEmotionSamplesMapAndArray();

        try{
            tflite=new Interpreter(this.loadModelFile(MainActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
//        AdRequest adRequest= new AdRequest.Builder().build();
//
//        //test ad url - "ca-app-pub-3940256099942544/1033173712"
//        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        // The mInterstitialAd reference will be null until
//                        // an ad is loaded.
//                        mInterstitialAd = interstitialAd;
//                        Log.i("TAG", "onAdLoaded");
//                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
//                            @Override
//                            public void onAdDismissedFullScreenContent() {
//                                // Called when fullscreen content is dismissed.
//                                Log.d("TAG", "The ad was dismissed.");
//                                if(!isBackgroundMusicMuted) {
//                                    notPlayingGameMusic.start();
//                                }
//                            }
//
//                            @Override
//                            public void onAdFailedToShowFullScreenContent(AdError adError) {
//                                // Called when fullscreen content failed to show.
//                                Log.d("TAG", "The ad failed to show.");
//                            }
//
//                            @Override
//                            public void onAdShowedFullScreenContent() {
//                                // Called when fullscreen content is shown.
//                                // Make sure to set your reference to null so you don't
//                                // show it a second time.
//                                mInterstitialAd = null;
//                                Log.d("TAG", "The ad was shown.");
//                                if(!isBackgroundMusicMuted) {
//                                    notPlayingGameMusic.pause();
//                                }
//                            }
//                        });
//
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error
//                        Log.i("TAG", loadAdError.getMessage());
//                        mInterstitialAd = null;
//                    }
//                });


//        gamesToShowAd =r.nextInt(3)+1;
        gamesToShowAd=2;
        gamesCount=sharedPreferences.getInt("gamescount", 0);

        //first time user show instructions
        if(sharedPreferences.getBoolean("isfirsttime",true)){
            popUpHelp();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isfirsttime", false);
            editor.apply();
        }

        //google play sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });




    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    private void updateUI(GoogleSignInAccount account){
        if(account!=null){
            isSignedIn=true;
        }
        else{
            isSignedIn=false;
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            mGoogleSignInAccount = completedTask.getResult(ApiException.class);
            Log.w("TAG", "Sign in worked partially");
            leaderboardsClient = Games.getLeaderboardsClient(MainActivity.this,mGoogleSignInAccount);
            // Signed in successfully, show authenticated UI.
            updateUI(mGoogleSignInAccount);
            Log.w("TAG", "Sign in worked");

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    private void showLeaderboard() {
        Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .getLeaderboardIntent(getString(R.string.leaderboard_id))
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_LEADERBOARD_UI);
                    }
                });
    }

    private void bringGameGridToFront(){
        iv_11.setElevation(1000);
        iv_21.setElevation(1000);
        iv_31.setElevation(1000);
        iv_41.bringToFront();
        iv_51.bringToFront();

    }

//    private void startCountDownTimer(){
//        this.timer=new CountDownTimer(currentMillisLeft,500) {
//
//            @Override
//            public void onTick(long l) {
//                time.setText("Time:"+millisToSeconds(l+1));
//                currentMillisLeft=l;
//            }
//
//            @Override
//            public void onFinish() {
//                time.setText("Time:"+0);
//                iv_11.setEnabled(false);
//                iv_12.setEnabled(false);
//                iv_13.setEnabled(false);
//
//                endGame("Game Over!");
//
//
//            }
//        }.start();
//    }

    private void startCountDownTimerWithPause(){
        this.timerWithPause = new CountDownTimerWithPause(currentMillisLeft,500)
        {
            @Override
            public void onTick(long l) {
                time.setText("Time:"+millisToSeconds(l+1));
                currentMillisLeft=l;
            }

            @Override
            public void onFinish() {
                time.setText("Time:"+0);
//                iv_11.setEnabled(false);


                endGame("Game Over!");



            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        }
    }

    //need to run it on every frame made by the camera
    //tflite.run(imgData, labelProbArray);

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("tf_lite_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        Log.d("startOffset",""+startOffset);
        Log.d("declaredLength",""+declaredLength);
//        long startOffset=0;
//        long declaredLength = 16000000;
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isPlay) {
            if(this.timerWithPause != null){
                this.timerWithPause.pause();
                Log.d("timer pause", "timer pause");
            }
//            iv_31.setEnabled(false);
            b_pause.setText(R.string.resume_string);
            isPlay=false;
            isPaused=true;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(isStopped) {//reprepare media players if needed
//            riseAndShineBackgroundMusic.prepareAsync();
//            notPlayingGameMusic.prepareAsync();
//        }
//        if(isPlay){// if in play continue pause
//            isPaused=true;
//        }
        if(!isBackgroundMusicMuted){
            if(isPlay) {
                Log.d("entered music","rise and shine");
                riseAndShineBackgroundMusic.start();
            }
            else{
                Log.d("entered music","ukulele");
                notPlayingGameMusic.start();
            }
        }
    }

    private void onPauseGame(){
        if(this.timerWithPause != null){
            this.timerWithPause.pause();
        }
//        iv_31.setEnabled(false);
        b_pause.setText(R.string.resume_string);
        isPlay=false;
        isPaused=true;
    }

    private void onResumeGame(){
        if(isPaused) {// if in pause continue play
            startCountDownTimerWithPause();
//            iv_31.setEnabled(true);
            b_pause.setText(R.string.pause_string);
            isPlay=true;
            isPaused=false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseMusic();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("gamescount", gamesCount);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        riseAndShineBackgroundMusic.stop();
        notPlayingGameMusic.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        isPaused=true;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pause_menu,menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(isPlay) {
            MainActivity.this.onPause();
        }
//        if(!isBackgroundMusicMuted) {//if the button title is mute, the user is on unmute state.
//            riseAndShineBackgroundMusic.start();
//        }
        if(isPaused){//if the game is paused
            menu.getItem(1).setTitle("Resume Game");
        }
        init=true;
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.help_btn:
                //pause the app
                MainActivity.this.onPause();
                popUpHelp();
                break;
            case R.id.leaderboards_btn:
                //check if is signed in
                if(isSignedIn){
                    showLeaderboard();
                }
                else{
                    signIn();
                }
                break;
            case R.id.resume_btn:
//                MainActivity.this.onResume();
                if(item.getTitle().equals("Resume Game")){
                    onResumeGame();
                }
//                isPaused=false;
                break;
            case R.id.end_game_btn:
                endGame("");
                break;
            case R.id.restart_btn:
                endGame("");
                initGame();
                break;
            case R.id.mute_un_mute_background_music_btn:
                if(!isBackgroundMusicMuted){
                    pauseMusic();
                    isBackgroundMusicMuted =true;
                    item.setTitle(R.string.unmute_background_music_btn_string);
                }
                else{
                    riseAndShineBackgroundMusic.start();
                    isBackgroundMusicMuted =false;
                    item.setTitle(R.string.mute_background_music_btn_string);
                }
                break;
            case R.id.mute_un_mute_sound_btn:
                if(!isSoundsMuted){
                    isSoundsMuted=true;
                    item.setTitle(R.string.unmute_sounds_btn_string);
                }
                else{
                    isSoundsMuted =false;
                    item.setTitle(R.string.mute_sounds_btn_string);
                }
                break;
            case R.id.exit_btn:
                this.finishAffinity();
                break;
        }
        Log.d("onCreateOptionsMenu","enter menu");

        return super.onOptionsItemSelected(item);
    }

    private void popUpHelp(){
        //inflate
        LayoutInflater helpInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View helpPopUp = helpInflater.inflate(R.layout.help_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(helpPopUp, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        View view = new View(MainActivity.this);
        findViewById(R.id.activity_main).post(new Runnable() {
            @Override
            public void run() {
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            }
        });

        // dismiss the popup window when touched
        helpPopUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MainActivity.this.onResume();
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private void pauseMusic(){
        riseAndShineBackgroundMusic.pause();
        notPlayingGameMusic.pause();
    }

    private int millisToSeconds(long millis){
        return (int)millis/1000;
    }

    private void loadImages(){
        frameImage=R.drawable.blackframe;
        pawInImage =R.drawable.happyframefull;
        happyImage =R.drawable.happyframe;
        sadImage=R.drawable.sadframe;
        disgustImage=R.drawable.disgustframe;
        surpriseImage=R.drawable.surpriseframe;
        fearImage=R.drawable.fearframe;
        angryImage=R.drawable.angryframe;
        neutralImage=R.drawable.neutralframe;
        emptyImage=R.drawable.emptyimage;
    }

    private void continueGame(){
        if(!isSoundsMuted) {
            playSound();
        }
        rockLocationRow5=rockLocationRow4;
        setRockLocation(rockLocationRow5,5);

        rockLocationRow4=rockLocationRow3;
        setRockLocation(rockLocationRow4,4);

        rockLocationRow3=rockLocationRow2;
        setRockLocation(rockLocationRow3,3);

        rockLocationRow2=rockLocationRow1;
        setRockLocation(rockLocationRow2,2);

        rockLocationRow1=r.nextInt(3)+1;
        Log.d("rockLocationRow1",""+rockLocationRow1);
        setRockLocation(rockLocationRow1,1);

        currScore=currScore+1;
        score.setText("Score: "+currScore);
    }


    private void endGame(String str) {
        b_play.setVisibility(View.VISIBLE);
        b_pause.setVisibility(View.INVISIBLE);

        iv_11.setImageResource(emptyImage);

        iv_21.setImageResource(emptyImage);


        iv_31.setImageResource(emptyImage);

        iv_41.setImageResource(emptyImage);

        iv_51.setImageResource(emptyImage);

        setImagesInvisible();


//        iv_31.setEnabled(false);

        if(!str.equals("")) {
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
        }
        if (currScore > bestScore) {
            bestScore = currScore;
            best.setText("Best:" + bestScore);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("bestscore", bestScore);
            editor.apply();
        }

        if(currentMillisLeft<=25000){
        gamesCount=gamesCount+1;
        }
        if(gamesCount>= gamesToShowAd){
            gamesCount=0;
            gamesToShowAd=r.nextInt(2)+3;
            if (mInterstitialAd != null) {
                mInterstitialAd.show(MainActivity.this);
                Log.d("TAG", "The interstitial has shown.");

            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
            }
        }
        else if(gamesCount==gamesToShowAd-1){
            loadInteristitialAd();
        }

//        timer.cancel();
        if(this.timerWithPause!=null) {
            this.timerWithPause.cancel();
        }
        isPlay=false;

        if(!isBackgroundMusicMuted) {
            notPlayingGameMusic.start();
        }
        riseAndShineBackgroundMusic.pause();



    }

    public void loadInteristitialAd(){
        AdRequest adRequest= new AdRequest.Builder().build();

        //my ads url - "ca-app-pub-7650736180602884/8768926599"
        //test ads url - "ca-app-pub-3940256099942544/1033173712"
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i("TAG", "onAdLoaded");
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                                Log.d("TAG", "The ad was dismissed.");
                                if(!isBackgroundMusicMuted) {
                                    notPlayingGameMusic.start();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                                Log.d("TAG", "The ad failed to show.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                mInterstitialAd = null;
                                Log.d("TAG", "The ad was shown.");
                                if(!isBackgroundMusicMuted) {
                                    notPlayingGameMusic.pause();
                                }
                            }
                        });

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }


    private void setRockLocation(int place, int row){
        if(row==1) {
//            iv_11.setImageResource(emptyImage);
//            iv_12.setImageResource(emptyImage);
//            iv_13.setImageResource(emptyImage);
            iv_11.setImageResource(frameImage);
            iv_11.setAlpha(1f);


        }

        else if(row==2) {
//            iv_21.setImageResource(emptyImage);
//            iv_22.setImageResource(emptyImage);
//            iv_23.setImageResource(emptyImage);

            iv_21.setImageResource(frameImage);
            iv_21.setAlpha(1f);


        }

        else if(row==3) {
//            iv_31.setImageResource(emptyImage);
//            iv_32.setImageResource(emptyImage);
//            iv_33.setImageResource(emptyImage);

            MainActivity.emotionPrediction=getEmotion();
            iv_31.setImageResource(emotionPrediction);
            iv_31.setAlpha(1f);

        }
        else if(row==4) {
//            iv_41.setImageResource(emptyImage);
//            iv_42.setImageResource(emptyImage);
//            iv_43.setImageResource(emptyImage);


            iv_41.setImageResource(pawInImage);
            iv_41.setAlpha(1f);


        }

        else if(row==5) {
//            iv_51.setImageResource(emptyImage);
//            iv_52.setImageResource(emptyImage);
//            iv_53.setImageResource(emptyImage);

            iv_51.setImageResource(pawInImage);
            iv_51.setAlpha(1f);


        }

    }

    private void initGame(){
//        iv_31.setEnabled(true);

        b_play.setVisibility(View.INVISIBLE);
        b_pause.setVisibility(View.VISIBLE);
        setImagesVisible();

        currentMillisLeft=30000;

        currScore=0;
        score.setText("Score: "+currScore);

        startCountDownTimerWithPause();

        rockLocationRow4=2;
        iv_41.setImageResource(emptyImage);
        rockLocationRow3=2;
        emotionPrediction=getEmotion();
        iv_31.setImageResource(emotionPrediction);
        rockLocationRow2=r.nextInt(3)+1;
        setRockLocation(rockLocationRow2,2);
        rockLocationRow1=r.nextInt(3)+1;
        setRockLocation(rockLocationRow1,1);
        bringGameGridToFront();

        isPlay=true;
        isPaused=false;
        notPlayingGameMusic.pause();
        if(!isBackgroundMusicMuted) {
            riseAndShineBackgroundMusic.start();
        }
    }

    private void setImagesVisible(){
        iv_11.setVisibility(View.VISIBLE);
        iv_21.setVisibility(View.VISIBLE);
        iv_31.setVisibility(View.VISIBLE);
        iv_41.setVisibility(View.VISIBLE);
        iv_51.setVisibility(View.VISIBLE);

    }

    private void setImagesInvisible(){
        iv_11.setVisibility(View.INVISIBLE);
        iv_21.setVisibility(View.INVISIBLE);
        iv_31.setVisibility(View.INVISIBLE);
        iv_41.setVisibility(View.INVISIBLE);
        iv_51.setVisibility(View.INVISIBLE);
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

//    private void plusOne(int column){//when getting +1 point, jumps a +1 graphic to the screen
//        switch(column){
//            case 1:
//                break;
//            case 2:
//                break;
//            case 3:
//                break;
//        }
//    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();


        FaceDetector faceDetector = FaceDetection.getClient();

        // Real-time contour detection
        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();


        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)//a frame analysis will not block more recent frames to come
                .build();
        imageAnalysis.setAnalyzer(executor,
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        result.clear();
                        @SuppressLint("UnsafeOptInUsageError")
                        Image mediaImage = image.getImage();
                        if (mediaImage != null) {
                            InputImage inputImage =
                                    InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                            Task<List<Face>> facesResult =
                                    faceDetector.process(inputImage)
                                            .addOnSuccessListener(
                                                    new OnSuccessListener<List<Face>>() {

                                                        @SuppressLint("UnsafeOptInUsageError")
                                                        @Override
                                                        public void onSuccess(List<Face> faces) {
                                                            int numOfFacesDetected=0;
                                                            if(prevFacesBounds !=null) {
                                                                for (int i=0;i<prevFacesBounds.size();i++) {
                                                                    graphicOverlay.remove(prevFacesBounds.remove());
                                                                }
                                                            }
                                                            for(Face face : faces){

                                                                numOfFacesDetected++;
                                                                CurrFaceBounds = face.getBoundingBox();
                                                                faceDrawRect = new FaceDrawRect(graphicOverlay, CurrFaceBounds);
                                                                graphicOverlay.add(faceDrawRect);
                                                                prevFacesBounds.add(faceDrawRect);
                                                                image.setCropRect(CurrFaceBounds);
                                                                MainActivity.result.add(Classify(image));

                                                            }
                                                            if(numOfFacesDetected>0) {
                                                                runOnUiThread(new Runnable() {

                                                                    @Override
                                                                    public void run() {
                                                                        String outputResult=MainActivity.result.toString();
                                                                        emotionPredictText.setText(outputResult.substring(1,outputResult.length()-1));
                                                                        if(isPlay&&checkResultEmotion(result)){
                                                                            continueGame();
                                                                            Log.d("checkResultEmotion true","In result:"+result);
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                            else{
                                                                emotionPredictText.setText(noFaceDetectedText);
                                                                graphicOverlay.clear();

                                                            }
                                                        }
                                                    })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            String result;
                                                            result="Got an Error while trying to detect a face";
                                                            emotionPredictText.setText(result);
                                                            graphicOverlay.clear();

                                                        }
                                                    })
                                            .addOnCompleteListener(
                                                    new OnCompleteListener<List<Face>>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<List<Face>> task) {
                                                            image.close();
                                                        }
                                                    }
                                            );

                        }
                        else{
                                                    image.close();
                        }
                    }
                });



        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);



//        captureImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
//                File file = new File(getBatchDirectoryName(), mDateFormat.format(new Date())+ ".jpg");
//
//                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
//                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
//                    @Override
//                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                        new Handler().post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(MainActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                    @Override
//                    public void onError(@NonNull ImageCaptureException error) {
//                        error.printStackTrace();
//                    }
//                });
//            }
//        });
    }


    public String Classify(ImageProxy image){
        @SuppressLint({"UnsafeOptInUsageError"})
        Image img = image.getImage();
        Rect faceBounds = image.getCropRect();
        Bitmap originalbitmap = Utils.toBitmap(img);
        int faceWidth=faceBounds.width();
        int faceHeight=faceBounds.height();
        String output="";

        Matrix rotationMatrix=new Matrix();
        rotationMatrix.postRotate(-90);

        originalbitmap=Bitmap.createBitmap(originalbitmap,0,0,640,480,rotationMatrix,false);
        Bitmap bitmap;
        if(faceWidth>0&&faceBounds.left+faceWidth<=originalbitmap.getWidth()) {
            bitmap = Bitmap.createBitmap(originalbitmap, faceBounds.left, faceBounds.top, faceWidth, faceHeight);
        }
        else{
            bitmap=originalbitmap;
        }
//        int rotation = Utils.getImageRotation(image);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = height > width ? width : height;

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(size, size))
                .add(new ResizeOp(48, 48, ResizeOp.ResizeMethod.BILINEAR))
                .add(new TransformToGrayScaleOpChanged())
                .build();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);

        tensorImage.load(bitmap);

        tensorImage = imageProcessor.process(tensorImage);
//        Bitmap tensorBitmap=tensorImage.getBitmap();
        TensorBuffer probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 7}, DataType.FLOAT32);

        if(null != tflite) {
            tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
        }
        TensorProcessor probabilityProcessor =
                new TensorProcessor.Builder().add(new NormalizeOp(0, 255)).build();

//        this.result = " ";

        if (null != associatedAxisLabels) {
            // Map of labels and their corresponding probability
            TensorLabel labels = new TensorLabel(associatedAxisLabels,
                    probabilityProcessor.process(probabilityBuffer));
            // Create a map to access the result based on label
            Map<String, Float> floatMap = labels.getMapWithFloatValue();
            Utils.correctPredictEmotions(floatMap);
            sumEmotionsProbs(floatMap);

            output = Utils.writeResults(emotionSums1);
        }
        return output;
    }



    private int getEmotion(){
        int emotionTap=r.nextInt(7)+1;
        emotionTap=convertNumToImageId(emotionTap);
        while(MainActivity.emotionPrediction == emotionTap){
            emotionTap=r.nextInt(7)+1;
            emotionTap=convertNumToImageId(emotionTap);
        }

        return emotionTap;
    }

    private void playSound(){
        if(MainActivity.emotionPrediction==happyImage){
            happySound.start();
        }
        else if(MainActivity.emotionPrediction==neutralImage){
            neutralSound.start();
        }
        else if(MainActivity.emotionPrediction==fearImage){
            fearSound.start();
        }
        else if(MainActivity.emotionPrediction==disgustImage){
            disgustSound.start();
        }
        else if(MainActivity.emotionPrediction==surpriseImage){
            surpriseSound.start();
        }
    }

    private int convertNumToImageId(int emotionTap){
        switch(emotionTap){
            case 1:
                return happyImage;
//            case 2:
//                return sadImage; //not working well
            case 2:
                return happyImage;
            case 3:
                return disgustImage;
            case 4:
                return surpriseImage;
            case 5:
                return fearImage;
//            case 6:
//                return angryImage;// not working well
            case 6:
                return neutralImage;
            case 7:
                return neutralImage;
        }
        return -1;
    }

    private boolean checkResultEmotion(LinkedList<String> result) {
        Boolean output=false;
        for(String res : result){
            switch (res) {
                case "Happy":
                    output=output||happyImage == emotionPrediction;
                    break;
//            case "Sad":
//                return sadImage==emotionPrediction; not working well
                case "Disgust":
                    output=output||disgustImage == emotionPrediction;
                    break;
                case "Surprise":
                    output=output||surpriseImage == emotionPrediction;
                    break;
                case "Fear":
                    output=output||fearImage == emotionPrediction;
                    break;
//            case "Angry":
//                return angryImage==emotionPrediction; not working well
                case "Neutral":
                    output=output||neutralImage == emotionPrediction;
                    break;
            }
        }
        return output;
    }

    private void initEmotionSamplesMapAndArray(){
        emotionMapLast5Samples1 = new HashMap<>();
        emotionMapLast5Samples1.put("Happy",new LinkedList<>());
        emotionMapLast5Samples1.put("Neutral",new LinkedList<>());
        emotionMapLast5Samples1.put("Surprise",new LinkedList<>());
        emotionMapLast5Samples1.put("Fear",new LinkedList<>());
        emotionMapLast5Samples1.put("Sad",new LinkedList<>());
        emotionMapLast5Samples1.put("Angry",new LinkedList<>());
        emotionMapLast5Samples1.put("Disgust",new LinkedList<>());
        for(int i=0; i<5;i++){//initiallize 5 samples in each list
            emotionMapLast5Samples1.get("Happy").add(0f);
            emotionMapLast5Samples1.get("Neutral").add(0f);
            emotionMapLast5Samples1.get("Surprise").add(0f);
            emotionMapLast5Samples1.get("Fear").add(0f);
            emotionMapLast5Samples1.get("Sad").add(0f);
            emotionMapLast5Samples1.get("Angry").add(0f);
            emotionMapLast5Samples1.get("Disgust").add(0f);
        }
        //initiallize
        emotionSums1.put("Happy",0f);
        emotionSums1.put("Neutral",0f);
        emotionSums1.put("Surprise",0f);
        emotionSums1.put("Fear",0f);
        emotionSums1.put("Sad",0f);
        emotionSums1.put("Angry",0f);
        emotionSums1.put("Disgust",0f);

    }

    private void sumEmotionsProbs(Map<String, Float> probMap){
        Float valueDecTmp;
        for(Map.Entry<String, Float> entry : probMap.entrySet()){
            valueDecTmp=this.emotionMapLast5Samples1.get(entry.getKey()).removeFirst();
            this.emotionMapLast5Samples1.get(entry.getKey()).add(entry.getValue());
            emotionSums1.put(entry.getKey(), emotionSums1.get(entry.getKey())+entry.getValue()-valueDecTmp);
        }
    }

}



