package cn.cercis.ui.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.cercis.R
import cn.cercis.databinding.FragmentEmptyBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmptyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (resources.getBoolean(R.bool.is_master_detail)) {
            val binding = FragmentEmptyBinding.inflate(inflater, container, false)
            return binding.root
        }
        return null
    }
}
