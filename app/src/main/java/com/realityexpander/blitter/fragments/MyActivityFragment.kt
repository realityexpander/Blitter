package com.realityexpander.blitter.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.realityexpander.blitter.R
import com.realityexpander.blitter.databinding.FragmentMyActivityBinding

/**
 * A simple [Fragment] subclass.
 * Use the [MyActivityFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyActivityFragment : BlitterFragment() {

    private lateinit var bind: FragmentMyActivityBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentMyActivityBinding.inflate(inflater, container, false);
        return bind.root;

    }

    override fun updateUI() {
        //TODO("Not yet implemented")
    }

}