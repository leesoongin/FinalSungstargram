package com.example.instartest2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
//이메일 패스워드 id email .. password _editText

    var auth: FirebaseAuth?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth=FirebaseAuth.getInstance()

        email_loginBtn.setOnClickListener {
            signinAndsignup() //이메일 / 계정생성 또는 로그인
        }
    }
//자동로그인 부분
    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }
    fun signinAndsignup(){
        auth?.createUserWithEmailAndPassword(email_editText.text.toString(),password_editText.text.toString())
            ?.addOnCompleteListener {
                task->
                    if(task.isSuccessful){//성공적으로 계정이 만들어졌다
                        moveMainPage(task.result?.user)
                        Log.d("Login","계정 생성")
                    }else{
                        signinEmail()
                    }
            }//complete
    }//fun

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_editText.text.toString(),password_editText.text.toString())
            ?.addOnCompleteListener{
                task->
                    if(task.isSuccessful){//로그인 성공시
                        moveMainPage(task.result?.user)
                        Log.d("Login","로그인성공")
                    }else{//로그인 실패시
                       Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                    }
            }//complete
    }//fun

    fun moveMainPage(user:FirebaseUser?){
        if(user != null){//user상태가 null이 아니라면,
            startActivity(Intent(this,MainActivity::class.java))  //메인화면으로 전환하기
            finish()
        }
    }//fun
}
