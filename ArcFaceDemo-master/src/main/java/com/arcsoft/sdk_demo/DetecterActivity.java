package com.arcsoft.sdk_demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.arcsoft.sdk_demo.tool.BtBitmapUtils;
import com.example.faceview.java.AbsLoop;
import com.example.faceview.widget.CameraFrameData;
import com.example.faceview.widget.CameraGLSurfaceView;
import com.example.faceview.widget.CameraSurfaceView;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class DetecterActivity extends Activity implements OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback, CameraSurfaceView.OnCameraListener {
	private final String TAG = "DetecterActivity";

	private int mWidth, mHeight, mFormat;
	private CameraSurfaceView mSurfaceView;
	private CameraGLSurfaceView mGLSurfaceView;
	private Camera mCamera;

	AFT_FSDKVersion version = new AFT_FSDKVersion();
	AFT_FSDKEngine engine = new AFT_FSDKEngine();
	ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
	ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
	ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
	ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
	List<AFT_FSDKFace> result = new ArrayList<>();
	List<ASAE_FSDKAge> ages = new ArrayList<>();
	List<ASGE_FSDKGender> genders = new ArrayList<>();

	int mCameraID;
	int mCameraRotate;
	boolean mCameraMirror;
	byte[] mImageNV21 = null;
	FRAbsLoop mFRAbsLoop = null;
	AFT_FSDKFace mAFT_FSDKFace = null;
	Handler mHandler;
	private boolean isTAG = true;

	Runnable hide = new Runnable() {
		@Override
		public void run() {
//			mTextView.setAlpha(0.5f);
//			mImageView.setImageAlpha(128);
		}
	};

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case 0:
					Bitmap bitmap = (Bitmap) msg.obj;
					mImageView.setImageBitmap(bitmap);
					break;
			}
		}
	};


	private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(final byte[] data, Camera camera) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					bitmap = BtBitmapUtils.Byte2Bitmap(data);
					bitmap = BtBitmapUtils.rotaingImageView(bitmap);
					bitmap = BtBitmapUtils.getImage(bitmap);
					handler.sendEmptyMessage(0);
					//需要数据传递，用下面方法；
					Message msg = new Message();
					msg.obj = bitmap;//可以是基本类型，可以是对象，可以是List、map等；
					handler.sendMessage(msg);
				}
			}).start();
//			bitmap = BtBitmapUtils.Byte2Bitmap(data);
//			bitmap = BtBitmapUtils.rotaingImageView(bitmap);
//			bitmap = BtBitmapUtils.getImage(bitmap);

//			mImageView.setImageBitmap(BtBitmapUtils.Byte2Bitmap(data));
			mCamera.startPreview();
		}
	};
	private Camera.PictureCallback raw = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
//			bitmap = BtBitmapUtils.Byte2Bitmap(data);
//			mImageView.setImageBitmap(bitmap);
//			bitmap = BtBitmapUtils.convert(BtBitmapUtils.Byte2Bitmap(data));
//			mImageView.setImageBitmap(bitmap);
//			mCamera.startPreview();

		}
	};


	private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
		@Override
		public void onShutter() {
		}
	};
	private Bitmap bitmap;
	private int currentVolume;

	class FRAbsLoop extends AbsLoop {

		AFR_FSDKVersion version = new AFR_FSDKVersion();
		AFR_FSDKEngine engine = new AFR_FSDKEngine();
		AFR_FSDKFace result = new AFR_FSDKFace();
		List<FaceDB.FaceRegist> mResgist = ((Application) DetecterActivity.this.getApplicationContext()).mFaceDB.mRegister;
		List<ASAE_FSDKFace> face1 = new ArrayList<>();
		List<ASGE_FSDKFace> face2 = new ArrayList<>();

		@Override
		public void setup() {
			AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
			Log.d(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
			error = engine.AFR_FSDK_GetVersion(version);
			Log.d(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
		}

		@Override
		public void loop() {
			if (mImageNV21 != null) {
				long time = System.currentTimeMillis();
				AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), result);
				Log.d(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms");
				Log.d(TAG, "Face=" + result.getFeatureData()[0] + "," + result.getFeatureData()[1] + "," + result.getFeatureData()[2] + "," + error.getCode());
				AFR_FSDKMatching score = new AFR_FSDKMatching();
				float max = 0.0f;
				String name = null;
				for (FaceDB.FaceRegist fr : mResgist) {
					for (AFR_FSDKFace face : fr.mFaceList) {
						error = engine.AFR_FSDK_FacePairMatching(result, face, score);
						Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
						if (max < score.getScore()) {
							max = score.getScore();
							name = fr.mName;
						}
					}
				}

				//age & gender
				face1.clear();
				face2.clear();
				face1.add(new ASAE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
				face2.add(new ASGE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
				ASAE_FSDKError error1 = mAgeEngine.ASAE_FSDK_AgeEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face1, ages);
				ASGE_FSDKError error2 = mGenderEngine.ASGE_FSDK_GenderEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face2, genders);
				Log.d(TAG, "ASAE_FSDK_AgeEstimation_Image:" + error1.getCode() + ",ASGE_FSDK_GenderEstimation_Image:" + error2.getCode());
				Log.d(TAG, "age:" + ages.get(0).getAge() + ",gender:" + genders.get(0).getGender());
				final String age = ages.get(0).getAge() == 0 ? "年龄未知" : ages.get(0).getAge() + "岁";
				final String gender = genders.get(0).getGender() == -1 ? "性别未知" : (genders.get(0).getGender() == 0 ? "男" : "女");

				//crop
				byte[] data = mImageNV21;
				YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
				ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
				yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
				final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
				try {
					ops.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (max > 0.6f) {
					//fr success.
					final float max_score = max;
					Log.d(TAG, "fit Score:" + max + ", NAME:" + name);
					final String mNameShow = name;
					mHandler.removeCallbacks(hide);
					mHandler.post(new Runnable() {
						@Override
						public void run() {

							mTextView.setAlpha(1.0f);
							mTextView.setText(mNameShow);
							mTextView.setTextColor(Color.RED);
							mTextView1.setVisibility(View.VISIBLE);
							mTextView1.setText("置信度：" + (float) ((int) (max_score * 1000)) / 1000.0);
							mTextView1.setTextColor(Color.RED);
							mImageView.setRotation(mCameraRotate);
							if (mCameraMirror) {
								mImageView.setScaleY(-1);
							}
							mImageView.setImageAlpha(255);
							mImageView.setImageBitmap(bmp);
						}
					});
				} else {
					if (isTAG) {
						mCamera.takePicture(null, raw, jpeg);

//						mHandler.post(new Runnable() {
//							@Override
//							public void run() {
//								mTextView.setAlpha(1.0f);
//								mTextView.setTextColor(Color.RED);
//								mTextView1.setVisibility(View.VISIBLE);
//								mTextView1.setTextColor(Color.RED);
//								mImageView.setRotation(mCameraRotate);
//								if (mCameraMirror) {
//									mImageView.setScaleY(-1);
//								}
//								mImageView.setImageAlpha(255);
//								mImageView.setImageBitmap(bmp);
//							}
//						});
						isTAG = false;
//						AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//						audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
//						if (currentVolume == 0) { //0代表静音或者震动
//							final Handler soundHandler = new Handler();
//							Timer t = new Timer();
//							t.schedule(new TimerTask() {
//								@Override
//								public void run() {
//									soundHandler.post(new Runnable() {
//										@Override
//										public void run() {
//											AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//											audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
//										}
//									});
//
//								}
//							}, 100);
//						}
					}
//					final String mNameShow = "未识别";
//					DetecterActivity.this.runOnUiThread(new Runnable() {
//						@Override
//						public void run() {
//							if (currentVolume == 0) { //0代表静音或者震动
//								final Handler soundHandler = new Handler();
//								Timer t = new Timer();
//								t.schedule(new TimerTask() {
//									@Override
//									public void run() {
//										soundHandler.post(new Runnable() {
//											@Override
//											public void run() {
//												AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//												audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
//											}
//										});
//
//									}
//								}, 100);
//							}
//							mTextView.setAlpha(1.0f);
//							mTextView1.setVisibility(View.VISIBLE);
//							mTextView1.setText(gender + "," + age);
//							mTextView1.setTextColor(Color.RED);
//							mTextView.setText(mNameShow);
//							mTextView.setTextColor(Color.RED);
//							mImageView.setImageAlpha(255);
//							mImageView.setRotation(mCameraRotate);
//							if (mCameraMirror) {
//								mImageView.setScaleY(-1);
//							}
//							mImageView.setImageBitmap(bitmap);
//						}
//					});
				}
				mImageNV21 = null;
			}

		}

		@Override
		public void over() {
			AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
			Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
		}
	}

	private TextView mTextView;
	private TextView mTextView1;
	private ImageView mImageView;
	;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mCameraID = getIntent().getIntExtra("Camera", 0) == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
		mCameraRotate = getIntent().getIntExtra("Camera", 0) == 0 ? 90 : 270;
		mCameraMirror = getIntent().getIntExtra("Camera", 0) == 0 ? false : true;
		mWidth = getWindowManager().getDefaultDisplay().getWidth();
		mHeight = getWindowManager().getDefaultDisplay().getHeight();
		mFormat = ImageFormat.NV21;
		mHandler = new Handler();

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// 下面这句代码可以根据系统音量的状态来开关拍照声音
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
		if (currentVolume == 0) {
			audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
		}
		setWindowBrightness(255);

		setContentView(R.layout.activity_camera);
		mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
		mGLSurfaceView.setOnTouchListener(this);
		mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
		mSurfaceView.setOnCameraListener(this);
		mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
		mSurfaceView.debug_print_fps(true, false);

		//snap
		mTextView = (TextView) findViewById(R.id.textView);
		mTextView.setText("");
		mTextView1 = (TextView) findViewById(R.id.textView1);
		mTextView1.setText("");

		mImageView = (ImageView) findViewById(R.id.imageView);

		AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 10);
		Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
		err = engine.AFT_FSDK_GetVersion(version);
		Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

		ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
		Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
		error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
		Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

		ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
		Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
		error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
		Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());

		mFRAbsLoop = new FRAbsLoop();
		mFRAbsLoop.start();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mFRAbsLoop.shutdown();
		AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
		Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

		ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
		Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

		ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
		Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
	}

	@Override
	public Camera setupCamera() {
		// TODO Auto-generated method stub
		mCamera = Camera.open(mCameraID);
		try {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
			parameters.setPreviewSize(mHeight, mWidth);
//			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//			parameters.setJpegThumbnailSize(1,2);
			parameters.setPreviewFormat(mFormat);
			parameters.setJpegQuality(90); // 设置照片质量
			List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
			WindowManager wm = (WindowManager) DetecterActivity.this
					.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			int maxSize = Math.max(display.getWidth(), display.getHeight());
			int length = sizes.size();
			if (maxSize > 0) {
				for (int i = 0; i < length; i++) {
					if (maxSize <= Math.max(sizes.get(i).width, sizes.get(i).height)) {
						parameters.setPictureSize(sizes.get(i).width, sizes.get(i).height);
						break;
					}
				}
			}
			List<Camera.Size> ShowSizes = parameters.getSupportedPreviewSizes();
			int showLength = ShowSizes.size();
			if (maxSize > 0) {
				for (int i = 0; i < showLength; i++) {
					if (maxSize <= Math.max(ShowSizes.get(i).width, ShowSizes.get(i).height)) {
						parameters.setPreviewSize(ShowSizes.get(i).width, ShowSizes.get(i).height);
						break;
					}
				}
			}
//			for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
//				Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
//			}
//			for (Integer format : parameters.getSupportedPreviewFormats()) {
//				Log.d(TAG, "FORMAT:" + format);
//			}
//
//			List<int[]> fps = parameters.getSupportedPreviewFpsRange();
//			for (int[] count : fps) {
//				Log.d(TAG, "T:");
//				for (int data : count) {
//					Log.d(TAG, "V=" + data);
//				}
//			}
			//parameters.setPreviewFpsRange(15000, 30000);
			//parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
			//parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
			//parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
			//parmeters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			//parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
			//parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
			mCamera.setDisplayOrientation(90);// 设置PreviewDisplay的方向，效果就是将捕获的画面旋转多少度显示
			mCamera.setParameters(parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mCamera != null) {
			mWidth = mCamera.getParameters().getPreviewSize().width;
			mHeight = mCamera.getParameters().getPreviewSize().height;
		}
		return mCamera;
	}

	@Override
	public void setupChanged(int format, int width, int height) {

	}

	@Override
	public boolean startPreviewLater() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
		AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
		Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
		Log.d(TAG, "Face=" + result.size());
		for (AFT_FSDKFace face : result) {
			Log.d(TAG, "Face:" + face.toString());
		}
		if (mImageNV21 == null) {
			if (!result.isEmpty()) {
				mAFT_FSDKFace = result.get(0).clone();
				mImageNV21 = data.clone();
			} else {
				mHandler.postDelayed(hide, 3000);
			}
		}
		//copy rects
		Rect[] rects = new Rect[result.size()];
		for (int i = 0; i < result.size(); i++) {
			rects[i] = new Rect(result.get(i).getRect());
		}
		//clear result.
		result.clear();
		//return the rects for render.
		return rects;
	}

	@Override
	public void onBeforeRender(com.guo.android_extend.widget.CameraFrameData data) {

	}

	@Override
	public void onAfterRender(com.guo.android_extend.widget.CameraFrameData data) {

	}

	@Override
	public void onBeforeRender(CameraFrameData data) {

	}

	@Override
	public void onAfterRender(CameraFrameData data) {
//		mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.TRANSPARENT, -1);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		CameraHelper.touchFocus(mCamera, event, v, this);
		return false;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (success) {
			Log.d(TAG, "Camera Focus SUCCESS!");
		}
	}

	/**
	 * 设置屏幕的亮度值
	 *
	 * @param brightness
	 */
	private void setWindowBrightness(int brightness) {
		Window window = getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.screenBrightness = brightness / 255.0f;
		window.setAttributes(lp);
	}
}
