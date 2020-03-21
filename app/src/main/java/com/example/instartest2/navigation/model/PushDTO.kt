package com.example.instartest2.navigation.model


data class PushDTO(
    var to:String?=null,//push를 받는사람의 토큰 id
    var notification: Notification = Notification()
){
    //body 는 푸시메세지의 주 내용 title은 제목
    data class Notification(
        var body:String?=null,
        var title:String?=null
    )
}