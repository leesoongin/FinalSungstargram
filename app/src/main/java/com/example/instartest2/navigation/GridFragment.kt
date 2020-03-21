package com.example.instartest2.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instartest2.R
import com.example.instartest2.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import kotlinx.android.synthetic.main.fragment_grid.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class GridFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var fragmentView: View? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_grid, container, false)

        firestore= FirebaseFirestore.getInstance()

        fragmentView?.gridfragment_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()//adapter setting
        fragmentView?.gridfragment_recyclerview?.layoutManager=GridLayoutManager(activity,3)//gridlayout설정

        return fragmentView
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }//for

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