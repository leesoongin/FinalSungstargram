package com.example.instartest2.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instartest2.LoginActivity
import com.example.instartest2.MainActivity
import com.example.instartest2.R
import com.example.instartest2.navigation.model.AlramDTO
import com.example.instartest2.navigation.model.ContentDTO
import com.example.instartest2.navigation.model.FollowDTO
import com.example.instartest2.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*


class UserFragment :Fragment(){
    var fragmentView:View?=null
    var firestore: FirebaseFirestore?=null
    var auth: FirebaseAuth?=null
    var uid:String?=null
    var currentUserId:String?=null

    companion object{//static 변수라 생각하면 됨됨
       var PICK_PROFILE_FROM_ALBUM=10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView=LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)

        uid=arguments!!.getString("destinationUid")
        firestore=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        currentUserId=auth?.currentUser?.uid

        if(uid == currentUserId){//객체가 생성될 때 이게 내 uid와 같은지 확인 같다면 내 창 띄우고
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.signout)

            fragmentView?.account_iv_profile?.setOnClickListener{//프로필사진 등록 클릭 이벤트
                var photoPickerIntent=Intent(Intent.ACTION_PICK)
                photoPickerIntent.type="image/*"
                activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)//여긴 activity니까
            }

            fragmentView?.account_btn_follow_signout?.setOnClickListener {//로그아웃버튼의 이벤트 리스너
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()//auth 로그아웃
            }
        }else{//다르면 그 유저의 창 띄우기
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)

            var mainActivity=(activity as MainActivity)
            mainActivity?.toolbar_username.text=arguments?.getString("userId")//유저의 email

            mainActivity?.toolbar_btn_back.setOnClickListener { //백버튼 누르면 기본화면으로 전환
                mainActivity?.bottom_navigation.selectedItemId=R.id.action_home
            }

            fragmentView?.account_btn_follow_signout?.setOnClickListener{
                requestFollow()
            }
            mainActivity?.toolbar_username?.visibility=View.VISIBLE
            mainActivity?.toolbar_btn_back?.visibility=View.VISIBLE
            mainActivity?.toolbar_title_image.visibility=View.GONE //gone 숨기다 invisible이랑 살짝다름 차이는 검색해보셈
        }

        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity!!,3)//한 행당 3

        getProfileImage()
        getFollowAndgetFollowing()

        return fragmentView
    }

    fun getFollowAndgetFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null ) return@addSnapshotListener

            var followDTO=documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text=followDTO?.followingCount.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text=followDTO?.followerCount.toString()

                if(followDTO?.followers?.containsKey(currentUserId!!)){//내가 팔로우중이라면
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                }else{ //내가 팔로우중이 아니라면
                    if (uid != currentUserId) {//내 유저 화면이 아니라면
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    }//inner if
                }//else
            }//if
        }//snapShot
    }

    fun requestFollow(){
        //user컬렉션에서 currentUserId를 가지고온다
        var tsDocFollowing=firestore?.collection("users")?.document(currentUserId!!)

        firestore?.runTransaction { transaction -> //내가 상대방을 팔로우할때 나의 팔로잉 수 카운트
            var followDTO=transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            if(followDTO == null ){
                Log.d("팔로잉","카운트+")
                followDTO=FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.followings[uid!!]=true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }//if

            if(followDTO.followings.containsKey(uid)){ //만약 이미 팔로우한사람이 버튼을 눌렀다면  -> 팔로우취소
                followDTO.followingCount=followDTO.followingCount-1
                followDTO.followings.remove(uid)
                Log.d("팔로잉","카운트-")
            }else{//
                followDTO.followingCount=followDTO.followingCount+1
                followDTO.followings[uid!!]=true
                Log.d("팔로잉","카운트+")
            }//else

            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }//transaction

        //user컬렉션에서 uid를 가지고온다
        var tsDocFollower=firestore?.collection("users")?.document(uid!!)

        firestore?.runTransaction { transaction -> //내가 누군가를 팔로우했을때 , 그 유저의 팔로워 카운트
            var followDTO=transaction.get(tsDocFollower!!)?.toObject(FollowDTO::class.java)

            if(followDTO == null){ //하나도 없다면
                Log.d("팔로워","팔로워+")
                followDTO=FollowDTO()
                followDTO!!.followerCount=1
                followDTO!!.followers[currentUserId!!]=true
                followerAlram(uid!!)

                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserId)){
                followDTO!!.followerCount =followDTO!!.followerCount -1
                followDTO!!.followers.remove(currentUserId)
                Log.d("팔로워","팔로워-")
            }else{
                followDTO!!.followerCount =followDTO!!.followerCount +1
                followDTO!!.followers[currentUserId!!]=true
                followerAlram(uid!!)
                Log.d("팔로워","팔로워+")
            }

            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }//transaction

    }//requestFollow fun

    fun followerAlram(destinationUid:String){
        var alramDTO=AlramDTO()

        alramDTO.destinationUid=destinationUid
        alramDTO.userId=FirebaseAuth.getInstance().currentUser?.email
        alramDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
        alramDTO.kind=2
        alramDTO.timestamp=System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alrams").document().set(alramDTO)

        var message = auth?.currentUser?.email+getString(R.string.alram_follow)
        FcmPush.instance.sendMessage(destinationUid,"sungstargram",message)
    }

    fun getProfileImage(){ //해당하는 uid의 프로필 이미지를 불러옴  onCreate에 선언해 user page가 뜰때마다
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null){
                return@addSnapshotListener
            }
            if(documentSnapshot.data != null){
                var url=documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }//fun

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs:ArrayList<ContentDTO> = arrayListOf()

       init{
           firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

               if(querySnapshot == null) return@addSnapshotListener

               for(snapshot in querySnapshot.documents){
                   contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
               }//for

               fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
               notifyDataSetChanged() //데이터 업데이트시 새로고침 자동
           }//add
       }//init

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView

            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageview)

            /*프로필 임시로 넣어봄
            Glide.with(holder.itemView.context)
                .load(contentDTOs[0].imageUrl)
                .into(fragmentView!!.account_iv_profile)*/
        }

    }
}