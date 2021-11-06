package com.example.landmarks;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.util.Log;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.glutil.EglManager;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.PacketGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import android.view.Window;
import android.view.WindowManager;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import com.google.cardboard.sdk.screenparams.ScreenParamsUtils;

public class HandLandmarks {

    //private static final String BINARY_GRAPH_NAME = "hand_tracking_landmarks_mobile_gpu.binarypb";
	private static final String BINARY_GRAPH_NAME = "hand_tracking_landmarks_mobile_gpu.binarypb"; 
	private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
	private static final String TAG = "HandLandmarks";
	private List<NormalizedLandmarkList> multiHandLandmarks;
	
	protected float customDensity = 300;

    public static String getOutputLandmarksStreamName() {
        return OUTPUT_LANDMARKS_STREAM_NAME;
    }

    //private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    private static final String INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands";
	private static final String OUTPUT_HAND_PRESENCE_SCORE = "palm_detections";
    private static final int NUM_HANDS = 1;
    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.BACK;
	private boolean isHandPresent = false;
	private static final float maxHandDistance = -0.00006f;
	private boolean isGestureForwardPresent = false;
	private boolean isGestureBackwardPresent = false;
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at t

    // Number of output frames allocated in ExternalTextureConverter.
    // NOTE: use "converterNumBuffers" in manifest metadata to override number of buffers. For
    // example, when there is a FlowLimiterCalculator in the graph, number of buffers should be at
    // least `max_in_flight + max_in_queue + 1` (where max_in_flight and max_in_queue are used in
    // FlowLimiterCalculator options). That's because we need buffers for all the frames that are in
    // flight/queue plus one for the next frame from the camera.
    private static final int NUM_BUFFERS = 2;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (java.lang.UnsatisfiedLinkError e) {
            // Some example apps (e.g. template matching) require OpenCV 4.
            System.loadLibrary("opencv_java4");
        }
    }
	
	public boolean getisGestureForwardPresent() {
		return isGestureForwardPresent;
	}
	
	public boolean getisGestureBackwardPresent() {
		return isGestureBackwardPresent;
	}
	
	public synchronized void setisGestureForwardPresent(boolean value) {
		isGestureForwardPresent = value;
	}
	
	public synchronized List<NormalizedLandmarkList> getMultiHandLandmarks() {
		//Log.v(TAG, "getMultiHandLandmarks " + Thread.currentThread());
        return multiHandLandmarks;
    }
	
	public synchronized void setMultiHandLandmarks(List<NormalizedLandmarkList> value) {
		multiHandLandmarks = value;
	}

    public FrameProcessor getProcessor() {
        return processor;
    }

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    protected FrameProcessor processor;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    protected CameraXPreviewHelper cameraHelper;

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;

    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;

    private Activity activity;


    public HandLandmarks(Activity activity) {
		//Log.v(TAG, "HandLandmarks constructor " + Thread.currentThread());
		//setVRDensity();
        this.activity = activity;
    }

    public void initOnCreate() {
        //previewDisplayView = new SurfaceView(this);
        //setupPreviewDisplayView();

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
		//Log.v(TAG, "initOnCreate " + Thread.currentThread());
        AndroidAssetUtil.initializeNativeAssetManager(activity);
        eglManager = new EglManager(null);
        processor =
                new FrameProcessor(
                        activity,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        null);
    /*processor
        .getVideoSurfaceOutput()
        .setFlipY(
            applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));*/


        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);
    }

    public void initConverter() {
		//Log.v(TAG, "initConverter " + Thread.currentThread());
        converter =
                new ExternalTextureConverter(
                        eglManager.getContext(),
                        NUM_BUFFERS);
        converter.setFlipY(false);
        //applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
        converter.setConsumer(processor);
    }

    public void closeConverter() {
		//Log.v(TAG, "closeConverter constructor " + Thread.currentThread());
        converter.close();
    }
	
	public synchronized boolean getIsHandPresent() {
		return isHandPresent;
	}
	
	public synchronized void setIsHandPresent(boolean value) {
		isHandPresent = value;
	}
	
	public void addHandCoordinateCallback() {
		//Log.v(TAG, "addHandCoordinateCallback " + Thread.currentThread());
		processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
					
					multiHandLandmarks = PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());
					
                    if(multiHandLandmarks.get(0).getLandmarkList().get(0).getZ() < maxHandDistance) {
						isHandPresent = true;
						handGestureCalculator(multiHandLandmarks);
					}
					
					/*Log.v(TAG, "Received multi-hand landmarks packet.");
                    Log.v(
                            TAG,
                            "[TS:"
                                    + packet.getTimestamp()
                                    + "] "
                                    + getMultiHandLandmarksDebugString(multiHandLandmarks));*/
                });
				
		processor.addPacketCallback(
			OUTPUT_HAND_PRESENCE_SCORE, 
			(packet) -> { 
				isGestureBackwardPresent = false;
				isGestureForwardPresent = false;
				isHandPresent = false;
			});
	}
	
	protected String getMultiHandLandmarksDebugString(List<NormalizedLandmarkList> multiHandLandmarks) {
		//Log.v(TAG, "getMultiHandLandmarksDebugString " + Thread.currentThread());
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        String multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size() + "\n";
        int handIndex = 0;
        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr +=
                    "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n";
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr +=
                        "\t\tLandmark ["
                                + landmarkIndex
                                + "]: ("
                                + landmark.getX()
                                + ", "
                                + landmark.getY()
                                + ", "
                                + landmark.getZ()
                                + ")\n";
                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr;
    }


    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture, cameraTargetResolution().getWidth(), cameraTargetResolution().getHeight());
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        //previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return new Size(1280, 960); // No preference and let the camera (helper) decide.
    }

    public void startCamera() {
		//Log.v(TAG, "startCamera " + Thread.currentThread());
        cameraHelper = new CameraXPreviewHelper();
        cameraHelper.setOnCameraStartedListener(
                this::onCameraStarted);
        cameraHelper.startCamera(
                activity, CAMERA_FACING, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
    }

    /*protected Size computeViewSize(int width, int height) {
        return new Size(1280, 960);
    }*/

    /*protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        //boolean isCameraRotated = cameraHelper.isCameraRotated();

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());

        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture, displaySize.getWidth(), displaySize.getHeight());
    }*/

    /*private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                //processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                //processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }*/
	
	private void handGestureCalculator(List<NormalizedLandmarkList> multiHandLandmarks) {
		//Log.v(TAG, "handGestureCalculator " + Thread.currentThread());
        if (multiHandLandmarks.isEmpty()) {
            isGestureBackwardPresent = false;
			isGestureForwardPresent = false;
        }
        boolean thumbIsOpen = false;
        boolean firstFingerIsOpen = false;
        boolean secondFingerIsOpen = false;
        boolean thirdFingerIsOpen = false;
        boolean fourthFingerIsOpen = false;

        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {

            List<NormalizedLandmark> landmarkList = landmarks.getLandmarkList();
            float pseudoFixKeyPoint = 1.0f - landmarkList.get(2).getY();
            if (pseudoFixKeyPoint < 1.0f - landmarkList.get(9).getY()) {
                if (1.0f - landmarkList.get(3).getY() < pseudoFixKeyPoint && 1.0f - landmarkList.get(4).getY() < pseudoFixKeyPoint) {
                    thumbIsOpen = true;
                }
            }
            if (pseudoFixKeyPoint > 1.0f - landmarkList.get(9).getY()) {
                if (1.0f - landmarkList.get(3).getY() > pseudoFixKeyPoint && 1.0f - landmarkList.get(4).getY() > pseudoFixKeyPoint) {
                    thumbIsOpen = true;
                }
            }
            /*Log.d(TAG, "pseudoFixKeyPoint == " + pseudoFixKeyPoint + "\nlandmarkList.get(2).getY() == " + landmarkList.get(2).getY()
                    + "\nlandmarkList.get(4).getY() = " + landmarkList.get(4).getY());*/

            pseudoFixKeyPoint = 1.0f - landmarkList.get(6).getX();
            if (1.0f - landmarkList.get(7).getX() < pseudoFixKeyPoint && 1.0f - landmarkList.get(8).getX() < 1.0f - landmarkList.get(7).getX()) {
                firstFingerIsOpen = true;
            }
            pseudoFixKeyPoint = 1.0f - landmarkList.get(10).getX();
            if (1.0f - landmarkList.get(11).getX() < pseudoFixKeyPoint && 1.0f - landmarkList.get(12).getX() < 1.0f - landmarkList.get(11).getX()) {
                secondFingerIsOpen = true;
            }
            pseudoFixKeyPoint = 1.0f - landmarkList.get(14).getX();
            if (1.0f - landmarkList.get(15).getX() < pseudoFixKeyPoint && 1.0f - landmarkList.get(16).getX() < 1.0f - landmarkList.get(15).getX()) {
                thirdFingerIsOpen = true;
            }
            pseudoFixKeyPoint = 1.0f - landmarkList.get(18).getX();
            if (1.0f - landmarkList.get(19).getX() < pseudoFixKeyPoint && 1.0f - landmarkList.get(20).getX() < 1.0f - landmarkList.get(19).getX()) {
                fourthFingerIsOpen = true;
            }

            // Hand gesture recognition
            /*if (thumbIsOpen && firstFingerIsOpen && secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen) {
                return "FIVE";
            } else if (!thumbIsOpen && firstFingerIsOpen && secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen) {
                return "FOUR";
            } else if (thumbIsOpen && firstFingerIsOpen && secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                return "TREE";
            } else if (thumbIsOpen && firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                return "TWO";
            } else if (!thumbIsOpen && firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                return "ONE";
            } else if (!thumbIsOpen && firstFingerIsOpen && secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                return "YEAH";
            } else if (!thumbIsOpen && firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen && fourthFingerIsOpen) {
                return "ROCK";
            } else if (thumbIsOpen && firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen && fourthFingerIsOpen) {
                return "Spider-Man";
            } else if (!thumbIsOpen && !firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                return "fist";
            } else if (!firstFingerIsOpen && secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen && isThumbNearFirstFinger(landmarkList.get(4), landmarkList.get(8))) {
                return "OK";
            } else {
                String info = "thumbIsOpen " + thumbIsOpen + "firstFingerIsOpen" + firstFingerIsOpen
                        + "secondFingerIsOpen" + secondFingerIsOpen +
                        "thirdFingerIsOpen" + thirdFingerIsOpen + "fourthFingerIsOpen" + fourthFingerIsOpen;
                Log.d(TAG, "handGestureCalculator: == " + info);
                return "___";
            }*/
            if (!thumbIsOpen && firstFingerIsOpen && secondFingerIsOpen && !thirdFingerIsOpen && !fourthFingerIsOpen) {
                isGestureForwardPresent = true;
				isGestureBackwardPresent = false;
			} else if (!firstFingerIsOpen && secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen && isThumbNearFirstFinger(landmarkList.get(4), landmarkList.get(8))) {
                isGestureBackwardPresent = true;
				isGestureForwardPresent = false;
			} else {
				isGestureBackwardPresent = false;
				isGestureForwardPresent = false;
			}
        }
    }

    private boolean isThumbNearFirstFinger(LandmarkProto.NormalizedLandmark point1, LandmarkProto.NormalizedLandmark point2) {
        double distance = getEuclideanDistanceAB(point1.getY(), point1.getX(), point2.getY(), point2.getX());
        return distance < 0.1;
    }

    private double getEuclideanDistanceAB(double a_x, double a_y, double b_x, double b_y) {
        double dist = Math.pow(a_x - b_x, 2) + Math.pow(a_y - b_y, 2);
        return Math.sqrt(dist);
    }
	
	public void onDisable() {
		//Log.v(TAG, "onDisable " + Thread.currentThread());
		eglManager.release();
		converter.close();
		processor.close();
	}
	
	public void stopCamera() {
		cameraHelper.stopCamera();
	}
	
	//Custom additions
	public void setVRDensity() {
		ScreenParamsUtils.xdpi = customDensity;
		ScreenParamsUtils.ydpi = customDensity;
	}
}
