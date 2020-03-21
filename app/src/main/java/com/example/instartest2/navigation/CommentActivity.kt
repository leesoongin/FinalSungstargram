package com.example.instartest2.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instartest2.R
import com.example.instartest2.navigation.model.AlramDTO
import com.example.instartest2.navigation.model.ContentDTO
import com.example.instartest2.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import org.w3c.dom.Comment

class CommentActivity : AppCompatActivity() {
    var contentUid:String?=null
    var destinationUid:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        contentUid=intent.getStringExtra("contentUid") //어떤 게시물을 올린 유저의 uid
        destinationUid=intent.getStringExtra("destinationUid")

        comment_recyclerview.adapter=CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager=LinearLayoutManager(this)

        comment_btn_send.setOnClickListener {
            var comment = ContentDTO.Comment() //comment 객체 생성
            //user의 이메일 ,  uid, 코멘트 ->  comment 객체에 저장
            comment.userId=FirebaseAuth.getInstance().currentUser?.email
            comment.uid=FirebaseAuth.getInstance().currentUser?.uid
            comment.comment=comment_edit_message.text.toString()
            comment.timestamp=System.currentTimeMillis() //현재시간

            //image 컬렉션안의 해당하는 contentuid에 comment 컬렉션을 만들어 거기에 Comment객체를 넣음
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).
                collection("comment").document().set(comment)

            commentAlram(destinationUid!!,comment_edit_message.text.toString())
            Log.d("uid",contentUid)
            comment_edit_message.setText("") //메세지 전송 후 메세지 입력창 초기화화
        }
   }//oncreate

    fun commentAlram(destinationUid:String, message:String){
        var alramDTO=AlramDTO()

        alramDTO.destinationUid=destinationUid
        alramDTO.userId=FirebaseAuth.getInstance().currentUser?.email
        alramDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
        alramDTO.kind=1
        alramDTO.timestamp=System.currentTimeMillis()
        alramDTO.message=message

        FirebaseFirestore.getInstance().collection("alrams").document().set(alramDTO)

        var message=FirebaseAuth.getInstance().currentUser?.email+getString(R.string.alram_comment) +"\n" + alramDTO.message
        FcmPush.instance.sendMessage(destinationUid,"sungstargram",message)
    }

    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comment :ArrayList<ContentDTO.Comment> = arrayListOf() //comment객체 형태로 arraylist 생성

        init{
            FirebaseFirestore.getInstance().collection("images")
                .document(contentUid!!)
                .collection("comment")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comment.clear() //매번 중복되는것을 막기위해서
                    if(querySnapshot == null)return@addSnapshotListener

                    for(snapshot in querySnapshot.documents){
                        comment.add(snapshot.toObject(ContentDTO.Comment::class.java)!!) //
                    }

                    notifyDataSetChanged() // 데이터의 변화ㅏ.. .새로고침?.. 고런 느낌으로다가...
                }//listener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view= LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CostomViewHolder(view)
        }

        private inner class CostomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comment.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view=holder.itemView

            view.commentviewitem_textview_comment.text=comment[position].comment
            view.commentviewitem_textview_profile.text=comment[position].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comment[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url=task.result!!["image"]

                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(view.commentviewitem_imageview_profile)
                    }
                }//listener
        }

    }//inner class
}
