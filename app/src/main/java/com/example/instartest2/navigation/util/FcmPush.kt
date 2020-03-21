package com.example.instartest2.navigation.util

import com.example.instartest2.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class FcmPush{
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url="https://fcm.googleapis.com/fcm/send"
    var serverKey= "AIzaSyD5NkKekbDiahdHAminuRh9ofE3_2IXqRU"
    var gson: Gson?=null
    var okHttpClient:OkHttpClient?=null

    companion object{
        var instance=FcmPush()
    }

    init{
        gson=Gson()
        okHttpClient= OkHttpClient()
    }

    fun sendMessage(destinationUid:String, title: String , message:String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener { task ->
            if(task.isSuccessful){
                var token=task?.result?.get("pushToken").toString() // 해당하는 상대방 uid에서 상대방의 토큰을 가지고온다

                var pushDTO=PushDTO()

                pushDTO.to=token
                pushDTO.notification.title=title
                pushDTO.notification.body=message

                var body =RequestBody.create(JSON,gson?.toJson(pushDTO)) //body를 만드는 requestBody, pushDTO를 json형식으로 전환해서 json형식의 body가 만들어진다
                var request=Request.Builder()
                    .addHeader("Content-Type","application/json") //json타입으로
                    .addHeader("Authorization","key="+serverKey) //서버키
                    .url(url) //보낼 서버 (구글의 서버인듯..?)
                    .post(body) // post 형식으로
                    .build() // 닫

                okHttpClient?.newCall(request)?.enqueue(object :Callback{
                    override fun onFailure(call: Call?, e: IOException?) {

                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }
}