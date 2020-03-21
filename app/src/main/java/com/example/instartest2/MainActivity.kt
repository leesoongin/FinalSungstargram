package com.example.instartest2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.instartest2.navigation.*
import com.example.instartest2.navigation.util.FcmPush
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity() , BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        /* setToolbarDefault를 여기다 하는 이유는 계속해서 보이고 사라지고를 반복해야하기떄문
        onCreate에서 호출하게되면 처음 생성될때만 적용되고 반복해서 액티비티를 전환할때에는 뷰가 망가짐*/
        setToolbarDefault()

       when(p0.itemId){
           R.id.action_home->{//main page
              var detailViewFragment=DetailViewFragment()
               supportFragmentManager.beginTransaction().replace(R.id.main_content,detailViewFragment).commit()
               return true
           }
           R.id.action_search->{//grid
               var gridFragment=GridFragment()
               supportFragmentManager.beginTransaction().replace(R.id.main_content,gridFragment).commit()
               return true
           }
           R.id.action_account->{ //user
               var userFragment=UserFragment()
               var bundle=Bundle()
               var uid= FirebaseAuth.getInstance().currentUser?.uid

               bundle.putString("destinationUid",uid)

               userFragment.arguments=bundle

               supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
               return true
           }
           R.id.action_favorite_alram->{
               var alramFragment=AlramFragment()
               supportFragmentManager.beginTransaction().replace(R.id.main_content,alramFragment).commit()
               return true
           }
           R.id.action_add_a_photo->{
               if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                   == PackageManager.PERMISSION_GRANTED) {
                   startActivity(Intent(this, AddPhotoActivity::class.java))
               } else {
                   Toast.makeText(this, "스토리지 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
               }
               return true
           }
       }//when
        return false //아무것도 걸리지 않았다면 false return
    }
    fun setToolbarDefault(){
        toolbar_username.visibility= View.GONE
        toolbar_btn_back.visibility=View.GONE
        toolbar.toolbar_title_image.visibility=View.VISIBLE
    }

    fun registPushToken(){
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            val token = task.result?.token
            val uid=FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String,Any>()

            map["pushToken"]=token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener(this)

        //사진 접근할수있는 권한 받아오기
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)

        //set default screen <- 메인화면
        bottom_navigation.selectedItemId=R.id.action_home

        registPushToken()
    }

   /* override fun onStop() {
        // 푸시메세지가 정상적으로 잘 작동하는지 테스트해보려고 ..  앱 일시정지시 title -> hi body -> bye
        super.onStop()
        FcmPush.instance.sendMessage("OlN4HMYVMzS9k4eg7mGmIMYftHL2","hi","bye")
    }*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            var imageUri=data?.data
            var uid=FirebaseAuth.getInstance().currentUser?.uid
            var storageRef=FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

            storageRef.putFile(imageUri!!).continueWithTask {task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map=HashMap<String,Any>()

                map["image"]=uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }//if

    }
}
