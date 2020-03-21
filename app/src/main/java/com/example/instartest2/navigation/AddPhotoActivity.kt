package com.example.instartest2.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.instartest2.R
import com.example.instartest2.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    var PICK_IMAGE_FROM_ALBUM = 0

    var storage: FirebaseStorage? = null
    var auth: FirebaseAuth?=null
    var firestore:FirebaseFirestore?=null

    var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        //storage 초기화
        storage = FirebaseStorage.getInstance()
        auth=FirebaseAuth.getInstance()
        firestore= FirebaseFirestore.getInstance()

        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    fun contentUpload() {

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_.png"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)//images라는 폴더안에 image넣기

        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()
                val contentDTO = ContentDTO()

                //이미지 주소
                contentDTO.imageUrl = uri.toString()
                //유저의 UID
                contentDTO.uid = auth?.currentUser?.uid
                //게시물의 설명
                contentDTO.explain = addphoto_edit_explain.text.toString()
                //유저의 아이디
                contentDTO.userId = auth?.currentUser?.email
                //게시물 업로드 시간
                contentDTO.timestamp = System.currentTimeMillis()

                //게시물을 데이터를 생성 및 엑티비티 종료
                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)
                finish()

                Log.d("upload","업로드 성공")
            }
        }
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){//이미지 선택시
            if(resultCode == Activity.RESULT_OK){//이미지 뷰에 세팅
                photoUri=data?.data
                addphoto_image.setImageURI(photoUri)
            }else{//취소
                finish()
            }
        }//if
    }//overrid fun
}



