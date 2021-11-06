using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Lines : MonoBehaviour
{
    private Transform fingers;
    private LineRenderer fingerLine;
    private LineRenderer palm;
    // Start is called before the first frame update
    void Start()
    {
		fingerLine = new LineRenderer();
        fingers = gameObject.transform.GetChild(0);
        palm = gameObject.transform.GetChild(1).GetComponent<LineRenderer>();
        
    }

    // Update is called once per frame
    void Update()
    {
        
       
    }

    private void lineAnimation()
    {
        int j = 0;
        foreach (Transform finger in fingers)
        {
            palm.SetPosition(j, finger.GetChild(0).position);
            j++;
            int i = 0;
            foreach (Transform child in finger)
            {
                fingerLine = finger.GetComponent<LineRenderer>();
                fingerLine.SetPosition(i, child.position);
                i++;
            }
        }
        palm.SetPosition(j, fingers.GetChild(0).GetChild(0).position);
    }


    private void OnEnable()
    {
        Application.onBeforeRender += lineAnimation;
    }

    private void OnDisable()
    {
        Application.onBeforeRender -= lineAnimation;
    }


    public GameObject[] getArrayOfShperes ()
    {
        fingers = gameObject.transform.GetChild(0);
        GameObject[] spheres = new GameObject[21];
        int j = 0;
        foreach (Transform finger in fingers)
        {
            foreach (Transform child in finger)
            {
                spheres[j] = child.gameObject;
                j++;
            }
        }
        return spheres;
    }

   
}
