package com.example.instartest2.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instartest2.R
import com.example.instartest2.navigation.model.AlramDTO
import com.example.instartest2.navigation.model.ContentDTO
import com.example.instartest2.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment :Fragment(){
    var firestore:FirebaseFirestore?=null
    var uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view=LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)

        firestore= FirebaseFirestore.getInstance()


        view.detailviewfragment_recyclerview.adapter=DetailviewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager= LinearLayoutManager(activity)//recyclerview 를 linearlayout으로..?

        return view
    }
    inner class DetailviewRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs:ArrayList<ContentDTO> = arrayListOf()
        var contentUidList:ArrayList<String> = arrayListOf()

        init{                                                       //시간순서대로
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null)return@addSnapshotListener //로그아웃시 앱 종료 방지 코드

                for(snapshot in querySnapshot!!.documents){
                    var item=snapshot.toObject(ContentDTO::class.java)

                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }//for
                notifyDataSetChanged() //값이 새로고침 되도록.
            }//add
        }//init

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
          var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)

            return CostomViewHolder(view)
        }

        inner class CostomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
          return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder=(holder as CostomViewHolder).itemView

            //user id
            viewHolder.detailviewitem_profile_textview.text=contentDTOs[position].userId

            //image
            Log.d("@@1",contentDTOs[position].imageUrl)
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitem_imageview_content)

            //explain of content
            viewHolder.detailviewitem_explain_textview.text=contentDTOs[position].explain

            //favorite count
            viewHolder.detailviewitem_favoritecounter_textview.text="Likes "+contentDTOs!![position].favoriteCount

            //profile image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitem_profile_image)

            viewHolder.detailviewitem_favorite_imageview.setOnClickListener { //좋아요 이미지에 이벤트리스너 달기
                favoriteEvent(position)//좋아요 카운트, 좋아요 한 계정정보 관리
            }

            viewHolder.detailviewitem_profile_image.setOnClickListener{
                var fragment=UserFragment()
                var bundle=Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)

                fragment.arguments=bundle

                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
                //uid는 게시물에 해당하는 계정의 uid 정보를 담음 / uid는 계속 변해서 생성될수있는반면 currentUid는 현재 나의 uid이므로 항상 고정되있음 (로그인시)
            }

            viewHolder.detailviewitem_comment_imageview.setOnClickListener {//댓글달기 이미지 이벤트 리스너
                view ->
                var intent= Intent(view.context,CommentActivity::class.java)

                //유저의 uid list에서 해당하는 순서의 게시물에 저장된 uid를 뽑아 intent로 값을 넘김
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)

                startActivity(intent)//comment activity 로 화면 전환
            }
            if(contentDTOs[position].favorites.containsKey(uid)){ //내 uid가 map에 포함되어있다면 진한하트
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{//내 uid가 map에 포함되어있지않다면 빈 하트
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        fun favoriteEvent(position:Int) {//좋아요 카운트, 좋아요 한 계정정보 관리
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {//이미 좋아요가 눌려있다면
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1 //좋아요 하나 없애주고
                    contentDTO.favorites.remove(uid) //좋아요를 누른 계정들을 저장하는 map에서 uid를 삭제
                } else {//좋아요가 눌려있지 않다면
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1 //좋아요 카운트하나증가
                    contentDTO.favorites[uid!!] = true //좋아요 누른계정map에 추가가 // //            }

                    favoriteAlram(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc,contentDTO)
            }//transaction
        }//fun

        fun favoriteAlram(destinationUid:String){
            var alramDTO= AlramDTO()
            alramDTO.destinationUid=destinationUid
            alramDTO.userId=FirebaseAuth.getInstance().currentUser?.email
            alramDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
            alramDTO.kind=0
            alramDTO.timestamp=System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alrams").document().set(alramDTO)

            var message=FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alram_favorite)

            FcmPush.instance.sendMessage(destinationUid,"sungstargram",message)//내가 좋아요 누른 당사자에게 푸시 메세지 보냄
        }
    }//recyclerView
}