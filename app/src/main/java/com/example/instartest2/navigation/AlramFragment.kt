package com.example.instartest2.navigation

import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.example.instartest2.R
import com.example.instartest2.navigation.model.AlramDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alram.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.*
import kotlin.collections.ArrayList

class AlramFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alram, container, false)

        view.alramfragment_recyclerview.adapter=AlramRecyclerviewAdapter()
        view.alramfragment_recyclerview.layoutManager=LinearLayoutManager(activity)

        Log.d("alram","알람들어옴")
        return view
    }

    inner class AlramRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var alramDTOList: ArrayList<AlramDTO> = arrayListOf()

        init{
            var uid=FirebaseAuth.getInstance().currentUser?.uid //현재 나의 uid를 받아와서.

            alramDTOList.clear()
            // alram컬렉션에 있는 destinationUid필드의 값과 나의 uid 값이 일치하는것이 있다면 alramDTOList에 넣는다
            FirebaseFirestore.getInstance().collection("alrams").whereEqualTo("destinationUid",uid)
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    alramDTOList.add(snapshot.toObject(AlramDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }//snapshot
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)


        override fun getItemCount(): Int {
            return alramDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView

            //상대방의 프로필을 불러와 화면에 띄우기
           FirebaseFirestore.getInstance().collection("profileImages").document(alramDTOList[position].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]

                    Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                }
            }

            when (alramDTOList[position].kind) { //어떤 알람의 종류인지
                0 -> { // 0은 좋아요 알람
                    val str_0 = alramDTOList[position].userId + " " + getString(R.string.alram_favorite)
                    view.commentviewitem_textview_profile.text=str_0 // item comment fragment를 재활용
                }//0
                1 -> { // 1은 댓글 알람
                    val str_0 = alramDTOList[position].userId + " " +getString(R.string.alram_comment) + " \n "+ "'" +alramDTOList[position].message +"'"
                    view.commentviewitem_textview_profile.text=str_0 //
                }//1
                2 -> { // 2는 팔로우 알람
                    val str_0 = alramDTOList[position].userId + " " + getString(R.string.alram_follow)
                    view.commentviewitem_textview_profile.text=str_0
                }//2
            }//when
        }//bind

    }
}