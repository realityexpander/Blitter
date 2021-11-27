package com.realityexpander.blitter.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.realityexpander.blitter.R
import com.realityexpander.blitter.databinding.FragmentHomeBinding
import com.realityexpander.blitter.databinding.FragmentSearchBinding

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : BlitterFragment() {

    private lateinit var bind: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentSearchBinding.inflate(inflater, container, false);
        return bind.root;
    }

}