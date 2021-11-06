using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Android;
//using com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
//using com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;

public class HandCoordinates : MonoBehaviour
{
    private AndroidJavaObject handLandmarksObject;
    private AndroidJavaObject activity;
	private AndroidJavaObject handLandmarks;
	public GameObject handPrefab;
	GameObject[] handPoints;
	private float thrust = 2.0f;
	private float zeroCoordinate = 1.0f;
	//private Camera cam;
	// Start is called before the first frame update

	void Start()
    {
		handPoints = new GameObject[21];
		handPrefab = Instantiate(handPrefab, new Vector3(0, 0, 0), Quaternion.identity);
		handPoints = handPrefab.GetComponent<Lines>().getArrayOfShperes();
		//handPrefab.gameObject.SetActive(false);
		//unityMainThreadDispather = Instantiate(unityMainThreadDispather);
		var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		activity = player.GetStatic<AndroidJavaObject>("currentActivity");
		handLandmarksObject = new AndroidJavaObject("com.example.landmarks.HandLandmarks", activity);
		handLandmarksObject.Call("initOnCreate");
		handLandmarksObject.Call("addHandCoordinateCallback");
		handLandmarksObject.Call("initConverter");
		if (!Permission.HasUserAuthorizedPermission(Permission.Camera))
		{
			Permission.RequestUserPermission(Permission.Camera);
		}
		else
		{
			handLandmarksObject.Call("startCamera");
		}

	}


    // Update is called once per frame
    void Update()
    {
		
		//handSphere.transform.position = Camera.main.ScreenToWorldPoint(new Vector3(0.7f*Camera.main.pixelWidth, 0.7f*Camera.main.pixelHeight, 2f));
		//handLandmarks = new List<AndroidJavaClass>();
		//handLandmarks = new AndroidJavaObject("com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList");
        
		if(handLandmarksObject.Call<bool>("getIsHandPresent")) {
			handLandmarks = handLandmarksObject.Call<AndroidJavaObject>("getMultiHandLandmarks");
			if (!handPrefab.gameObject.activeSelf)
			{
				handPrefab.gameObject.SetActive(true);
			}
			//var landmarkListInstance = new AndroidJavaObject();
			//var landmark = new AndroidJavaObject();
			//var landmarkList = new AndroidJavaObject();
			var landmarkListInstance = handLandmarks.Call<AndroidJavaObject>("get", 0);
			var landmarkList = landmarkListInstance.Call<AndroidJavaObject>("getLandmarkList");
			//Debug.Log("Got a hand boi");
			//var zeroCoordinate;
			

			var landmark = landmarkList.Call<AndroidJavaObject>("get", 0);
			handPoints[0].transform.position = Camera.main.ScreenToWorldPoint(new Vector3((1.0f - landmark.Call<float>("getY")) * Camera.main.pixelWidth, landmark.Call<float>("getX") * Camera.main.pixelHeight, zeroCoordinate));

			for (int i = 1; i < 21; i++) {
				landmark = landmarkList.Call<AndroidJavaObject>("get", i);
				handPoints[i].transform.position = Camera.main.ScreenToWorldPoint(new Vector3((1.0f - landmark.Call<float>("getY"))*Camera.main.pixelWidth, landmark.Call<float>("getX")*Camera.main.pixelHeight, landmark.Call<float>("getZ") + zeroCoordinate));
						
				/*Debug.Log(i + " " + 
						landmark.Call<float>("getX") + " " +
						landmark.Call<float>("getY") + " " +
						landmark.Call<float>("getZ"));*/
			}
			if(handLandmarksObject.Call<bool>("getisGestureForwardPresent"))
            {
				transform.position += Camera.main.transform.forward * thrust * Time.deltaTime;
            }
			if (handLandmarksObject.Call<bool>("getisGestureBackwardPresent"))
			{
				transform.position -= Camera.main.transform.forward * thrust * Time.deltaTime;
			}
			//handLandmarksObject.Call("setMultiHandLandmarks", null);
		} else {
			if (handPrefab.gameObject.activeSelf)
			{
				handPrefab.gameObject.SetActive(false);
			}
		}
		//callOnDisableOnMainThread();
	}

    void OnApplicationPause(bool pauseStatus)
    {
        if (pauseStatus) {

            //android onPause()
            handLandmarksObject.Call("closeConverter");
            
        } else {
            //android onResume()
            handLandmarksObject.Call("initConverter");
            if (!Permission.HasUserAuthorizedPermission(Permission.Camera))
            {
                handLandmarksObject.Call("startCamera");
            }
        }

    }
	


	/*private void OnDestroy()
	{
		
		//Destroy(this.gameObject);

	}

	private void stopCamera()
    {

		activity.Call("runOnUiThread", new AndroidJavaRunnable(runOnUiThread));
	}

	void runOnUiThread()
	{
		handLandmarksObject.Call("stopCamera");
		Debug.Log("I'm running on the Java UI thread!");
	}*/

	
}
