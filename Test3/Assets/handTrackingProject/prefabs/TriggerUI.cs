using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System;

public class TriggerUI : MonoBehaviour
{
    private GameObject gameObjectToFindUi;
    private TMP_Text address;
    
    void Start()
    {
        gameObjectToFindUi = GameObject.FindGameObjectWithTag("HandUiText");
        if (gameObjectToFindUi == null) enabled = false;
        address = gameObjectToFindUi.GetComponent<TMP_Text>();
        address.gameObject.SetActive(false);
    }
    // Start is called before the first frame update
    private void OnTriggerEnter(Collider other)
    {
        address.gameObject.SetActive(true);
        address.text = "Greetings to all. I'm probably giving a presenation right now, so wish me luck!";
    }

    private void OnTriggerExit(Collider other)
    {
        address.gameObject.SetActive(false);
    }
}
